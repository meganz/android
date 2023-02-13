package mega.privacy.android.feature.sync.domain.usecase

import mega.privacy.android.feature.sync.domain.entity.RemoteFolder

/**
 * Sets the path to remote folder that the user has selected
 */
fun interface SetSyncRemotePath {

    operator fun invoke(path: RemoteFolder)
}
