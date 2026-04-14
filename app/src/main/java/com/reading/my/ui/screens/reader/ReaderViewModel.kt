package com.reading.my.ui.screens.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reading.my.core.reader.engine.L2DatabaseCache
import com.reading.my.core.reader.engine.PageLayoutManager
import com.reading.my.core.reader.engine.RenderCache
import com.reading.my.core.reader.domain.ChapterPages
import com.reading.my.core.reader.domain.PageLayoutConfig
import com.reading.my.core.reader.domain.ReaderTheme
import com.reading.my.domain.model.Chapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 阅读器 ViewModel
 *
 * 职责：
 * 1. 管理当前章节状态（索引、分页数据、加载状态）
 * 2. 管理跨章跳转目标页（targetInitialPage：首页/末页/指定页）
 * 3. 执行章节分页加载（L1/L2 缓存）
 *
 * 核心设计：跨章跳转时原子化更新状态，避免 Compose recomposition 时序竞争。
 */
data class ReaderUiState(
    /** 当前活跃章节索引 */
    val activeChapterIndex: Int = 0,
    /** 当前章节的分页结果 */
    val chapterPages: ChapterPages? = null,
    /** 是否正在加载 */
    val isLoading: Boolean = true,
    /**
     * 目标初始页码：
     * - -1 → 末页（跨章回翻时使用，加载完成后根据 pageCount 计算）
     * -  0 → 首页（默认/跨章前进）
     * - >0 → 具体页码（未来扩展：记住阅读位置）
     */
    val targetInitialPage: Int = 0,
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val l2Cache: L2DatabaseCache?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    // 持有不可变配置数据（由 UI 层传入一次）
    private var chapters: List<Chapter> = emptyList()
    private var bookId: String = ""
    private var config: PageLayoutConfig? = null
    private var theme: ReaderTheme? = null

    /**
     * 初始化阅读器（从外部传入配置和初始章节）
     *
     * @param chapters   全部章节列表
     * @param startIndex 初始章节索引
     * @param bookId     书籍 ID（用于 L2 缓存 key）
     */
    fun initReader(chapters: List<Chapter>, startIndex: Int, bookId: String) {
        this.chapters = chapters
        this.bookId = bookId
        _uiState.value = ReaderUiState(
            activeChapterIndex = startIndex.coerceIn(0, chapters.size - 1),
            isLoading = true,
            targetInitialPage = 0,
        )
    }

    /**
     * 设置排版配置和主题（由 UI 层在 remember 中计算后传入）
     *
     * 必须在 initReader 之后、loadCurrentChapter 之前调用。
     */
    fun setConfig(config: PageLayoutConfig, theme: ReaderTheme) {
        this.config = config
        this.theme = theme
    }

    /**
     * 加载当前活跃章节的分页数据
     *
     * 在 LaunchedEffect(configHash) 中调用，当 config 或章节变化时重新执行。
     * 加载完成后自动解析 targetInitialPage（-1 → 末页）。
     */
    fun loadCurrentChapter(configHash: Int) {
        val currentConfig = config ?: return
        val currentTheme = theme ?: return
        val currentChapters = chapters
        val state = _uiState.value
        val currentChapter = currentChapters.getOrNull(state.activeChapterIndex)

        if (currentChapter == null) {
            _uiState.value = state.copy(isLoading = false, chapterPages = null)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val result = if (l2Cache != null && bookId.isNotEmpty()) {
                    l2Cache.getChapterPages(
                        bookId = bookId,
                        chapterIndex = currentChapter.chapterIndex,
                        configHash = configHash,
                        compute = {
                            val layoutManager = PageLayoutManager(currentConfig)
                            layoutManager.paginateChapter(
                                currentChapter.chapterIndex,
                                currentChapter.content
                            )
                        },
                    )
                } else {
                    val layoutManager = PageLayoutManager(currentConfig)
                    layoutManager.paginateChapter(
                        currentChapter.chapterIndex,
                        currentChapter.content
                    )
                }

                // 解析目标初始页码：-1 表示末页
                val resolvedInitialPage = when (_uiState.value.targetInitialPage) {
                    -1 -> (result.pageCount - 1).coerceAtLeast(0)
                    else -> _uiState.value.targetInitialPage.coerceAtLeast(0)
                }

                _uiState.update {
                    it.copy(
                        chapterPages = result,
                        isLoading = false,
                        targetInitialPage = resolvedInitialPage,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, chapterPages = null)
                }
            }
        }
    }

    /**
     * 往回翻 → 跳到上一章**末页**
     *
     * 原子操作：同时设置方向标记 + 切换章节 + 触发重载，
     * 不存在 Compose recomposition 时序竞争。
     */
    fun navigateToPrevChapter() {
        val state = _uiState.value
        val prevIndex = state.activeChapterIndex - 1
        if (prevIndex < 0 || prevIndex >= chapters.size) return

        _uiState.value = state.copy(
            activeChapterIndex = prevIndex,
            chapterPages = null,
            isLoading = true,
            targetInitialPage = -1, // ★ 标记：加载完后显示末页
        )
    }

    /**
     * 往前翻 → 跳到下一章**首页**
     */
    fun navigateToNextChapter() {
        val state = _uiState.value
        val nextIndex = state.activeChapterIndex + 1
        if (nextIndex < 0 || nextIndex >= chapters.size) return

        _uiState.value = state.copy(
            activeChapterIndex = nextIndex,
            chapterPages = null,
            isLoading = true,
            targetInitialPage = 0, // ★ 显示首页
        )
    }

    /**
     * 获取当前章节对象（供 UI 层显示标题等）
     */
    fun getCurrentChapter(): Chapter? = chapters.getOrNull(_uiState.value.activeChapterIndex)

    /**
     * 获取总章数
     */
    fun getTotalChapters(): Int = chapters.size
}
