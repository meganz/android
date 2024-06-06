package mega.privacy.android.app.presentation.offline.optionbottomsheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFileInformationByIdUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OfflineOptionsViewModelTest {

    private lateinit var underTest: OfflineOptionsViewModel
    private val savedStateHandle = mock<SavedStateHandle>()
    private val getOfflineFileInformationByIdUseCase = mock<GetOfflineFileInformationByIdUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()

    @BeforeEach
    fun initStubCommon() {
        runBlocking {
            whenever(savedStateHandle.get<Long>(OfflineOptionsViewModel.NODE_HANDLE)) doReturn (1)
            whenever(monitorConnectivityUseCase()).thenReturn(flowOf(false))
        }
    }

    private fun initUnderTest() {
        underTest = OfflineOptionsViewModel(
            savedStateHandle = savedStateHandle,
            getOfflineFileInformationByIdUseCase = getOfflineFileInformationByIdUseCase,
            monitorConnectivityUseCase
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        initUnderTest()
        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.offlineFileInformation).isNull()
            assertThat(initial.isOnline).isFalse()
            assertThat(initial.isLoading).isTrue()
        }
    }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the online status is updated correctly`(isOnline: Boolean) = runTest {
        whenever(monitorConnectivityUseCase()) doReturn flowOf(isOnline)

        initUnderTest()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().isOnline).isEqualTo(isOnline)
        }
    }


    @Test
    fun `test that error event is sent when offline node is null`() = runTest {
        whenever(getOfflineFileInformationByIdUseCase(NodeId(any()), any())) doReturn null

        initUnderTest()
        val event = underTest.uiState.value.errorEvent
        assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
        val content = (event as StateEventWithContentTriggered).content
        assertThat(content).isTrue()
    }

    @AfterEach
    fun resetMocks() {
        reset(
            savedStateHandle,
            getOfflineFileInformationByIdUseCase,
            monitorConnectivityUseCase
        )
    }
}