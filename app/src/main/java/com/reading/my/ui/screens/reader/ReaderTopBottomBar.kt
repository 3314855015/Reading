package com.reading.my.ui.screens.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reading.my.core.reader.domain.ReaderTheme

/**
 * 阅读器顶部信息栏
 *
 * 显示：返回按钮 | 书名 | 章节标题 | 进度
 * 点击外部区域或关闭按钮后隐藏。
 */
@Composable
fun ReaderTopBar(
    modifier: Modifier = Modifier,
    bookTitle: String,
    chapterTitle: String,
    currentChapter: Int,
    totalChapters: Int,
    currentPageCount: Int,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onClose),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookTitle,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = chapterTitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                )
            }

            // 章节进度
            Text(
                text = "$currentChapter / $totalChapters",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.width(12.dp))
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = Color.White.copy(alpha = 0.7f),
                )
            }
        }

        // 细线进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
        ) {
            LinearProgressIndicator(
                progress = { currentChapter.toFloat() / totalChapters.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFF9800),
                trackColor = Color.White.copy(alpha = 0.2f),
            )
        }
    }
}

/**
 * 阅读器底部信息栏
 *
 * 显示：时间 | 电池占位 | 页码 | 设置入口
 */
@Composable
fun ReaderBottomBar(
    modifier: Modifier = Modifier,
    theme: ReaderTheme,
    onClose: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(theme.barBackgroundColor.copy(alpha = 0.95f))
            .clickable(onClick = onClose)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左侧：时间（模拟）
        Text(
            text = "12:00",   // TODO: 接入实际时间
            fontSize = 13.sp,
            color = theme.secondaryColor,
        )

        Spacer(modifier = Modifier.weight(1f))

        // 中间：页码（后续由翻页事件更新）
        Text(
            text = "第 1 页 / 共 ? 页",  // TODO: 接入实时页码
            fontSize = 12.sp,
            color = theme.secondaryColor,
        )

        Spacer(modifier = Modifier.weight(1f))

        // 右侧：设置图标（占位）
        Box(modifier = Modifier.size(28.dp)) {
            // 占位，保持对称
        }
    }
}
