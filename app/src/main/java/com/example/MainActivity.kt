package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ReelGaugeTheme
import com.example.ui.viewmodel.ReelGaugeViewModel

const val ROUTE_OVERVIEW = "overview"
const val ROUTE_HISTORY = "history"
const val ROUTE_SETTINGS = "settings"

private data class NavItem(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { ReelGaugeTheme { ReelGaugeRoot() } }
  }
}

@Composable
fun ReelGaugeRoot(viewModel: ReelGaugeViewModel = viewModel()) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: ROUTE_OVERVIEW

  val notificationLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
      onResult = { viewModel.refreshPermissions() },
    )

  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissions() }

  val navItems =
    listOf(
      NavItem(
        route = ROUTE_OVERVIEW,
        labelRes = R.string.nav_overview,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_overview",
      ),
      NavItem(
        route = ROUTE_HISTORY,
        labelRes = R.string.nav_history,
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
        testTag = "nav_history",
      ),
      NavItem(
        route = ROUTE_SETTINGS,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune,
        testTag = "nav_settings",
      ),
    )

  fun navigateTab(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  fun requestNotifications() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
          putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
      context.startActivity(intent)
    }
  }

  fun requestOverlay() {
    val intent =
      Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
      )
    context.startActivity(intent)
  }

  fun requestAccessibility() {
    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      contentWindowInsets = WindowInsets.safeDrawing,
      bottomBar = {
        if (!useNavRail) {
          NavigationBar {
            navItems.forEach { item ->
              val selected = currentRoute == item.route
              val label = stringResource(item.labelRes)
              NavigationBarItem(
                selected = selected,
                onClick = { navigateTab(item.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag(item.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useNavRail) {
          NavigationRail {
            navItems.forEach { item ->
              val selected = currentRoute == item.route
              val label = stringResource(item.labelRes)
              NavigationRailItem(
                selected = selected,
                onClick = { navigateTab(item.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag(item.testTag),
              )
            }
          }
        }

        NavHost(
          navController = navController,
          startDestination = ROUTE_OVERVIEW,
          modifier = Modifier.weight(1f).fillMaxSize(),
        ) {
          composable(ROUTE_OVERVIEW) {
            OverviewScreen(
              state = uiState,
              onRequestNotifications = ::requestNotifications,
              onRequestOverlay = ::requestOverlay,
              onRequestAccessibility = ::requestAccessibility,
              onSnooze15m = { viewModel.snooze15Minutes() },
              onResumeTracking = { viewModel.resumeTracking() },
            )
          }
          composable(ROUTE_HISTORY) {
            HistoryScreen(
              state = uiState,
              onSelectDate = { viewModel.selectHistoryDate(it) },
            )
          }
          composable(ROUTE_SETTINGS) {
            SettingsScreen(
              state = uiState,
              onDailyLimitChange = { viewModel.setDailyLimit(it) },
              onMilestoneChange = { viewModel.setMilestoneInterval(it) },
              onToggleHaptic = { viewModel.setHapticNudge(it) },
              onToggleToast = { viewModel.setToastNudge(it) },
              onToggleOverlay = { viewModel.setOverlayEnabled(it) },
              onToggleNotification = { viewModel.setPersistentNotificationEnabled(it) },
              onResetToday = { viewModel.resetTodayCount() },
              onClearHistory = { viewModel.clearAllHistory() },
            )
          }
        }
      }
    }
  }
}
