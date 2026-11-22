package com.example

import com.example.ads.AdMobConfig
import com.example.util.FileExportHelper
import com.example.util.QrHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NexaKitCoreTest {

    @Test
    fun admob_config_has_valid_ids() {
        assertTrue(AdMobConfig.BANNER_AD_UNIT_ID.startsWith("ca-app-pub-"))
        assertTrue(AdMobConfig.INTERSTITIAL_AD_UNIT_ID.startsWith("ca-app-pub-"))
        assertTrue(AdMobConfig.REWARDED_AD_UNIT_ID.startsWith("ca-app-pub-"))
    }

    @Test
    fun format_bytes_outputs_readable_units() {
        assertEquals("0 KB", FileExportHelper.formatBytes(0))
        assertEquals("512 B", FileExportHelper.formatBytes(512))
        assertEquals("10.0 KB", FileExportHelper.formatBytes(10 * 1024L))
        assertEquals("1.50 MB", FileExportHelper.formatBytes((1.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun qr_helper_generates_bitmap_for_valid_content() {
        val bmp = QrHelper.generateQrBitmap("https://nexakit.app", sizePx = 256)
        assertNotNull(bmp)
        assertEquals(256, bmp!!.width)
        assertEquals(256, bmp.height)
    }
}
