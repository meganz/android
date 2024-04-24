package mega.privacy.android.data.preferences.base

import androidx.datastore.core.Serializer
import com.google.crypto.tink.StreamingAead
import java.io.InputStream
import java.io.OutputStream

internal class StreamingAeadEncryptingSerializer<T>(
    private val streamingAead: StreamingAead,
    private val associatedData: ByteArray,
    val delegate: Serializer<T>,
) : Serializer<T> {

    override val defaultValue: T
        get() = delegate.defaultValue

    override suspend fun readFrom(input: InputStream): T {
        return streamingAead.newDecryptingStream(input, associatedData).use { decryptingStream ->
            delegate.readFrom(decryptingStream)
        }
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        streamingAead.newEncryptingStream(output, associatedData).use { encryptingStream ->
            delegate.writeTo(t, encryptingStream)
        }
    }
}

/**
 * Adds encryption to [this] serializer using the given [StreamingAead] and [associatedData]
 * as an associated authenticated data.
 *
 * Associated data is authenticated but not encrypted. In some cases, binding ciphertext
 * to associated data strengthens security:
 * [I want to bind ciphertext to its context](https://developers.google.com/tink/bind-ciphertext)
 */
internal fun <T> Serializer<T>.encrypted(
    streamingAead: StreamingAead,
    associatedData: ByteArray = byteArrayOf(),
) = StreamingAeadEncryptingSerializer(streamingAead, associatedData, delegate = this)
