package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.meeting.FakeIncomingCallState
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFakeIncomingCallStateUseCaseTest {

    private lateinit var underTest: GetFakeIncomingCallStateUseCase

    private val callRepository: CallRepository = mock()

    private val chatId = 1L

    @BeforeEach
    fun setUp() {
        underTest = GetFakeIncomingCallStateUseCase(
            callRepository = callRepository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(callRepository)
    }

    @Test
    fun `test that the fake incoming call Notification is returned`() = runTest {
        whenever(callRepository.getFakeIncomingCall(chatId)) doReturn FakeIncomingCallState.Notification

        assertThat(underTest(chatId = chatId)).isEqualTo(FakeIncomingCallState.Notification)
    }

    @Test
    fun `test that the fake incoming call Screen is returned`() = runTest {
        whenever(callRepository.getFakeIncomingCall(chatId)) doReturn FakeIncomingCallState.Screen

        assertThat(underTest(chatId = chatId)).isEqualTo(FakeIncomingCallState.Screen)
    }

    @Test
    fun `test that the fake incoming call Dismiss is returned`() = runTest {
        whenever(callRepository.getFakeIncomingCall(chatId)) doReturn FakeIncomingCallState.Dismiss

        assertThat(underTest(chatId = chatId)).isEqualTo(FakeIncomingCallState.Dismiss)
    }

    @Test
    fun `test that the fake incoming call Remove is returned`() = runTest {
        whenever(callRepository.getFakeIncomingCall(chatId)) doReturn FakeIncomingCallState.Remove

        assertThat(underTest(chatId = chatId)).isEqualTo(FakeIncomingCallState.Remove)
    }
}