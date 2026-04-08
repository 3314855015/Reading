package com.reading.my.data.network.model

import com.google.gson.annotations.SerializedName

/**
 * 用户信息模型
 * GET /api/v1/users/me 响应
 */
data class UserInfo(
    val id: Long = 0,
    val email: String = "",
    val username: String = "",
    @SerializedName("avatar")
    val avatar: String? = null,
    val status: Int = 1,  // 1-正常, 0-封禁
    @SerializedName("createdAt")
    val createdAt: String? = null
)
