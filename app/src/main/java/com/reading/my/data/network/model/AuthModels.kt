package com.reading.my.data.network.model

import com.google.gson.annotations.SerializedName

// ==================== 发送验证码请求 ====================

/**
 * 发送邮箱验证码请求体
 * POST /api/v1/auth/email-code
 */
data class SendEmailCodeRequest(
    val email: String
)

/**
 * 发送验证码响应数据
 */
data class SendEmailCodeData(
    @SerializedName("expiresIn")
    val expiresIn: Int = 300  // 验证码有效期(秒)
)

// ==================== 登录请求/响应 ====================

/**
 * 登录请求体（邮箱验证码方式）
 * POST /api/v1/auth/login
 */
data class LoginRequest(
    val email: String,
    val code: String,
    @SerializedName("deviceId")
    val deviceId: String? = null
)

/**
 * 登录响应数据
 * 包含用户基本信息 + Token信息
 */
data class LoginResponseData(
    @SerializedName("userId")
    val userId: Long = 0,
    val email: String = "",
    val username: String = "",
    @SerializedName("avatar")
    val avatar: String? = null,
    @SerializedName("isNewUser")
    val isNewUser: Boolean = false,
    @SerializedName("accessToken")
    val accessToken: String = "",
    @SerializedName("refreshToken")
    val refreshToken: String = "",
    @SerializedName("expiresIn")
    val expiresIn: Int = 7200  // Token过期时间(秒)
)

// ==================== 刷新Token ====================

/**
 * 刷新Token请求体
 */
data class RefreshTokenRequest(
    @SerializedName("refreshToken")
    val refreshToken: String
)

/**
 * 刷新Token响应数据
 */
data class RefreshTokenData(
    @SerializedName("accessToken")
    val accessToken: String = "",
    @SerializedName("refreshToken")
    val refreshToken: String = "",
    @SerializedName("expiresIn")
    val expiresIn: Int = 7200
)
