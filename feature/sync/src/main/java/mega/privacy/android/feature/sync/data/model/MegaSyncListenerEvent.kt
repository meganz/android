package mega.privacy.android.feature.sync.data.model

import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSyncStats

internal sealed interface MegaSyncListenerEvent {

    data class OnSyncDeleted(val sync: MegaSync) : MegaSyncListenerEvent

    data class OnSyncStatsUpdated(val syncStats: MegaSyncStats) : MegaSyncListenerEvent
}
