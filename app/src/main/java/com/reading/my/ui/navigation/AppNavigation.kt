package com.reading.my.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.reading.my.domain.model.Chapter



/**
 * 在 AppPage 加 data class Xxx(...) : AppPage()
 * 在 RenderCurrentPage() 的 when 加分支，用 navState.push/pop
 * 不需要加 BackHandler — 自动由栈管理
 */

/**
 * 应用页面定义（密封类，覆盖所有可导航的页面）
 *
 * ## 页面层级关系
 *
 * ### 顶级页面（Root Tabs）— 栈底元素
 *   Bookshelf / Bookstore / Community / Profile
 *   → 在这些页面按返回键 → 退出应用（返回桌面）
 *
 * ### 次级页面（Sub Pages）— 压入栈中
 *   从顶级页面进入的子页面，按父子关系压栈：
 *
 *   书架(Bookshelf)
 *     └→ BookDetail(bookId)        ← 点击书籍进入
 *           └→ Reader(...)          ← 从详情页开始阅读
 *
 *   我的(Profile)
 *     └→ ProfileActivity           ← 点击动态入口
 *           └→ EditProfile          ← 从动态页点编辑资料
 *
 *   同步(SyncImport)               ← 从书架菜单或 Intent 进入
 *
 * ## 翻页不压栈
 *   阅读器内的章节切换（翻页/跨章）是同一 Reader 页面的内部状态变化，
 *   不产生新的 AppPage 入栈。
 */
sealed class AppPage {

    // ==================== 顶级 Tab 页面 ====================

    /** 书架（默认首页） */
    data object Bookshelf : AppPage()

    /** 书库/发现 */
    data object Bookstore : AppPage()

    /** 同好/圈子 */
    data object Community : AppPage()

    /** 我的 */
    data object Profile : AppPage()

    // ==================== 次级子页面 ====================

    /** 书籍详情页（从书架点击书籍进入） */
    data class BookDetail(val bookId: Long) : AppPage()

    /**
     * 阅读器页（从书籍详情页开始阅读）
     *
     * 注意：阅读器内的翻页/跨章翻页不产生新页面，
     * chapters + chapterIndex 是阅读器内部状态。
     */
    data class Reader(
        val chapters: List<Chapter>,
        val chapterIndex: Int,
        val bookId: String,
        val bookTitle: String,
    ) : AppPage()

    /** 个人动态页（从"我的"Tab进入） */
    data object ProfileActivity : AppPage()

    /** 个人资料编辑页（从个人动态页进入） */
    data object EditProfile : AppPage()

    /** 同步导入页（从书架菜单或 Cwriter Intent 进入） */
    data object SyncImport : AppPage()

    /** 是否为顶级 Tab 页面（栈底） */
    fun isRootTab(): Boolean = this is Bookshelf || this is Bookstore || this is Community || this is Profile

    companion object {
        /** 默认根页面 */
        val DEFAULT_ROOT = Bookshelf
    }
}

/**
 * 导航状态：用栈管理页面层级
 *
 * ## 设计原则
 * 1. 栈永远至少有一个元素（当前 Root Tab）
 * 2. push(page) → 压入次级页面
 * 3. pop() → 弹出顶层，返回上一级
 * 4. replaceRoot(tab) → 切换底部 Tab（清空次级页面，替换根）
 * 5. 栈只有 1 个 Root 元素时按返回 → 应退出应用
 *
 * ## 示例状态演变
 * ```
 * 初始:       [Bookshelf]
 * 点书籍:      [Bookshelf, BookDetail(1)]
 * 开始阅读:    [Bookshelf, BookDetail(1), Reader(...)]
 * 返回:        [Bookshelf, BookDetail(1)]
 * 返回:        [Bookshelf]
 * 切到"我的":   [Profile]            ← replaceRoot
 * 点动态:      [Profile, ProfileActivity]
 * ```
 */
/**
 * 导航状态：用 [SnapshotStateList] 栈管理页面层级
 *
 * ## Compose 可观察性
 *
 * 内部使用 **mutableStateListOf** 而非普通 MutableList，
 * 使 Compose 快照系统能自动追踪 add/remove/clear/set 等列表变更，
 * 无需额外的 version 手动标记。
 *
 * 当 MainScreen 中读取 navState.current / hasSubPages / rootTab 时，
 * Compose 会自动建立对这些快照状态的依赖，栈变化时触发重组。
 *
 * ## 设计原则
 * 1. 栈永远至少有一个元素（当前 Root Tab）
 * 2. push(page) → 压入次级页面
 * 3. pop() → 弹出顶层，返回上一级
 * 4. replaceRoot(tab) → 切换底部导航（清空次级页面，替换根）
 * 5. 栈只有 1 个 Root 元素时按返回 → 应退出应用
 *
 * ## 示例状态演变
 * ```
 * 初始:       [Bookshelf]
 * 点书籍:      [Bookshelf, BookDetail(1)]
 * 开始阅读:    [Bookshelf, BookDetail(1), Reader(...)]
 * 返回:        [Bookshelf, BookDetail(1)]
 * 返回:        [Bookshelf]
 * 切到"我的":   [Profile]            ← replaceRoot
 * 点动态:      [Profile, ProfileActivity]
 * ```
 */
class NavigationState(
    initialRoot: AppPage = AppPage.DEFAULT_ROOT,
) {
    /**
     * 页面栈：index 0 = 根 Tab，末尾 = 当前显示页面
     *
     * 使用 **SnapshotStateList**（Compose 可观察列表），
     * 每次 add/remove/clear/set 操作都会通知 Compose 快照系统，
     * 使依赖 navState.current / hasSubPages / rootTab 的 composable 自动重组。
     */
    val _stack: SnapshotStateList<AppPage> = mutableStateListOf(initialRoot)

    /** 观察者用的不可变快照 */
    val stack: List<AppPage> get() = _stack.toList()

    /** 当前显示的页面（栈顶）——Compose 可观察（读 _stack.last() 触发快照读取） */
    val current: AppPage get() = _stack.last()

    /** 当前根 Tab —— Compose 可观察 */
    val rootTab: AppPage get() = _stack.first()

    /** 是否有次级页面在栈中 —— Compose 可观察 */
    val hasSubPages: Boolean get() = _stack.size > 1

    /** 同步导入的 payload JSON（非页面状态，挂在此处方便访问） */
    var syncPayloadJson: String? = null

    /**
     * 压入次级页面
     *
     * @param page 要显示的新页面
     */
    fun push(page: AppPage) {
        require(!page.isRootTab()) { "Root tabs should use replaceRoot(), not push()" }
        _stack.add(page)
    }

    /**
     * 弹出当前页面，返回上一级
     *
     * @return 被弹出的页面，如果只剩 Root 则返回 null
     */
    fun pop(): AppPage? {
        if (_stack.size <= 1) return null  // 不能弹出最后一个 Root
        return _stack.removeAt(_stack.lastIndex)
    }

    /**
     * 替换根 Tab（切换底部导航时使用）
     *
     * 清空所有次级页面，设置新的根页面。
     */
    fun replaceRoot(newRoot: AppPage) {
        require(newRoot.isRootTab()) { "replaceRoot() 只接受 Root Tab" }
        _stack.clear()
        _stack.add(newRoot)
    }

    /**
     * 原地替换栈顶页面（不产生新入栈/出栈记录）
     *
     * 用例：阅读器内翻页/跨章 → 更新 Reader 的 chapterIndex，
     * 但用户按返回应直接回详情页，而不是逐章回退。
     */
    fun replaceTop(newTop: AppPage) {
        require(_stack.isNotEmpty()) { "栈为空，无法 replaceTop" }
        _stack[_stack.lastIndex] = newTop
    }

    /** 获取特定类型的栈中页面（如查找 BookDetail 的 bookId） */
    inline fun <reified T : AppPage> findInstanceOf(): T? {
        return _stack.filterIsInstance<T>().lastOrNull()
    }

    override fun toString(): String =
        _stack.joinToString(" → ") { it.simpleName() }
}

private fun AppPage.simpleName(): String = when (this) {
    is AppPage.Bookshelf -> "书架"
    is AppPage.Bookstore -> "书库"
    is AppPage.Community -> "圈子"
    is AppPage.Profile -> "我的"
    is AppPage.BookDetail -> "详情(${bookId})"
    is AppPage.Reader -> "阅读器"
    is AppPage.ProfileActivity -> "动态"
    is AppPage.EditProfile -> "编辑资料"
    is AppPage.SyncImport -> "同步"
}
