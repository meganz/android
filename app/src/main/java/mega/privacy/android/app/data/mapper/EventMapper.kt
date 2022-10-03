package mega.privacy.android.app.data.mapper

import mega.privacy.android.domain.entity.Event
import mega.privacy.android.domain.entity.EventType
import nz.mega.sdk.MegaEvent

/**
 * Map [MegaEvent] to [Event]
 */
typealias EventMapper = (@JvmSuppressWildcards MegaEvent) -> Event

/**
 * Map [MegaEvent] to [Event]
 */
internal fun toEvent(megaEvent: MegaEvent) = Event(
    handle = megaEvent.handle,
    eventString = megaEvent.eventString,
    number = megaEvent.number,
    text = megaEvent.text,
    type = mapType(megaEvent.type),
)

private fun mapType(type: Int): EventType = when (type) {
    MegaEvent.EVENT_COMMIT_DB -> EventType.CommitDb
    MegaEvent.EVENT_ACCOUNT_CONFIRMATION -> EventType.AccountConfirmation
    MegaEvent.EVENT_CHANGE_TO_HTTPS -> EventType.ChangeToHttps
    MegaEvent.EVENT_DISCONNECT -> EventType.Disconnect
    MegaEvent.EVENT_ACCOUNT_BLOCKED -> EventType.AccountBlocked
    MegaEvent.EVENT_STORAGE -> EventType.Storage
    MegaEvent.EVENT_NODES_CURRENT -> EventType.NodesCurrent
    MegaEvent.EVENT_MEDIA_INFO_READY -> EventType.MediaInfoReady
    MegaEvent.EVENT_STORAGE_SUM_CHANGED -> EventType.StorageSumChanged
    MegaEvent.EVENT_BUSINESS_STATUS -> EventType.BusinessStatus
    MegaEvent.EVENT_KEY_MODIFIED -> EventType.KeyModified
    MegaEvent.EVENT_MISC_FLAGS_READY -> EventType.MiscFlagsReady
    else -> EventType.Unknown

}

