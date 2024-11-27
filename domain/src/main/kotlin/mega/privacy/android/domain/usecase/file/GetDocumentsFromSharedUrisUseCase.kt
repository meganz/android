package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.transfers.uploads.IsMalformedPathFromExternalAppUseCase
import javax.inject.Inject

/**
 * Use case to get a list of [DocumentEntity]s from an action and a list of [UriPath]s
 */
class GetDocumentsFromSharedUrisUseCase @Inject constructor(
    private val filePrepareUseCase: FilePrepareUseCase,
    private val isMalformedPathFromExternalAppUseCase: IsMalformedPathFromExternalAppUseCase,
) {

    /**
     * Invoke
     * @param action the action from the intent that is sharing to MEGA app
     * @param uriPaths the [UriPath]s shared in the intent
     * @return a list of the [DocumentEntity]s represented by the [uriPaths]. Malformed uris will be filtered out.
     */
    suspend operator fun invoke(action: String?, uriPaths: List<UriPath>) =
        filePrepareUseCase(uriPaths.filterNot {
            isMalformedPathFromExternalAppUseCase(action, it.value)
        })
}