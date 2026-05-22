package org.acoustixaudio.opiqo.remotecontroloverssh.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object KeyFileHelper {
    private const val KEYS_DIR = "ssh_keys"

    fun copyKeyToInternal(context: Context, uri: Uri, fileName: String): String? {
        val keysDir = File(context.filesDir, KEYS_DIR)
        if (!keysDir.exists()) {
            keysDir.mkdirs()
        }

        val destFile = File(keysDir, fileName)
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteKey(path: String) {
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
    }
}
