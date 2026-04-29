package com.reading.my.ui.screens.sync

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reading.my.domain.model.ChapterSyncItem
import com.reading.my.domain.model.SyncImportResult
import com.reading.my.domain.model.SyncPayload
import com.reading.my.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * 同步导入 ViewModel
 *
 * 负责：解析 Cwriter 的 JSON payload → 调用 Repository 导入 → 暴露 UI 状态
 */
@HiltViewModel
class SyncImportViewModel @Inject constructor(
    private val bookRepository: BookRepository,
) : ViewModel() {

    companion object {
        const val TAG = "SyncImport"
    }

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    data class SyncUiState(
        val isLoading: Boolean = false,
        val isImporting: Boolean = false,
        /** 解析后的同步数据（null 表示尚未解析或解析失败） */
        val payload: SyncPayload? = null,
        /** 原始 JSON 字符串（用于显示） */
        val rawJson: String? = null,
        /** 导入结果 */
        val result: SyncImportResult? = null,
        /** 错误信息 */
        val errorMessage: String? = null,
    )

    /**
     * 解析 Cwriter 发送的 JSON Payload
     *
     * @param jsonStr Intent extra 中的 JSON 字符串
     */
    fun parsePayload(jsonStr: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val payload = parseSyncPayloadJson(jsonStr)
                Log.i(TAG, "Payload 解析成功: 「${payload.bookTitle}」${payload.chapters.size}章")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    payload = payload,
                    rawJson = jsonStr,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Payload 解析失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "数据格式错误：${e.message}",
                )
            }
        }
    }

    /**
     * 执行导入操作
     */
    fun executeImport() {
        val payload = _uiState.value.payload
        if (payload == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "没有可导入的数据")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, errorMessage = null, result = null)

            try {
                val result = bookRepository.importFromSyncPayload(payload)
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    result = result,
                )
                if (!result.success) {
                    _uiState.value = _uiState.value.copy(errorMessage = result.message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "导入异常", e)
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    errorMessage = "导入异常：${e.message}",
                )
            }
        }
    }

    /** 重置状态（返回时清理） */
    fun reset() {
        _uiState.value = SyncUiState()
    }

    // ==================== JSON 解析 ====================

    private fun parseSyncPayloadJson(jsonStr: String): SyncPayload {
        val json = JSONObject(jsonStr)

        // 记录原始 payload 大小（用于诊断传输是否完整）
        Log.i(TAG, "📦 原始 JSON 大小: ${jsonStr.length} 字符, ${jsonStr.toByteArray().size / 1024}KB")

        val chaptersArray = json.optJSONArray("chapters") ?: JSONArray()
        Log.i(TAG, "📦 章节数组长度: ${chaptersArray.length()}")
        Log.d(TAG, "📦 book_id=${json.optString("book_id", "?")}, sync_version=${json.optInt("sync_version", -1)}")

        val chapters = mutableListOf<ChapterSyncItem>()

        for (i in 0 until chaptersArray.length()) {
            val ch = chaptersArray.getJSONObject(i)
            val rawContent = ch.optString("content", "")
            // 记录每个章节的原始内容长度
            Log.d(TAG, "  📖 [ch${ch.optInt("chapter_index", i)}] title='${ch.optString("title", "?").take(15)}' contentLen=${rawContent.length}")

            chapters.add(
                ChapterSyncItem(
                    chapterId = ch.getString("chapter_id"),
                    chapterIndex = ch.getInt("chapter_index"),
                    title = ch.optString("title", "第${ch.getInt("chapter_index") + 1}章"),
                    content = rawContent,
                    contentHash = ch.optString("contentHash", ""),
                    volumeName = ch.optString("volume_name", null).ifEmpty { null },
                )
            )
        }

        val totalContentLen = chapters.sumOf { it.content.length }
        Log.i(TAG, "📦 解析完成: 「${json.optString("book_title", "?")}」${chapters.size}章, 总内容=${totalContentLen}字符, v${json.optInt("sync_version", 0)}")

        return SyncPayload(
            bookId = json.getString("book_id"),
            bookTitle = json.optString("book_title", "未命名"),
            author = json.optString("author", "未知作者"),
            description = json.optString("description", null).ifEmpty { null },
            syncVersion = json.optInt("sync_version", 0),
            lastModified = json.optLong("last_modified", 0L),
            chapters = chapters,
        )
    }
}
