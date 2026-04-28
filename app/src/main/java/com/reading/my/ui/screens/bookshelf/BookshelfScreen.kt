package com.reading.my.ui.screens.bookshelf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Surface
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.offset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reading.my.domain.model.Book
import com.reading.my.ui.theme.*

/**
 * 书架页面（V4 — UI重构版，参考 Bookshelf HTML设计稿）
 *
 * 视觉层次：
 * ┌──────────────────────────────────────┐
 * │  ░░ 沉浸式深色头图区域 (420dp)        │  ← 背景图+渐变+标题+操作按钮
 * │     ┌─ 编辑推荐卡片 ─┐               │  ← 毛玻璃白色卡片
 * │     └───────────────┘               │
 * │  ░░ Tab药丸切换栏                     │  ← 书架/最近更新/本地上传
 * ├──────────────────────────────────────╮
 * │  3列书籍网格 (奶油色背景 #fcf9f8)      │  ← 圆角白卡 + 阅读进度条
 * │  [+添加新书] 虚线卡片                │
 * ├──────────────────────────────────────┤
 * │  名言引用区块 (左侧橙色边框)          │
 * └──────────────────────────────────────┘
 *
 * 业务功能保留：
 * - docx 文件导入（SAF选择器）
 * - 清空缓存
 * - 书籍列表展示（真实数据）
 * - 点击进入详情
 * - 空状态引导导入
 */
@Composable
fun BookshelfScreen(
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: BookshelfViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var selectedTagIndex by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=书架, 1=最近更新, 2=本地上传

    // ===== SAF 文件选择器（docx 格式过滤） =====
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importFromUri(uri)
        }
    }

    // 监听书籍列表数据
    LaunchedEffect(Unit) {
        viewModel.loadBooks()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 整体一体滚动布局：头部 + 编辑推荐 + Tab + 内容区一起滚动
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFfcf9f8))
            .verticalScroll(scrollState)
    ) {
        // ===== 1. 沉浸式头部区域（含编辑推荐卡片） =====
        BookshelfImmersiveHeader(
            onImportDocx = {
                filePicker.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            },
            onClearCache = { viewModel.clearAllBooks() }
        )

        // ===== 2. Tab药丸切换栏 =====
        ShelfPillTabs(
            selectedTab = selectedTab,
            tabs = listOf("书架", "最近更新", "本地上传"),
            onTabSelected = { selectedTab = it }
        )

        // ===== 4. 主内容区（书籍网格，随整体滚动） =====
        when {
            uiState.isLoading && uiState.books.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryOrange)
                }
            }
            uiState.books.isEmpty() -> {
                EmptyShelfView(onImport = {
                    filePicker.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                })
            }
            else -> {
                ShelfBookGrid(
                    books = uiState.books,
                    onBookClick = { book ->
                        if (book.id > 0L) onNavigateToDetail(book.id)
                    },
                    onAddNew = {
                        filePicker.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    }
                )
            }
        }

        // 导入状态提示（在底部显示）
        uiState.importMessage?.let { msg ->
            Text(
                text = msg, fontSize = 12.sp, color = PrimaryOrange,
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 4.dp)
            )
        }
        if (uiState.isImporting) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp), color = PrimaryOrange, strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "正在解析文件...", fontSize = 12.sp, color = TextHint)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ==================== 1. 沉浸式头部区域 ====================

/** 深色渐变头部：标题 + 操作按钮 */
@Composable
private fun BookshelfImmersiveHeader(
    onImportDocx: () -> Unit,
    onClearCache: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF4a3728), Color(0xFF1b1c1c))
                )
            )
    ) {
        // TODO: 头部背景图加载 - 需要网络图片或本地资源
        // 参考设计: 图书馆内部全景图 + blur + opacity 0.3
        // 当前使用纯渐变替代

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp)) // 状态栏占位

            // 标题行：书架 | 历史 · 下载 · 更多
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "书架",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color.White
                )

                // 右侧操作图标组
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderActionIcon(
                        icon = Icons.Outlined.History,
                        label = "历史",
                        onClick = { /* TODO: 跳转阅读历史 */ }
                    )
                    HeaderActionIcon(
                        icon = Icons.Outlined.Download,
                        label = "下载",
                        onClick = { /* TODO: 跳转离线下载管理 */ }
                    )
                    Box {
                        HeaderActionIcon(
                            icon = Icons.Outlined.MoreHoriz,
                            label = "更多",
                            onClick = { showMenu = true }
                        )
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("导入小说", fontSize = 14.sp) },
                                onClick = { showMenu = false; onImportDocx() }
                            )
                            DropdownMenuItem(
                                text = { Text("清空缓存", fontSize = 14.sp) },
                                onClick = { showMenu = false; onClearCache() }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ===== 编辑推荐卡片（在头部底部） =====
            EditorChoiceCard()

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HeaderActionIcon(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

// ==================== 2. 编辑推荐卡片 ====================

@Composable
private fun EditorChoiceCard() {
    // TODO: 编辑推荐数据加载 - 需要从后端获取推荐书籍
    // 当前展示占位UI

    // 卡片在头部底部，自然显示
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面占位
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF0ECF5)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📖",
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 标签
                Text(
                    text = "编辑推荐",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryOrange,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                // 书名
                Text(
                    text = "发现你的下一本好书",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                // 简介
                Text(
                    text = "根据你的阅读偏好智能推荐，探索更多精彩内容...",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 按钮
                Text(
                    text = "开始阅读 →",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(PrimaryOrange)
                        .clickable(enabled = false) { /* TODO: 打开推荐详情 */ }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ==================== 3. Tab药丸切换栏 ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShelfPillTabs(
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedTab
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) PrimaryOrange.copy(alpha = 0.15f)
                              else Color(0xFFE8E4DF),
                    modifier = Modifier.clickable { onTabSelected(index) }
                ) {
                    Text(
                        text = tab,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) PrimaryOrange else Color(0xFF666666)
                    )
                }
            }
        }
    }
}

// ==================== 4. 书籍网格 ====================

@Composable
private fun ShelfBookGrid(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    onAddNew: () -> Unit,
) {
    // 补偿头部偏移的间距
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 20.dp, end = 20.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.height((((books.size / 3) + 1) * 220).dp) // 估算高度
        ) {
            items(books, key = { it.id.toString() }) { book ->
                ModernBookCard(
                    title = book.title.ifBlank { "未命名" },
                    author = book.author.ifBlank { "未知作者" },
                    progress = 0.65f, // TODO: 加载真实阅读进度
                    coverColor = Color(0xFFF0ECF5),
                    onClick = { onBookClick(book) }
                )
            }
            // 添加新书卡片
            item {
                AddNewBookCard(onClick = onAddNew)
            }
        }
    }
}

/** 现代风格书籍卡片：圆角白色 + 进度条 */
@Composable
private fun ModernBookCard(
    title: String,
    author: String,
    progress: Float,
    coverColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFe4e2e1), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            // TODO: 封面图加载 - 当前使用占位色块
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .background(coverColor)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 书名
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(3.dp))

        // 阅读进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFe4e2e1))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFa03b00)) // 日落橙
            )
        }
    }
}

/** 添加新书的虚线卡片 */
@Composable
private fun AddNewBookCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 虚线边框占位
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFf6f3f2))
                .border(2.dp, Color(0xFFe4e2e1), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加新书",
                tint = Color(0xFF8d7166),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "添加新书",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF8d7166),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ==================== 5. 空状态视图 ====================

@Composable
private fun EmptyShelfView(onImport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "📖", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "书架空空如也",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击下方按钮导入你的小说文件",
            fontSize = 14.sp,
            color = TextHint,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "+ 选择文件导入",
            fontSize = 15.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(PrimaryOrange)
                .clickable { onImport() }
                .padding(horizontal = 28.dp, vertical = 12.dp)
        )
    }
}
