package com.reading.my.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.reading.my.ui.theme.*

/**
 * 个人动态页
 *
 * 结构：
 * ┌──────────────────────────────┐
 * │ < 返回                       │  ← 顶部导航栏
 * ├──────────────────────────────┤
 * │ [头像] 昵称      [关注/已关注]│  ← 个人详细展示（点击弹出资料页）
 * ├──────────────────────────────┤
 * │       动态列表（UI占位）       │  ← 后续圈子模块关联
 * └──────────────────────────────┘
 */
@Composable
fun ProfileActivityScreen(
    isSelf: Boolean = true,                          // 是否是本人空间
    onBack: () -> Unit = {},
    onOpenEditProfile: () -> Unit = {},              // 打开个人资料编辑页
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // ── 顶部导航栏 ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimary
                )
            }
            Text(
                text = if (isSelf) "我的主页" else uiState.username,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // ── 个人详细展示模块 ────────────────────────────────
            item {
                UserHeaderCard(
                    username = uiState.username,
                    avatarUrl = uiState.avatarUrl,
                    isSelf = isSelf,
                    onClick = onOpenEditProfile
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── 动态列表（UI占位） ──────────────────────────────
            item {
                ActivityPlaceholder()
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// 个人详细展示卡片
// ══════════════════════════════════════════════════════════════

@Composable
private fun UserHeaderCard(
    username: String,
    avatarUrl: String?,
    isSelf: Boolean,
    onClick: () -> Unit
) {
    // TODO: [关注功能] 后续圈子模块开发时实现关注/取消关注业务
    var isFollowing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),  // 点击整个卡片（除关注按钮外）打开资料页
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左 1/4：圆形头像
            AvatarView(
                username = username,
                avatarUrl = avatarUrl,
                size = 64
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 右侧：昵称
            Text(
                text = username.ifBlank { "未设置昵称" },
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 关注按钮（本人查看时置灰）
            FollowButton(
                isSelf = isSelf,
                isFollowing = isFollowing,
                onClick = {
                    if (!isSelf) isFollowing = !isFollowing
                    // TODO: [关注功能] 调用关注/取消关注 API
                }
            )
        }
    }
}

@Composable
fun AvatarView(username: String, avatarUrl: String?, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(PrimaryOrange.copy(alpha = 0.12f))
            .border(1.5.dp, PrimaryOrange.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "用户头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = username.firstOrNull()?.uppercase() ?: "?",
                fontSize = (size * 0.4f).sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryOrange
            )
        }
    }
}

@Composable
private fun FollowButton(isSelf: Boolean, isFollowing: Boolean, onClick: () -> Unit) {
    val (text, containerColor, contentColor) = when {
        isSelf -> Triple("本人", Color(0xFFEEEEEE), TextDisabled)
        isFollowing -> Triple("已关注", Color(0xFFEEEEEE), TextSecondary)
        else -> Triple("+ 关注", PrimaryOrange, Color.White)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        modifier = Modifier.clickable(enabled = !isSelf, onClick = onClick)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = contentColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════
// 动态列表占位
// ══════════════════════════════════════════════════════════════

@Composable
private fun ActivityPlaceholder() {
    // TODO: [动态列表] 后续圈子模块开发时关联真实动态数据
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "暂无动态", fontSize = 14.sp, color = TextHint)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "圈子功能开发后将在此展示", fontSize = 12.sp, color = TextDisabled)
            }
        }
    }
}
