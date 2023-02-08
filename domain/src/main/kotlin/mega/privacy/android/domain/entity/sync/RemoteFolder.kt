package mega.privacy.android.domain.entity.sync

/**
 * Represents a folder on Remote Storage.
 * @param id the id of the remote folder
 * @param name name of the folder
 */
data class RemoteFolder(
    val id: Long,
    val name: String,
)
