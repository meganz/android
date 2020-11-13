package mega.privacy.android.app.audioplayer.trackinfo

import java.io.File

data class NodeInfo(
    val thumbnail: File,
    val availableOffline: Boolean,
    val size: String,
    val location: String,
    val added: String,
    val lastModified: String,
)
