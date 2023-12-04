package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.entity.node.ViewerNode

/**
 * Use case for downloading a file in background.
 */
@Deprecated(
    message = "ViewerNode should be replaced by [TypedNode], there's a similar use-case to download any type of [TypedNode] and receive a flow of the progress: StartDownloadUseCase. Please add [TransferAppData.BackgroundTransfer] to avoid this transfers to be added in the counters of the DownloadService notification",
    replaceWith = ReplaceWith("StartDownloadUseCase"),
)
fun interface DownloadBackgroundFile {

    /**
     * Invoke.
     *
     * @param node  [ViewerNode] of the file to download.
     * @return The path of the downloaded file.
     */
    suspend operator fun invoke(node: ViewerNode): String
}