package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.usecase.chat.IsAnonymousModeUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeAttachmentBottomSheetViewModelTest {
    private lateinit var underTest: NodeAttachmentBottomSheetViewModel
    private val savedStateHandle = mock<SavedStateHandle>()
    private val getChatFileUseCase = mock<GetChatFileUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val isAnonymousModeUseCase = mock<IsAnonymousModeUseCase>()
    private val isAvailableOfflineUseCase = mock<IsAvailableOfflineUseCase>()

    @BeforeEach
    fun initStubCommon() {
        runBlocking {
            whenever(savedStateHandle.get<Long>(NodeAttachmentBottomSheetViewModel.CHAT_ID)) doReturn (1)
            whenever(savedStateHandle.get<Long>(NodeAttachmentBottomSheetViewModel.MESSAGE_ID)) doReturn (2)
            whenever(monitorConnectivityUseCase()).thenReturn(flowOf(false))
        }
    }

    private fun initUnderTest() {
        underTest = NodeAttachmentBottomSheetViewModel(
            savedStateHandle = savedStateHandle,
            getChatFileUseCase = getChatFileUseCase,
            isAnonymousModeUseCase = isAnonymousModeUseCase,
            isAvailableOfflineUseCase = isAvailableOfflineUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        initUnderTest()
        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.item).isNull()
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
    fun `test that error event is sent when chat file is null`() = runTest {
        whenever(getChatFileUseCase(1, 2, 0)) doReturn null

        initUnderTest()
        val event = underTest.uiState.value.errorEvent
        assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
        val content = (event as StateEventWithContentTriggered).content
        assertThat(content).isTrue()
    }

    @Test
    fun `test that UI state is updated with correct data`() = runTest {
        val chatFile = mock<ChatDefaultFile> {
            on { id } doReturn NodeId(123)
            on { name } doReturn "Title"
            on { size } doReturn 1230
            on { thumbnailPath } doReturn null
        }

        whenever(getChatFileUseCase(1, 2, 0)) doReturn chatFile
        whenever(isAnonymousModeUseCase()) doReturn false
        whenever(isAvailableOfflineUseCase(chatFile)) doReturn true

        initUnderTest()

        underTest.uiState.test {
            val uiState = expectMostRecentItem()
            assertThat(uiState.item).isNotNull()
            assertThat(uiState.item?.nodeId?.longValue).isEqualTo(123)
            assertThat(uiState.item?.name).isEqualTo("Title")
            assertThat(uiState.item?.size).isEqualTo(1230)
            assertThat(uiState.item?.thumbnailPath).isNull()
            assertThat(uiState.item?.isInAnonymousMode).isFalse()
            assertThat(uiState.item?.isAvailableOffline).isTrue()
            assertThat(uiState.isLoading).isFalse()
        }
    }

    @AfterEach
    fun resetMocks() {
        reset(
            savedStateHandle,
            getChatFileUseCase,
            monitorConnectivityUseCase,
            isAnonymousModeUseCase,
            isAvailableOfflineUseCase
        )
    }
}