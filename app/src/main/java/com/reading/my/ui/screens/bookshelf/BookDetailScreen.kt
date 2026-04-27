package com.reading.my.ui.screens.bookshelf

import android.Manifest
import android.os.Build
import com.reading.my.ui.imagecrop.CoverCropScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.reading.my.domain.model.Book
import com.reading.my.domain.model.Chapter
import com.reading.my.ui.theme.*

@Composable
fun BookDetailScreen(
    bookId: Long,
    onBack: () -> Unit = {},
    onNavigateToReader: ((List<Chapter>, chapterIndex: Int) -> Unit)? = null,
    onNavigateToChapterList: (() -> Unit)? = null,
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(bookId) { viewModel.loadBookDetail(bookId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.onCoverImagePicked(it.toString()) } }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) imagePickerLauncher.launch("image/*") }

    if (uiState.showCoverCrop && uiState.pendingCoverUri != null) {
        val ctx = LocalContext.current
        CoverCropScreen(
            imageUri = uiState.pendingCoverUri!!,
            onConfirm = { viewModel.saveCover(ctx, it) },
            onDismiss = { viewModel.dismissCoverCrop() }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        when {
            uiState.isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center), color = PrimaryOrange
            )
            uiState.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(uiState.error!!, color = TextSecondary)
            }
            uiState.book != null -> BookDetailContent(
                book = uiState.book!!,
                chapters = uiState.chapters,
                username = uiState.username,
                userAvatarUri = uiState.userAvatarUri,
                onBack = onBack,
                onMenuEditTitle = { viewModel.showEditTitle() },
                onMenuEditDesc = { viewModel.showEditDesc() },
                onChapterClick = { _, index -> onNavigateToReader?.invoke(uiState.chapters, index) },
                onNavigateToChapterList = onNavigateToChapterList,
                onPickCoverImage = {
                    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        Manifest.permission.READ_MEDIA_IMAGES
                    else Manifest.permission.READ_EXTERNAL_STORAGE
                    permissionLauncher.launch(perm)
                }
            )
        }

        if (uiState.showEditTitle) {
            EditTextDialog(
                title = "修改标题",
                initial = uiState.book?.title ?: "",
                hint = "请输入书籍标题",
                onConfirm = { viewModel.saveTitle(it) },
                onDismiss = { viewModel.dismissEdit() }
            )
        }
        if (uiState.showEditDesc) {
            EditTextDialog(
                title = "修改简介",
                initial = uiState.book?.description ?: "",
                hint = "请输入书籍简介",
                multiLine = true,
                onConfirm = { viewModel.saveDescription(it) },
                onDismiss = { viewModel.dismissEdit() }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// 主体内容
// ══════════════════════════════════════════════════════════════

@Composable
private fun BookDetailContent(
    book: Book,
    chapters: List<Chapter>,
    username: String,
    userAvatarUri: String?,
    onBack: () -> Unit,
    onMenuEditTitle: () -> Unit,
    onMenuEditDesc: () -> Unit,
    onChapterClick: (Chapter, Int) -> Unit,
    onNavigateToChapterList: (() -> Unit)?,
    onPickCoverImage: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val totalWords = chapters.sumOf { it.wordCount }
    val preview = chapters.take(20)

    Scaffold(
        containerColor = Color.White,
        topBar = {
            // ── 顶部 Icon 栏 ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.weight(1f))
                // TODO: [上传/同步] 后续实现上传到云/同步到圈子功能
                IconButton(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "上传", tint = TextSecondary)
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "更多", tint = TextPrimary)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("修改标题") },
                            onClick = { menuExpanded = false; onMenuEditTitle() }
                        )
                        DropdownMenuItem(
                            text = { Text("修改简介") },
                            onClick = { menuExpanded = false; onMenuEditDesc() }
                        )
                    }
                }
            }
        },
        bottomBar = {
            // ── 底部工具栏 ────────────────────────────────────
            BottomToolBar(
                onCatalog = { onNavigateToChapterList?.invoke() },
                onStartRead = {
                    if (chapters.isNotEmpty()) onChapterClick(chapters[0], 0)
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ── 书籍信息展示框 ────────────────────────────────
            item { BookInfoSection(book = book, totalWords = totalWords, username = username, userAvatarUri = userAvatarUri, onPickCoverImage = onPickCoverImage) }

            // ── 分隔 ──────────────────────────────────────────
            item { HorizontalDivider(color = DividerColor, thickness = 0.5.dp) }

            // ── 简介 ──────────────────────────────────────────
            item { DescriptionSection(description = book.description) }

            item { HorizontalDivider(color = DividerColor, thickness = 6.dp, modifier = Modifier.padding(vertical = 8.dp)) }

            // ── 目录栏（点击跳转目录列表，留白） ──────────────
            item {
                CatalogHeader(
                    chapterCount = chapters.size,
                    onClick = { onNavigateToChapterList?.invoke() }
                )
            }

            // ── 20章预览 ──────────────────────────────────────
            itemsIndexed(preview, key = { _, ch -> ch.chapterIndex }) { index, chapter ->
                ChapterPreviewRow(
                    chapter = chapter,
                    isFirst = index == 0,
                    onClick = { onChapterClick(chapter, index) }
                )
            }

            if (chapters.size > 20) {
                item {
                    Text(
                        text = "查看全部 ${chapters.size} 章 →",
                        fontSize = 13.sp,
                        color = PrimaryOrange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToChapterList?.invoke() }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// 书籍信息展示框
// ══════════════════════════════════════════════════════════════

@Composable
private fun BookInfoSection(
    book: Book,
    totalWords: Int,
    username: String,
    userAvatarUri: String?,
    onPickCoverImage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 左侧：标题 + 作者头像昵称 + 统计
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 作者行：圆形头像 + 昵称
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(PrimaryOrange.copy(alpha = 0.12f))
                        .border(1.dp, PrimaryOrange.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!userAvatarUri.isNullOrBlank()) {
                        android.util.Log.d("AvatarRender", "BookDetailScreen userAvatarUri: ${userAvatarUri.take(80)}")
                        AsyncImage(
                            model = userAvatarUri,
                            contentDescription = "头像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = username.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 10.sp,
                            color = PrimaryOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = username.ifBlank { book.author }, fontSize = 13.sp, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${book.chapterCount}章 · 共${formatWords(totalWords)}字",
                fontSize = 12.sp,
                color = TextHint
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 右侧：书籍封面（可点击换图）
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 108.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onPickCoverImage),
            contentAlignment = Alignment.Center
        ) {
            if (!book.coverPath.isNullOrBlank()) {
                android.util.Log.d("CoverRender", "AsyncImage model: ${book.coverPath.take(80)}, isBlank=${book.coverPath.isBlank()}")
                AsyncImage(
                    model = book.coverPath,
                    contentDescription = "书籍封面",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
                )
            } else {
                // 默认封面：纯色渐变占位，不显示文字
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(PrimaryOrange.copy(alpha = 0.15f), PrimaryOrange.copy(alpha = 0.05f))
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .border(1.dp, PrimaryOrange.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                )
            }
        }
    }
}

private fun formatWords(count: Int): String = when {
    count >= 10000 -> "${count / 10000}万"
    else -> count.toString()
}

// ══════════════════════════════════════════════════════════════
// 简介区域
// ══════════════════════════════════════════════════════════════

@Composable
private fun DescriptionSection(description: String?) {
    var expanded by remember { mutableStateOf(false) }
    val text = description?.takeIf { it.isNotBlank() } ?: "暂无简介"

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (description.isNullOrBlank()) TextDisabled else TextSecondary,
            lineHeight = 22.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis
        )
        if (!description.isNullOrBlank() && text.length > 60) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (expanded) "收起" else "展开",
                fontSize = 12.sp,
                color = PrimaryOrange,
                modifier = Modifier.clickable { expanded = !expanded }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// 目录栏标题
// ══════════════════════════════════════════════════════════════

@Composable
private fun CatalogHeader(chapterCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "目录",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(text = "共${chapterCount}章", fontSize = 12.sp, color = TextHint)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "›", fontSize = 16.sp, color = TextHint)
    }
}

// ══════════════════════════════════════════════════════════════
// 章节预览行（父子感：左侧橙色竖线 + 缩进）
// ══════════════════════════════════════════════════════════════

@Composable
private fun ChapterPreviewRow(chapter: Chapter, isFirst: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 20.dp, top = if (isFirst) 4.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧橙色竖线（父子感）
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(40.dp)
                .background(
                    if (isFirst) PrimaryOrange else PrimaryOrange.copy(alpha = 0.2f),
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = chapter.title,
            fontSize = 14.sp,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${chapter.wordCount}字",
            fontSize = 11.sp,
            color = TextDisabled
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 35.dp, end = 20.dp),
        color = DividerColor,
        thickness = 0.5.dp
    )
}

// ══════════════════════════════════════════════════════════════
// 底部工具栏
// ══════════════════════════════════════════════════════════════

@Composable
private fun BottomToolBar(onCatalog: () -> Unit, onStartRead: () -> Unit) {
    Surface(
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 目录按钮
            Column(
                modifier = Modifier
                    .clickable(onClick = onCatalog)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "☰", fontSize = 20.sp, color = TextSecondary)
                Text(text = "目录", fontSize = 11.sp, color = TextHint)
            }

            Spacer(modifier = Modifier.weight(1f))

            // 开始阅读按钮
            Button(
                onClick = onStartRead,
                modifier = Modifier
                    .height(44.dp)
                    .widthIn(min = 160.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                Text(text = "开始阅读", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// 编辑文本模态框
// ══════════════════════════════════════════════════════════════

@Composable
private fun EditTextDialog(
    title: String,
    initial: String,
    hint: String,
    multiLine: Boolean = false,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundGray, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (multiLine) Modifier.heightIn(min = 80.dp) else Modifier),
                        textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                        maxLines = if (multiLine) 6 else 1,
                        decorationBox = { inner ->
                            if (text.isEmpty()) Text(hint, fontSize = 15.sp, color = TextDisabled)
                            inner()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = TextHint)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { if (text.isNotBlank()) onConfirm(text) },
                        enabled = text.isNotBlank()
                    ) {
                        Text("确定", color = PrimaryOrange, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
