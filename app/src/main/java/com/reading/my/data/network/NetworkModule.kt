package com.reading.my.data.network

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import javax.inject.Singleton

/**
 * 网络层 Hilt 依赖注入模块
 * 
 * 提供单例：
 *   - OkHttp Client (含日志拦截器 + 认证拦截器 + Mock拦截器)
 *   - Retrofit 实例
 *   - ApiService 接口实现
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            // 忽略未知字段（后端新增字段时不会崩溃）
            ignoreUnknownKeys = true
            // 允许宽松的JSON格式
            isLenient = true
            // 不强制要求@Serializable注解（可选）
            coerceInputValues = true
            // 显式null值（可选，根据后端规范调整）
            explicitNulls = false
        }
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY  // Debug时用BODY，Release改为NONE
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(@ApplicationContext context: Context): AuthInterceptor {
        return AuthInterceptor(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(ApiConstants.CONNECT_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(ApiConstants.READ_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(ApiConstants.WRITE_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
            // 日志拦截器 - 最先执行
            .addInterceptor(loggingInterceptor)
            // ★★★ Mock拦截器 - 后端接入前启用 ★★★
            .addInterceptor(MockApiInterceptor())
            // 认证拦截器 - 最后执行（添加Header）
            .addInterceptor(authInterceptor)
            // 重试机制（可选）
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
