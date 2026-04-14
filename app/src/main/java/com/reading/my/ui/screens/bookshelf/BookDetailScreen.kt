package com.reading.my.ui.screens.bookshelf

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reading.my.domain.model.Book
import com.reading.my.domain.model.Chapter
import com.reading.my.ui.theme.BackgroundGray
import com.reading.my.ui.theme.DividerColor
import com.reading.my.ui.theme.PrimaryOrange
import com.reading.my.ui.theme.TextDisabled
import com.reading.my.ui.theme.TextHint
import com.reading.my.ui.theme.TextPrimary
import com.reading.my.ui.theme.TextSecondary

/**
 * 书籍详情页
 *
 * 用于验证导入解析是否正确：
 * - 显示书籍元数据（标题、作者、简介、章节数）
 * - 显示章节目录列表
 * - 点击章节 → 进入自研阅读器（ReaderScreen，带分页+翻页渲染）
 */
@Composable
fun BookDetailScreen(
    bookId: Long,
    onBack: () -> Unit = {},
    onNavigateToReader: ((List<Chapter>, chapterIndex: Int) -> Unit)? = null,  // ★ 跳转阅读器（带完整章节列表）
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    // 加载书籍详情和章节列表
    LaunchedEffect(bookId) {
        viewModel.loadBookDetail(bookId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryOrange
                )
            }
            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "加载失败", fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = uiState.error!!, fontSize = 13.sp, color = TextSecondary)
                }
            }
            uiState.book != null -> {
                BookDetailContent(
                    book = uiState.book!!,
                    chapters = uiState.chapters,
                    onChapterClick = { chapter, index ->
                        if (onNavigateToReader != null) {
                            onNavigateToReader(uiState.chapters, index)
                        } else {
                            viewModel.selectChapter(chapter)
                        }
                    },
                    selectedChapter = uiState.selectedChapter,
                    onBack = onBack,
                )
            }
        }
    }
}

// ==================== 详情内容区 ====================

@Composable
private fun BookDetailContent(
    book: Book,
    chapters: List<Chapter>,
    onChapterClick: (Chapter, Int) -> Unit,
    selectedChapter: Chapter?,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ===== 顶部导航栏：返回按钮 + 书名 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimary
                )
            }
            Text(
                text = "书籍详情",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        // ===== 头部：书名 + 作者 + 元数据 =====
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                text = book.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${book.author} · ${book.chapterCount}章",
                fontSize = 13.sp,
                color = TextSecondary
            )
            if (!book.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = book.description!!,
                    fontSize = 13.sp,
                    color = TextSecondary.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
        }

        // ===== 章节目录 / 正文显示切换 =====
        if (selectedChapter != null) {
            // 显示选中章节的完整正文
            ChapterContentView(
                chapter = selectedChapter,
                totalChapters = chapters.size,
                onBackToList = { /* TODO: 返回目录 */ }
            )
        } else {
            // 显示章节目录列表
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "目录",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "共${chapters.size}章",
                            fontSize = 12.sp,
                            color = TextHint
                        )
                    }
                }
                itemsIndexed(chapters, key = { _, chapter -> chapter.chapterIndex }) { index, chapter ->
                    ChapterItemRow(chapter = chapter, onClick = { onChapterClick(chapter, index) })
                }
            }
        }
    }
}

/** 单个章节行 */
@Composable
private fun ChapterItemRow(chapter: Chapter, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 章节序号圆圈
        Text(
            text = "${chapter.chapterIndex + 1}",
            fontSize = 12.sp,
            color = PrimaryOrange,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PrimaryOrange.copy(alpha = 0.1f)),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 章节标题
        Text(
            text = chapter.title,
            fontSize = 14.sp,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 字数
        Text(
            text = "${chapter.wordCount}字",
            fontSize = 11.sp,
            color = TextDisabled
        )
    }
}

/** 章节正文查看（验证解析结果的核心区域） */
@Composable
private fun ChapterContentView(
    chapter: Chapter,
    totalChapters: Int,
    onBackToList: () -> Unit,
) {
    Column(
        modifier = Modifier
//            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // 章节头部
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        ) {
            Text(
                text = "第${chapter.chapterIndex + 1}章",
                fontSize = 11.sp,
                color = PrimaryOrange,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "/ $totalChapters",
                fontSize = 11.sp,
                color = TextDisabled
            )
        }

        Text(
            text = chapter.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (chapter.volumeName != null) {
            Text(
                text = "【${chapter.volumeName}】",
                fontSize = 12.sp,
                color = PrimaryOrange.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Divider(color = DividerColor)

        Spacer(modifier = Modifier.height(8.dp))

        // 正文内容（可滚动）
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Text(
                    text = chapter.content.ifBlank { "(该章节无内容)" },
                    fontSize = 15.sp,
                    lineHeight = 26.sp,
                    color = TextPrimary,
                )
            }
        }
    }
}

