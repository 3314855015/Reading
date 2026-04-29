package com.reading.my

import android.content.Intent
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

/** Cwriter 同步 Intent 的 Action 常量 */
const val ACTION_IMPORT_BOOK = "com.reading.app.IMPORT_BOOK"
const val EXTRA_SYNC_PAYLOAD = "SYNC_PAYLOAD"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: UserSessionManager

    /** 从 Cwriter 接收到的同步 payload JSON（在 onCreate/onNewIntent 中提取） */
    var pendingSyncPayload: String? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        extractSyncIntent(intent)
        enableEdgeToEdge()
        setContent {
            ReadingTheme {
                val navController = rememberNavController()
                // 纯容器：各页面自行控制沉浸式状态栏和背景
                Box(modifier = Modifier.fillMaxSize()) {
                    NavGraph(
                        navController = navController,
                        sessionManager = sessionManager,
                        pendingSyncPayload = pendingSyncPayload,
                    )
                }
            }
        }
    }

    /**
     * 当 Activity 已在栈顶时，新的 Intent 通过 onNewIntent 送达
     * （Cwriter 使用 FLAG_ACTIVITY_NEW_TASK 可能触发此路径）
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractSyncIntent(intent)
    }

    /**
     * 从 Intent 中提取 Cwriter 同步数据
     */
    private fun extractSyncIntent(intent: Intent?) {
        if (intent?.action == ACTION_IMPORT_BOOK) {
            pendingSyncPayload = intent.getStringExtra(EXTRA_SYNC_PAYLOAD)
            android.util.Log.i("MainActivity", "收到 Cwriter 同步Intent, payload size=${pendingSyncPayload?.length ?: 0}")
        }
    }
}
