package com.reading.my.ui.screens.bookshelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reading.my.domain.model.Book
import com.reading.my.domain.model.Chapter
import com.reading.my.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 书籍详情页 ViewModel
 *
 * 职责：
 * 1. 加载书籍详情 + 章节列表
 * 2. 管理选中章节状态（目录 ↔ 正文切换）
 */
data class BookDetailUiState(
    val isLoading: Boolean = true,
    val book: Book? = null,
    val chapters: List<Chapter> = emptyList(),
    val selectedChapter: Chapter? = null,
    val error: String? = null,
)

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val bookRepository: BookRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    fun loadBookDetail(bookId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val book = bookRepository.getBookById(bookId)
                val chapters = if (book != null) bookRepository.getChaptersByBookId(bookId) else emptyList()

                if (book == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "书籍不存在")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        book = book,
                        chapters = chapters,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "未知错误")
            }
        }
    }

    fun selectChapter(chapter: Chapter) {
        _uiState.value = _uiState.value.copy(selectedChapter = chapter)
    }
}
