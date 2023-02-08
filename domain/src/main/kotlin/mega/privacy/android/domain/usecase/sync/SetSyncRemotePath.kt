package mega.privacy.android.domain.usecase.sync

import mega.privacy.android.domain.entity.sync.RemoteFolder

/**
 * Sets the path to remote folder that the user has selected
 */
fun interface SetSyncRemotePath {

    operator fun invoke(path: RemoteFolder)
}
