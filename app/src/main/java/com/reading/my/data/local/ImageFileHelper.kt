package com.reading.my.data.local

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.File
import java.io.FileOutputStream

/**
 * 图片文件工具类
 *
 * 职责：
 * - 将 Base64 字符串解码并写入本地文件系统（app私有目录）
 * - 返回 file:// URI 供 Coil AsyncImage 直接加载
 * - 支持封面和头像两种存储位置，自动覆盖同名旧文件
 *
 * 为什么不用 data URI？
 *   Coil 对超大 Base64 data URI (>50KB) 渲染不稳定/失败，
 *   本地文件是 Coil 最可靠的加载方式。
 */
object ImageFileHelper {

    private const val COVER_DIR = "covers"
    private const val AVATAR_FILE = "avatar.jpg"
    private const val TAG = "ImageFileHelper"

    /**
     * 将 Base64 解码为本地封面图片文件
     *
     * @param context Android Context
     * @param base64  纯 base64 字符串（不含 data: 前缀）
     * @param bookId   书籍ID，用于生成唯一文件名
     * @return file:// URI，如 "file:///data/data/com.reading.my/files/covers/book_4.jpg"；失败返回 null
     */
    fun saveCoverFromBase64(context: Context, base64: String, bookId: Long): String? {
        return saveBase64ToFile(
            context = context,
            base64 = base64,
            subDir = COVER_DIR,
            fileName = "book_$bookId.jpg"
        )
    }

    /**
     * 将 Base64 解码为本地头像文件
     *
     * @param context Android Context
     * @param base64  纯 base64 字符串（不含 data: 前缀）
     * @return file:// URI；失败返回 null
     */
    fun saveAvatarFromBase64(context: Context, base64: String): String? {
        return saveBase64ToFile(
            context = context,
            base64 = base64,
            subDir = null, // 头像放 files 根目录
            fileName = AVATAR_FILE
        )
    }

    /**
     * 删除指定书籍的封面缓存文件
     */
    fun deleteCover(context: Context, bookId: Long) {
        try {
            val dir = File(context.filesDir, COVER_DIR)
            val file = File(dir, "book_$bookId.jpg")
            if (file.exists()) {
                file.delete()
                android.util.Log.d(TAG, "删除封面文件: ${file.absolutePath}")
            }
        } catch (_: Exception) {}
    }

    /**
     * 删除头像缓存文件
     */
    fun deleteAvatar(context: Context) {
        try {
            val file = File(context.filesDir, AVATAR_FILE)
            if (file.exists()) {
                file.delete()
                android.util.Log.d(TAG, "删除头像文件: ${file.absolutePath}")
            }
        } catch (_: Exception) {}
    }

    // ── 内部实现 ──────────────────────────────────────

    private fun saveBase64ToFile(context: Context, base64: String, subDir: String?, fileName: String): String? {
        try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)

            val targetDir = if (subDir != null) {
                File(context.filesDir, subDir).apply { if (!exists()) mkdirs() }
            } else {
                context.filesDir
            }

            val file = File(targetDir, fileName)
            FileOutputStream(file).use { it.write(bytes) }

            val uri = Uri.fromFile(file).toString()
            android.util.Log.i(TAG, "文件已保存: $uri (${bytes.size} bytes)")
            return uri
        } catch (e: Exception) {
            android.util.Log.e(TAG, "保存图片文件失败", e)
            return null
        }
    }
}
