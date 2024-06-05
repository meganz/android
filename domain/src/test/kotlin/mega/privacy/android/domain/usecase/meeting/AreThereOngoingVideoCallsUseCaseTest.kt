package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallStatus.Connecting
import mega.privacy.android.domain.entity.meeting.ChatCallStatus.InProgress
import mega.privacy.android.domain.entity.meeting.ChatCallStatus.Initial
import mega.privacy.android.domain.entity.meeting.ChatCallStatus.Joining
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AreThereOngoingVideoCallsUseCaseTest {

    private lateinit var underTest: AreThereOngoingVideoCallsUseCase

    private val getCallHandleListUseCase: GetCallHandleListUseCase = mock()
    private val getChatCallUseCase: GetChatCallUseCase = mock()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val defaultDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        underTest = AreThereOngoingVideoCallsUseCase(
            getCallHandleListUseCase = getCallHandleListUseCase,
            getChatCallUseCase = getChatCallUseCase,
            defaultDispatcher = defaultDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            getCallHandleListUseCase,
            getChatCallUseCase
        )
    }

    @Test
    fun `test that False is returned when there is no ongoing call`() = runTest {
        whenever(getCallHandleListUseCase(any())) doReturn emptyList()

        val actual = underTest()

        assertThat(actual).isFalse()
    }

    @Test
    fun `test that False is returned when the current ongoing call is on hold`() = runTest {
        val inProgressCallID = 123L
        whenever(getCallHandleListUseCase(InProgress)) doReturn listOf(inProgressCallID)
        whenever(getCallHandleListUseCase(Joining)) doReturn emptyList()
        whenever(getCallHandleListUseCase(Connecting)) doReturn emptyList()
        whenever(getCallHandleListUseCase(Initial)) doReturn emptyList()
        whenever(getChatCallUseCase(inProgressCallID)) doReturn ChatCall(
            chatId = 1,
            callId = inProgressCallID,
            isOnHold = true
        )

        val actual = underTest()

        assertThat(actual).isFalse()
    }

    @Test
    fun `test that False is returned when the current ongoing call isn't a video call`() = runTest {
        val inProgressCallID = 123L
        whenever(getCallHandleListUseCase(InProgress)) doReturn listOf(inProgressCallID)
        whenever(getCallHandleListUseCase(Joining)) doReturn emptyList()
        whenever(getCallHandleListUseCase(Connecting)) doReturn emptyList()
        whenever(getCallHandleListUseCase(Initial)) doReturn emptyList()
        whenever(getChatCallUseCase(inProgressCallID)) doReturn ChatCall(
            chatId = 1,
            callId = inProgressCallID,
            isOnHold = false,
            hasLocalVideo = false
        )

        val actual = underTest()

        assertThat(actual).isFalse()
    }

    @Test
    fun `test that True is returned when there is an ongoing video call`() = runTest {
        val inProgressCallID = 123L
        whenever(getCallHandleListUseCase(InProgress)) doReturn listOf(inProgressCallID)
        whenever(getCallHandleListUseCase(Joining)) doReturn emptyList()
        whenever(getCallHandleListUseCase(Connecting)) doReturn emptyList()
        whenever(getCallHandleListUseCase(Initial)) doReturn emptyList()
        whenever(getChatCallUseCase(inProgressCallID)) doReturn ChatCall(
            chatId = 1,
            callId = inProgressCallID,
            isOnHold = false,
            hasLocalVideo = true
        )

        val actual = underTest()

        assertThat(actual).isTrue()
    }
}
