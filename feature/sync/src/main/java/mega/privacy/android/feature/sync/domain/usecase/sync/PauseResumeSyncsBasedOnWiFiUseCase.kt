package mega.privacy.android.feature.sync.domain.usecase.sync

import mega.privacy.android.domain.usecase.IsOnWifiNetworkUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.IsSyncPausedByTheUserUseCase
import javax.inject.Inject

internal class PauseResumeSyncsBasedOnWiFiUseCase @Inject constructor(
    private val isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase,
    private val pauseSyncUseCase: PauseSyncUseCase,
    private val resumeSyncUseCase: ResumeSyncUseCase,
    private val getFolderPairsUseCase: GetFolderPairsUseCase,
    private val isSyncPausedByTheUserUseCase: IsSyncPausedByTheUserUseCase,
) {

    suspend operator fun invoke(
        connectedToInternet: Boolean,
        syncOnlyByWifi: Boolean,
    ) {
        val internetNotAvailable = !connectedToInternet
        val userNotOnWifi = !isOnWifiNetworkUseCase()
        val activeSyncs = getFolderPairsUseCase()
            .filter { !isSyncPausedByTheUserUseCase(it.id) }

        if (internetNotAvailable || syncOnlyByWifi && userNotOnWifi) {
            activeSyncs.forEach { pauseSyncUseCase(it.id) }
        } else {
            activeSyncs.forEach { resumeSyncUseCase(it.id) }
        }
    }
}