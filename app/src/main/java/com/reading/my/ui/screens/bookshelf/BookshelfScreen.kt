package com.reading.my.ui.screens.bookshelf

import android.util.Log
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.reading.my.domain.model.Book
import com.reading.my.domain.repository.BookRepository
import com.reading.my.ui.theme.BackgroundGray
import com.reading.my.ui.theme.PrimaryOrange
import com.reading.my.ui.theme.TextHint
import com.reading.my.ui.theme.TextPrimary
import com.reading.my.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 书架页面（V2 — 接入真实数据）
 *
 * 数据来源：BookRepository → Room DB → UI
 * 导入流程：点击"导入小说" → DocxParser 解析测试文件 → 存DB → 刷新列表
 */
@Composable
fun BookshelfScreen(
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: BookshelfViewModel = hiltViewModel(),
) {
    var selectedTagIndex by remember { mutableIntStateOf(0) }

    // 监听书籍列表数据
    LaunchedEffect(Unit) {
        viewModel.loadBooks()
    }

    val uiState = viewModel.uiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // ===== 菜单栏：标题 + 下拉菜单 =====
        ShelfHeaderBar(
            onImportTest = { viewModel.importTestDocx() },
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
                EmptyShelfView(onImport = { viewModel.importTestDocx() })
            }
            else -> {
                ShelfBookGrid(
                    books = uiState.books,
                    onBookClick = { book ->
                        // TODO: 后续导航到 BookDetailScreen
                        Log.d("Bookshelf", "点击书籍: ${book.title} (id=${book.id})")
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
    }
}

// ==================== UI State & ViewModel ====================

data class BookshelfUiState(
    val isLoading: Boolean = false,
    val books: List<Book> = emptyList(),
    val importMessage: String? = null,
)

@HiltViewModel
class BookshelfViewModel @Inject constructor(
    private val bookRepository: BookRepository,
) : ViewModel() {

    var uiState by mutableStateOf(BookshelfUiState())
        private set

    fun loadBooks() {
        viewModelScope.launch {
            bookRepository.observeAllBooks().collectLatest { books ->
                uiState = uiState.copy(isLoading = false, books = books)
            }
        }
    }

    /**
     * 导入测试 docx 文件（模拟用户选择）
     *
     * 使用项目根目录下的测试文件: 测试docx导入渲染/_1772516010073.docx
     */
    fun importTestDocx() {
        viewModelScope.launch {
            uiState = uiState.copy(importMessage = null)

            // ★ 模拟：直接使用本地测试文件路径 ★
            // TODO: 正式版这里应该打开系统文件选择器
            val testFilePath = "/storage/emulated/0/测试docx导入渲染/_1772516010073.docx"

            try {
                Log.i("Bookshelf", "开始模拟导入: $testFilePath")

                val book = bookRepository.importBook(testFilePath, authorName = "阅读者")

                if (book != null) {
                    uiState = uiState.copy(
                        importMessage = "✅ 导入成功：「${book.title}」${book.chapterCount}章"
                    )
                    Log.i("Bookshelf", "导入成功: id=${book.id}, title=${book.title}")
                } else {
                    uiState = uiState.copy(importMessage = "❌ 解析失败（请确认测试文件存在）")
                }
            } catch (e: Exception) {
                uiState = uiState.copy(importMessage = "❌ 异常: ${e.message}")
                Log.e("Bookshelf", "导入异常", e)
            }
        }
    }

    fun clearAllBooks() {
        viewModelScope.launch {
            bookRepository.deleteAllBooks()
            uiState = uiState.copy(importMessage = "🗑 已清空全部书籍")
        }
    }
}

// ==================== 菜单栏 ====================

@Composable
private fun ShelfHeaderBar(
    onImportTest: () -> Unit,
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
                    text = { Text("导入小说（测试）", fontSize = 14.sp) },
                    onClick = { showMenu = false; onImportTest() }
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
        Text(text = "点击「更多→导入小说」来添加你的第一本书", fontSize = 13.sp, color = TextHint)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "+ 导入测试文件",
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

/** 单个书籍卡片 */
@Composable
private fun ShelfBookCard(book: Book, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 90.dp, height = 120.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF0ECF5)),
            contentAlignment = Alignment.Center
        ){

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = book.title.ifBlank { "未命名" },
            fontSize = 13.sp, fontWeight = FontWeight.Medium,
            color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = book.author.ifBlank { "未知作者" },
            fontSize = 11.sp, color = TextHint,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        }
    }
}

// ==================== @Preview ====================

@Preview(name = "书架页面预览", showBackground = true, backgroundColor = 0xFFF5F5F5L)
@Composable
private fun BookshelfScreenPreview() {
    BookshelfScreen()
}
