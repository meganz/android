package mega.privacy.android.domain.entity.transfer

/**
 * Represents an active transfer group, all transfers started by a single user action.
 *
 * @param groupId The id of this group
 * @param transferType The [TransferType] of the transfers in this group
 * @param destination The destination of the transfers in this group, UriPath value in case of downloads
 */
interface ActiveTransferGroup {
    val groupId: Int?
    val transferType: TransferType
    val destination: String
}