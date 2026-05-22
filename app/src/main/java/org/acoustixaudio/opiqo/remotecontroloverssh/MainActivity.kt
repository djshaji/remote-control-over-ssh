package org.acoustixaudio.opiqo.remotecontroloverssh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import org.acoustixaudio.opiqo.remotecontroloverssh.ui.NavRoute
import org.acoustixaudio.opiqo.remotecontroloverssh.ui.dashboard.DashboardScreen
import org.acoustixaudio.opiqo.remotecontroloverssh.ui.dashboard.DashboardViewModel
import org.acoustixaudio.opiqo.remotecontroloverssh.ui.profiles.*
import org.acoustixaudio.opiqo.remotecontroloverssh.ui.theme.RemoteControlOverSshTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RemoteControlOverSshTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val backStack = rememberNavBackStack(NavRoute.SshProfiles)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Linux Remote\nControl (SSH)",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                NavigationDrawerItem(
                    label = { Text("SSH Profiles") },
                    selected = backStack.last() is NavRoute.SshProfiles,
                    onClick = {
                        while (backStack.isNotEmpty()) backStack.removeAt(backStack.size - 1)
                        backStack.add(NavRoute.SshProfiles)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Remote Profiles") },
                    selected = backStack.last() is NavRoute.RemoteProfiles,
                    onClick = {
                        while (backStack.isNotEmpty()) backStack.removeAt(backStack.size - 1)
                        backStack.add(NavRoute.RemoteProfiles)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = backStack.last() is NavRoute.Settings,
                    onClick = {
                        while (backStack.isNotEmpty()) backStack.removeAt(backStack.size - 1)
                        backStack.add(NavRoute.Settings)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                )
            }
        }
    ) {
    val context = LocalContext.current.applicationContext
    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
        entryProvider = entryProvider {
            entry<NavRoute.SshProfiles> {
                SshProfilesScreen(
                    viewModel = viewModel { SshProfilesViewModel(context) },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            entry<NavRoute.RemoteProfiles> {
                RemoteProfilesScreen(
                    viewModel = viewModel { RemoteProfilesViewModel(context) },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onRemoteClick = { id -> backStack.add(NavRoute.Dashboard(id)) }
                )
            }
                entry<NavRoute.Dashboard> { route ->
                    DashboardScreen(
                        viewModel = viewModel { DashboardViewModel(context, route.remoteProfileId) },
                        onBackClick = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
                    )
                }
                entry<NavRoute.Settings> {
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(title = { Text("Settings") })
                        }
                    ) { p ->
                        Box(Modifier.padding(p).fillMaxSize()) {
                            Text("Settings Screen", modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        )
    }
}
