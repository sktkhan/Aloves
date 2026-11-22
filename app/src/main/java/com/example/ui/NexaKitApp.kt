package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ads.AdManager
import com.example.ads.AdMobConsoleSheet
import com.example.ads.AnchoredAdaptiveBannerAd
import com.example.ui.screens.FinanceToolsScreen
import com.example.ui.screens.HealthAndSensorsScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.NexaToolRoute
import com.example.ui.screens.PdfMakerScreen
import com.example.ui.screens.PhotoCompressorScreen
import com.example.ui.screens.QrStudioScreen
import com.example.ui.screens.UnitConverterScreen
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldAccent

private data class TopNavItem(
    val route: NexaToolRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val NAV_ITEMS = listOf(
    TopNavItem(
        route = NexaToolRoute.HOME,
        label = "Home",
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home"
    ),
    TopNavItem(
        route = NexaToolRoute.PDF_MAKER,
        label = "PDF",
        selectedIcon = Icons.Rounded.PictureAsPdf,
        unselectedIcon = Icons.Outlined.PictureAsPdf,
        testTag = "nav_pdf"
    ),
    TopNavItem(
        route = NexaToolRoute.COMPRESSOR,
        label = "Shrink",
        selectedIcon = Icons.Rounded.Compress,
        unselectedIcon = Icons.Outlined.Compress,
        testTag = "nav_compress"
    ),
    TopNavItem(
        route = NexaToolRoute.QR_STUDIO,
        label = "QR",
        selectedIcon = Icons.Rounded.QrCodeScanner,
        unselectedIcon = Icons.Outlined.QrCodeScanner,
        testTag = "nav_qr"
    ),
    TopNavItem(
        route = NexaToolRoute.FINANCE,
        label = "Finance",
        selectedIcon = Icons.Rounded.Calculate,
        unselectedIcon = Icons.Outlined.Calculate,
        testTag = "nav_finance"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexaKitApp(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: NexaToolRoute.HOME.route
    val adState by AdManager.state.collectAsStateWithLifecycle()
    var showAdConsole by remember { mutableStateOf(false) }

    val navigateToTool: (NexaToolRoute) -> Unit = { target ->
        navController.navigate(target.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val screenTitle = when (currentRoute) {
        NexaToolRoute.PDF_MAKER.route -> "Photo to PDF"
        NexaToolRoute.COMPRESSOR.route -> "Photo Compressor"
        NexaToolRoute.QR_STUDIO.route -> "QR Studio"
        NexaToolRoute.FINANCE.route -> "Finance & EMI"
        NexaToolRoute.CONVERTER.route -> "Unit Converter"
        NexaToolRoute.HEALTH_SENSORS.route -> "Health & Compass"
        else -> "NexaKit"
    }

    val isHome = currentRoute == NexaToolRoute.HOME.route

    if (showAdConsole) {
        AdMobConsoleSheet(onDismiss = { showAdConsole = false })
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    navigationIcon = {
                        if (!isHome) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.testTag("topbar_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = screenTitle,
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (isHome) {
                                Text(
                                    text = "Daily Problem-Solver Suite",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        Surface(
                            onClick = { showAdConsole = true },
                            color = if (adState.isAdFreeActive) {
                                EmeraldAccent.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = CircleShape,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("topbar_ad_console_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (adState.isAdFreeActive) {
                                        Icons.Rounded.Verified
                                    } else {
                                        Icons.Rounded.AdsClick
                                    },
                                    contentDescription = "AdMob Status",
                                    tint = if (adState.isAdFreeActive) EmeraldAccent else AmberAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (adState.isAdFreeActive) "Pro" else "Ads",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier.testTag("toggle_theme_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = "Toggle theme"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    Column {
                        AnchoredAdaptiveBannerAd()
                        NavigationBar {
                            NAV_ITEMS.forEach { item ->
                                val selected = backStackEntry?.destination?.hierarchy?.any {
                                    it.route == item.route.route
                                } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTool(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        NAV_ITEMS.forEach { item ->
                            val selected = backStackEntry?.destination?.hierarchy?.any {
                                it.route == item.route.route
                            } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTool(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = NexaToolRoute.HOME.route
                        ) {
                            composable(NexaToolRoute.HOME.route) {
                                HomeDashboardScreen(
                                    onNavigateTool = navigateToTool,
                                    onOpenAdConsole = { showAdConsole = true }
                                )
                            }
                            composable(NexaToolRoute.PDF_MAKER.route) {
                                PdfMakerScreen()
                            }
                            composable(NexaToolRoute.COMPRESSOR.route) {
                                PhotoCompressorScreen()
                            }
                            composable(NexaToolRoute.QR_STUDIO.route) {
                                QrStudioScreen()
                            }
                            composable(NexaToolRoute.FINANCE.route) {
                                FinanceToolsScreen()
                            }
                            composable(NexaToolRoute.CONVERTER.route) {
                                UnitConverterScreen()
                            }
                            composable(NexaToolRoute.HEALTH_SENSORS.route) {
                                HealthAndSensorsScreen()
                            }
                        }
                    }
                    if (useNavigationRail) {
                        AnchoredAdaptiveBannerAd(
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }
                }
            }
        }
    }
}
