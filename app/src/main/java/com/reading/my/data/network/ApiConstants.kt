package com.reading.my.data.network

object ApiConstants {
    // 手机和电脑同一局域网，直连 Spring Boot（不需要 Nginx）
    // 电脑 IPv4 地址 + Spring Boot 端口 8080
    const val BASE_URL = "http://192.168.1.108:8080/"
    const val API_VERSION = "v1"

    // 认证接口
    const val EMAIL_CODE = "api/$API_VERSION/auth/email-code"
    const val LOGIN = "api/$API_VERSION/auth/login"
    const val REFRESH_TOKEN = "api/$API_VERSION/auth/refresh"
    const val LOGOUT = "api/$API_VERSION/auth/logout"

    // 用户接口
    const val USER_ME = "api/$API_VERSION/users/me"

    // 超时时间
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}
