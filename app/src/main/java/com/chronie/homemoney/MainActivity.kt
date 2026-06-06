package com.chronie.homemoney

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chronie.homemoney.core.common.LanguageManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.chronie.homemoney.ui.expense.AddExpenseScreen
import com.chronie.homemoney.ui.expense.AIExpenseScreen
import com.chronie.homemoney.ui.main.MainScreen
import com.chronie.homemoney.ui.settings.SettingsScreen
import com.chronie.homemoney.ui.settings.OpenSourceLicensesScreen
import com.chronie.homemoney.ui.sync.LanSyncScreen
import com.chronie.homemoney.ui.test.DatabaseTestScreen
import com.chronie.homemoney.ui.theme.HomeMoneyTheme
import com.chronie.homemoney.ui.welcome.WelcomeScreen
import com.chronie.homemoney.ui.membership.MembershipScreen
import com.chronie.homemoney.domain.usecase.CheckLoginStatusUseCase
import com.chronie.homemoney.service.HealthCheckService
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

val LocalLanguageManager = staticCompositionLocalOf<LanguageManager> {
    error("No LanguageManager provided")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var languageManager: LanguageManager

    @Inject
    lateinit var syncScheduler: com.chronie.homemoney.data.sync.SyncScheduler

    @Inject
    lateinit var checkLoginStatusUseCase: CheckLoginStatusUseCase

    @Inject
    lateinit var healthCheckService: HealthCheckService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化同步调度器
        syncScheduler.initialize()

        // 应用启动时触发云同步尝试（允许失败）
        lifecycleScope.launch {
            try {
                syncScheduler.triggerImmediateSync()
            } catch (e: Exception) {
                // 同步失败不影响应用启动
                android.util.Log.w("MainActivity", "Failed to trigger sync on app start", e)
            }
        }

        // 立即切换到正常主题，避免启动图背景影响 Popup 窗口
        setTheme(R.style.AppTheme_NoActionBar)

        // 清除启动图背景，设置为透明背景
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Make sure the window draws behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 启动健康检查服务
        healthCheckService.start()

        setContent {
            val currentLanguage by languageManager.currentLanguage.collectAsState()

            // Update configuration when language changes
            val context = LocalContext.current
            val locale = currentLanguage.locale
            Locale.setDefault(locale)
            val configuration = Configuration(context.resources.configuration)
            configuration.setLocale(locale)
            val localizedContext = context.createConfigurationContext(configuration)

            CompositionLocalProvider(
                LocalLanguageManager provides languageManager
            ) {
                HomeMoneyTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        HomeMoneyApp(
                            context = localizedContext,
                            checkLoginStatusUseCase = checkLoginStatusUseCase
                        )
                    }
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        if (::languageManager.isInitialized) {
            languageManager.checkAndApplySystemLanguage()
        }
    }

    override fun onResume() {
        super.onResume()
        languageManager.checkAndApplySystemLanguage()
    }

    override fun onDestroy() {
        super.onDestroy()
        healthCheckService.stop()
    }
}

@Composable
fun HomeMoneyApp(
    context: Context,
    checkLoginStatusUseCase: CheckLoginStatusUseCase
) {
    val navController = rememberNavController()
    var shouldRefreshExpenses by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    // 确定初始路由
    val startDestination = remember {
        val isLoggedIn = checkLoginStatusUseCase()
        if (isLoggedIn) "main" else "welcome"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
        },
        exitTransition = {
            scaleOut(
                targetScale = 0.9f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
        },
        popEnterTransition = {
            scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
        }
    ) {
        composable("welcome") {
            WelcomeScreen(
                context = context,
                onSettingsClick = {
                    navController.navigate("settings")
                },
                onGetStartedClick = {
                    navController.navigate("main") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToMembership = {
                    navController.navigate("membership") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("membership") {
            MembershipScreen(
                context = context,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                context = context,
                onNavigateToDatabaseTest = {
                    navController.navigate("database_test")
                },
                onNavigateToMembership = {
                    navController.navigate("membership")
                },
                onNavigateToLanSync = {
                    navController.navigate("lan_sync")
                },
                onNavigateToOpenSourceLicenses = {
                    navController.navigate("open_source_licenses")
                },
                onLogout = {
                    // 退出登录后，清空整个导航栈并返回欢迎页
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onRequireLogin = {
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("lan_sync") {
            LanSyncScreen(
                context = context,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("main") {
            MainScreen(
                context = context,
                shouldRefreshExpenses = shouldRefreshExpenses,
                onRefreshHandled = { shouldRefreshExpenses = false },
                selectedTab = selectedTab,
                onTabChange = { selectedTab = it },
                onNavigateToSettings = {
                    selectedTab = 2
                    navController.navigate("settings")
                },
                onNavigateToDatabaseTest = {
                    selectedTab = 2
                    navController.navigate("database_test")
                },
                onNavigateToAddExpense = {
                    selectedTab = 0
                    navController.navigate("add_expense")
                },
                onNavigateToEditExpense = { expenseId ->
                        selectedTab = 0
                        navController.navigate(
                            route = "add_expense?expenseId=$expenseId"
                        )
                    },
                onNavigateToWeekdayDetail = { dayOfWeek, amount, count, percentage, startDate, endDate ->
                    selectedTab = 1
                    navController.navigate(
                        route = "weekday_detail?dayOfWeek=$dayOfWeek&amount=$amount&count=$count&percentage=$percentage&startDate=$startDate&endDate=$endDate"
                    )
                },
                onNavigateToLanSync = {
                    selectedTab = 2
                    navController.navigate("lan_sync")
                },
                onNavigateToOpenSourceLicenses = {
                    selectedTab = 2
                    navController.navigate("open_source_licenses")
                },
                onRequireLogin = {
                    // 未登录时，清空导航栈并返回欢迎页
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            "add_expense?expenseId={expenseId}",
            arguments = listOf(
                navArgument("expenseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId")
            AddExpenseScreen(
                context = context,
                expenseId = expenseId,
                onNavigateBack = {
                    shouldRefreshExpenses = true
                    navController.popBackStack()
                },
                onNavigateToAI = {
                    navController.navigate("ai_expense")
                },
                onRequireLogin = {
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("ai_expense") {
            AIExpenseScreen(
                context = context,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRecordsSaved = {
                    shouldRefreshExpenses = true
                    navController.popBackStack()
                }
            )
        }

        composable("database_test") {
            DatabaseTestScreen(
                context = context,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("open_source_licenses") {
            OpenSourceLicensesScreen(
                context = context,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            "weekday_detail?dayOfWeek={dayOfWeek}&amount={amount}&count={count}&percentage={percentage}&startDate={startDate}&endDate={endDate}",
            arguments = listOf(
                navArgument("dayOfWeek") { type = NavType.IntType },
                navArgument("amount") { type = NavType.FloatType },
                navArgument("count") { type = NavType.IntType },
                navArgument("percentage") { type = NavType.FloatType },
                navArgument("startDate") { type = NavType.StringType },
                navArgument("endDate") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val dayOfWeek = backStackEntry.arguments?.getInt("dayOfWeek") ?: 0
            val amount = backStackEntry.arguments?.getFloat("amount")?.toDouble() ?: 0.0
            val count = backStackEntry.arguments?.getInt("count") ?: 0
            val percentage = backStackEntry.arguments?.getFloat("percentage") ?: 0f
            val startDate = backStackEntry.arguments?.getString("startDate") ?: LocalDate.now().minusMonths(1).toString()
            val endDate = backStackEntry.arguments?.getString("endDate") ?: LocalDate.now().toString()

            com.chronie.homemoney.ui.charts.WeekdayDetailScreen(
                context = context,
                dayOfWeek = dayOfWeek,
                amount = amount,
                count = count,
                percentage = percentage,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
