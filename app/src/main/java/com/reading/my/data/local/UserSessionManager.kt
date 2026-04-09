package com.reading.my.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_session"
)

/**
 * 用户会话管理器（单例）
 *
 * 使用 DataStore Preferences 持久化登录状态：
 * - accessToken / refreshToken / userId / email / username / avatar / loginTime
 * - 7天无活跃使用后自动过期，重新进入登录流程
 *
 * 【黑屏问题修复】
 * DataStore 是异步的，不会阻塞主线程。NavGraph 中通过 collect 首次发射来决定导航目标，
 * 而非同步阻塞读取，避免了启动时的黑屏/白屏。
 */
@Singleton
class UserSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /** Session 有效期：7天（毫秒） */
        const val SESSION_VALID_DURATION_MS = 7L * 24 * 60 * 60 * 1000L

        // ==================== DataStore Key 定义 ====================
        private val KEY_LOGGED_IN = booleanPreferencesKey("logged_in")
        private val KEY_USER_ID = longPreferencesKey("user_id")
        private val KEY_EMAIL = stringPreferencesKey("email")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_AVATAR = stringPreferencesKey("avatar")
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_LOGIN_TIME = longPreferencesKey("login_time") // 登录时间戳，用于计算7天过期

        const val TAG = "UserSession"
    }

    private val dataStore: DataStore<Preferences> = context.sessionDataStore

    // ==================== 是否已登录（Flow形式，供UI观察）====================

    /**
     * Flow<Boolean>：当前是否处于有效登录状态
     * 自动检查7天过期逻辑
     */
    val isLoggedInFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        val loggedIn = prefs[KEY_LOGGED_IN] == true
        val loginTime = prefs[KEY_LOGIN_TIME] ?: 0L

        if (loggedIn && loginTime > 0) {
            val elapsed = System.currentTimeMillis() - loginTime
            elapsed < SESSION_VALID_DURATION_MS
        } else {
            false
        }
    }

    // ==================== 同步读取（首次启动时用）====================

    /**
     * 同步判断是否已登录（用于 NavGraph 启动路由选择）
     * 注意：这是 suspend 函数，在协程中调用
     */
    suspend fun isLoggedIn(): Boolean {
        return isLoggedInFlow.first()
    }

    // ==================== 用户信息 Flow ====================

    data class SessionInfo(
        val userId: Long,
        val email: String,
        val username: String,
        val avatar: String?,
        val accessToken: String?,
        val refreshToken: String?
    )

    val sessionInfoFlow: Flow<SessionInfo?> = dataStore.data.map { prefs ->
        val loggedIn = prefs[KEY_LOGGED_IN] == true ?: false
        if (!loggedIn) return@map null

        SessionInfo(
            userId = prefs[KEY_USER_ID] ?: 0L,
            email = prefs[KEY_EMAIL] ?: "",
            username = prefs[KEY_USERNAME] ?: "",
            avatar = prefs[KEY_AVATAR],
            accessToken = prefs[KEY_ACCESS_TOKEN],
            refreshToken = prefs[KEY_REFRESH_TOKEN]
        )
    }

    // ==================== 写入操作 ====================

    /**
     * 保存登录会话信息（登录成功后调用）
     *
     * @param userId 用户ID
     * @param email 邮箱
     * @param username 用户名
     * @param avatar 头像URL（可为null）
     * @param accessToken 访问Token
     * @param refreshToken 刷新Token
     */
    suspend fun saveSession(
        userId: Long,
        email: String,
        username: String,
        avatar: String? = null,
        accessToken: String,
        refreshToken: String
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = true
            prefs[KEY_USER_ID] = userId
            prefs[KEY_EMAIL] = email
            prefs[KEY_USERNAME] = username
            prefs[KEY_AVATAR] = avatar ?: ""
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
            prefs[KEY_LOGIN_TIME] = System.currentTimeMillis() // 记录登录时间 → 7天过期依据
        }
        android.util.Log.i(TAG, "Session saved: userId=$userId, username=$username")
    }

    /**
     * 刷新最后活跃时间（每次用户打开APP时调用，重置7天计时）
     */
    suspend fun refreshLastActiveTime() {
        dataStore.edit { it[KEY_LOGIN_TIME] = System.currentTimeMillis() }
    }

    /**
     * 更新 Token（刷新 Token 成功后调用）
     */
    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
            prefs[KEY_LOGIN_TIME] = System.currentTimeMillis()
        }
        android.util.Log.d(TAG, "Tokens updated")
    }

    /**
     * 更新用户头像
     */
    suspend fun updateAvatar(avatarUrl: String) {
        dataStore.edit { it[KEY_AVATAR] = avatarUrl }
    }

    /**
     * 清除所有会话数据（退出登录时调用）
     */
    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
        android.util.Log.i(TAG, "Session cleared (logout)")
    }
}
