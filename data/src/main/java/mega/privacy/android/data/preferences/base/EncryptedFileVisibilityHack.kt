// We use fake package to hack package-private visibility
@file:Suppress("PackageDirectoryMismatch")

package androidx.security.crypto

import com.google.crypto.tink.StreamingAead
import java.io.File

internal val EncryptedFile.file: File get() = mFile
internal val EncryptedFile.streamingAead: StreamingAead get() = mStreamingAead
