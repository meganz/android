package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.entity.node.ViewerNode

/**
 * Use case for downloading a file in background.
 */
fun interface DownloadBackgroundFile {

    /**
     * Invoke.
     *
     * @param node  [ViewerNode] of the file to download.
     * @return The path of the downloaded file.
     */
    suspend operator fun invoke(node: ViewerNode): String
}