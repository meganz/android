package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.usecase.call.AreThereOngoingVideoCallsUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallInProgress
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AreThereOngoingVideoCallsUseCaseTest {

    private lateinit var underTest: AreThereOngoingVideoCallsUseCase

    private val getChatCallInProgress: GetChatCallInProgress = mock()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val defaultDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        underTest = AreThereOngoingVideoCallsUseCase(
            getChatCallInProgress = getChatCallInProgress,
            defaultDispatcher = defaultDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        reset(getChatCallInProgress)
    }

    @Test
    fun `test that False is returned when there is no ongoing call`() = runTest {
        whenever(getChatCallInProgress()) doReturn null

        val actual = underTest()

        assertThat(actual).isFalse()
    }

    @Test
    fun `test that False is returned when the current ongoing call isn't a video call`() = runTest {
        whenever(getChatCallInProgress()) doReturn ChatCall(
            chatId = 1,
            callId = 123L,
            isOnHold = false,
            hasLocalVideo = false
        )

        val actual = underTest()

        assertThat(actual).isFalse()
    }

    @Test
    fun `test that True is returned when there is an ongoing video call`() = runTest {
        whenever(getChatCallInProgress()) doReturn ChatCall(
            chatId = 1,
            callId = 123L,
            isOnHold = false,
            hasLocalVideo = true
        )

        val actual = underTest()

        assertThat(actual).isTrue()
    }
}
