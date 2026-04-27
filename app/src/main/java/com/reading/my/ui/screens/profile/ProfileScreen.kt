package com.reading.my.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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

@Composable
fun ProfileScreen(
    onNavigateToProfile: () -> Unit = {},   // 跳转个人动态
    onNavigateToAssociate: () -> Unit = {}, // 跳转关联页
    onNavigateToBookList: () -> Unit = {},  // 本地书籍列表
    onNavigateToPublish: () -> Unit = {},   // 发布列表
    onNavigateToGroup: () -> Unit = {},     // 圈子列表
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── 模块一：个人信息（用户） ──────────────────────────────
        UserInfoCard(
            username = uiState.username,
            bio = uiState.bio,
            avatarUrl = uiState.avatarUrl,
            onClick = onNavigateToProfile
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── 模块二：个人信息（APP） ───────────────────────────────
        AppInfoCard(
            isAssociated = uiState.isAssociated,
            localCount = uiState.localCount,
            publishCount = uiState.publishCount,
            groupCount = uiState.groupCount,
            onAssociateClick = onNavigateToAssociate,
            onLocalClick = onNavigateToBookList,
            onPublishClick = onNavigateToPublish,
            onGroupClick = onNavigateToGroup
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── 模块三：系统设置 ──────────────────────────────────────
        SettingsCard(
            isDarkMode = uiState.isDarkMode,
            isNetworkVerify = uiState.isNetworkVerify,
            onDarkModeToggle = { viewModel.toggleDarkMode() },
            onNetworkVerifyToggle = { viewModel.toggleNetworkVerify() }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ══════════════════════════════════════════════════════════════
// 模块一：个人信息（用户）
// ══════════════════════════════════════════════════════════════

@Composable
private fun UserInfoCard(
    username: String,
    bio: String?,
    avatarUrl: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左 1/4：圆形头像
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(PrimaryOrange.copy(alpha = 0.12f)),
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
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 右 3/4：昵称 + 简介
            Column(
                modifier = Modifier.weight(3f),
                verticalArrangement = Arrangement.Center
            ) {
                // 昵称（1/3 高度感）
                Text(
                    text = username.ifBlank { "未设置昵称" },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 简介（2/3 高度感，无简介时显示占位文案）
                Text(
                    text = bio?.takeIf { it.isNotBlank() } ?: "该作家没有介绍自己噢",
                    fontSize = 13.sp,
                    color = if (bio.isNullOrBlank()) TextDisabled else TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }

            // 右箭头提示可点击
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextDisabled,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════
// 模块二：个人信息（APP）
// ══════════════════════════════════════════════════════════════

@Composable
private fun AppInfoCard(
    isAssociated: Boolean,
    localCount: Int,
    publishCount: Int,
    groupCount: Int,
    onAssociateClick: () -> Unit,
    onLocalClick: () -> Unit,
    onPublishClick: () -> Unit,
    onGroupClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "我的账户",
                    fontSize = 12.sp,
                    color = TextHint,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = if (isAssociated) "已关联" else "去关联",
                    fontSize = 12.sp,
                    color = if (isAssociated) SuccessGreen else PrimaryOrange,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onAssociateClick)
                )
            }

            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

            // 统计行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "本地", count = localCount, onClick = onLocalClick)
                StatItem(label = "发布", count = publishCount, onClick = onPublishClick)
                StatItem(label = "圈子", count = groupCount, onClick = onGroupClick)
                // 留白占位
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RowScope.StatItem(label: String, count: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextHint
        )
    }
}

// ══════════════════════════════════════════════════════════════
// 模块三：系统设置
// ══════════════════════════════════════════════════════════════

@Composable
private fun SettingsCard(
    isDarkMode: Boolean,
    isNetworkVerify: Boolean,
    onDarkModeToggle: () -> Unit,
    onNetworkVerifyToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // 第一行：1:1 两个 Toggle 设置
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                ToggleSettingItem(
                    modifier = Modifier.weight(1f),
                    label = if (isDarkMode) "夜间模式" else "日间模式",
                    checked = isDarkMode,
                    onToggle = onDarkModeToggle
                )
                VerticalDivider(
                    modifier = Modifier
                        .height(48.dp)
                        .padding(vertical = 8.dp),
                    color = DividerColor
                )
                ToggleSettingItem(
                    modifier = Modifier.weight(1f),
                    label = if (isNetworkVerify) "网络验证" else "本地验证",
                    checked = isNetworkVerify,
                    onToggle = onNetworkVerifyToggle
                )
            }

            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

            // 以下为占位设置项（后续按需添加）
            SettingRow(label = "阅读偏好", value = "默认")
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            SettingRow(label = "缓存管理", value = "清理")
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            SettingRow(label = "关于", value = "v1.0.0")
        }
    }
}

@Composable
private fun ToggleSettingItem(
    modifier: Modifier = Modifier,
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = TextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryOrange,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = DividerColor
            )
        )
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = TextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, fontSize = 13.sp, color = TextHint)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextDisabled,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
