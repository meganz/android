package mega.privacy.android.app.appstate.global.initialisation.appcreate

import mega.privacy.android.feature.sync.domain.usecase.progress.StartSyncProgressMonitoringUseCase
import mega.privacy.android.navigation.contract.initialisation.AsyncAppCreateInitialiser
import javax.inject.Inject

/**
 * Runs sync progress monitoring for the life of the process, so worker-started processes are covered too.
 */
internal class SyncProgressMonitoringInitialiser @Inject constructor(
    private val startSyncProgressMonitoringUseCase: StartSyncProgressMonitoringUseCase,
) : AsyncAppCreateInitialiser {
    override val name = "SyncProgressMonitoringInitialiser"

    override suspend operator fun invoke() = startSyncProgressMonitoringUseCase()
}
