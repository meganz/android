package mega.privacy.android.data.gateway.zipremote

import mega.privacy.android.domain.entity.zipbrowser.RemoteZipArchiveSource

/**
 * The local streaming link a remote archive is read through.
 */
internal interface RemoteArchiveUrlGateway {

    /**
     * @return null when the node cannot be found or the server cannot serve it
     */
    suspend fun urlFor(source: RemoteZipArchiveSource): String?

    /**
     * Starts the streaming server if it is not running, for when another screen stopped it while
     * the archive was still being read. A running server is left alone because other players
     * stream through it.
     */
    suspend fun ensureServerRunning()
}
