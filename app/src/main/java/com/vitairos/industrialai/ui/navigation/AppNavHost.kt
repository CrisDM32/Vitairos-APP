package com.vitairos.industrialai.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.navigation.NavBackStackEntry
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vitairos.industrialai.ui.ReportsViewModel
import com.vitairos.industrialai.ui.screens.AttachmentsScreen
import com.vitairos.industrialai.ui.screens.DashboardScreen
import com.vitairos.industrialai.ui.screens.ExtractionScreen
import com.vitairos.industrialai.ui.screens.HistoryScreen
import com.vitairos.industrialai.ui.screens.LoginScreen
import com.vitairos.industrialai.ui.screens.NewReportScreen
import com.vitairos.industrialai.ui.screens.ProfileScreen
import com.vitairos.industrialai.ui.screens.QueueScreen
import com.vitairos.industrialai.ui.screens.ReportDetailScreen
import com.vitairos.industrialai.ui.screens.ReviewScreen
import com.vitairos.industrialai.ui.theme.Acero
import com.vitairos.industrialai.ui.theme.Cobre
import com.vitairos.industrialai.ui.theme.Hueso
import com.vitairos.industrialai.ui.theme.VerdeNoche

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val QUEUE = "queue"
    const val HISTORY = "history"
    const val PROFILE = "profile"
    const val NEW_REPORT = "new_report"
    const val ATTACHMENTS = "attachments"
    const val EXTRACTION = "extraction"
    const val REVIEW = "review"
    const val DETAIL = "detail/{id}"
    fun detail(id: String) = "detail/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Inicio", Icons.Filled.Home),
    Tab(Routes.QUEUE, "Cola", Icons.AutoMirrored.Filled.List),
    Tab(Routes.HISTORY, "Historial", Icons.Filled.DateRange),
    Tab(Routes.PROFILE, "Perfil", Icons.Filled.Person),
)

private fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun AppNavHost(vm: ReportsViewModel = viewModel()) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    // Espera a que Room y DataStore carguen para elegir la pantalla inicial (sesión guardada).
    if (!vm.ready) {
        Box(Modifier.fillMaxSize().background(VerdeNoche))
        return
    }
    val startRoute = remember { if (vm.user != null) Routes.HOME else Routes.LOGIN }
    val tabRoutes = remember { tabs.map { it.route } }
    fun AnimatedContentTransitionScope<NavBackStackEntry>.betweenTabs() =
        initialState.destination.route in tabRoutes && targetState.destination.route in tabRoutes

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar(containerColor = VerdeNoche) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.goTab(tab.route) },
                            icon = {
                                if (tab.route == Routes.QUEUE && vm.pendingCount > 0) {
                                    BadgedBox(badge = { Badge(containerColor = Cobre, contentColor = VerdeNoche) { Text("${vm.pendingCount}") } }) {
                                        Icon(tab.icon, contentDescription = null)
                                    }
                                } else Icon(tab.icon, contentDescription = null)
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VerdeNoche, indicatorColor = Cobre,
                                selectedTextColor = Hueso, unselectedIconColor = Acero, unselectedTextColor = Acero,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            nav, startDestination = startRoute, modifier = Modifier.fillMaxSize().padding(padding),
            enterTransition = {
                if (betweenTabs()) fadeIn(tween(250)) else slideInHorizontally(tween(300)) { it / 6 } + fadeIn(tween(300))
            },
            exitTransition = {
                if (betweenTabs()) fadeOut(tween(150)) else slideOutHorizontally(tween(300)) { -it / 10 } + fadeOut(tween(200))
            },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 10 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it / 6 } + fadeOut(tween(200)) },
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(vm) { nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } }
            }
            composable(Routes.HOME) {
                DashboardScreen(
                    vm,
                    onNewReport = { vm.startNewDraft(); nav.navigate(Routes.NEW_REPORT) },
                    onOpenReport = { nav.navigate(Routes.detail(it)) },
                    onOpenQueue = { nav.goTab(Routes.QUEUE) },
                    onOpenHistory = { nav.goTab(Routes.HISTORY) },
                )
            }
            composable(Routes.QUEUE) {
                QueueScreen(vm, onOpenReport = { nav.navigate(Routes.detail(it)) }, onEdit = { vm.startEdit(it); nav.navigate(Routes.NEW_REPORT) })
            }
            composable(Routes.HISTORY) { HistoryScreen(vm) { nav.navigate(Routes.detail(it)) } }
            composable(Routes.PROFILE) {
                ProfileScreen(vm) { vm.logout(); nav.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } } }
            }
            composable(Routes.NEW_REPORT) {
                NewReportScreen(vm, onBack = { nav.popBackStack() }, onContinue = { nav.navigate(Routes.ATTACHMENTS) })
            }
            composable(Routes.ATTACHMENTS) {
                AttachmentsScreen(
                    vm, onBack = { nav.popBackStack() },
                    onExtract = { nav.navigate(Routes.EXTRACTION) },
                    onContinue = { nav.navigate(Routes.REVIEW) },
                )
            }
            composable(Routes.EXTRACTION) {
                ExtractionScreen(vm, onBack = { nav.popBackStack() }, onConfirm = { nav.navigate(Routes.REVIEW) { popUpTo(Routes.ATTACHMENTS) } })
            }
            composable(Routes.REVIEW) {
                ReviewScreen(vm, onBack = { nav.popBackStack() }, onSaved = { id ->
                    nav.navigate(Routes.detail(id)) { popUpTo(Routes.HOME) }
                })
            }
            composable(Routes.DETAIL) { entry ->
                ReportDetailScreen(
                    vm, reportId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onEdit = { vm.startEdit(it); nav.navigate(Routes.NEW_REPORT) },
                )
            }
        }
    }
}
