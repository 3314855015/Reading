package com.reading.my.ui.screens.profile

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.reading.my.ui.imagecrop.AvatarCropScreen
import com.reading.my.ui.theme.*

/**
 * 个人资料编辑页
 *
 * 布局：标题栏（< 个人资料  保存）+ 修改区域列表
 * 当前实现：头像选择（相册 → 裁剪页）+ 昵称编辑
 * 留白项：等级、个人简介、账号绑定、修改密码
 *
 * 头像流程：
 *   点击"个人头像" → 申请权限 → 系统相册 → 选中图片 → AvatarCropScreen（裁剪）
 *   → 确认后存 Base64 → 点击"保存" → 上传到服务器
 */
@Composable
fun EditProfileScreen(
    onBack: () -> Unit = {},
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showUsernameEditor by remember { mutableStateOf(false) }

    // 相册权限 + 图片选择
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onAvatarSelected(it.toString()) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) imagePickerLauncher.launch("image/*")
    }

    // ── 头像裁剪页（全屏覆盖）───
    if (uiState.showAvatarCrop && uiState.pendingAvatarUri != null) {
        val ctx = LocalContext.current
        AvatarCropScreen(
            imageUri = uiState.pendingAvatarUri!!,
            onConfirm = { base64 -> viewModel.onAvatarCropped(ctx, base64) },
            onDismiss = { viewModel.dismissAvatarCrop() }
        )
        return
    }

    // ── 主页面 ──────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // ── 标题栏 ──────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            val ctx = LocalContext.current
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary)
            }
            Text(
                text = "个人资料",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
            TextButton(
                onClick = { viewModel.saveAll(ctx, onBack) },
                modifier = Modifier.align(Alignment.CenterEnd),
                enabled = uiState.hasChanges && !uiState.isLoading
            ) {
                Text(
                    text = "保存",
                    fontSize = 16.sp,
                    color = if (uiState.hasChanges) PrimaryOrange else TextDisabled,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── 修改区域 ────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column {
                // 头像行（带预览图 + 跳转裁剪）
                ProfileRow(
                    label = "个人头像",
                    onClick = {
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            Manifest.permission.READ_MEDIA_IMAGES
                        else
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        permissionLauncher.launch(permission)
                    }
                ) {
                    // 头像预览：优先显示裁剪后的待保存图 > 已保存的头像 URL > 首字母占位
                    val previewUri = uiState.pendingAvatarBase64?.let { "data:image/jpeg;base64,$it" }
                    val displayUrl = previewUri ?: uiState.avatarUrl

                    if (!displayUrl.isNullOrBlank()) {
                        android.util.Log.d("AvatarRender", "EditProfileScreen displayUrl: ${displayUrl.take(80)}")
                        AsyncImage(
                            model = displayUrl,
                            contentDescription = "头像预览",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryOrange.copy(alpha = 0.08f)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryOrange.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.username.firstOrNull()?.uppercase() ?: "?",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryOrange
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    color = DividerColor,
                    thickness = 0.5.dp
                )

                // 昵称行
                ProfileRow(
                    label = "昵称",
                    onClick = { showUsernameEditor = true }
                ) {
                    Text(
                        text = uiState.username.ifBlank { "未设置" },
                        fontSize = 14.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    color = DividerColor,
                    thickness = 0.5.dp
                )

                // 等级（留白）
                ProfileRowPlaceholder(label = "等级", value = "Lv.1")

                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    color = DividerColor,
                    thickness = 0.5.dp
                )

                // 个人简介（留白）
                ProfileRowPlaceholder(label = "个人简介", value = "未填写")

                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    color = DividerColor,
                    thickness = 0.5.dp
                )

                // 账号绑定（留白）
                ProfileRowPlaceholder(label = "账号绑定", value = "未绑定")

                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    color = DividerColor,
                    thickness = 0.5.dp
                )

                // 修改密码（留白）
                ProfileRowPlaceholder(label = "修改密码", value = "")
            }
        }

        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }

    // ── 昵称编辑弹窗 ──────────────────────
    if (showUsernameEditor) {
        UsernameEditorSheet(
            current = uiState.username,
            isLoading = uiState.isLoading,
            error = uiState.usernameError,
            onConfirm = { newName ->
                viewModel.updateUsername(newName) { showUsernameEditor = false }
            },
            onDismiss = { showUsernameEditor = false }
        )
    }
}

// ══════════════════════════════════════════════════════════════
// 通用行组件（复用）
// ══════════════════════════════════════════════════════════════

@Composable
private fun ProfileRow(
    label: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 15.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        trailing()
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextDisabled,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ProfileRowPlaceholder(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 15.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        if (value.isNotBlank()) {
            Text(text = value, fontSize = 14.sp, color = TextHint)
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextDisabled,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════
// 昵称编辑页（全屏替换，参考截图设计）
// ══════════════════════════════════════════════════════════════

@Composable
private fun UsernameEditorSheet(
    current: String,
    isLoading: Boolean,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf(current) }
    val isValid = input.length in 2..10 && input.matches(Regex("^[\\u4e00-\\u9fa5a-zA-Z0-9]+$"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 标题栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary)
            }
            Text(
                text = "个人资料",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
            TextButton(
                onClick = { if (isValid) onConfirm(input) },
                modifier = Modifier.align(Alignment.CenterEnd),
                enabled = isValid && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryOrange)
                } else {
                    Text(
                        text = "保存",
                        fontSize = 16.sp,
                        color = if (isValid) PrimaryOrange else TextDisabled,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

        // 输入区域
        Box(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = input,
                onValueChange = { if (it.length <= 10) input = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 20.sp,
                    color = TextPrimary
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (input.isEmpty()) {
                        Text(
                            text = "2-10个字符 允许中文，字母和数字",
                            fontSize = 20.sp,
                            color = TextDisabled
                        )
                    }
                    innerTextField()
                }
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = if (error != null) MaterialTheme.colorScheme.error else PrimaryOrange,
            thickness = 1.dp
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = error,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}
