package com.reading.my.ui.screens.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reading.my.ui.theme.*

/**
 * 从 Cwriter 写作APP同步导入页面
 *
 * 两种进入方式：
 * 1. Cwriter 通过 Intent 发起 → payloadJson 非空，自动解析并展示预览
 * 2. 用户从书架菜单手动点击 → payloadJson 为空，显示等待状态
 */
@Composable
fun SyncImportScreen(
    payloadJson: String? = null,
    onBack: () -> Unit,
    viewModel: SyncImportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 收到 payload 时自动解析
    LaunchedEffect(payloadJson) {
        if (!payloadJson.isNullOrEmpty() && uiState.payload == null) {
            viewModel.parsePayload(payloadJson)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1b1c1c))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // 顶栏
        SyncImportTopBar(onBack = onBack)

        Spacer(Modifier.height(24.dp))

        when {
            // 加载中/解析中
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryOrange)
                        Spacer(Modifier.height(16.dp))
                        Text("正在解析数据...", color = Color.White, fontSize = 14.sp)
                    }
                }
            }

            // 有错误
            uiState.errorMessage != null && uiState.result == null -> {
                ErrorView(
                    message = uiState.errorMessage!!,
                    onRetry = { payloadJson?.let { viewModel.parsePayload(it) } }
                )
            }

            // 有 payload 数据，展示预览或结果
            uiState.payload != null -> {
                val payload = uiState.payload!!

                // 导入已完成，显示结果
                if (uiState.result != null) {
                    ImportResultView(result = uiState.result!!)
                } else {
                    // 显示预览信息 + 导入按钮
                    PayloadPreviewCard(
                        bookTitle = payload.bookTitle,
                        author = payload.author,
                        chapterCount = payload.chapters.size,
                        syncVersion = payload.syncVersion,
                        description = payload.description,
                        isImporting = uiState.isImporting,
                        onImport = { viewModel.executeImport() },
                    )
                }
            }

            // 无 payload 且无错误（手动打开，等待 Intent）
            else -> {
                EmptySyncView()
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

// ─── 顶栏 ──────────────────────────────────────

@Composable
private fun SyncImportTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Text("← 返回", fontSize = 15.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
        }
        Text(
            text = "同步导入",
            modifier = Modifier.weight(1f),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.width(60.dp))
    }
}

// ─── Payload 预览卡片 ───────────────────────────

@Composable
private fun PayloadPreviewCard(
    bookTitle: String,
    author: String,
    chapterCount: Int,
    syncVersion: Int,
    description: String?,
    isImporting: Boolean,
    onImport: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 来源标识
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PrimaryOrange.copy(alpha = 0.15f)
        ) {
            Text(
                text = "来自 Cwriter 写作APP",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                fontSize = 12.sp,
                color = PrimaryOrange,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(20.dp))

        // 书籍信息卡片
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF2D2D2D),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                Text(bookTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text("作者：$author", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoChip(label = "章节", value = "$chapterCount 章")
                    InfoChip(label = "版本", value = "v$syncVersion")
                }

                if (!description.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(description, fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f), lineHeight = 18.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 导入按钮
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = PrimaryOrange,
            modifier = Modifier.clickable(enabled = !isImporting) { onImport() },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isImporting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("正在导入...", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Text("确认导入到书架", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "首次导入将创建新书，后续同步将增量更新",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.08f)) {
        Text(
            text = "$label：$value",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

// ─── 导入结果视图 ───────────────────────────────

@Composable
private fun ImportResultView(result: com.reading.my.domain.model.SyncImportResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 结果图标
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (result.success) SuccessGreen else ErrorRed.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (result.success) "\u2713" else "\u2717",
                fontSize = 28.sp,
                color = if (result.success) SuccessGreen else ErrorRed,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = if (result.success) "导入成功" else "导入失败",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )

        Spacer(Modifier.height(12.dp))

        // 统计卡片
        if (result.success) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF2D2D2D),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StatRow("总章节数", "${result.totalChapters} 章")
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    StatRow("新增", "${result.newChapters} 章")
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    StatRow("更新", "${result.updatedChapters} 章")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                result.message,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )
        } else {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ErrorRed.copy(alpha = 0.1f),
            ) {
                Text(
                    text = result.message,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 13.sp,
                    color = ErrorRed,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
        Text(value, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

// ─── 错误视图 ──────────────────────────────────

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "\u26A0", fontSize = 48.sp, color = PrimaryOrange)
        Spacer(Modifier.height(16.dp))
        Text("数据解析失败", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(message, fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Surface(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            color = PrimaryOrange,
        ) {
            Text(
                text = "重新尝试",
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ─── 空状态（手动打开时）──────────────────────

@Composable
private fun EmptySyncView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "\uD83D\uDCBE", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "等待同步数据",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "请在 Cwriter 写作APP 中点击「同步」按钮\n将书籍发送到此应用",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
    }
}
