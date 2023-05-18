package mega.privacy.android.feature.sync.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import javax.inject.Inject

internal class SyncPreferencesRepositoryImpl @Inject constructor() : SyncPreferencesRepository {

    private var syncByWiFi = MutableStateFlow(false)

    override fun setSyncByWiFi(checked: Boolean) {
        syncByWiFi.value = checked
    }

    override fun monitorSyncByWiFi(): StateFlow<Boolean> = syncByWiFi.asStateFlow()
}
