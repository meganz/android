package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.account.AccountBlockedType

/**
 * Event class that from MegaSDK
 *
 * @property handle
 */
interface Event {
    val handle: Long
}

/**
 * Sub type of [Event] for Storage Event
 *
 * @property storageState storage state
 */
data class StorageStateEvent(
    override val handle: Long,
    val storageState: StorageState,
) : Event


data class RequestStatusProgressEvent(
    override val handle: Long,
    val progress: Long,
) : Event

data class AccountBlockedEvent(
    override val handle: Long,
    val type: AccountBlockedType,
    val text: String,
) : Event

data class CommitDbEvent(
    override val handle: Long,
) : Event

data class AccountConfirmationEvent(
    override val handle: Long,
) : Event

data class ChangeToHttpsEvent(
    override val handle: Long,
) : Event

data class DisconnectEvent(
    override val handle: Long,
) : Event

data class NodesCurrentEvent(
    override val handle: Long,
) : Event

data class MediaInfoReadyEvent(
    override val handle: Long,
) : Event

data class StorageSumChangedEvent(
    override val handle: Long,
) : Event

data class BusinessStatusEvent(
    override val handle: Long,
) : Event

data class KeyModifiedEvent(
    override val handle: Long,
) : Event

data class MiscFlagsReadyEvent(
    override val handle: Long,
) : Event

data class UnknownEvent(
    override val handle: Long,
) : Event

