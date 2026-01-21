package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.offline.StartOfflineSyncWorkerUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OfflineSyncPostLoginInitialiserTest {
    private lateinit var underTest: OfflineSyncPostLoginInitialiser

    private val startOfflineSyncWorkerUseCase = mock<StartOfflineSyncWorkerUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = OfflineSyncPostLoginInitialiser(
            startOfflineSyncWorkerUseCase = startOfflineSyncWorkerUseCase
        )
    }

    @AfterEach
    fun resetMock() {
        reset(startOfflineSyncWorkerUseCase)
    }

    @ParameterizedTest(name = "isFastLogin: {0}")
    @ValueSource(booleans = [false, true])
    fun `test that offline sync worker is started`(isFastLogin: Boolean) = runTest {
        underTest("session", isFastLogin)
        verify(startOfflineSyncWorkerUseCase).invoke()
    }

    @Test
    fun `test that exception is caught when starting offline sync worker fails`() = runTest {
        whenever(startOfflineSyncWorkerUseCase()).thenThrow(RuntimeException("Test exception"))

        // Should not throw exception
        underTest("session", false)

        verify(startOfflineSyncWorkerUseCase).invoke()
    }
}
