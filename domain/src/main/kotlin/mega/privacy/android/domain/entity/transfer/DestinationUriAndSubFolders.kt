package mega.privacy.android.domain.entity.transfer

/**
 * Data class to return the result of GetTransferDestinationUriUseCase.
 * @property destinationUri the destination uri for the transfer
 * @property subFolders The sub folders required for transferring child files and replicate the same hierarchy starting from the user-selected destination ([destinationUri])
 */
data class DestinationUriAndSubFolders(
    val destinationUri: String,
    val subFolders: List<String> = emptyList(),
)