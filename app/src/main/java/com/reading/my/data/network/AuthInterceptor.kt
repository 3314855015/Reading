package com.reading.my.data.network

import android.content.Context
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Token认证拦截器
 * 自动为请求添加 Authorization: Bearer {token} 头
 *
 * 后端接入后，Token从本地存储读取即可
 */
class AuthInterceptor(
    private val context: Context  // 后续替换为 TokenManager
) : Interceptor {

    private var currentToken: String? = null

    fun setToken(token: String?) {
        currentToken = token
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val requestWithAuth = if (!currentToken.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .header("X-Platform", "android")
                .header("Accept-Language", "zh-CN")
                .build()
        } else {
            originalRequest.newBuilder()
                .header("X-Platform", "android")
                .header("Accept-Language", "zh-CN")
                .build()
        }

        return chain.proceed(requestWithAuth)
    }
}
