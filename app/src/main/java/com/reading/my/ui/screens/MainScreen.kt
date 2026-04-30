package com.reading.my.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reading.my.ui.navigation.AppPage
import com.reading.my.ui.navigation.BottomNavItem
import com.reading.my.ui.navigation.NavigationState
import com.reading.my.ui.navigation.Screen
import com.reading.my.ui.theme.BackgroundGray
import com.reading.my.ui.theme.PrimaryOrange
import com.reading.my.ui.theme.TextSecondary
import com.reading.my.ui.screens.bookstore.BookstoreScreen
import com.reading.my.ui.screens.bookshelf.BookshelfScreen
import com.reading.my.ui.screens.bookshelf.BookDetailScreen
import com.reading.my.domain.model.Chapter
import com.reading.my.ui.screens.reader.ReaderScreen
import com.reading.my.ui.screens.profile.ProfileScreen
import com.reading.my.ui.screens.profile.ProfileActivityScreen
import com.reading.my.ui.screens.profile.EditProfileScreen
import com.reading.my.ui.screens.sync.SyncImportScreen
import android.util.Log

/**
 * 主界面 - 底部导航容器
 *
 * 结构：
 * ┌──────────────────────────────┐
 * │                              │
 * │      当前页面内容区域         │  ← 由 NavigationState 页面栈驱动
 *│                              │
 *├──────────────────────────────┤
 *│ 📚书架  │📖书库  │👥同好  │👤我的 │  ← 仅在根 Tab 时显示底部导航栏
 *└──────────────────────────────┘
 *
 * ## 导航架构
 *
 * 用 [NavigationState] 页面栈管理所有页面跳转：
 * - **Root Tab**（栈底）：书架 / 书库 / 圈子 / 我的 → 按返回键退出 APP
 * - **Sub Page**（压栈）：详情 / 阅读器 / 动态 / 编辑资料 / 同步 → 按返回键 pop 回上一级
 *
 * 示例：`[书架] → [书架, 详情(1)] → [书架, 详情(1), 阅读器]`
 */
@Composable
fun MainScreen(
    userAvatarUrl: String? = null,
    onNavigateToLogin: () -> Unit = {},
    pendingSyncPayload: String? = null,
) {
    val items = BottomNavItem.tabs

    // ===== 核心导航状态：用栈管理所有页面 =====
    val navState = remember { NavigationState(AppPage.Bookshelf) }

    // 当从 Cwriter 收到同步 Intent 时，压入同步页面
    LaunchedEffect(pendingSyncPayload) {
        if (!pendingSyncPayload.isNullOrEmpty()) {
            navState.syncPayloadJson = pendingSyncPayload
            navState.push(AppPage.SyncImport)
        }
    }

    // ===== 统一返回键处理：栈非空则 pop，空（仅剩 Root Tab）则放行给系统退出 APP =====
    BackHandler(enabled = navState.hasSubPages) {
        val popped = navState.pop()
        Log.d("MainScreen", "⬅️ 返回键: 弹出=${popped}, 当前栈=[${navState}]")
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundGray,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // 仅在根 Tab 层级（无次级页面）时显示底部导航栏
        bottomBar = {
            if (!navState.hasSubPages) {
                BottomNavigationBar(
                    items = items,
                    selectedRoute = navState.rootTab.toScreenRoute(),
                    userAvatarUrl = userAvatarUrl,
                    onItemSelected = { route ->
                        val newRoot = route.toAppPage()
                        Log.d("MainScreen", "🔄 切换Tab: ${navState.rootTab} → $newRoot")
                        navState.replaceRoot(newRoot)
                    }
                )
            }
        }
    ) { innerPadding ->
        // 全屏页面（详情/阅读器等）不应用 Scaffold bottom padding
        val isFullScreen = navState.hasSubPages
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isFullScreen) Modifier
                    else Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
        ) {
            // 用当前页面作为 AnimatedContent 目标驱动转场动画
            val currentPage = navState.current
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    // 进入次级页面：右侧滑入；返回：左侧滑出
                    if (!targetState.isRootTab()) {
                        slideInHorizontally(initialOffsetX = { it }) +
                                fadeIn() togetherWith
                                slideOutHorizontally(targetOffsetX = { -it }) +
                                fadeOut()
                    } else {
                        slideInHorizontally(initialOffsetX = { -it }) +
                                fadeIn() togetherWith
                                slideOutHorizontally(targetOffsetX = { it }) +
                                fadeOut()
                    }
                },
                label = "pageTransition"
            ) { page ->
                RenderCurrentPage(
                    page = page,
                    navState = navState,
                )
            }
        }
    }
}

/**
 * 渲染当前页面（根据栈顶的 AppPage 分发）
 */
@Composable
private fun RenderCurrentPage(
    page: AppPage,
    navState: NavigationState,
) {
    when (page) {
        // ==================== 次级子页面 ====================

        is AppPage.EditProfile -> EditProfileScreen(
            onBack = { navState.pop() }
        )

        is AppPage.ProfileActivity -> ProfileActivityScreen(
            isSelf = true,
            onBack = { navState.pop() },
            onOpenEditProfile = { navState.push(AppPage.EditProfile) }
        )

        is AppPage.Reader -> {
            ReaderScreen(
                chapters = page.chapters,
                currentChapterIndex = page.chapterIndex,
                bookTitle = page.bookTitle,
                bookId = page.bookId,
                onBack = { navState.pop() },
                onChapterChange = { newIdx ->
                    // 翻页/跨章：更新 Reader 页面的 chapterIndex，不产生新页面（不压栈）
                    updateReaderChapterIndex(navState, newIdx)
                },
            )
        }

        is AppPage.BookDetail -> {
            // 从栈中查找关联的 bookId 用于 Reader 的 bookId 参数
            BookDetailScreen(
                bookId = page.bookId,
                onBack = { navState.pop() },
                onNavigateToReader = { chapters, chapterIndex ->
                    navState.push(AppPage.Reader(
                        chapters = chapters,
                        chapterIndex = chapterIndex,
                        bookId = page.bookId.toString(),
                        bookTitle = "书籍",
                    ))
                }
            )
        }

        is AppPage.SyncImport -> SyncImportScreen(
            payloadJson = navState.syncPayloadJson,
            onBack = { navState.pop() }
        )

        // ==================== 顶级 Root Tab 页面 ====================

        is AppPage.Bookshelf -> BookshelfTab(
            onNavigateToDetail = { bid ->
                navState.push(AppPage.BookDetail(bookId = bid))
            },
            onNavigateToSync = {
                navState.syncPayloadJson = null
                navState.push(AppPage.SyncImport)
            }
        )

        is AppPage.Bookstore -> BookstoreTab()

        is AppPage.Community -> CommunityTab()

        is AppPage.Profile -> ProfileScreen(
            onNavigateToProfile = { navState.push(AppPage.ProfileActivity) }
        )
    }
}

/**
 * 更新阅读器中的章节索引（原地替换栈顶 Reader，不产生新入栈）
 *
 * 这是因为翻页/跨章是阅读器内部状态变化，不应产生新的历史记录。
 * 用户按返回应该回到书籍详情页，而不是逐章回退。
 */
private fun updateReaderChapterIndex(navState: NavigationState, newIndex: Int) {
    val current = navState.current
    if (current is AppPage.Reader) {
        navState.replaceTop(current.copy(chapterIndex = newIndex))
    }
}

// ==================== 路由转换工具函数 ====================

/** Screen 路由字符串 → AppPage */
private fun String.toAppPage(): AppPage = when (this) {
    Screen.Bookshelf.route -> AppPage.Bookshelf
    Screen.Bookstore.route -> AppPage.Bookstore
    Screen.Community.route -> AppPage.Community
    Screen.Profile.route -> AppPage.Profile
    else -> AppPage.DEFAULT_ROOT
}

/** AppPage → Screen 路由字符串（用于底部导航选中态） */
private fun AppPage.toScreenRoute(): String = when (this) {
    is AppPage.Bookshelf -> Screen.Bookshelf.route
    is AppPage.Bookstore -> Screen.Bookstore.route
    is AppPage.Community -> Screen.Community.route
    is AppPage.Profile -> Screen.Profile.route
    else -> Screen.Bookshelf.route  // 不应到达（次级页面无底部导航）
}

// ==================== 底部导航栏（参考"我的"页面样式：圆角+毛玻璃+FILL图标）====================

@Composable
private fun BottomNavigationBar(
    items: List<BottomNavItem>,
    selectedRoute: String,
    userAvatarUrl: String?,
    onItemSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
//            .navigationBarsPadding(),
        color = Color.White,
        tonalElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = selectedRoute == item.route
                val isProfileItem = item is BottomNavItem.Profile

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onItemSelected(item.route) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 图标
                    if (isProfileItem && !userAvatarUrl.isNullOrBlank()) {
                        // "我的"有头像时显示圆形头像
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(PrimaryOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "我",
                                color = PrimaryOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (isSelected) item.iconVectorFilled() else item.iconVectorOutlined(),
                            contentDescription = item.title,
                            tint = if (isSelected) PrimaryOrange else TextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    // 标签文字
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) PrimaryOrange else TextSecondary.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/** BottomNavItem 的描边图标（未选中态） */
private fun BottomNavItem.iconVectorOutlined(): ImageVector = when (this) {
    is BottomNavItem.Bookshelf -> Icons.Outlined.MenuBook
    is BottomNavItem.Bookstore -> Icons.Outlined.Explore
    is BottomNavItem.Community -> Icons.Outlined.Groups
    is BottomNavItem.Profile -> Icons.Outlined.Person
}

/** BottomNavItem 的实心图标（选中态） */
private fun BottomNavItem.iconVectorFilled(): ImageVector = when (this) {
    is BottomNavItem.Bookshelf -> Icons.AutoMirrored.Filled.LibraryBooks
    is BottomNavItem.Bookstore -> Icons.Default.Search
    is BottomNavItem.Community -> Icons.Default.Groups
    is BottomNavItem.Profile -> Icons.Default.Person
}

/** 头像占位符（后续替换为真实头像加载） */
@Composable
private fun ProfileAvatarPlaceholder(username: String) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(PrimaryOrange.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = username.first().uppercase(),
            color = PrimaryOrange,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ==================== 书架页（默认首页）====================

/** 书架页 - 默认首页（书架页面） */
@Composable
private fun BookshelfTab(
    onNavigateToDetail: (Long) -> Unit = {},
    onNavigateToSync: () -> Unit = {},
) {
    BookshelfScreen(
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToSync = onNavigateToSync
    )
}

// ==================== 书库/发现页 ====================

@Composable
private fun BookstoreTab() {
    BookstoreScreen()
}

// ==================== 同好/圈子页 ====================

@Composable
private fun CommunityTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        Text(text = "👥 同好圈子", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
        Spacer(modifier = Modifier.height(16.dp))
        PlaceholderContent("发现 · 加入圈子 · 分享作品")
    }
}

// ==================== 通用组件 ====================

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) PrimaryOrange else Color.White,
        onClick = onClick
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            fontSize = 13.sp,
            color = if (selected) Color.White else Color(0xFF666666),
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun PlaceholderContent(description: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = description, textAlign = TextAlign.Center, fontSize = 14.sp, color = Color(0xFFCCCCCC))
    }
}
