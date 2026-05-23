package org.acoustixaudio.opiqo.remotecontroloverssh.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object KeyFileHelper {
    private const val KEYS_DIR = "ssh_keys"

    fun copyKeyToInternal(context: Context, uri: Uri, fileName: String): Result<String> {
        val keysDir = File(context.filesDir, KEYS_DIR)
        if (!keysDir.exists()) {
            keysDir.mkdirs()
        }

        val destFile = File(keysDir, fileName)
        return runCatching {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Unable to open the selected private key file.")
            inputStream.use { input ->
                FileOutputStream(destFile).use { outputStream ->
                    input.copyTo(outputStream)
                }
            }
            destFile.absolutePath
        }
    }

    fun deleteKey(path: String) {
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
    }
}
