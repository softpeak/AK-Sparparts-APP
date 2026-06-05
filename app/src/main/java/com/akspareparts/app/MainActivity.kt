package com.akspareparts.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.akspareparts.app.ui.components.ApiKeyDialog
import com.akspareparts.app.ui.screens.AllPartsScreen
import com.akspareparts.app.ui.screens.CustomerDetailScreen
import com.akspareparts.app.ui.screens.CustomersScreen
import com.akspareparts.app.ui.screens.LoginScreen
import com.akspareparts.app.ui.screens.NewPartScreen
import com.akspareparts.app.ui.theme.AKSparepartsTheme
import com.akspareparts.app.ui.viewmodel.AuthViewModel
import com.akspareparts.app.ui.viewmodel.CustomerDetailViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AKSparepartsTheme {
                val authVm: AuthViewModel = viewModel()
                val loggedIn by authVm.loggedIn.collectAsState()
                if (loggedIn) MainShell(authVm) else LoginScreen(authVm)
            }
        }
    }
}

private data class DrawerItem(val route: String, val label: String, val icon: ImageVector)

private val DRAWER_ITEMS = listOf(
    DrawerItem("customers", "Customers", Icons.Filled.People),
    DrawerItem("allParts", "All Parts List", Icons.AutoMirrored.Filled.ListAlt),
    DrawerItem("newPart", "New Part", Icons.Filled.Add)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(authVm: AuthViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val needsApiKey by authVm.needsApiKey.collectAsState()
    var currentTitle by remember { mutableStateOf("Customers") }

    if (needsApiKey) {
        ApiKeyDialog(
            onSave = { authVm.saveApiKey(it) },
            onDismiss = { authVm.dismissApiKeyPrompt() }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(24.dp))
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("AK Spareparts", style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    authVm.fullName?.let {
                        Text("Signed in as $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                DRAWER_ITEMS.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        selected = false,
                        onClick = {
                            currentTitle = item.label
                            navController.navigate(item.route) {
                                popUpTo("customers") { inclusive = false }
                                launchSingleTop = true
                            }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                    label = { Text("Logout") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        authVm.logout()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentTitle) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { pad ->
            NavHost(
                navController = navController,
                startDestination = "customers",
                modifier = Modifier.padding(pad)
            ) {
                composable("customers") {
                    CustomersScreen(onOpenCustomer = { id ->
                        navController.navigate("customer/$id")
                    })
                }
                composable("allParts") { AllPartsScreen() }
                composable("newPart") { NewPartScreen() }
                composable(
                    route = "customer/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.IntType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("id") ?: return@composable
                    val app = LocalContext.current.applicationContext as Application
                    val detailVm: CustomerDetailViewModel = viewModel(
                        key = "customer_$id",
                        factory = CustomerDetailViewModel.Factory(app, id)
                    )
                    CustomerDetailScreen(vm = detailVm, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
