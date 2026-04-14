package com.reading.my

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.reading.my.core.reader.engine.L2DatabaseCache
import com.reading.my.data.local.UserSessionManager
import com.reading.my.ui.navigation.NavGraph
import com.reading.my.ui.theme.ReadingTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: UserSessionManager  // ★ 注入 Session 管理器
    @Inject lateinit var l2Cache: L2DatabaseCache          // ★ 注入 L2 缓存管理器

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReadingTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // ★ 传入 sessionManager + l2Cache 实例
                    NavGraph(navController = navController, sessionManager = sessionManager, l2Cache = l2Cache)
                }
            }
        }
    }
}
