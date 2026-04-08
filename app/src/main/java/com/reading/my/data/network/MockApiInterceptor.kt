package com.reading.my.data.network

import android.util.Log
import com.google.gson.Gson
import com.reading.my.data.network.model.*
import okhttp3.*
import okio.Buffer
import java.io.IOException

/**
 * Mock API 拦截器
 * ================
 * 在后端未就绪时，拦截所有网络请求并返回符合后端规范的模拟数据。
 *
 * 【使用方式】
 *   - 默认开启 Mock 模式（ENABLED = true）
 *   - 后端就绪后设置 ENABLED = false 即可无缝切换到真实接口
 *   - 删除此文件 + NetworkModule 中的 addInterceptor(MockApiInterceptor()) 即可完全移除
 *
 * 【Mock 数据格式】
 *   完全遵循 spec/docs/05-API接口设计文档.md 中定义的 JSON 响应格式。
 */
class MockApiInterceptor : Interceptor {

    companion object {
        const val TAG = "MockApi"
        
        /** ★★ 开关：设为 false 即可切换到真实后端 ★★ */
        var ENABLED = true
        
        // Mock用户数据（模拟已登录状态）
        const val MOCK_USER_ID = 1L
        const val MOCK_EMAIL = "user@example.com"
        const val MOCK_USERNAME = "阅读者"
        const val MOCK_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mock_access_token"
        const val MOCK_REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mock_refresh_token"
    }

    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!ENABLED) return chain.proceed(chain.request())

        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method.uppercase()

        Log.d(TAG, "[Mock] $method $path")

        // 解析请求体
        val bodyString = request.body?.let { body ->
            try {
                val buffer = Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            } catch (e: Exception) {
                Log.w(TAG, "无法解析请求体", e)
                null
            }
        }

        // 根据路由分发Mock响应
        val mockResponse = when {
            // POST /api/v1/auth/email-code → 发送验证码
            method == "POST" && path.contains("auth/email-code") -> handleSendEmailCode(bodyString)
            
            // POST /api/v1/auth/login → 登录
            method == "POST" && path.contains("auth/login") -> handleLogin(bodyString)
            
            // POST /api/v1/auth/refresh → 刷新Token
            method == "POST" && path.contains("auth/refresh") -> handleRefreshToken(bodyString)
            
            // POST /api/v1/auth/logout → 退出登录
            method == "POST" && path.contains("auth/logout") -> handleLogout()
            
            // GET /api/v1/users/me → 获取用户信息
            method == "GET" && path.contains("users/me") -> handleGetUserInfo()
            
            else -> createErrorResponse(10002, "未实现的Mock接口: $method $path")
        }

        return mockResponse.build()
    }

    // ==================== 发送验证码 ====================
    
    private fun handleSendEmailCode(bodyString: String?): Response.Builder {
        // 简单校验邮箱格式
        if (bodyString.isNullOrBlank()) {
            return createErrorResponse(10001, "参数错误：缺少email字段")
        }

        val request = try {
            gson.fromJson(bodyString, SendEmailCodeRequest::class.java)
        } catch (e: Exception) {
            return createErrorResponse(10001, "参数格式错误")
        }

        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        if (!emailRegex.matches(request.email)) {
            return createErrorResponse(20005, "邮箱格式不正确")
        }

        Log.i(TAG, "[Mock] 验证码已发送至 ${request.email}")

        val responseData = ApiResponse(
            code = 0,
            message = "验证码已发送",
            data = SendEmailCodeData(expiresIn = 300),
            timestamp = System.currentTimeMillis()
        )

        return createSuccessResponse(gson.toJson(responseData))
    }

    // ==================== 登录 ====================
    
    private fun handleLogin(bodyString: String?): Response.Builder {
        if (bodyString.isNullOrBlank()) {
            return createErrorResponse(10001, "参数错误")
        }

        val request = try {
            gson.fromJson(bodyString, LoginRequest::class.java)
        } catch (e: Exception) {
            return createErrorResponse(10001, "参数格式错误")
        }

        // 校验邮箱
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        if (!emailRegex.matches(request.email)) {
            return createErrorResponse(20005, "邮箱格式不正确")
        }

        // 校验验证码（Mock模式：任意6位数字均可）
        if (request.code.length != 6 || !request.code.all { it.isDigit() }) {
            return createErrorResponse(20006, "验证码错误")
        }
        
        // Mock：特定验证码触发不同场景（方便测试各种情况）
        when (request.code) {
            "000001" -> return createErrorResponse(20007, "验证码已过期")
            "000002" -> return createErrorResponse(20008, "验证码已使用")
            "000003" -> return createErrorResponse(20009, "账户已被封禁")
        }

        val isNewUser = request.email.endsWith(".new")  // 测试新用户场景
        val username = if (isNewUser) {
            request.email.substringBefore("@").take(10)
        } else MOCK_USERNAME

        Log.i(TAG, "[Mock] 登录成功: ${request.email}, 新用户=$isNewUser")

        val responseData = ApiResponse(
            code = 0,
            message = "登录成功",
            data = LoginResponseData(
                userId = MOCK_USER_ID,
                email = request.email,
                username = username,
                avatar = null,
                isNewUser = isNewUser,
                accessToken = MOCK_ACCESS_TOKEN,
                refreshToken = MOCK_REFRESH_TOKEN,
                expiresIn = 7200
            ),
            timestamp = System.currentTimeMillis()
        )

        return createSuccessResponse(gson.toJson(responseData))
    }

    // ==================== 刷新Token ====================
    
    private fun handleRefreshToken(bodyString: String?): Response.Builder {
        if (bodyString.isNullOrBlank()) {
            return createErrorResponse(10001, "缺少refreshToken")
        }

        val responseData = ApiResponse(
            code = 0,
            message = "success",
            data = RefreshTokenData(
                accessToken = MOCK_ACCESS_TOKEN + "_refreshed",
                refreshToken = MOCK_REFRESH_TOKEN + "_refreshed",
                expiresIn = 7200
            ),
            timestamp = System.currentTimeMillis()
        )

        return createSuccessResponse(gson.toJson(responseData))
    }

    // ==================== 退出登录 ====================
    
    private fun handleLogout(): Response.Builder {
        Log.i(TAG, "[Mock] 已退出登录")

        val responseData = ApiResponse<Unit>(
            code = 0,
            message = "退出成功",
            data = null,
            timestamp = System.currentTimeMillis()
        )

        return createSuccessResponse(gson.toJson(responseData))
    }

    // ==================== 获取用户信息 ====================
    
    private fun handleGetUserInfo(): Response.Builder {
        val responseData = ApiResponse(
            code = 0,
            message = "success",
            data = com.reading.my.data.network.model.UserInfo(
                id = MOCK_USER_ID,
                email = MOCK_EMAIL,
                username = MOCK_USERNAME,
                avatar = null,
                status = 1,
                createdAt = "2026-04-07T10:00:00.000Z"
            ),
            timestamp = System.currentTimeMillis()
        )

        return createSuccessResponse(gson.toJson(responseData))
    }

    // ==================== 响应构建工具 ====================

    private fun createSuccessResponse(jsonBody: String): Response.Builder {
        return Response.Builder()
            .request(Request.Builder().url(ApiConstants.BASE_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .header("Content-Type", "application/json")
            .body(jsonBody.toResponseBody("application/json".toMediaType()))
    }

    private fun createErrorResponse(code: Int, message: String): Response.Builder {
        val errorJson = gson.toJson(
            ApiResponse<Nothing>(
                code = code,
                message = message,
                data = null,
                timestamp = System.currentTimeMillis()
            )
        )

        return Response.Builder()
            .request(Request.Builder().url(ApiConstants.BASE_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(200) // HTTP 200, 但业务code为错误码
            .message("OK")
            .header("Content-Type", "application/json")
            .body(errorJson.toResponseBody("application/json".toMediaType()))
    }
}
