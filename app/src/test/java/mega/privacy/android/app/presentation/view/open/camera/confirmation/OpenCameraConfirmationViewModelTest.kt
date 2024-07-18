package mega.privacy.android.app.presentation.view.open.camera.confirmation

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRequestType
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.usecase.meeting.EnableOrDisableVideoUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallInProgress
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenCameraConfirmationViewModelTest {

    private lateinit var underTest: OpenCameraConfirmationViewModel

    private val getChatCallInProgress: GetChatCallInProgress = mock()
    private val enableOrDisableVideoUseCase: EnableOrDisableVideoUseCase = mock()

    @BeforeEach
    fun setUp() {
        underTest = OpenCameraConfirmationViewModel(
            getChatCallInProgress = getChatCallInProgress,
            enableOrDisableVideoUseCase = enableOrDisableVideoUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            getChatCallInProgress,
            enableOrDisableVideoUseCase
        )
    }

    @Test
    fun `test that the ongoing video is successfully disabled`() = runTest {
        val chatId = 1L
        whenever(getChatCallInProgress()) doReturn ChatCall(
            chatId = chatId,
            callId = 123L
        )
        whenever(enableOrDisableVideoUseCase(chatId, false)) doReturn ChatRequest(
            type = ChatRequestType.LoadPreview,
            requestString = null,
            tag = 0,
            number = 0,
            numRetry = 0,
            flag = false,
            peersList = null,
            chatHandle = 1L,
            userHandle = 2L,
            privilege = 0,
            text = null,
            link = null,
            peersListByChatHandle = null,
            handleList = null,
            paramType = null
        )

        underTest.disableOngoingVideo()

        assertThat(underTest.hasSuccessfullyDisableOngoingVideo).isTrue()
    }

    @Test
    fun `test that no video is disabled when there are no ongoing video calls`() =
        runTest {
            whenever(getChatCallInProgress()) doReturn null

            underTest.disableOngoingVideo()

            verify(enableOrDisableVideoUseCase, never()).invoke(any(), eq(false))
            assertThat(underTest.hasSuccessfullyDisableOngoingVideo).isFalse()
        }
}
