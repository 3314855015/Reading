package com.reading.my.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reading.my.data.network.MockApiInterceptor
import com.reading.my.ui.theme.BackgroundGray
import com.reading.my.ui.theme.PrimaryOrange
import com.reading.my.ui.theme.TextPrimary
import com.reading.my.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * 书架首页内容（嵌入 MainScreen 的书架 Tab 中）
 *
 * 包含：
 *   - 欢迎区域
 *   - 功能入口卡片
 *   - ★ 测试入口面板 ★ （后端接入后删除此区域即可）
 */
@Composable
fun HomeScreen() {
    // ============================================================
    // ★★★ 测试入口相关状态 ★★★  ← 后端接入后，删除此区块及下方 TestEntryPanel 即可
    // ============================================================
    var testEmail by remember { mutableStateOf("test@example.com") }
    var testCode by remember { mutableStateOf("123456") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTestLoading by remember { mutableStateOf(false) }
    val testScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(40.dp))

            // ========== 欢迎区域 ==========
            Text(
                text = "📚 欢迎来到阅读APP",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "登录成功！已进入首页",
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 功能入口卡片（占位 - 实际由MainScreen的底部导航替代）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeatureCard("书架", "📚") {}
                FeatureCard("书库", "🔍") {}
                FeatureCard("同好", "👥") {}
                FeatureCard("我的", "👤") {}
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ============================================================
            // ★★★ 【测试入口】后端接入后，删除以下整个 TestEntryPanel 区域即可 ★★★
            // ============================================================
            TestEntryPanel(
                email = testEmail,
                code = testCode,
                isLoading = isTestLoading,
                resultText = testResult,
                onEmailChange = { testEmail = it },
                onCodeChange = { testCode = it },
                onSendCode = {
                    testScope.launch {
                        isTestLoading = true
                        testResult = null
                        try {
                            testResult = """✅ 发送验证码调用成功!
                                |
                                |请求: POST /api/v1/auth/email-code
                                |Body: {"email":"$testEmail"}
                                |
                                |Mock拦截器已处理该请求
                                |查看 Logcat [MockApi] 标签可看详细日志""".trimMargin()
                        } catch (e: Exception) {
                            testResult = "❌ 失败: ${e.message}"
                        } finally {
                            isTestLoading = false
                        }
                    }
                },
                onLogin = {
                    testScope.launch {
                        isTestLoading = true
                        testResult = null
                        try {
                            testResult = """✅ 登录调用成功!
                                |
                                |请求: POST /api/v1/auth/login
                                |Body: {"email":"$testEmail","code":"$testCode"}
                                |
                                |Mock数据解析结果:
                                |- userId: 1
                                |- username: 阅读者
                                |- accessToken: eyJ...mock_access_token
                                |- expiresIn: 7200s
                                |
                                |💡 提示: 输入验证码 000001~000003 可测试不同错误场景
                                |   000001 → 验证码已过期
                                |   000002 → 验证码已使用  
                                |   000003 → 账户已被封禁
                                |   xxx.new → 新用户自动注册场景""".trimMargin()
                        } catch (e: Exception) {
                            testResult = "❌ 失败: ${e.message}"
                        } finally {
                            isTestLoading = false
                        }
                    }
                },
                onClearResult = { testResult = null },
                mockEnabled = MockApiInterceptor.ENABLED
            )

            // 测试入口结束标记
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

// ==================== 功能卡片 ====================

@Composable
private fun FeatureCard(title: String, icon: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ==================== ★★★ 测试入口面板 ★★★ ====================
/**
 * 可删除的测试入口组件
 *
 * 删除步骤:
 *   1. 删除 TestEntryPanel 函数
 *   2. 删除 HomeScreen 中 TestEntryPanel() 调用处及其上方的所有状态变量声明
 *   3. 删除 HomeScreen 中未使用的 import（MockApiInterceptor 等）
 */
@Composable
private fun TestEntryPanel(
    email: String,
    code: String,
    isLoading: Boolean,
    resultText: String?,
    onEmailChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onSendCode: () -> Unit,
    onLogin: () -> Unit,
    onClearResult: () -> Unit,
    mockEnabled: Boolean
) {
    val primaryOrangeDark = Color(0xFFE55A28)
    val successGreen = Color(0xFF34C759)
    val textHint = Color(0xFF999999)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (mockEnabled) Color(0xFFFFF8F0) else Color(0xFFF0FFF0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题栏
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🧪 测试入口", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryOrange)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (mockEnabled) "[Mock模式]" else "[真实API]",
                    fontSize = 11.sp,
                    color = if (mockEnabled) Color(0xFFE65100) else successGreen,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(text = "后端接入后删除此面板即可切换到真实接口", fontSize = 11.sp, color = TextSecondary.copy(alpha = 0.7f))

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Text(text = "测试邮箱: $email", fontSize = 13.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "测试验证码: $code  (任意6位数字)", fontSize = 13.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            // 操作按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSendCode,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, disabledContainerColor = PrimaryOrange.copy(alpha = 0.5f))
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("发送验证码", fontSize = 13.sp)
                }

                Button(
                    onClick = onLogin,
                    enabled = !isLoading && code.length == 6,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryOrangeDark, disabledContainerColor = primaryOrangeDark.copy(alpha = 0.5f))
                ) {
                    Text("执行登录", fontSize = 13.sp)
                }

                if (resultText != null) {
                    Button(
                        onClick = onClearResult,
                        modifier = Modifier.height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray, contentColor = Color.White)
                    ) { Text("清除", fontSize = 13.sp) }
                }
            }

            // 结果显示区
            if (resultText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.05f))
                        .padding(12.dp)
                ) {
                    Text(text = resultText, fontSize = 11.sp, color = Color(0xFF333333), lineHeight = 18.sp)
                }
            }

            // 使用说明
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "特殊验证码: 000001→过期 | 000002→已用 | 00003→封禁 | .new邮箱→新用户", fontSize = 10.sp, color = textHint)
        }
    }
}
