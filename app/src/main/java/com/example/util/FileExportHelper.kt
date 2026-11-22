package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

enum class PdfPagePreset(val label: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 Standard", 595, 842),
    LETTER("US Letter", 612, 792),
    FIT("Fit Image", 0, 0)
}

data class PdfExportResult(
    val file: File,
    val pageCount: Int,
    val sizeBytes: Long,
    val savedToDownloads: Boolean,
    val displayName: String
)

data class CompressedImageResult(
    val file: File,
    val originalBytes: Long,
    val compressedBytes: Long,
    val width: Int,
    val height: Int,
    val qualityUsed: Int,
    val bitmap: Bitmap
)

object FileExportHelper {

    fun queryDisplaySizeBytes(context: Context, uri: Uri): Long {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0) cursor.getLong(idx) else 0L
                } else 0L
            } ?: 0L
        }.getOrDefault(0L)
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 KB"
        return when {
            bytes < 1024L -> "$bytes B"
            bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }

    suspend fun createPdfFromImages(
        context: Context,
        uris: List<Uri>,
        preset: PdfPagePreset,
        titlePrefix: String = "NexaKit_Doc"
    ): PdfExportResult = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            uris.forEachIndexed { index, uri ->
                val bmp = decodeSampledOrientedBitmap(context, uri, maxDimension = 1800)
                    ?: return@forEachIndexed
                try {
                    val pageWidth: Int
                    val pageHeight: Int
                    if (preset == PdfPagePreset.FIT) {
                        val maxSide = 1200f
                        val scale = min(maxSide / bmp.width, maxSide / bmp.height).coerceAtMost(1f)
                        pageWidth = (bmp.width * scale).roundToInt().coerceAtLeast(200)
                        pageHeight = (bmp.height * scale).roundToInt().coerceAtLeast(200)
                    } else {
                        pageWidth = preset.widthPt
                        pageHeight = preset.heightPt
                    }

                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                    val page = document.startPage(pageInfo)
                    val canvas: Canvas = page.canvas
                    canvas.drawColor(Color.WHITE)

                    val margin = if (preset == PdfPagePreset.FIT) 0f else 24f
                    val availW = (pageWidth - margin * 2).coerceAtLeast(10f)
                    val availH = (pageHeight - margin * 2).coerceAtLeast(10f)
                    val scale = min(availW / bmp.width, availH / bmp.height)
                    val drawW = bmp.width * scale
                    val drawH = bmp.height * scale
                    val left = (pageWidth - drawW) / 2f
                    val top = (pageHeight - drawH) / 2f
                    canvas.drawBitmap(bmp, null, RectF(left, top, left + drawW, top + drawH), paint)
                    document.finishPage(page)
                } finally {
                    bmp.recycle()
                }
            }

            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "${titlePrefix}_$stamp.pdf"
            val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val outFile = File(outDir, fileName)
            FileOutputStream(outFile).use { fos ->
                document.writeTo(fos)
            }

            val savedToDownloads = savePdfToDownloadsIfPossible(context, outFile, fileName)
            PdfExportResult(
                file = outFile,
                pageCount = document.pages.size,
                sizeBytes = outFile.length(),
                savedToDownloads = savedToDownloads,
                displayName = fileName
            )
        } finally {
            document.close()
        }
    }

    suspend fun compressImageToTarget(
        context: Context,
        uri: Uri,
        targetKb: Int,
        maxDimension: Int = 1600
    ): CompressedImageResult = withContext(Dispatchers.IO) {
        val rawOriginalBytes = queryDisplaySizeBytes(context, uri)
        val originalBitmap = decodeSampledOrientedBitmap(context, uri, maxDimension = maxDimension)
            ?: throw IllegalStateException("Could not decode selected image")

        val targetBytes = targetKb.coerceAtLeast(15) * 1024
        var currentBitmap = originalBitmap
        var bestBytes: ByteArray
        var bestQuality = 85

        // Try quality sweep first
        var low = 12
        var high = 95
        bestBytes = compressJpeg(currentBitmap, 80)

        if (bestBytes.size > targetBytes) {
            while (low <= high) {
                val mid = (low + high) / 2
                val attempt = compressJpeg(currentBitmap, mid)
                if (attempt.size <= targetBytes) {
                    bestBytes = attempt
                    bestQuality = mid
                    low = mid + 1
                } else {
                    high = mid - 1
                }
            }
            // If even lowest quality is larger than target, downscale dimensions progressively
            var scaleFactor = 0.8f
            while (bestBytes.size > targetBytes && currentBitmap.width > 320 && currentBitmap.height > 320) {
                val newW = (currentBitmap.width * scaleFactor).roundToInt().coerceAtLeast(240)
                val newH = (currentBitmap.height * scaleFactor).roundToInt().coerceAtLeast(240)
                val scaled = Bitmap.createScaledBitmap(currentBitmap, newW, newH, true)
                if (currentBitmap !== originalBitmap) {
                    currentBitmap.recycle()
                }
                currentBitmap = scaled
                val attempt = compressJpeg(currentBitmap, 60)
                bestBytes = attempt
                bestQuality = 60
                if (bestBytes.size <= targetBytes) {
                    // Fine tune upwards
                    for (q in 65..85 step 5) {
                        val higher = compressJpeg(currentBitmap, q)
                        if (higher.size <= targetBytes) {
                            bestBytes = higher
                            bestQuality = q
                        } else break
                    }
                    break
                }
            }
        } else {
            // Even quality 80 is below target -> try raising up to 92
            for (q in 85..92 step 3) {
                val attempt = compressJpeg(currentBitmap, q)
                if (attempt.size <= targetBytes) {
                    bestBytes = attempt
                    bestQuality = q
                }
            }
        }

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outDir = File(context.cacheDir, "compressed").apply { mkdirs() }
        val outFile = File(outDir, "NexaKit_Compressed_$stamp.jpg")
        FileOutputStream(outFile).use { it.write(bestBytes) }

        val previewBitmap = BitmapFactory.decodeByteArray(bestBytes, 0, bestBytes.size)
            ?: currentBitmap
        if (currentBitmap !== originalBitmap && currentBitmap !== previewBitmap) {
            currentBitmap.recycle()
        }
        if (originalBitmap !== previewBitmap) {
            originalBitmap.recycle()
        }

        val effectiveOriginal = if (rawOriginalBytes > 0L) rawOriginalBytes else bestBytes.size.toLong()
        CompressedImageResult(
            file = outFile,
            originalBytes = effectiveOriginal,
            compressedBytes = outFile.length(),
            width = previewBitmap.width,
            height = previewBitmap.height,
            qualityUsed = bestQuality,
            bitmap = previewBitmap
        )
    }

    fun saveImageFileToGallery(context: Context, sourceFile: File, displayName: String): Boolean {
        return runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NexaKit")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { it.copyTo(out) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        }.getOrDefault(false)
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, displayName: String): Boolean {
        return runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NexaKit")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        }.getOrDefault(false)
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun openFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Open with"))
        }
    }

    private fun savePdfToDownloadsIfPossible(context: Context, file: File, displayName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/NexaKit")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        }.getOrDefault(false)
    }

    private fun compressJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(5, 100), baos)
        return baos.toByteArray()
    }

    private fun decodeSampledOrientedBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int
    ): Bitmap? {
        val resolver = context.contentResolver
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }
        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

        var sampleSize = 1
        while (boundsOpts.outWidth / sampleSize > maxDimension || boundsOpts.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return null

        val orientation = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        return if (degrees != 0f) {
            val matrix = Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (rotated !== decoded) decoded.recycle()
            rotated
        } else {
            decoded
        }
    }
}
