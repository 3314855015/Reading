package com.reading.my.ui.screens.bookshelf

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reading.my.domain.model.Book
import com.reading.my.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 书架页面 ViewModel
 *
 * 职责：
 * 1. 加载/观察书籍列表（Flow → UiState）
 * 2. 处理导入操作（从 SAF URI 导入 docx）
 * 3. 处理清空缓存操作
 *
 * 注意：ContentResolver 不在这里持有，由 UI 层传入 URI 后调用 Repository。
 */
data class BookshelfUiState(
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val books: List<Book> = emptyList(),
    val importMessage: String? = null,
)

@HiltViewModel
class BookshelfViewModel @Inject constructor(
    private val bookRepository: BookRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookshelfUiState())
    val uiState: StateFlow<BookshelfUiState> = _uiState.asStateFlow()

    fun loadBooks() {
        viewModelScope.launch {
            bookRepository.observeAllBooks().collectLatest { books ->
                _uiState.value = _uiState.value.copy(isLoading = false, books = books)
            }
        }
    }

    /**
     * 从 SAF 文件选择器的 URI 导入 docx 文件
     *
     * @param uri SAF 返回的文件 URI
     */
    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(importMessage = null, isImporting = true)

            try {
                Log.i("BookshelfVM", "开始导入 URI: $uri")

                val book = bookRepository.importFromUri(uri, authorName = "阅读者")

                if (book != null) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importMessage = "✅ 导入成功：「${book.title}」${book.chapterCount}章"
                    )
                    Log.i("BookshelfVM", "导入成功: id=${book.id}, title=${book.title}")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importMessage = "❌ 解析失败（请确认是有效的 docx 文件）"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isImporting = false, importMessage = "❌ 异常: ${e.message}")
                Log.e("BookshelfVM", "导入异常", e)
            }
        }
    }

    fun clearAllBooks() {
        viewModelScope.launch {
            bookRepository.deleteAllBooks()
            _uiState.value = _uiState.value.copy(importMessage = "🗑 已清空全部书籍")
        }
    }
}
