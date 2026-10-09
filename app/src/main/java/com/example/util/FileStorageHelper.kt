package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileStorageHelper {
    private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10MB limit

    fun saveBitmap(context: Context, bitmap: Bitmap): String? {
        return try {
            val reportsDir = File(context.filesDir, "waste_reports").apply { if (!exists()) mkdirs() }
            val filename = "waste_report_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
            val file = File(reportsDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (file.exists() && file.length() > 0) file.absolutePath else null
        } catch (_: Exception) {
            null
        }
    }

    fun copyUriToInternal(context: Context, sourceUri: Uri): String? {
        return try {
            val reportsDir = File(context.filesDir, "waste_reports").apply { if (!exists()) mkdirs() }
            val filename = "waste_report_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
            val destFile = File(reportsDir, filename)
            var bytesCopied = 0L

            val stream = context.contentResolver.openInputStream(sourceUri) ?: return null
            stream.use { input: InputStream ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        bytesCopied += read
                        if (bytesCopied > MAX_FILE_SIZE_BYTES) {
                            destFile.delete()
                            return null
                        }
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }

            if (destFile.exists() && destFile.length() > 0) {
                destFile.absolutePath
            } else {
                destFile.delete()
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
