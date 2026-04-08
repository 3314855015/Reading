package com.reading.my.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 用户信息模型
 * GET /api/v1/users/me 响应
 */
@Serializable
data class UserInfo(
    val id: Long = 0,
    val email: String = "",
    val username: String = "",
    @SerialName("avatar")
    val avatar: String? = null,
    val status: Int = 1,  // 1-正常, 0-封禁
    @SerialName("createdAt")
    val createdAt: String? = null
)
