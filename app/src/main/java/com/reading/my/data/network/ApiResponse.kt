package com.reading.my.data.network

import com.google.gson.annotations.SerializedName

/**
 * 统一API响应格式
 * 对应后端规范: { code, message, data, timestamp }
 */
data class ApiResponse<T>(
    val code: Int = 0,
    val message: String = "",
    val data: T? = null,
    @SerializedName("timestamp")
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
data class PageData<T>(
    val content: List<T> = emptyList(),
    @SerializedName("totalElements")
    val totalElements: Int = 0,
    @SerializedName("totalPages")
    val totalPages: Int = 0,
    val size: Int = 20,
    val number: Int = 1,
    val first: Boolean = true,
    val last: Boolean = false
)
