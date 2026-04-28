package com.reading.my.ui.screens.bookstore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HotelClass
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.reading.my.R
import com.reading.my.ui.theme.*

/**
 * 书库页面（V2 — UI重构版，参考 Library HTML设计稿）
 *
 * 视觉层次：
 * ┌──────────────────────────────────────┐
 * │ ░░ 沉浸式深色头图区域                │  ← 背景(blur) + 分类Tab + AI对话按钮
 * │     毛玻璃搜索栏                     │
 * │     热门轮播 Banner                  │  ← 指示器圆点
 * ├──────────────────────────────────────╮
 * │ 功能入口网格 (6列): 更新/排行/分类...  │  ← 橙色图标 + 文字标签
 * ├──────────────────────────────────────┤
 * │ 人气风向标              更多 >       │  ← 4列书籍封面网格
 * │ [书][书][书][书]                    │
 * ├──────────────────────────────────────┤
 * │ 热门书籍横向列表                     │  ← 封面 + 标题 + 元信息
 * └──────────────────────────────────────┘
 *
 * 业务功能保留：
 * - 类型切换（开源图书/同好小说）
 * - 搜索入口
 * - 书籍展示网格
 */
@Composable
fun BookstoreScreen() {
    var selectedCategory by remember { mutableIntStateOf(0) } // 0=轻小说, 1=漫画, 2=有声, 3=短篇

    // 整体一体滚动容器：头部 + 内容区一起滚动
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1b1c1c))
            .verticalScroll(scrollState)
    ) {
        // ===== 1. 沉浸式头部区域 =====
        BookstoreImmersiveHeader(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )

        // ===== 2. 内容区（浅色背景，随头部一起滚动） =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF9F9F9))
        ) {
            FunctionIconGrid()
            PopularBooksSection()
            HotBookListSection()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==================== 1. 沉浸式头部区域 ====================

@Composable
private fun BookstoreImmersiveHeader(
    selectedCategory: Int,
    onCategorySelected: (Int) -> Unit,
) {
    // 头部区域：限定高度 + 裁剪防溢出
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp) // 限制背景图最大高度
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
    ) {
        // 背景图（模糊 + Crop填充）
        Image(
            painter = painterResource(id = R.drawable.backround),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(16.dp), // 强模糊去细节
            contentScale = ContentScale.Crop
        )
        // 暗色遮罩层（确保文字可读）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            // 状态栏占位 + 内边距
            Spacer(modifier = Modifier.height(32.dp))

            // --- 顶部分类Tab + AI对话按钮 ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧分类Tab组
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val categories = listOf("轻小说", "漫画", "有声", "短篇")
                    categories.forEachIndexed { index, category ->
                        val isSelected = index == selectedCategory
                        Text(
                            text = category,
                            fontSize = 17.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.clickable { onCategorySelected(index) }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 右侧：AI对话按钮
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubble,
                            contentDescription = null,
                            tint = Color(0xFFff6419),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "对话",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- 毛玻璃搜索栏 ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable(enabled = false) { /* TODO: 打开搜索页 */ },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(14.dp))
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "搜索热门书籍",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- 热门轮播Banner ---
            HotBannerCarousel()
        }
    }
}

/** 热门轮播Banner */
@Composable
private fun HotBannerCarousel() {
    // TODO: Banner数据加载 - 需要从后端获取轮播数据
    // 当前使用占位UI
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(
                colors = listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.2f))
            ))
    ) {
        // Banner 内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "热门书籍推荐",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "精彩内容不容错过...",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666)
            )
        }

        // 指示器圆点
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (index == 0) Color.White else Color.White.copy(alpha = 0.4f))
                )
            }
        }
    }
}

// ==================== 2. 功能图标网格 ====================

data class FunctionIconItem(val icon: ImageVector, val label: String)

private val functionIcons = listOf(
    FunctionIconItem(Icons.Default.HotelClass, "今日更新"),
    FunctionIconItem(Icons.Default.GridView, "排行榜"),
    FunctionIconItem(Icons.Default.Category, "分类"),
    FunctionIconItem(Icons.Default.AutoStories, "书单"),
    FunctionIconItem(Icons.Default.CalendarToday, "男主"),
    FunctionIconItem(Icons.Default.Face, "女主")
)

@Composable
private fun FunctionIconGrid() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 12.dp)
            .background(Color(0xFFF9F9F9))
    ) {
        // 6列网格
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            functionIcons.forEachIndexed { index, item ->
                FunctionIconCard(
                    icon = item.icon,
                    label = item.label,
                    onClick = {
                        when (index) {
                            0 -> { /* TODO: 进入今日更新页 */ }
                            1 -> { /* TODO: 进入排行榜页 */ }
                            2 -> { /* TODO: 进入分类浏览页 */ }
                            3 -> { /* TODO: 进入书单页 */ }
                            4 -> { /* TODO: 进入男主分类 */ }
                            5 -> { /* TODO: 进入女主分类 */ }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FunctionIconCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(52.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = PrimaryOrange,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
    }
}

// ==================== 3. 人气风向标 ====================

@Composable
private fun PopularBooksSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "人气风向标",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(更新稳定有特点)",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Text(
                text = "更多",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryOrange,
                modifier = Modifier.clickable(enabled = false) { /* TODO: 进入更多人气榜单 */ }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4列书籍网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.height(180.dp), // 固定高度约一排
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(popularSampleBooks.take(4)) { book ->
                PopularBookItem(title = book.title)
            }
        }
    }
}

// ==================== 4. 热门书籍横向列表 ====================

@Composable
private fun HotBookListSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(hotListBooks) { book ->
                HotListBookItem(
                    title = book.title,
                    subtitle = book.subtitle
                )
            }
        }
    }
}

/** 人气风向标单个书籍 */
@Composable
private fun PopularBookItem(title: String) {
    Column(
        modifier = Modifier.clickable(enabled = false) { /* TODO: 进入书籍详情 */ },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 封面占位
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFe8e4df)), // stone-200
            contentAlignment = Alignment.Center
        ) {
            // TODO: 封面图加载
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 书名
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** 热门列表项（横排：小封面 + 标题 + 信息） */
@Composable
private fun HotListBookItem(title: String, subtitle: String) {
    val rowModifier = Modifier
        .width(280.dp)
        .clickable(enabled = false) { }

    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 85.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFe8e4df)),
            contentAlignment = Alignment.Center
        ) { /* TODO */ }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==================== 示例数据 ====================

/** 人气风向标示例数据 */
private data class SimpleBook(val title: String)
private val popularSampleBooks = listOf(
    SimpleBook("【书籍A】"), SimpleBook("【书籍B】"),
    SimpleBook("【书籍C】"), SimpleBook("【书籍D】"),
    SimpleBook("【书籍E】"), SimpleBook("【书籍F】"),
    SimpleBook("【书籍G】"), SimpleBook("【书籍H】")
)

/** 热门列表示例数据 */
private data class HotListBook(val title: String, val subtitle: String)
private val hotListBooks = listOf(
    HotListBook("【热门书籍】", "分类 / 标签 / 状态"),
    HotListBook("【精品推荐】", "分类 / 标签 / 状态"),
    HotListBook("【新书速递】", "分类 / 标签 / 状态"),
    HotListBook("【编辑精选】", "分类 / 标签 / 状态")
)
