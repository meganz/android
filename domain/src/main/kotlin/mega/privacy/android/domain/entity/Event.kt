package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.account.AccountBlockedType
import kotlin.time.Duration

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

data class TransfersResumedEvent(
    override val handle: Long,
    val uniqueIds: List<Int>,
) : Event

/**
 * Sub type of [Event] for the last purge event (EVENT_LAST_PURGE).
 *
 * Fired when account data was purged by MEGA.
 *
 * @property ts the Unix timestamp (seconds) of the purge, to pass back when acknowledging.
 * @property reason the purge reason code (see PurgeReason in the SDK).
 * @property warningTs the Unix timestamp (seconds) of the first inactivity warning, or null
 *                     when not present (only set for the inactive reason).
 * @property lastActiveTs the Unix timestamp (seconds) of the user's last activity prior to the
 *                        warning, or null when not present (only set alongside [warningTs]).
 */
data class LastPurgeEvent(
    override val handle: Long,
    val ts: Long,
    val reason: Int,
    val warningTs: Long?,
    val lastActiveTs: Long?,
) : Event

/**
 * Sub type of [Event] for the streaming bandwidth over quota event (EVENT_STREAM_OVERQUOTA).
 *
 * Streaming reads are detached from the transfer subsystem, so the SDK reports a bandwidth over
 * quota hit while streaming through this global event instead of a transfer temporary error.
 *
 * @property timeLeft time remaining until the bandwidth over quota state ends.
 */
data class StreamOverQuotaEvent(
    override val handle: Long,
    val timeLeft: Duration,
) : Event

data class UnknownEvent(
    override val handle: Long,
) : Event

