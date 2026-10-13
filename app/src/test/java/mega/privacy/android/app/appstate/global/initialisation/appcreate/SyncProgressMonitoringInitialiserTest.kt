package mega.privacy.android.app.appstate.global.initialisation.appcreate

import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.usecase.progress.StartSyncProgressMonitoringUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncProgressMonitoringInitialiserTest {
    private lateinit var underTest: SyncProgressMonitoringInitialiser

    private val startSyncProgressMonitoringUseCase = mock<StartSyncProgressMonitoringUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = SyncProgressMonitoringInitialiser(
            startSyncProgressMonitoringUseCase = startSyncProgressMonitoringUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(startSyncProgressMonitoringUseCase)
    }

    @Test
    fun `test that invoke runs the sync progress monitoring`() = runTest {
        underTest()

        verify(startSyncProgressMonitoringUseCase).invoke()
    }
}
