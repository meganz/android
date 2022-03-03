package test.mega.privacy.android.app.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.entity.*
import mega.privacy.android.app.domain.repository.ChatRepository
import mega.privacy.android.app.domain.usecase.DefaultHasIncomingCall
import mega.privacy.android.app.domain.usecase.HasIncomingCall
import mega.privacy.android.app.domain.usecase.IsOnCall
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DefaultHasIncomingCallTest {
    private lateinit var underTest: HasIncomingCall

    private val chatRepository = mock<ChatRepository>()
    private val isOnCall = mock<IsOnCall>()

    @Before
    fun setUp() {
        underTest = DefaultHasIncomingCall(chatRepository = chatRepository, isOnCall = isOnCall)
    }

    @After
    fun tearDown() {
        Mockito.reset(
            chatRepository,
            isOnCall
        )
    }

    @Test
    fun `test that value is false if there are no calls`() = runTest {
        whenever(chatRepository.getNumberOfCalls()).thenReturn(0)

        underTest().test {
            assertThat(awaitItem()).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that value is true if there is a call`() = runTest {
        whenever(chatRepository.getNumberOfCalls()).thenReturn(1)
        whenever(isOnCall()).thenReturn(false)

        underTest().test {
            assertThat(awaitItem()).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that value is false if only one call and user is in call`() = runTest {
        whenever(chatRepository.getNumberOfCalls()).thenReturn(1)
        whenever(isOnCall()).thenReturn(true)


        underTest().test {
            assertThat(awaitItem()).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that status changes re-evaluates`() = runTest{
        whenever(chatRepository.getNumberOfCalls()).thenReturn(1)
        whenever(isOnCall()).thenReturn(true)
        whenever(chatRepository.monitorCallStateChanges()).thenReturn(flowOf(CallStatusChange(1, CallStatus.Connecting)))

        underTest().test {
            cancelAndIgnoreRemainingEvents()
        }

        verify(chatRepository, times(2)).getNumberOfCalls()
        verify(isOnCall, times(2)).invoke()
    }

    @Test
    fun `test that hold changes re-evaluates`() = runTest{
        whenever(chatRepository.getNumberOfCalls()).thenReturn(1)
        whenever(isOnCall()).thenReturn(true)
        whenever(chatRepository.monitorCallStateChanges()).thenReturn(flowOf(CallOnHoldChange(1)))

        underTest().test {
            cancelAndIgnoreRemainingEvents()
        }

        verify(chatRepository, times(2)).getNumberOfCalls()
        verify(isOnCall, times(2)).invoke()
    }

    @Test
    fun `test that other changes do not re-evaluates`() = runTest{
        whenever(chatRepository.getNumberOfCalls()).thenReturn(1)
        whenever(isOnCall()).thenReturn(true)
        whenever(chatRepository.monitorCallStateChanges()).thenReturn(flowOf(CallSpeakChange(1)))

        underTest().test {
            cancelAndIgnoreRemainingEvents()
        }

        verify(chatRepository, times(1)).getNumberOfCalls()
        verify(isOnCall, times(1)).invoke()
    }
}