package com.akspareparts.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.akspareparts.app.ui.screens.AllPartsScreen
import com.akspareparts.app.ui.screens.CustomerDetailScreen
import com.akspareparts.app.ui.screens.CustomersScreen
import com.akspareparts.app.ui.screens.LoginScreen
import com.akspareparts.app.ui.screens.NewPartScreen
import com.akspareparts.app.ui.theme.AKSparepartsTheme
import com.akspareparts.app.ui.theme.DeepBlue
import com.akspareparts.app.ui.theme.DeepBlueDarker
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
    var currentRoute by remember { mutableStateOf("customers") }
    var currentTitle by remember { mutableStateOf("Customers") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                // ---- Drawer header ----
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(DeepBlueDarker, DeepBlue)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Image(
                            painter = painterResource(R.drawable.ak_logo),
                            contentDescription = "AK Spareparts",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .aspectRatio(2.4f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (authVm.fullName ?: "?").take(1).uppercase(),
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    authVm.fullName ?: "",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "Signed in",
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                DRAWER_ITEMS.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label, fontWeight = FontWeight.Medium) },
                        selected = currentRoute == item.route,
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        onClick = {
                            currentRoute = item.route
                            currentTitle = item.label
                            navController.navigate(item.route) {
                                popUpTo("customers") { inclusive = false }
                                launchSingleTop = true
                            }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                HorizontalDivider(Modifier.padding(horizontal = 24.dp))
                NavigationDrawerItem(
                    icon = {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error)
                    },
                    label = {
                        Text("Logout", fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error)
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        authVm.logout()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentTitle, fontWeight = FontWeight.SemiBold) },
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
