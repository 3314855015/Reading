package com.reading.my

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.reading.my.data.local.UserSessionManager
import com.reading.my.ui.navigation.NavGraph
import com.reading.my.ui.theme.ReadingTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Cwriter 同步 Intent 的 Action 常量 */
const val ACTION_IMPORT_BOOK = "com.reading.app.IMPORT_BOOK"
const val EXTRA_SYNC_PAYLOAD = "SYNC_PAYLOAD"    // 旧：直传 JSON 字符串（兼容）
const val EXTRA_SYNC_URI = "SYNC_DATA_URI"        // 新：文件 URI（无大小限制）

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: UserSessionManager

    /**
     * 从 Cwriter 接收到的同步 payload JSON
     *
     * ★ 使用 mutableStateOf 而非普通 var：
     * - onNewIntent 更新此值时 Compose 自动重组
     * - 解决第二次（及后续）导入时同步页面不弹出的问题
     */
    var pendingSyncPayload by mutableStateOf<String?>(null)
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
     *
     * 优先级：
     * 1. EXTRA_SYNC_URI（content:// 文件URI）— 新方式，无大小限制
     * 2. EXTRA_SYNC_PAYLOAD（JSON 字符串） — 旧方式，兼容旧版 Cwriter
     */
    private fun extractSyncIntent(intent: Intent?) {
        if (intent?.action != ACTION_IMPORT_BOOK) return

        // ★ 方式一（新）：从 FileProvider URI 读取文件内容
        val syncUri = intent.getParcelableExtra<android.net.Uri>(EXTRA_SYNC_URI)
        if (syncUri != null) {
            try {
                val jsonStr = contentResolver.openInputStream(syncUri)?.use { input ->
                    input.bufferedReader().readText()
                }
                if (jsonStr != null && jsonStr.isNotEmpty()) {
                    pendingSyncPayload = jsonStr
                    android.util.Log.i("MainActivity", "✅ 收到 Cwriter 同步(URI方式): uri=$syncUri, size=${jsonStr.length}字符")
                    return
                } else {
                    android.util.Log.w("MainActivity", "⚠️ URI方式读取为空, fallback到字符串方式")
                }
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "⚠️ URI读取失败: ${e.message}, fallback到字符串方式")
            }
        }

        // ★ 方式二（旧/兼容）：直接从 Intent extra 读字符串
        val payloadStr = intent.getStringExtra(EXTRA_SYNC_PAYLOAD)
        if (!payloadStr.isNullOrEmpty()) {
            pendingSyncPayload = payloadStr
            android.util.Log.i("MainActivity", "收到 Cwriter 同步(字符串方式): size=${payloadStr.length}字符")
        } else {
            android.util.Log.w("MainActivity", "❌ 未从 Intent 中获取到任何同步数据")
        }
    }
}
