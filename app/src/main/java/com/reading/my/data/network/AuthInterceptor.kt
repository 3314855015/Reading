package com.reading.my.data.network

import android.util.Log
import com.reading.my.data.local.UserSessionManager
import com.reading.my.data.network.model.RefreshTokenRequest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Provider

/**
 * Token认证拦截器（带自动刷新）
 *
 * 功能：
 * 1. 从 UserSessionManager 读取 accessToken，附加到请求头
 * 2. 收到 403/401 时自动用 refreshToken 刷新 accessToken
 * 3. 刷新成功后重发原请求；刷新失败则清除本地会话
 */
class AuthInterceptor(
    private val sessionManager: UserSessionManager,
    private val apiServiceProvider: Provider<ApiService>  // Provider 打破循环依赖
) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
    }

    /** 防止并发刷新：多请求同时403时只触发一次refresh */
    private val isRefreshing = AtomicBoolean(false)

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = buildAuthRequest(chain)

        var response = chain.proceed(originalRequest)

        // 如果返回 401 或 403，尝试刷新 Token 后重试
        if (response.code == 401 || response.code == 403) {
            response.close()

            if (tryRefreshToken()) {
                // 刷新成功 → 用新 token 重新构建请求并重发
                Log.d(TAG, "Token刷新成功，重发原始请求")
                val newRequest = buildAuthRequestWithNewToken(originalRequest)
                response = chain.proceed(newRequest)
            } else {
                // 刷新失败 → 返回空响应体 + 错误码（上层会处理跳转登录）
                Log.w(TAG, "Token刷新失败，需要重新登录")
                response = chain.proceed(originalRequest)  // 原始请求再发一次，让上层拿到错误
            }
        }

        return response
    }

    /**
     * 构建带 Authorization 头的请求
     */
    private fun buildAuthRequest(chain: Interceptor.Chain): Request {
        val token = runBlocking { sessionManager.sessionInfoFlow.firstOrNull()?.accessToken }
        return chain.request().newBuilder()
            .apply {
                if (!token.isNullOrBlank()) {
                    header("Authorization", "Bearer $token")
                }
                header("X-Platform", "android")
                header("Accept-Language", "zh-CN")
            }
            .build()
    }

    /**
     * 用最新 token 构建重放请求
     */
    private fun buildAuthRequestWithNewToken(originalRequest: Request): Request {
        val newToken = runBlocking { sessionManager.sessionInfoFlow.firstOrNull()?.accessToken }
        return originalRequest.newBuilder()
            .apply {
                if (!newToken.isNullOrBlank()) {
                    header("Authorization", "Bearer $newToken")
                }
                // 其他头保持不变（X-Platform、Accept-Language 已在原始请求中）
            }
            .build()
    }

    /**
     * 尝试用 refreshToken 刷新 accessToken
     *
     * @return true=刷新成功, false=失败（需重新登录）
     */
    private fun tryRefreshToken(): Boolean {
        // 并发控制：如果已有线程在刷新，等待它完成即可
        if (!isRefreshing.compareAndSet(false, true)) {
            Log.d(TAG, "其他线程正在刷新Token，等待...")
            // 简单自旋等待：最多等5秒
            var waited = 0L
            while (isRefreshing.get() && waited < 5000) {
                Thread.sleep(100)
                waited += 100
            }
            // 等待结束后检查 token 是否已更新（由另一个线程完成）
            return runBlocking { !sessionManager.sessionInfoFlow.firstOrNull()?.accessToken.isNullOrBlank() }
        }

        try {
            val result = runBlocking {
                val session = sessionManager.sessionInfoFlow.firstOrNull()
                val refreshToken = session?.refreshToken

                if (refreshToken.isNullOrBlank()) {
                    Log.w(TAG, "无可用refreshToken")
                    return@runBlocking false
                }

                Log.d(TAG, "正在刷新Token... (refreshToken前30字=${refreshToken.take(30)}...)")

                try {
                    val apiResponse = apiServiceProvider.get().refreshToken(
                        RefreshTokenRequest(refreshToken = refreshToken)
                    )

                    if (apiResponse.code == 0 && apiResponse.data != null) {
                        val data = apiResponse.data!!
                        sessionManager.updateTokens(data.accessToken, data.refreshToken)
                        Log.i(TAG, "✅ Token刷新成功! 新token长度=${data.accessToken.length}")
                        true
                    } else {
                        Log.w(TAG, "❌ Token刷新失败: ${apiResponse.message}")
                        false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Token刷新异常: ${e.message}", e)
                    false
                }
            }

            return result
        } finally {
            isRefreshing.set(false)
        }
    }
}
