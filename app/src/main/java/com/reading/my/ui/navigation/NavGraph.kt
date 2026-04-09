package com.reading.my.ui.navigation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.reading.my.data.local.UserSessionManager
import com.reading.my.ui.screens.MainScreen
import com.reading.my.ui.screens.login.LoginScreen

/**
 * 应用导航图
 *
 * 【黑屏问题根因与修复】
 * 原因：启动时同步读取登录状态导致导航目标不确定，Compose 重组期间空白帧。
 * 修复：remember + LaunchedEffect 异步读取 DataStore，先显示极简占位再导航。
 *
 * 【7天过期逻辑】
 * UserSessionManager.isLoggedInFlow 内部自动检查 loginTime，超过7天返回false。
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    sessionManager: UserSessionManager
) {
    var isSessionChecked by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.d("NavGraph", "Checking user session...")
        sessionManager.isLoggedInFlow.collect { loggedIn ->
            Log.d("NavGraph", "Session result: loggedIn=$loggedIn")
            isLoggedIn = loggedIn
            isSessionChecked = true
        }
    }

    if (!isSessionChecked) {
        SplashScreen()
        return@NavGraph
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) Screen.Main.route else Screen.Login.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                })
            }

            composable(Screen.Main.route) {
                MainScreen()
            }
        }
    }
}

/** 启动占位屏 - 防止导航切换时的空白帧 */
@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "📚", fontSize = 48.sp)
    }
}
