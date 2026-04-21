package com.reading.my.data.network

import com.reading.my.data.local.UserSessionManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Token认证拦截器
 * 从 UserSessionManager (DataStore) 读取 accessToken，自动附加到请求头
 */
class AuthInterceptor(
    private val sessionManager: UserSessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            sessionManager.sessionInfoFlow.firstOrNull()?.accessToken
        }

        val request = chain.request().newBuilder()
            .apply {
                if (!token.isNullOrBlank()) {
                    header("Authorization", "Bearer $token")
                }
                header("X-Platform", "android")
                header("Accept-Language", "zh-CN")
            }
            .build()

        return chain.proceed(request)
    }
}
