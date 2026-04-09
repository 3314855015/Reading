package com.reading.my.ui.screens.bookstore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reading.my.ui.theme.PrimaryOrange

/**
 * 书库页面（在线阅读 / 发现页）
 *
 * 层次设计（同一色系，通过透明度/圆角/毛玻璃区分层次）：
 *
 * ┌──────────────────────────────────────┐
 * │ ░░ 状态栏占位 (padding)              │  ← 最顶层，无额外背景
 * ├──────────────────────────────────────┤
 * │  开源图书    同好小说                │  ← A 类型切换栏
 * │  ┌────────────────────────────┐      │
 * │  │ 🔍 搜索书籍...            │      │  ← B 搜索框 (alpha=0.08 毛玻璃)
 * │  └────────────────────────────┘      │
 * ├──────────────────────────────────────┤
 * │  ┌────────────────────────────┐      │
 * │  │ 📚 热门图书推荐            │      │  ← 热门卡片 (alpha=0.06 渐变)
 * │  │   (悬浮卡片 · 待实现)      │      │
 * │  └────────────────────────────┘      │
 * ├──────────────────────────────────────╮
 * │  人气图书                   更多 >   │  ← 白底圆角区域 (alpha=1.0 实心)
 * │  ┌───┐┌───┐┌───┐┌───┐               │
 * │  │📖│ │📖│ │📖│ │📖│  4列 × 2排     │  ← 固定网格，不做LazyColumn
 * │  │书名│...                       │
 * │  └───┘└───┘└───┘└───┘               │
 * │  ┌───┐┌───┐┌───┐┌───┐               │
 * │  │📖│ │📖│ │📖│ │📖│               │
 * │  └───┘└───┘└───┘└───┘               │
 * ├──────────────────────────────────────┤
 * │  (下方留白 / 后续可扩展)             │
 * └──────────────────────────────────────┘
 */
@Composable
fun BookstoreScreen(
    // ========== 主题预留参数 ==========
    /** 页面背景色（默认跟随主题） */
    pageBackgroundColor: Color = Color(0xFF9C9CA8),
    /** 文字主色 */
    textColorPrimary: Color = Color.White,
    /** 文字辅助色 */
    textColorSecondary: Color = Color.White.copy(alpha = 0.5f),
    /** 卡片/容器背景的叠加层 alpha 值（用于区分层次） */
    surfaceAlpha: Float = 0.06f,
    /** 强调色（如橘色的"更多"按钮、选中态等） */
    accentColor: Color = PrimaryOrange,
) {
    var selectedType by remember { mutableIntStateOf(0) } // 0=开源图书, 1=同好小说
    val isOpenSource = selectedType == 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackgroundColor)   // ← 颜色应延伸到状态栏
            .statusBarsPadding()  
    ) {
        // ===== A + B 组件：类型切换栏 + 搜索框 =====
        TypeAndSearchArea(
            selectedType = selectedType,
            onTypeSelected = { selectedType = it },
            textColorPrimary = textColorPrimary,
            textColorSecondary = textColorSecondary,
            surfaceAlpha = surfaceAlpha,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 热门图书卡片 =====
        HotBooksPlaceholder(
            isOpenSource = isOpenSource,
            surfaceAlpha = surfaceAlpha,
            textColorPrimary = textColorPrimary,
            textColorSecondary = textColorSecondary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 内容区域：标题 + 固定4×2书籍网格 =====
        BookContentArea(
            isOpenSource = isOpenSource,
            accentColor = accentColor,
        )
    }
}

// ==================== AB 组件 ====================

@Composable
private fun TypeAndSearchArea(
    selectedType: Int,
    onTypeSelected: (Int) -> Unit,
    textColorPrimary: Color,
    textColorSecondary: Color,
    surfaceAlpha: Float,
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
    ) {
        // --- A: 类型切换栏 ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            TypeTabItem(
                text = "开源图书",
                selected = selectedType == 0,
                onClick = { onTypeSelected(0) },
                textColorPrimary = textColorPrimary,
                textColorSecondary = textColorSecondary,
            )
            Spacer(modifier = Modifier.width(24.dp))
            TypeTabItem(
                text = "同好小说",
                selected = selectedType == 1,
                onClick = { onTypeSelected(1) },
                textColorPrimary = textColorPrimary,
                textColorSecondary = textColorSecondary,
            )
        }

        // --- B: 搜索框 ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(textColorPrimary.copy(alpha = surfaceAlpha + 0.02f)) // 毛玻璃感
                .clickable(enabled = false) { /* TODO: 跳转搜索页 */ },
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "搜索书籍...",
                color = textColorSecondary.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

/** 单个类型标签 */
@Composable
private fun TypeTabItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    textColorPrimary: Color,
    textColorSecondary: Color,
) {
    Text(
        text = text,
        fontSize = if (selected) 18.sp else 15.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) textColorPrimary else textColorSecondary,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    )
}

// ==================== 热门图书占位 ====================

@Composable
private fun HotBooksPlaceholder(
    isOpenSource: Boolean,
    surfaceAlpha: Float,
    textColorPrimary: Color,
    textColorSecondary: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        textColorPrimary.copy(alpha = surfaceAlpha + 0.04f),
                        textColorPrimary.copy(alpha = surfaceAlpha),
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isOpenSource) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "热门图书推荐",
                    color = textColorPrimary.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "悬浮卡片 · 待实现",
                    color = textColorSecondary.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        } else {
            Text(
                text = "同好小说 · 敬请期待",
                color = textColorSecondary.copy(alpha = 0.35f),
                fontSize = 14.sp
            )
        }
    }
}

// ==================== 书籍内容区 ====================

/**
 * 内容区：标题栏 + 固定4列×2排网格
 * 正方形全宽填充（非圆角卡片），填满剩余空间
 */
@Composable
private fun BookContentArea(
    isOpenSource: Boolean,
    accentColor: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight() // 填满剩余全部空间，正方形
            .background(Color(0xFFDCD1D1))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // 标题栏
        BookSectionHeader(accentColor = accentColor)

        // 固定 4列 × 2排 网格（不做 LazyColumn）
        if (isOpenSource) {
            FixedBookGrid(books = sampleBooks)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "同好小说内容 · 敬请期待",
                    color = Color(0xFFCCCCCC),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun BookSectionHeader(accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "人气图书",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
        Text(
            text = "更多",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = accentColor,
            modifier = Modifier
                .clickable(enabled = false) { /* TODO: 进入更多页(LazyColumn) */ }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

// ==================== 固定书籍网格（4列 × 2排）====================

/** 书籍数据模型 */
data class BookItem(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String? = null
)

/** 示例数据（8本 = 4列×2排） */
private val sampleBooks = listOf(
    BookItem("1", "三体", "刘慈欣"),
    BookItem("2", "活着", "余华"),
    BookItem("3", "百年孤独", "加西亚·马尔克斯"),
    BookItem("4", "围城", "钱钟书"),
    BookItem("5", "平凡的世界", "路遥"),
    BookItem("6", "白夜行", "东野圭吾"),
    BookItem("7", "红楼梦", "曹雪芹"),
    BookItem("8", "追风筝的人", "卡勒德·胡赛尼"),
)

/**
 * 固定 4 列 x 2 排 书籍网格
 *
 * 使用 Row + Column 手动布局而非 LazyVerticalGrid，
 * 因为这里只需要展示固定数量的书籍入口，
 * 更多列表功能移入"更多"页面。
 */
@Composable
private fun FixedBookGrid(books: List<BookItem>) {
    // 分成两行，每行4本
    val rows = books.chunked(4)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { rowBooks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowBooks.forEach { book ->
                    BookGridItem(book = book)
                }
            }
        }
    }
}

/** 单个书籍单元 */
@Composable
private fun BookGridItem(book: BookItem) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(enabled = false) { /* TODO: 进入详情 */ },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 封面（圆角矩形，无边框，无图时淡紫底+图标）
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 96.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF0ECF5)), // 极淡紫灰
            contentAlignment = Alignment.Center
        ) {

        }

        Spacer(modifier = Modifier.height(6.dp))

        // 书名（最多2行省略）
        Text(
            text = book.title.ifBlank { "未命名" },
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 作者
        Text(
            text = book.author.ifBlank { "未知作者" },
            fontSize = 10.sp,
            color = Color(0xFF999999),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
