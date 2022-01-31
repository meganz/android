package mega.privacy.android.app.uploadFolder.list.data

import java.io.Serializable

data class UploadFolderResult(
    val absolutePath: String,
    val name: String,
    val lastModified: Long,
    val parentHandle: Long
) : Serializable
