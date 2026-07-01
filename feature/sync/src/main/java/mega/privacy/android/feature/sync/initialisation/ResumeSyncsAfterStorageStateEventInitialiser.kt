package mega.privacy.android.feature.sync.initialisation

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.ResumeSyncsSuspendedByStorageOverquotaUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiserAction
import timber.log.Timber
import javax.inject.Inject

/**
 * Initialiser that resumes syncs suspended by storage overquota once the storage state
 * returns to normal (e.g. after the account is upgraded back to a Pro plan).
 */
internal class ResumeSyncsAfterStorageStateEventInitialiser @Inject constructor(
    monitorMyAccountUpdateUseCase: MonitorMyAccountUpdateUseCase,
    resumeSyncsSuspendedByStorageOverquotaUseCase: ResumeSyncsSuspendedByStorageOverquotaUseCase,
) : PostLoginInitialiserAction(
    action = { _, _ ->
        monitorMyAccountUpdateUseCase()
            .catch { Timber.e(it, "Failed to monitor my account update event") }
            .mapNotNull { it.storageState }
            .distinctUntilChanged()
            .filter {
                it == StorageState.Green || it == StorageState.Orange
            }
            .collect {
                runCatching {
                    resumeSyncsSuspendedByStorageOverquotaUseCase()
                }.onFailure {
                    Timber.e(it, "Failed to resume syncs after storage state update")
                }
            }
    }
)
