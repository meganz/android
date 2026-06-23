package mega.privacy.android.data.mapper

import mega.privacy.android.data.mapper.account.AccountBlockedTypeMapper
import mega.privacy.android.data.mapper.meeting.IntegerListMapper
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.AccountConfirmationEvent
import mega.privacy.android.domain.entity.BusinessStatusEvent
import mega.privacy.android.domain.entity.CommitDbEvent
import mega.privacy.android.domain.entity.DisconnectEvent
import mega.privacy.android.domain.entity.Event
import mega.privacy.android.domain.entity.KeyModifiedEvent
import mega.privacy.android.domain.entity.LastPurgeEvent
import mega.privacy.android.domain.entity.MediaInfoReadyEvent
import mega.privacy.android.domain.entity.MiscFlagsReadyEvent
import mega.privacy.android.domain.entity.NodesCurrentEvent
import mega.privacy.android.domain.entity.RequestStatusProgressEvent
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.StorageSumChangedEvent
import mega.privacy.android.domain.entity.TransfersResumedEvent
import mega.privacy.android.domain.entity.UnknownEvent
import nz.mega.sdk.MegaEvent
import javax.inject.Inject

/**
 * Map [MegaEvent] to [Event]
 */
internal class EventMapper @Inject constructor(
    private val storageStateMapper: StorageStateMapper,
    private val accountBlockedTypeMapper: AccountBlockedTypeMapper,
    private val integerListMapper: IntegerListMapper,
) {
    /**
     * Map [MegaEvent] to [Event].
     *
     * Return value can be subclass of [Event]
     */
    operator fun invoke(megaEvent: MegaEvent): Event = when (megaEvent.type) {

        MegaEvent.EVENT_COMMIT_DB -> {
            CommitDbEvent(
                handle = megaEvent.handle,
            )
        }

        MegaEvent.EVENT_ACCOUNT_CONFIRMATION -> {
            AccountConfirmationEvent(
                handle = megaEvent.handle,
            )
        }

        MegaEvent.EVENT_DISCONNECT -> {
            DisconnectEvent(
                handle = megaEvent.handle,
            )
        }

        MegaEvent.EVENT_ACCOUNT_BLOCKED -> {
            AccountBlockedEvent(
                handle = megaEvent.handle,
                type = accountBlockedTypeMapper(megaEvent.number),
                text = megaEvent.text.orEmpty(),
            )
        }

        MegaEvent.EVENT_STORAGE -> {
            StorageStateEvent(
                handle = megaEvent.handle,
                storageState = storageStateMapper(megaEvent.number.toInt())
            )
        }

        MegaEvent.EVENT_NODES_CURRENT -> {
            NodesCurrentEvent(
                handle = megaEvent.handle,
            )
        }

        MegaEvent.EVENT_MEDIA_INFO_READY -> {
            MediaInfoReadyEvent(
                handle = megaEvent.handle,
            )
        }

        MegaEvent.EVENT_STORAGE_SUM_CHANGED -> {
            StorageSumChangedEvent(
                handle = megaEvent.handle,
            )
        }

        MegaEvent.EVENT_BUSINESS_STATUS -> {
            BusinessStatusEvent(
                handle = megaEvent.handle,
            )
        }

        MegaEvent.EVENT_KEY_MODIFIED -> {
            KeyModifiedEvent(
                handle = megaEvent.handle,
            )
        }

        MegaEvent.EVENT_MISC_FLAGS_READY -> {
            MiscFlagsReadyEvent(
                handle = megaEvent.handle,
            )
        }

        MegaEvent.EVENT_REQSTAT_PROGRESS -> {
            RequestStatusProgressEvent(
                handle = megaEvent.handle,
                progress = megaEvent.number,
            )
        }

        MegaEvent.EVENT_TRANSFERS_RESUMED -> {
            TransfersResumedEvent(
                handle = megaEvent.handle,
                uniqueIds = integerListMapper(megaEvent.integerList),
            )
        }

        MegaEvent.EVENT_LAST_PURGE -> {
            LastPurgeEvent(
                handle = megaEvent.handle,
                ts = megaEvent.getNumber(KEY_TS),
                reason = megaEvent.getNumber(KEY_REASON).toInt(),
                warningTs = KEY_WARNING_TS.takeIf(megaEvent::hasNumber)?.let(megaEvent::getNumber),
                lastActiveTs = KEY_LAST_ACTIVE_TS.takeIf(megaEvent::hasNumber)
                    ?.let(megaEvent::getNumber),
            )
        }

        else -> {
            UnknownEvent(
                handle = megaEvent.handle,
            )
        }
    }

    companion object {
        private const val KEY_TS = "ts"
        private const val KEY_REASON = "reason"
        private const val KEY_WARNING_TS = "warningTs"
        private const val KEY_LAST_ACTIVE_TS = "lastActiveTs"
    }
}
