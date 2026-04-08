package com.reading.my.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== 发送验证码请求 ====================

/**
 * 发送邮箱验证码请求体
 * POST /api/v1/auth/email-code
 */
@Serializable
data class SendEmailCodeRequest(
    val email: String
)

/**
 * 发送验证码响应数据
 */
@Serializable
data class SendEmailCodeData(
    @SerialName("expiresIn")
    val expiresIn: Int = 300  // 验证码有效期(秒)
)

// ==================== 登录请求/响应 ====================

/**
 * 登录请求体（邮箱验证码方式）
 * POST /api/v1/auth/login
 */
@Serializable
data class LoginRequest(
    val email: String,
    val code: String,
    @SerialName("deviceId")
    val deviceId: String? = null
)

/**
 * 登录响应数据
 * 包含用户基本信息 + Token信息
 */
@Serializable
data class LoginResponseData(
    @SerialName("userId")
    val userId: Long = 0,
    val email: String = "",
    val username: String = "",
    @SerialName("avatar")
    val avatar: String? = null,
    @SerialName("isNewUser")
    val isNewUser: Boolean = false,
    @SerialName("accessToken")
    val accessToken: String = "",
    @SerialName("refreshToken")
    val refreshToken: String = "",
    @SerialName("expiresIn")
    val expiresIn: Int = 7200  // Token过期时间(秒)
)

// ==================== 刷新Token ====================

/**
 * 刷新Token请求体
 */
@Serializable
data class RefreshTokenRequest(
    @SerialName("refreshToken")
    val refreshToken: String
)

/**
 * 刷新Token响应数据
 */
@Serializable
data class RefreshTokenData(
    @SerialName("accessToken")
    val accessToken: String = "",
    @SerialName("refreshToken")
    val refreshToken: String = "",
    @SerialName("expiresIn")
    val expiresIn: Int = 7200
)
