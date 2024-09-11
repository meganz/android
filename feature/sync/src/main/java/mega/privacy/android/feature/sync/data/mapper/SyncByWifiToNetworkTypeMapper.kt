package mega.privacy.android.feature.sync.data.mapper

import androidx.work.NetworkType
import javax.inject.Inject

internal class SyncByWifiToNetworkTypeMapper @Inject constructor() {

    operator fun invoke(syncByWifi: Boolean): NetworkType {
        return if (syncByWifi) {
            NetworkType.UNMETERED
        } else {
            NetworkType.CONNECTED
        }
    }
}
