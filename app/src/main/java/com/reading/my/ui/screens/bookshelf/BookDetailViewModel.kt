package com.reading.my.ui.screens.bookshelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reading.my.data.local.UserSessionManager
import com.reading.my.domain.model.Book
import com.reading.my.domain.model.Chapter
import com.reading.my.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookDetailUiState(
    val isLoading: Boolean = true,
    val book: Book? = null,
    val chapters: List<Chapter> = emptyList(),
    val error: String? = null,
    // 用户信息（顶部展示）
    val username: String = "",
    val userAvatarUri: String? = null,
    // 编辑模态框
    val showEditTitle: Boolean = false,
    val showEditDesc: Boolean = false,
    // 封面裁剪页
    val showCoverCrop: Boolean = false,
    val pendingCoverUri: String? = null,
)

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val sessionManager: UserSessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.sessionInfoFlow.firstOrNull()?.let { session ->
                _uiState.update {
                    it.copy(
                        username = session.username.ifBlank { session.email.substringBefore("@") },
                        userAvatarUri = session.avatar?.takeIf { a -> a.isNotBlank() }
                    )
                }
            }
        }
    }

    fun loadBookDetail(bookId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val book = bookRepository.getBookById(bookId)
                val chapters = if (book != null) bookRepository.getChaptersByBookId(bookId) else emptyList()
                if (book == null) {
                    _uiState.update { it.copy(isLoading = false, error = "书籍不存在") }
                } else {
                    _uiState.update { it.copy(isLoading = false, book = book, chapters = chapters) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "未知错误") }
            }
        }
    }

    // ── 封面 ──────────────────────────────────────────────────
    /** 用户从相册选择了图片，进入裁剪页 */
    fun onCoverImagePicked(uri: String) {
        _uiState.update { it.copy(pendingCoverUri = uri, showCoverCrop = true) }
    }

    /** 裁剪完成，保存封面到本地 DB */
    fun saveCover(context: android.content.Context, base64: String) {
        val book = _uiState.value.book ?: run { android.util.Log.w("CoverSave", "book is null!"); return }
        viewModelScope.launch {
            // Base64 → 本地文件 → file:// URI（Coil 可靠加载本地文件）
            val coverUri = com.reading.my.data.local.ImageFileHelper.saveCoverFromBase64(context, base64, book.id)
                ?: run { android.util.Log.e("CoverSave", "保存封面文件失败"); return@launch }
            bookRepository.updateBookCover(book.id, coverUri)
            _uiState.update {
                it.copy(
                    book = book.copy(coverPath = coverUri),
                    showCoverCrop = false,
                    pendingCoverUri = null
                )
            }
        }
    }

    fun dismissCoverCrop() = _uiState.update { it.copy(showCoverCrop = false, pendingCoverUri = null) }

    // ── 元数据编辑 ────────────────────────────────────────────
    fun showEditTitle() = _uiState.update { it.copy(showEditTitle = true) }
    fun showEditDesc() = _uiState.update { it.copy(showEditDesc = true) }
    fun dismissEdit() = _uiState.update { it.copy(showEditTitle = false, showEditDesc = false) }

    fun saveTitle(newTitle: String) {
        val book = _uiState.value.book ?: return
        viewModelScope.launch {
            bookRepository.updateBookMeta(book.id, newTitle.trim(), book.description)
            _uiState.update { it.copy(book = book.copy(title = newTitle.trim()), showEditTitle = false) }
        }
    }

    fun saveDescription(newDesc: String) {
        val book = _uiState.value.book ?: return
        viewModelScope.launch {
            bookRepository.updateBookMeta(book.id, book.title, newDesc.trim().ifBlank { null })
            _uiState.update { it.copy(book = book.copy(description = newDesc.trim().ifBlank { null }), showEditDesc = false) }
        }
    }
}
