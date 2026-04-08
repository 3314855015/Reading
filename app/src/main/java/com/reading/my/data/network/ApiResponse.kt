package com.reading.my.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 统一API响应格式
 * 对应后端规范: { code, message, data, timestamp }
 */
@Serializable
data class ApiResponse<T>(
    val code: Int = 0,
    val message: String = "",
    val data: T? = null,
    @SerialName("timestamp")
    val timestamp: Long? = null
) {
    /** 判断是否成功 (code == 0) */
    fun isSuccess(): Boolean = code == 0

    /** 获取错误信息 */
    fun getErrorMessage(): String =
        if (message.isNotBlank()) message else "未知错误"
}

/**
 * 分页响应数据
 */
@Serializable
data class PageData<T>(
    val content: List<T> = emptyList(),
    @SerialName("totalElements")
    val totalElements: Int = 0,
    @SerialName("totalPages")
    val totalPages: Int = 0,
    val size: Int = 20,
    val number: Int = 1,
    val first: Boolean = true,
    val last: Boolean = false
)
