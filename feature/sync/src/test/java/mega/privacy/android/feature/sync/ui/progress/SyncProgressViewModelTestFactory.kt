package mega.privacy.android.feature.sync.ui.progress

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressPhase
import mega.privacy.android.feature.sync.domain.usecase.progress.GetSyncTransferRowStateUseCase
import mega.privacy.android.feature.sync.domain.usecase.progress.MonitorRetryingSyncTransferOverQuotaUseCase
import mega.privacy.android.feature.sync.domain.usecase.progress.MonitorSyncProgressBlockersUseCase
import mega.privacy.android.feature.sync.domain.usecase.progress.MonitorSyncProgressStatusUseCase
import mega.privacy.android.feature.sync.domain.usecase.progress.MonitorSyncTransferListUseCase
import mega.privacy.android.feature.sync.domain.usecase.progress.MonitorSyncTransferSpeedsUseCase
import mega.privacy.android.feature.sync.ui.mapper.progress.SyncActiveTransferUiModelMapper
import mega.privacy.android.feature.sync.ui.mapper.progress.SyncCompletedTransferUiModelMapper
import mega.privacy.android.feature.sync.ui.mapper.progress.SyncProgressBodyMapper
import mega.privacy.android.feature.sync.ui.mapper.progress.SyncProgressCompletedBodyMapper
import mega.privacy.android.feature.sync.ui.mapper.progress.SyncProgressUiStateMapper
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/**
 * Builds the real [SyncProgressUiStateMapper] chain. A test that needs its own mapper passes only
 * that one by name.
 */
internal fun createSyncProgressUiStateMapper(
    bodyMapper: SyncProgressBodyMapper = SyncProgressBodyMapper(SyncActiveTransferUiModelMapper()),
): SyncProgressUiStateMapper = SyncProgressUiStateMapper(
    bodyMapper = bodyMapper,
)

/**
 * Builds a [SyncProgressViewModel] with neutral mocks and the real pure chain. A test passes by
 * name only the mocks it stubs or verifies. The status and blockers defaults emit at once, and
 * the list, speeds and retrying over quota defaults emit nothing, which the ViewModel must read
 * as "not arrived".
 */
internal fun createSyncProgressViewModel(
    monitorSyncProgressStatusUseCase: MonitorSyncProgressStatusUseCase = mock {
        on { invoke() } doReturn MutableStateFlow(status(phase = SyncProgressPhase.UpToDate))
    },
    monitorSyncTransferListUseCase: MonitorSyncTransferListUseCase = mock {
        on { invoke(any(), any()) } doReturn emptyFlow()
    },
    monitorSyncProgressBlockersUseCase: MonitorSyncProgressBlockersUseCase = mock {
        on { invoke() } doReturn MutableStateFlow(blockers())
    },
    monitorSyncTransferSpeedsUseCase: MonitorSyncTransferSpeedsUseCase = mock {
        on { invoke() } doReturn emptyFlow()
    },
    getSyncTransferRowStateUseCase: GetSyncTransferRowStateUseCase = GetSyncTransferRowStateUseCase(),
    monitorRetryingSyncTransferOverQuotaUseCase: MonitorRetryingSyncTransferOverQuotaUseCase = mock {
        on { invoke() } doReturn emptyFlow()
    },
    syncProgressCompletedBodyMapper: SyncProgressCompletedBodyMapper =
        SyncProgressCompletedBodyMapper(SyncCompletedTransferUiModelMapper()),
    syncProgressUiStateMapper: SyncProgressUiStateMapper = createSyncProgressUiStateMapper(),
    defaultDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
): SyncProgressViewModel = SyncProgressViewModel(
    monitorSyncProgressStatusUseCase = monitorSyncProgressStatusUseCase,
    monitorSyncTransferListUseCase = monitorSyncTransferListUseCase,
    monitorSyncProgressBlockersUseCase = monitorSyncProgressBlockersUseCase,
    monitorSyncTransferSpeedsUseCase = monitorSyncTransferSpeedsUseCase,
    getSyncTransferRowStateUseCase = getSyncTransferRowStateUseCase,
    monitorRetryingSyncTransferOverQuotaUseCase = monitorRetryingSyncTransferOverQuotaUseCase,
    syncProgressCompletedBodyMapper = syncProgressCompletedBodyMapper,
    syncProgressUiStateMapper = syncProgressUiStateMapper,
    defaultDispatcher = defaultDispatcher,
)
