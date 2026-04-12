package com.reading.my.ui.screens.bookshelf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.reading.my.domain.model.Book
import com.reading.my.ui.theme.BackgroundGray
import com.reading.my.ui.theme.PrimaryOrange
import com.reading.my.ui.theme.TextHint
import com.reading.my.ui.theme.TextPrimary
import com.reading.my.ui.theme.TextSecondary

/**
 * 书架页面（V3 — 真实文件选择 + 真实数据）
 *
 * 纯 UI 层：Composable + 布局 + 用户交互回调
 * 业务逻辑委托给 BookshelfViewModel（独立文件）
 */
@Composable
fun BookshelfScreen(
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: BookshelfViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var selectedTagIndex by remember { mutableIntStateOf(0) }

    // ===== SAF 文件选择器（docx 格式过滤） =====
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importFromUri(uri)
        }
    }

    // 监听书籍列表数据
    LaunchedEffect(Unit) {
        viewModel.loadBooks()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // ===== 菜单栏：标题 + 下拉菜单 =====
        ShelfHeaderBar(
            onImportDocx = {
                filePicker.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            },
            onClearCache = { viewModel.clearAllBooks() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ===== 筛选栏：Tag列表 + 编辑按钮 =====
        ShelfFilterBar(
            tags = shelfTags,
            selectedIndex = selectedTagIndex,
            onTagSelected = { selectedTagIndex = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ===== 书籍网格（3列，真实数据） =====
        when {
            uiState.isLoading && uiState.books.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryOrange)
                }
            }
            uiState.books.isEmpty() -> {
                EmptyShelfView(onImport = {
                    filePicker.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                })
            }
            else -> {
                ShelfBookGrid(
                    books = uiState.books,
                    onBookClick = { book ->
                        if (book.id > 0L) onNavigateToDetail(book.id)
                    },
                )
            }
        }

        // 导入结果提示
        uiState.importMessage?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = msg,
                fontSize = 12.sp,
                color = PrimaryOrange,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // 导入中状态
        if (uiState.isImporting) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = PrimaryOrange,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "正在解析文件...", fontSize = 12.sp, color = TextHint)
            }
        }
    }
}

// ==================== 菜单栏 ====================

@Composable
private fun ShelfHeaderBar(
    onImportDocx: () -> Unit,
    onClearCache: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "书架", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.weight(1f))
        Box {
            Text(
                text = "更多",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier
                    .clickable { showMenu = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color.White)
            ) {
                DropdownMenuItem(
                    text = { Text("导入小说", fontSize = 14.sp) },
                    onClick = { showMenu = false; onImportDocx() }
                )
                DropdownMenuItem(
                    text = { Text("清空缓存", fontSize = 14.sp) },
                    onClick = { showMenu = false; onClearCache() }
                )
            }
        }
    }
}

// ==================== 筛选栏 ====================

data class ShelfTag(val name: String)
private val shelfTags = listOf(ShelfTag("本地小说"), ShelfTag("在线小说"))

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShelfFilterBar(tags: List<ShelfTag>, selectedIndex: Int, onTagSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            tags.forEachIndexed { index, tag ->
                val isSelected = index == selectedIndex
                Text(
                    text = tag.name,
                    fontSize = if (isSelected) 15.sp else 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) PrimaryOrange else TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onTagSelected(index) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "编辑",
            fontSize = 13.sp, fontWeight = FontWeight.Medium,
            color = PrimaryOrange,
            modifier = Modifier.clickable(enabled = false) { }.padding(horizontal = 4.dp, vertical = 6.dp)
        )
    }
}

// ==================== 空状态 ====================

@Composable
private fun EmptyShelfView(onImport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "📖", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "书架空空如也", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "点击下方按钮导入你的 docx 小说文件", fontSize = 13.sp, color = TextHint)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "+ 选择 docx 文件",
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(PrimaryOrange)
                .clickable { onImport() }
                .padding(horizontal = 24.dp, vertical = 10.dp)
        )
    }
}

// ==================== 书籍网格 ====================

@Composable
private fun ShelfBookGrid(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(books, key = { it.id.toString() }) { book ->
            ShelfBookCard(book = book, onClick = { onBookClick(book) })
        }
    }
}

/** 单个书籍卡片：封面图 + 标题 + 作者 */
@Composable
private fun ShelfBookCard(book: Book, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ===== 封面图区域（纯色占位，后续替换为真实封面） =====
        Box(
            modifier = Modifier
                .size(width = 90.dp, height = 120.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF0ECF5)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "",
                fontSize = 32.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ===== 书名（最多2行，约6字/行） =====
        Text(
            text = book.title.ifBlank { "未命名" },
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(2.dp))

        // ===== 作者名（1行，灰色透明，约8字） =====
        Text(
            text = book.author.ifBlank { "未知作者" },
            fontSize = 12.sp,
            color = TextHint.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )
    }
}

// ==================== @Preview ====================

@Preview(name = "书架页面预览", showBackground = true, backgroundColor = 0xFFF5F5F5L)
@Composable
private fun BookshelfScreenPreview() {
    BookshelfScreen()
}
