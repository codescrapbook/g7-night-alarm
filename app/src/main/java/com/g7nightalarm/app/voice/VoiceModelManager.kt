package com.g7nightalarm.app.voice

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

object VoiceModelManager {
    private const val TAG = "VoiceModelManager"
    private const val MODEL_DIR_NAME = "vosk-model-small-en-us"
    private const val ZIP_FILE_NAME = "vosk-model-small-en-us-0.15.zip"
    private const val MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
    // SHA-256 for 0.15 zip
    private const val ZIP_SHA256 = "30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498"

    fun modelDir(context: Context): File = File(context.filesDir, MODEL_DIR_NAME)

    fun hasModel(context: Context): Boolean {
        val dir = modelDir(context)
        return dir.exists() && dir.isDirectory && dir.list()?.isNotEmpty() == true
    }

    suspend fun ensureModelInstalled(
        context: Context,
        onProgress: (Int) -> Unit
    ): Boolean {
        if (hasModel(context)) return true
        val client = OkHttpClient()
        val tmpZip = File(context.cacheDir, ZIP_FILE_NAME)
        repeat(3) { attempt ->
            try {
                // download
                val req = Request.Builder().url(MODEL_URL).build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                    val total = resp.body?.contentLength() ?: -1L
                    resp.body?.source()?.let { source ->
                        tmpZip.sink().buffer().use { sink ->
                            var read = 0L
                            val buf = okio.Buffer()
                            var lastPct = -1
                            while (true) {
                                val n = source.read(buf, 8192)
                                if (n == -1L) break
                                sink.write(buf, n)
                                read += n
                                if (total > 0) {
                                    val pct = (read * 100 / total).toInt()
                                    if (pct != lastPct) {
                                        lastPct = pct
                                        onProgress(pct)
                                    }
                                }
                            }
                        }
                    }
                }
                // checksum
                val ok = verifySha256(tmpZip, ZIP_SHA256)
                if (!ok) throw IllegalStateException("Checksum failed")
                // unzip
                unzip(tmpZip, context.filesDir)
                // rename extracted folder to MODEL_DIR_NAME if root folder name differs
                val extracted = File(context.filesDir, "vosk-model-small-en-us-0.15")
                if (extracted.exists() && extracted.isDirectory) {
                    extracted.renameTo(modelDir(context))
                }
                tmpZip.delete()
                return hasModel(context)
            } catch (t: Throwable) {
                Log.e(TAG, "Download/install failed attempt ${attempt + 1}", t)
                tmpZip.delete()
                onProgress(0)
                // backoff
                kotlinx.coroutines.delay(1500L * (attempt + 1))
            }
        }
        return false
    }

    private fun verifySha256(file: File, expectedHex: String): Boolean {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buf = ByteArray(8192)
            while (true) {
                val n = fis.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        val hex = md.digest().joinToString("") { "%02x".format(it) }
        return hex.equals(expectedHex, ignoreCase = true)
    }

    private fun unzip(zipFile: File, targetDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { out -> zis.copyTo(out) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}

