package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Use case for downloading a file in background.
 */
fun interface DownloadBackgroundFile {

    /**
     * Invoke.
     *
     * @param node  MegaNode of the file to download.
     * @return The path of the downloaded file.
     */
    suspend operator fun invoke(node: MegaNode): String
}