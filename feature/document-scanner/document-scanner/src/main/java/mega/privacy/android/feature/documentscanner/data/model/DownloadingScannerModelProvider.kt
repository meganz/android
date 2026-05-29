package mega.privacy.android.feature.documentscanner.data.model

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.feature.documentscanner.di.ScannerModelHttpClient
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads the TFLite boundary-detection model on first use and caches it in
 * the app's private files dir, verified against a hardcoded SHA-256 + size.
 *
 * The download streams to a `.tmp` sidecar and is only renamed into place once
 * verification passes, so a partial/corrupt download never leaves a bad file
 * that TFLite would later crash on. A cached file that fails verification (e.g.
 * a future model swap) is deleted and re-downloaded.
 */
@Singleton
internal class DownloadingScannerModelProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ScannerModelHttpClient private val client: OkHttpClient,
) : ScannerModelProvider {

    private val modelFile: File
        get() = File(context.filesDir, "$MODELS_DIR/$MODEL_FILE_NAME")

    override fun cachedModelFile(): File? =
        modelFile.takeIf { it.isFile && it.length() == MODEL_SIZE_BYTES }

    override suspend fun ensureModelReady(): File = withContext(ioDispatcher) {
        cachedModelFile()?.let { cached ->
            Timber.d("[DocScanner][model] Using cached model at ${cached.absolutePath}")
            return@withContext cached
        }
        download()
    }

    private fun download(): File {
        val target = modelFile.apply { parentFile?.mkdirs() }
        val tmp = File(target.parentFile, "$MODEL_FILE_NAME.tmp")
        tmp.delete()

        Timber.d("[DocScanner][model] Downloading model …")
        val request = Request.Builder().url(MODEL_URL).build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) {
                "Model download failed: HTTP ${response.code}"
            }
            tmp.outputStream().use { out -> response.body.byteStream().copyTo(out) }
        }

        verifyOrThrow(tmp)

        if (!tmp.renameTo(target)) {
            error("Failed to move downloaded model into place")
        }
        Timber.d("[DocScanner][model] Model ready at ${target.absolutePath}")
        return target
    }

    private fun verifyOrThrow(file: File) {
        val size = file.length()
        if (size != MODEL_SIZE_BYTES) {
            file.delete()
            error("Model size mismatch: $size != $MODEL_SIZE_BYTES")
        }
        val actualSha = file.sha256Hex()
        if (actualSha != MODEL_SHA256) {
            file.delete()
            error("Model checksum mismatch: $actualSha != $MODEL_SHA256")
        }
    }

    private fun File.sha256Hex(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val MODELS_DIR = "scanner-models"
        const val MODEL_FILE_NAME = "midv500_unet.tflite"

        // TEMPORARY staging link — replace with the production model endpoint
        // (ideally sourced from remote config) before rollout. Tracked in the
        // tech spec's "Model distribution" section.
        const val MODEL_URL =
            "https://staging.api.mega.co.nz/cs/dl?66fe68428762e81bfd888576993f5a756a692ac35152d1044a2604121f199fb56253924c27c15e4058208de25b433a05"

        // Integrity guards for the downloaded artifact. Regenerate both if the
        // model is ever re-trained / re-exported.
        const val MODEL_SHA256 = "e0c37a9a9590efa228696bbd9e882a46c2c60ae440641c4a0e77d3f3860b4e55"
        const val MODEL_SIZE_BYTES = 97_867_228L
    }
}
