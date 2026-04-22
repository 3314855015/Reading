package com.reading.my.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.reading.my.ui.navigation.BottomNavItem
import com.reading.my.ui.navigation.Screen
import com.reading.my.ui.theme.BackgroundGray
import com.reading.my.ui.theme.PrimaryOrange
import com.reading.my.ui.screens.home.HomeScreen
import com.reading.my.ui.screens.bookstore.BookstoreScreen
import com.reading.my.ui.screens.bookshelf.BookshelfScreen
import com.reading.my.ui.screens.bookshelf.BookDetailScreen
import com.reading.my.domain.model.Chapter
import com.reading.my.ui.screens.reader.ReaderScreen
import com.reading.my.ui.screens.profile.ProfileScreen

/**
 * 主界面 - 底部导航容器
 *
 * 结构：
 * ┌──────────────────────────────┐
 * │                              │
 * │      当前 Tab 内容区域        │  ← 动态切换：书架/书库/同好/我的
 *│                              │
 *├──────────────────────────────┤
 *│ 📚书架  │📖书库  │👥同好  │👤我的 │  ← 底部导航栏
 *└──────────────────────────────┘
 *
 * 登录后默认显示【书架】Tab。
 * "我的"Tab 有头像时用圆形头像代替图标。
 */
@Composable
fun MainScreen(
    userAvatarUrl: String? = null,
    onNavigateToLogin: () -> Unit = {},
) {
    val items = BottomNavItem.tabs
    var selectedRoute by remember { mutableStateOf(Screen.Bookshelf.route) }
    
    // 书籍详情导航状态
    var selectedBookId by remember { mutableStateOf<Long?>(null) }
    // 阅读器导航状态：(章节列表, 当前章节索引)
    var readerState by remember { mutableStateOf<Pair<List<Chapter>, Int>?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundGray,
        // 仅在非详情页时显示底部导航栏
        bottomBar = {
            if (selectedBookId == null) {
                BottomNavigationBar(
                    items = items,
                    selectedRoute = selectedRoute,
                    userAvatarUrl = userAvatarUrl,
                    onItemSelected = { route ->
                        selectedRoute = route
                    }
                )
            }
        }
    ) { innerPadding ->
        // 内容区域：根据选中的路由显示对应页面
        // 注意：BookstoreScreen 自行处理状态栏占位，故仅保留底部导航栏的 padding
        Box(
            modifier = Modifier
                .fillMaxSize()
                 .padding(
                     top = if (selectedRoute == Screen.Bookstore.route) 0.dp
                     else innerPadding.calculateTopPadding(),
                     bottom = innerPadding.calculateBottomPadding()
                 )
        ) {
            AnimatedContent(
                targetState = readerState to (selectedBookId to selectedRoute),
                transitionSpec = {
                    // 进入详情页/阅读器：从右侧滑入 + 淡入
                    // 返回：向左滑出 + 淡出
                    if (targetState.first != null) {
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
            ) { (readerData, bookIdAndRoute) ->
                val (bookId, route) = bookIdAndRoute

                when {
                    // 阅读器（最顶层）
                    readerData != null -> {
                        val (chapters, currentIdx) = readerData
                        ReaderScreen(
                            chapters = chapters,
                            currentChapterIndex = currentIdx,
                            bookTitle = "书籍",
                            bookId = bookId?.toString() ?: "",
                            onBack = { readerState = null },
                            onChapterChange = { newIdx ->
                                readerState = chapters to newIdx
                            },
                        )
                    }
                    // 书籍详情页（覆盖在书架之上）
                    bookId != null -> {
                        BookDetailScreen(
                            bookId = bookId,
                            onBack = { selectedBookId = null },
                            onNavigateToReader = { chapters, chapterIndex ->
                                readerState = chapters to chapterIndex
                            }
                        )
                    }
                    route == Screen.Bookshelf.route -> BookshelfTab(
                        onNavigateToDetail = { bid -> selectedBookId = bid }
                    )
                    route == Screen.Bookstore.route -> BookstoreTab()
                    route == Screen.Community.route -> CommunityTab()
                    route == Screen.Profile.route -> ProfileScreen()
                }
            }
        }
    }
}

// ==================== 底部导航栏 ====================

@Composable
private fun BottomNavigationBar(
    items: List<BottomNavItem>,
    selectedRoute: String,
    userAvatarUrl: String?,
    onItemSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isProfileItem = item is BottomNavItem.Profile

            NavigationBarItem(
                icon = {
                    if (isProfileItem && !userAvatarUrl.isNullOrBlank()) {
                        ProfileAvatarPlaceholder(username = "我")
                    } else {
                        Icon(
                            imageVector = item.iconVector(),
                            contentDescription = item.title,
                            tint = if (selectedRoute == item.route) PrimaryOrange
                            else Color.Gray.copy(alpha = 0.6f)
                        )
                    }
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 11.sp,
                        color = if (selectedRoute == item.route) PrimaryOrange
                        else Color.Gray.copy(alpha = 0.6f),
                        fontWeight = if (selectedRoute == item.route) FontWeight.Medium else FontWeight.Normal
                    )
                },
                selected = selectedRoute == item.route,
                onClick = { onItemSelected(item.route) }
            )
        }
    }
}

private fun BottomNavItem.iconVector(): ImageVector = when (this) {
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
private fun BookshelfTab(onNavigateToDetail: (Long) -> Unit = {}) {
    BookshelfScreen(onNavigateToDetail = onNavigateToDetail)
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
