package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Use case to collect and save photos and videos for camera upload
 */
interface ProcessMediaForUpload {

    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke(
        primaryUploadNode: MegaNode?,
        secondaryUploadNode: MegaNode?,
        tempRoot: String?,
    )
}
