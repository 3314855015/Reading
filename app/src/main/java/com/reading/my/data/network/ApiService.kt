package com.reading.my.data.network

import com.reading.my.data.network.model.*
import retrofit2.http.*

/**
 * API服务接口
 * 定义所有后端API调用
 */
interface ApiService {

    // ==================== 认证相关 ====================

    /**
     * 发送邮箱验证码
     * POST /api/v1/auth/email-code
     */
    @POST(ApiConstants.EMAIL_CODE)
    suspend fun sendEmailCode(
        @Body request: SendEmailCodeRequest
    ): ApiResponse<SendEmailCodeData>

    /**
     * 邮箱验证码登录（登录即注册）
     * POST /api/v1/auth/login
     */
    @POST(ApiConstants.LOGIN)
    suspend fun login(
        @Body request: LoginRequest
    ): ApiResponse<LoginResponseData>

    /**
     * 刷新AccessToken
     * POST /api/v1/auth/refresh
     */
    @POST(ApiConstants.REFRESH_TOKEN)
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): ApiResponse<RefreshTokenData>

    /**
     * 退出登录
     * POST /api/v1/auth/logout
     */
    @POST(ApiConstants.LOGOUT)
    suspend fun logout(): ApiResponse<Unit>

    // ==================== 用户相关 ====================

    /**
     * 获取当前用户信息
     * GET /api/v1/users/me
     */
    @GET(ApiConstants.USER_ME)
    suspend fun getUserInfo(): ApiResponse<UserInfo>
}
