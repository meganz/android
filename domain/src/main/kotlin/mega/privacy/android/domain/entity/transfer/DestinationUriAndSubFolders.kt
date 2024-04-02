package mega.privacy.android.domain.entity.transfer

import java.io.File

/**
 * Data class to return the result of GetTransferDestinationUriUseCase.
 * @property destinationUri the destination uri for the transfer
 * @property subFolders The sub folders required for transferring child files and replicate the same hierarchy starting from the user-selected destination ([destinationUri])
 */
data class DestinationUriAndSubFolders(
    val destinationUri: String,
    val subFolders: List<String> = emptyList(),
) {
    override fun toString() = destinationUri + subFolders.joinToString(
        separator = File.separator,
        postfix = File.separator
    )
}