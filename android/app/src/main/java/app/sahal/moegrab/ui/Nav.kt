package app.sahal.moegrab.ui

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.sahal.moegrab.R
import app.sahal.moegrab.ui.history.HistoryScreen
import app.sahal.moegrab.ui.home.HomeScreen
import app.sahal.moegrab.ui.info.InfoScreen
import app.sahal.moegrab.ui.queue.QueueScreen
import app.sahal.moegrab.ui.settings.SettingsScreen
import app.sahal.moegrab.ui.sites.SitesScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val QUEUE = "queue"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val SITES = "sites"

    const val INFO_PATTERN = "info/{url}"
    fun info(url: String): String = "info/${URLEncoder.encode(url, "UTF-8")}"

    fun decodeUrlArg(raw: String?): String? =
        raw?.let { URLDecoder.decode(it, "UTF-8") }
}

// Screen-transition durations tuned to feel snappy — a slow slide reads
// as sluggish on a phone. 220ms in, 180ms out.
private val NAV_ENTER = tween<Float>(220, easing = EaseInOutCubic)
private val NAV_EXIT = tween<Float>(180, easing = EaseInOutCubic)
private val NAV_SLIDE_IN = tween<androidx.compose.ui.unit.IntOffset>(220, easing = EaseInOutCubic)
private val NAV_SLIDE_OUT = tween<androidx.compose.ui.unit.IntOffset>(180, easing = EaseInOutCubic)

@Composable
fun AppNavHost(initialUrl: MutableState<String?>) {
    val nav = rememberNavController()
    Scaffold(
        bottomBar = { BottomBar(nav) },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = {
                slideInHorizontally(NAV_SLIDE_IN) { it / 6 } + fadeIn(NAV_ENTER)
            },
            exitTransition = {
                slideOutHorizontally(NAV_SLIDE_OUT) { -it / 8 } + fadeOut(NAV_EXIT)
            },
            popEnterTransition = {
                slideInHorizontally(NAV_SLIDE_IN) { -it / 6 } + fadeIn(NAV_ENTER)
            },
            popExitTransition = {
                slideOutHorizontally(NAV_SLIDE_OUT) { it / 8 } + fadeOut(NAV_EXIT)
            },
        ) {
            addHome(nav, initialUrl)
            composable(Routes.INFO_PATTERN) { back ->
                val raw = back.arguments?.getString("url")
                InfoScreen(url = Routes.decodeUrlArg(raw).orEmpty(), onBack = { nav.popBackStack() })
            }
            composable(Routes.QUEUE) { QueueScreen() }
            composable(Routes.HISTORY) { HistoryScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.SITES) { SitesScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

private fun NavGraphBuilder.addHome(nav: NavHostController, initialUrl: MutableState<String?>) {
    composable(Routes.HOME) {
        HomeScreen(
            initialUrl = initialUrl.value,
            onUrlConsumed = { initialUrl.value = null },
            onNavigateInfo = { nav.navigate(Routes.info(it)) },
            onNavigateSites = { nav.navigate(Routes.SITES) },
        )
    }
}

private data class TabDef(val route: String, val labelRes: Int, val icon: ImageVector)

private val TABS = listOf(
    TabDef(Routes.HOME, R.string.tab_home, Icons.Filled.Home),
    TabDef(Routes.QUEUE, R.string.tab_queue, Icons.Filled.Download),
    TabDef(Routes.HISTORY, R.string.tab_history, Icons.Filled.History),
    TabDef(Routes.SETTINGS, R.string.tab_settings, Icons.Filled.Settings),
)

@Composable
private fun BottomBar(nav: NavHostController) {
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    NavigationBar {
        TABS.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.route,
                onClick = {
                    if (current != tab.route) {
                        nav.navigate(tab.route) {
                            popUpTo(Routes.HOME) { inclusive = false; saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(androidx.compose.ui.res.stringResource(tab.labelRes)) },
            )
        }
    }
}
