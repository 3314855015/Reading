package com.reading.my

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.reading.my.data.local.UserSessionManager
import com.reading.my.ui.navigation.NavGraph
import com.reading.my.ui.theme.ReadingTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: UserSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReadingTheme {
                val navController = rememberNavController()
                // 纯容器：各页面自行控制沉浸式状态栏和背景
                Box(modifier = Modifier.fillMaxSize()) {
                    NavGraph(navController = navController, sessionManager = sessionManager)
                }
            }
        }
    }
}
