package mega.privacy.android.domain.entity.transfer

/**
 * Represents an active transfer group, all transfers started by a single user action.
 *
 * @param groupId The id of this group
 * @param transferType The [TransferType] of the transfers in this group
 * @param destination The destination of the transfers in this group, UriPath value in case of downloads
 * @param singleFileName in case of a single file transfer, the name of the file, null otherwise
 * @param startTime the local time in milliseconds when this action was started, it should be used for UX only as precision is not guaranteed
 */
interface ActiveTransferGroup {
    val groupId: Int?
    val transferType: TransferType
    val destination: String
    val singleFileName: String?
    val startTime: Long?
}