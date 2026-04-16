package mega.privacy.android.feature.transfers.presentation.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.transfers.GetMaxDownloadConnectionsUseCase
import mega.privacy.android.domain.usecase.transfers.GetMaxTransferConnectionsRangeUseCase
import mega.privacy.android.domain.usecase.transfers.GetMaxUploadConnectionsUseCase
import mega.privacy.android.domain.usecase.transfers.SetMaxDownloadConnectionsUseCase
import mega.privacy.android.domain.usecase.transfers.SetMaxUploadConnectionsUseCase
import mega.privacy.android.feature.transfers.presentation.settings.model.TransfersSettingsUiState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransfersSettingsViewModelTest {

    private lateinit var underTest: TransfersSettingsViewModel

    private val getMaxDownloadConnectionsUseCase = mock<GetMaxDownloadConnectionsUseCase>()
    private val getMaxUploadConnectionsUseCase = mock<GetMaxUploadConnectionsUseCase>()
    private val setMaxDownloadConnectionsUseCase = mock<SetMaxDownloadConnectionsUseCase>()
    private val setMaxUploadConnectionsUseCase = mock<SetMaxUploadConnectionsUseCase>()
    private val getMaxTransferConnectionsRangeUseCase =
        mock<GetMaxTransferConnectionsRangeUseCase>()

    @BeforeEach
    fun setUp() {
        reset(
            getMaxDownloadConnectionsUseCase,
            getMaxUploadConnectionsUseCase,
            setMaxDownloadConnectionsUseCase,
            setMaxUploadConnectionsUseCase,
            getMaxTransferConnectionsRangeUseCase,
        )
        getMaxDownloadConnectionsUseCase.stub {
            onBlocking { invoke() } doReturn INITIAL_DOWNLOAD_CONNECTIONS
        }
        getMaxUploadConnectionsUseCase.stub {
            onBlocking { invoke() } doReturn INITIAL_UPLOAD_CONNECTIONS
        }
        getMaxTransferConnectionsRangeUseCase.stub {
            on { invoke() } doReturn INITIAL_TRANSFER_CONNECTIONS_RANGE
        }
        underTest = TransfersSettingsViewModel(
            getMaxDownloadConnectionsUseCase = getMaxDownloadConnectionsUseCase,
            getMaxUploadConnectionsUseCase = getMaxUploadConnectionsUseCase,
            setMaxDownloadConnectionsUseCase = setMaxDownloadConnectionsUseCase,
            setMaxUploadConnectionsUseCase = setMaxUploadConnectionsUseCase,
            getMaxTransferConnectionsRangeUseCase = getMaxTransferConnectionsRangeUseCase,
        )
    }

    @Test
    fun `test that initial state is Loading`() {
        assertThat(underTest.uiState.value).isEqualTo(TransfersSettingsUiState.Loading)
    }

    @Test
    fun `test that uiState emits Data with values from use cases`() =
        runTest(testDispatcher) {
            underTest.uiState.test {
                var state = awaitItem()
                if (state is TransfersSettingsUiState.Loading) {
                    state = awaitItem()
                }
                assertThat(state).isEqualTo(
                    TransfersSettingsUiState.Data(
                        maxDownloadConnections = INITIAL_DOWNLOAD_CONNECTIONS,
                        maxUploadConnections = INITIAL_UPLOAD_CONNECTIONS,
                        maxTransferConnectionsRange = INITIAL_TRANSFER_CONNECTIONS_RANGE,
                    )
                )
            }
        }

    @Test
    fun `test that setMaxDownloadConnections calls use case and updates state`() =
        runTest(testDispatcher) {
            val newDownloadConnections = 6
            underTest.uiState.test {
                awaitDataState()

                underTest.setMaxDownloadConnections(newDownloadConnections)
                advanceUntilIdle()

                verify(setMaxDownloadConnectionsUseCase).invoke(newDownloadConnections)
                val updated = awaitDataState()
                assertThat(updated.maxDownloadConnections).isEqualTo(newDownloadConnections)
            }
        }

    @Test
    fun `test that setMaxUploadConnections calls use case and updates state`() =
        runTest(testDispatcher) {
            val newUploadConnections = 8
            underTest.uiState.test {
                awaitDataState()

                underTest.setMaxUploadConnections(newUploadConnections)
                advanceUntilIdle()

                verify(setMaxUploadConnectionsUseCase).invoke(newUploadConnections)
                val updated = awaitDataState()
                assertThat(updated.maxUploadConnections).isEqualTo(newUploadConnections)
            }
        }

    @Test
    fun `test that setMaxDownloadConnections does not update state when use case fails`() =
        runTest(testDispatcher) {
            val newDownloadConnections = 6
            setMaxDownloadConnectionsUseCase.stub {
                onBlocking { invoke(newDownloadConnections) }.thenThrow(RuntimeException())
            }

            underTest.uiState.test {
                val initial = awaitDataState()

                underTest.setMaxDownloadConnections(newDownloadConnections)
                advanceUntilIdle()

                expectNoEvents()
                assertThat(underTest.uiState.value).isEqualTo(initial)
            }
        }

    @Test
    fun `test that setMaxUploadConnections does not update state when use case fails`() =
        runTest(testDispatcher) {
            val newUploadConnections = 8
            setMaxUploadConnectionsUseCase.stub {
                onBlocking { invoke(newUploadConnections) }.thenThrow(RuntimeException())
            }

            underTest.uiState.test {
                val initial = awaitDataState()

                underTest.setMaxUploadConnections(newUploadConnections)
                advanceUntilIdle()

                expectNoEvents()
                assertThat(underTest.uiState.value).isEqualTo(initial)
            }
        }

    private suspend fun app.cash.turbine.ReceiveTurbine<TransfersSettingsUiState>.awaitDataState(): TransfersSettingsUiState.Data {
        var item = awaitItem()
        while (item !is TransfersSettingsUiState.Data) {
            item = awaitItem()
        }
        return item
    }

    companion object {
        private const val INITIAL_DOWNLOAD_CONNECTIONS = 4
        private const val INITIAL_UPLOAD_CONNECTIONS = 2
        private val INITIAL_TRANSFER_CONNECTIONS_RANGE = 1..8

        @JvmField
        val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}
