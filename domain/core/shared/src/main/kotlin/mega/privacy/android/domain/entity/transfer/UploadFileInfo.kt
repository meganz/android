package mega.privacy.android.domain.entity.transfer

import mega.privacy.android.domain.entity.uri.UriPath
import java.io.File

/**
 * Class to encapsulate the information needed to upload a file
 *
 * @param uriPath the [UriPath] to be uploaded
 * @param fileName the name of the file if it should be renamed, if null the original name will be kept
 * @param appData the appData for this file
 */
data class UploadFileInfo(
    val uriPath: UriPath,
    val fileName: String?,
    val appData: List<TransferAppData>? = null,
) {
    constructor(file: File, fileName: String?, appData: List<TransferAppData>? = null) :
            this(
                UriPath(file.absolutePath),
                fileName,
                appData,
            )
}