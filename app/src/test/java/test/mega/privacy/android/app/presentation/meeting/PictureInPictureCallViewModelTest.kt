package test.mega.privacy.android.app.presentation.meeting

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.meeting.pip.PictureInPictureCallViewModel
import mega.privacy.android.domain.entity.chat.ChatVideoUpdate
import mega.privacy.android.domain.usecase.call.GetCallRemoteVideoUpdatesUseCase
import mega.privacy.android.domain.usecase.chat.GetChatLocalVideoUpdatesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
internal class PictureInPictureCallViewModelTest {

    private lateinit var underTest: PictureInPictureCallViewModel

    private val getCallRemoteVideoUpdatesUseCase: GetCallRemoteVideoUpdatesUseCase = mock()
    private val getChatLocalVideoUpdatesUseCase: GetChatLocalVideoUpdatesUseCase = mock()


    @BeforeAll
    internal fun init() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun resetMocks() {
        reset(getCallRemoteVideoUpdatesUseCase, getChatLocalVideoUpdatesUseCase)
    }

    private fun initTestClass() {
        underTest = PictureInPictureCallViewModel(
            getCallRemoteVideoUpdatesUseCase = getCallRemoteVideoUpdatesUseCase,
            getChatLocalVideoUpdatesUseCase = getChatLocalVideoUpdatesUseCase
        )
    }

    @Test
    fun `test that chatId is updated when set `() = runTest {
        val chatId = 100L
        initTestClass()
        underTest.setChatId(chatId = chatId)
        underTest.uiState.test {
            val item = awaitItem()
            Truth.assertThat(item.chatId).isEqualTo(chatId)
        }
    }

    @Test
    fun `test that clientId and peerId are updated when set `() = runTest {
        val clientId = 2L
        val peerId = 123456L
        initTestClass()
        underTest.setClientAndPeerId(clientId = clientId, peerId = peerId)
        underTest.uiState.test {
            val item = awaitItem()
            Truth.assertThat(item.clientId).isEqualTo(clientId)
            Truth.assertThat(item.peerId).isEqualTo(peerId)
        }
    }

    @Test
    fun `test that video update is cancelled when cancelVideUpdates is called `() = runTest {
        initTestClass()
        underTest.showVideoUpdates()
        underTest.uiState.test {
            Truth.assertThat(awaitItem().isVideoOn).isTrue()
            underTest.cancelVideUpdates()
            Truth.assertThat(awaitItem().isVideoOn).isFalse()
        }
    }

    @Test
    fun `test that local video updates is returned when client id is -1L`() = runTest {
        val chatVideoUpdate = ChatVideoUpdate(100, 100, ByteArray(100))
        val chatId = 12L
        whenever(getChatLocalVideoUpdatesUseCase(chatId)).thenReturn(flowOf(chatVideoUpdate))
        initTestClass()
        underTest.setChatId(chatId = chatId)
        underTest.setClientAndPeerId(clientId = -1L, peerId = 123456L)
        underTest.getVideoUpdates()
        verify(getChatLocalVideoUpdatesUseCase).invoke(chatId)
    }

    @Test
    fun `test that remote video updates is returned when client id is not -1L`() = runTest {
        val chatVideoUpdate = ChatVideoUpdate(100, 100, ByteArray(100))
        val clientId = 12L
        val chatId = 2L
        whenever(
            getCallRemoteVideoUpdatesUseCase(
                chatId = 12L,
                clientId = clientId,
                isHighRes = true
            )
        ).thenReturn(flowOf(chatVideoUpdate))
        initTestClass()
        underTest.setChatId(chatId)
        underTest.setClientAndPeerId(clientId = clientId, peerId = 123456L)
        underTest.getVideoUpdates()
        verify(getCallRemoteVideoUpdatesUseCase).invoke(
            chatId = chatId,
            clientId = clientId,
            isHighRes = true
        )
    }
}
