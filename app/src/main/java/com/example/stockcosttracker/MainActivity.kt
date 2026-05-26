package com.example.stockcosttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.stockcosttracker.ui.AddTransactionScreen
import com.example.stockcosttracker.ui.PortfolioScreen
import com.example.stockcosttracker.ui.SettingsScreen
import com.example.stockcosttracker.ui.theme.StockCostTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StockCostTrackerTheme {
                StockCostTrackerApp()
            }
        }
    }
}

private object AppRoutes {
    const val PORTFOLIO = "portfolio"
    const val ADD_TRANSACTION = "add_transaction"
    const val SETTINGS = "settings"
}

@Composable
private fun StockCostTrackerApp(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.PORTFOLIO
    ) {
        composable(AppRoutes.PORTFOLIO) {
            PortfolioScreen(
                onOpenAddTransaction = {
                    navController.navigate(AppRoutes.ADD_TRANSACTION)
                },
                onOpenSettings = {
                    navController.navigate(AppRoutes.SETTINGS)
                }
            )
        }

        composable(AppRoutes.ADD_TRANSACTION) {
            AddTransactionScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(AppRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}