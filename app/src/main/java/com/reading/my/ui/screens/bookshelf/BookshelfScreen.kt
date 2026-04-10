package com.reading.my.ui.screens.bookshelf

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.reading.my.ui.theme.BackgroundGray
import com.reading.my.ui.theme.PrimaryOrange
import com.reading.my.ui.theme.TextPrimary
import com.reading.my.ui.theme.TextSecondary
import com.reading.my.ui.theme.TextHint

/**
 * 书架页面
 *
 * 布局结构：
 * ┌──────────────────────────────────────┐
 * │  书架                     更多  ▼    │  ← 菜单栏（标题 + 下拉菜单）
 * ├──────────────────────────────────────┤
 * │  [本地小说] [在线小说]      编辑(橘) │  ← Tag筛选栏 + 编辑按钮
 * ├──────────────────────────────────────┤
 * │  ┌─────┐  ┌─────┐  ┌─────┐         │
 * │  │封面 │  │封面 │  │封面 │         │  ← 3列网格 LazyVerticalGrid
 * │  │书名 │  │书名 │  │书名 │         │    初始加载12本模拟数据
 * │  │作者 │  │作者 │  │作者 │         │
 * │  └─────┘  └─────┘  └─────┘         │
 * │  ...                                │
 * └──────────────────────────────────────┘
 */
@Composable
fun BookshelfScreen(
    onImportBook: () -> Unit = {},
    onClearCache: () -> Unit = {},
) {
    var selectedTagIndex by remember { mutableIntStateOf(0) } // 当前选中的分类tag

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // ===== 菜单栏：标题 + 下拉菜单 =====
        ShelfHeaderBar(
            onImportBook = onImportBook,
            onClearCache = onClearCache
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ===== 筛选栏：Tag列表 + 编辑按钮 =====
        ShelfFilterBar(
            tags = shelfTags,
            selectedIndex = selectedTagIndex,
            onTagSelected = { selectedTagIndex = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ===== 书籍网格（3列） =====
        ShelfBookGrid()
    }
}

// ==================== 菜单栏 ====================

@Composable
private fun ShelfHeaderBar(
    onImportBook: () -> Unit,
    onClearCache: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：标题
        Text(
            text = "书架",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.weight(1f))

        // 右侧：更多下拉菜单
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
                    text = { Text("导入小说", fontSize = 14.sp) },
                    onClick = {
                        showMenu = false
                        onImportBook()
                    }
                )
                DropdownMenuItem(
                    text = { Text("清空缓存", fontSize = 14.sp) },
                    onClick = {
                        showMenu = false
                        onClearCache()
                    }
                )
            }
        }
    }
}

// ==================== 筛选栏（Tag切换） ====================

/** 书架分类标签数据 */
data class ShelfTag(val name: String)

/** 预置分类标签 */
private val shelfTags = listOf(
    ShelfTag("本地小说"),
    ShelfTag("在线小说"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShelfFilterBar(
    tags: List<ShelfTag>,
    selectedIndex: Int,
    onTagSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：横向 Tag 列表
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

        // 右侧：编辑按钮（橘色，占位）
        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "编辑",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = PrimaryOrange,
            modifier = Modifier
                .clickable(enabled = false) { /* TODO: 进入标签管理/多选模式 */ }
                .padding(horizontal = 4.dp, vertical = 6.dp)
        )
    }
}

// ==================== 书籍网格 ====================

/** 书籍单元数据 */
data class ShelfBookItem(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String? = null,
    /** 阅读进度 0f~1f */
    val progress: Float = 0f,
)

/** 模拟数据（12本 = 4行×3列） */
private val sampleShelfBooks = listOf(
    ShelfBookItem("1", "三体", "刘慈欣", progress = 0.35f),
    ShelfBookItem("2", "活着", "余华", progress = 0.62f),
    ShelfBookItem("3", "百年孤独", "加西亚·马尔克斯"),
    ShelfBookItem("4", "围城", "钱钟书", progress = 0.10f),
    ShelfBookItem("5", "平凡的世界", "路遥", progress = 0.88f),
    ShelfBookItem("6", "白夜行", "东野圭吾"),
    ShelfBookItem("7", "红楼梦", "曹雪芹", progress = 0.45f),
    ShelfBookItem("8", "追风筝的人", "卡勒德·胡赛尼"),
    ShelfBookItem("9", "解忧杂货店", "东野圭吾", progress = 0.22f),
    ShelfBookItem("10", "挪威的森林", "村上春树"),
    ShelfBookItem("11", "明朝那些事儿", "当年明月", progress = 0.73f),
    ShelfBookItem("12", "人类简史", "尤瓦尔·赫拉利"),
)

/**
 * 书籍展示网格
 *
 * - 固定 3 列布局
 * - 使用 LazyVerticalGrid 懒加载
 * - 初始显示 12 本模拟数据
 * - 书籍单元样式与书库页一致（封面 + 书名 + 作者）
 */
@Composable
private fun ShelfBookGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
    ) {
        items(sampleShelfBooks, key = { it.id }) { book ->
            ShelfBookCard(book = book)
        }
    }
}

/** 单个书籍卡片（3列网格中的单元） */
@Composable
private fun ShelfBookCard(book: ShelfBookItem) {
    Column(
        modifier = Modifier
            .clickable(enabled = false) { /* TODO: 进入阅读器 */ },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 封面（圆角矩形，无图时淡紫底色）
        Box(
            modifier = Modifier
                .size(width = 90.dp, height = 120.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF0ECF5)),
            contentAlignment = Alignment.Center
        ) {
            // TODO: 后续用 Coil 加载 coverUrl，此处占位
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 书名（最多2行省略）
        Text(
            text = book.title.ifBlank { "未命名" },
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 作者
        Text(
            text = book.author.ifBlank { "未知作者" },
            fontSize = 11.sp,
            color = TextHint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


//// ==================== @Preview ====================
//
//@Preview(
//    name = "书架页面预览",
//    showBackground = true,
//    backgroundColor = 0xFFF5F5F5L,
//)
//@Composable
//private fun BookshelfScreenPreview() {
//    BookshelfScreen(
//        onImportBook = {},
//        onClearCache = {},
//    )
//}
