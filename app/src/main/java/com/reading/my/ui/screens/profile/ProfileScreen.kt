package com.reading.my.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.offset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.reading.my.ui.theme.*
import com.reading.my.R

/**
 * 我的页面（V2 — UI重构版，参考 Profile HTML设计稿）
 *
 * 视觉层次：
 * ┌──────────────────────────────────────┐
 * │ ░░ 深色径向渐变头图区域               │  ← 暗棕→黑渐变 + 模糊背景
 * │     [签到] 按钮                      │  ← 右上角
 * │     ┌─ 用户信息卡（白圆角）────────┐│  ← 头像(悬浮) + 昵称 + 简介
 * │     └─────────────────────────────┘│
 * ├──────────────────────────────────────╮
 * │ 功能网格 (4×2): 预约|空间|消息|月票  │  ← 白色圆角图标卡片
 * │              道具|作者|商城|书单    │
 * ├──────────────────────────────────────┤
 * │ 设置列表（大白圆角卡片）              │  ← 夜间模式 / 阅读时长 ...
 * │                                     │
 * └──────────────────────────────────────┘
 */
@Composable
fun ProfileScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToAssociate: () -> Unit = {},
    onNavigateToBookList: () -> Unit = {},
    onNavigateToPublish: () -> Unit = {},
    onNavigateToGroup: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 分层布局：底层模糊图延伸至状态栏 + 顶层可滚动内容
    Box(modifier = Modifier.fillMaxSize()) {

        // ========== 底层：模糊背景图（无顶部约束，自然延伸到状态栏区域） ==========
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.backround),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(64.dp),
                contentScale = ContentScale.Crop
            )
            // 暗色遮罩层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            )
        }

        // ========== 顶层：可滚动内容 ==========
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 头部内容区域（透明底，让底层模糊图透出来；statusBarsPadding 让文字避开状态栏）
            ProfileHeaderSection(
                username = uiState.username,
                bio = uiState.bio,
                avatarUrl = uiState.avatarUrl,
                onClickUserCard = onNavigateToProfile,
                isDarkMode = uiState.isDarkMode,
                onDarkModeToggle = { viewModel.toggleDarkMode() }
            )

            // 功能网格 + 设置列表（米色/白色背景从这里开始）
            FunctionGridSection(
                localCount = uiState.localCount,
                onBookListClick = onNavigateToBookList,
                publishCount = uiState.publishCount,
                onPublishClick = onNavigateToPublish,
                groupCount = uiState.groupCount,
                onGroupClick = onNavigateToGroup
            )

            SettingsSection(
                readingTime = "12.5 小时",
                isDarkMode = uiState.isDarkMode,
                onDarkModeToggle = { viewModel.toggleDarkMode() }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ==================== 1. 渐变头部 + 用户信息卡 ====================

@Composable
private fun ProfileHeaderSection(
    username: String,
    bio: String?,
    avatarUrl: String?,
    onClickUserCard: () -> Unit,
    isDarkMode: Boolean,
    onDarkModeToggle: () -> Unit,
) {
    // 头部内容（透明底，底层模糊图透出；statusBarsPadding 让内容避开状态栏）
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            // 右上角签到按钮
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.40f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                        .clickable(enabled = false) { /* TODO: 打开签到页面 */ }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.CalendarToday, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(14.dp))
                    Text(text = "签到", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== 用户信息卡片（头像在卡片外部定位，不被裁剪） =====
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                // 毛玻璃圆角卡片（透出模糊背景图）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFFFFFFFF).copy(alpha = 0.0f)) // 毛玻璃暖色
//                        .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(28.dp))
                        .clickable(onClick = onClickUserCard)
                        .padding(top = 60.dp, bottom = 16.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // 昵称
                    Text(text = username.ifBlank { "未设置昵称" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = Color(0xFFAFB4B4),
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 简介
                    Text(
                        text = bio?.takeIf { it.isNotBlank() }
                            ?: "资深读者与策展人。热爱在静谧的午后探索文字的奥秘。",
                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        color = Color(0xFF999999), maxLines = 2,
                        overflow = TextOverflow.Ellipsis, lineHeight = 17.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                // 头像（绝对定位在卡片顶部中央，超出卡片边界，不被裁剪）
                Box(
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = (-36).dp),
                ) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, Color(0xFFf0ece9), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(model = avatarUrl, contentDescription = "用户头像",
                                modifier = Modifier.fillMaxSize().clip(CircleShape))
                        } else {
                            Text(text = username.firstOrNull()?.uppercase() ?: "?",
                                fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PrimaryOrange)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp)) // 卡片与功能网格间的紧凑过渡
        }
    }
}

// ==================== 2. 功能网格 ====================

data class FunctionGridItem(val icon: ImageVector, val label: String)

private val functionGridItems = listOf(
    FunctionGridItem(Icons.Outlined.EventAvailable, "我的预约"),
    FunctionGridItem(Icons.Outlined.Palette, "装饰空间"),
    FunctionGridItem(Icons.Outlined.ChatBubble, "我的消息"),
    FunctionGridItem(Icons.Outlined.ConfirmationNumber, "我的月票"),
    FunctionGridItem(Icons.Outlined.AutoAwesome, "我的道具"),
    FunctionGridItem(Icons.Outlined.HistoryEdu, "成为作者"),
    FunctionGridItem(Icons.Outlined.ShoppingBag, "周边商城"),
    FunctionGridItem(Icons.Outlined.FormatListBulleted, "我的书单")
)

@Composable
private fun FunctionGridSection(
    localCount: Int,
    onBookListClick: () -> Unit,
    publishCount: Int,
    onPublishClick: () -> Unit,
    groupCount: Int,
    onGroupClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFfcf9f8))
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // 第一行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            functionGridItems.take(4).forEachIndexed { index, item ->
                FunctionIconCard(
                    icon = item.icon,
                    label = item.label,
                    onClick = {
                        when (index) {
                            0 -> { /* TODO: 进入我的预约页 */ }
                            1 -> { /* TODO: 进入装饰空间 */ }
                            2 -> { /* TODO: 进入我的消息页 */ }
                            3 -> { /* TODO: 进入我的月票页 */ }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 第二行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 前三个功能项
            functionGridItems.drop(4).take(3).forEachIndexed { index, item ->
                FunctionIconCard(
                    icon = item.icon,
                    label = item.label,
                    onClick = {
                        when (index) {
                            0 -> { /* TODO: 进入我的道具页 */ }
                            1 -> { onPublishClick() } // 成为作者 → 已有业务逻辑
                            2 -> { /* TODO: 进入周边商城 */ }
                        }
                    }
                )
            }

            // 第四个：我的书单 → 关联已有业务逻辑
            FunctionIconCard(
                icon = Icons.Outlined.FormatListBulleted,
                label = "我的书单",
                onClick = onBookListClick
            )
        }
    }
}

/** 功能图标卡片：圆角白底 + 图标 + 文字 */
@Composable
private fun FunctionIconCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        // 图标容器
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xfcf9f8))
                .border(1.dp, Color(0xFFf5f0ed), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF666666), // stone-600
                modifier = Modifier.size(24.dp)
            )
        }
        // 标签文字
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF888888), // stone-500
            textAlign = TextAlign.Center
        )
    }
}

// ==================== 3. 设置列表 ====================

@Composable
private fun SettingsSection(
    readingTime: String,
    isDarkMode: Boolean,
    onDarkModeToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // 直接铺满剩余空间，无悬浮卡片
        // 夜间模式
        SettingRowWithSwitch(
                icon = Icons.Outlined.DarkMode,
                title = "夜间模式",
                checked = isDarkMode,
                onToggle = onDarkModeToggle
            )

            // 分割线
            HorizontalDivider(color = Color(0xFFFAFAF9), thickness = 0.5.dp)

            // 阅读时长
            SettingRowWithArrow(
                icon = Icons.Outlined.History,
                title = "阅读时长",
                value = readingTime,
                onClick = { /* TODO: 跳转阅读统计详情 */ }
            )

            HorizontalDivider(color = Color(0xFFFAFAF9), thickness = 0.5.dp)

            // 意见反馈
            SettingRowWithArrow(
                icon = Icons.Outlined.RateReview,
                title = "意见反馈",
                value = null,
                onClick = { /* TODO: 打开意见反馈 */ }
            )

            HorizontalDivider(color = Color(0xFFFAFAF9), thickness = 0.5.dp)

            // 关于
            SettingRowWithArrow(
                icon = Icons.Outlined.Info,
                title = "关于",
                value = null,
                onClick = { /* TODO: 打开关于页面 */ }
            )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** 设置项：左侧图标 + 标题 + 右侧开关 */
@Composable
private fun SettingRowWithSwitch(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFAAAAAA), // stone-400
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF555555) // stone-700
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryOrange,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFe0dcd9)
            ),
            modifier = Modifier.height(24.dp)
        )
    }
}

/** 设置项：左侧图标 + 标题 + 右侧值 + 箭头 */
@Composable
private fun SettingRowWithArrow(
    icon: ImageVector,
    title: String,
    value: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFAAAAAA),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF555555)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = Color(0xFFAAAAAA) // stone-400
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFDDDDDD), // stone-300
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun HorizontalDivider(
    modifier: Modifier = Modifier,
    color: Color = DividerColor,
    thickness: Dp = 1.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}
