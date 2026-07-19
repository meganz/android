package mega.privacy.android.domain.usecase.call

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorParticipantsLimitWarningUseCaseTest {

    private lateinit var underTest: MonitorParticipantsLimitWarningUseCase

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getChatCallUseCase = mock<GetChatCallUseCase>()
    private val monitorChatCallUpdatesUseCase = mock<MonitorChatCallUpdatesUseCase>()

    private val chatId = 42L

    @BeforeEach
    fun setUp() {
        reset(getFeatureFlagValueUseCase, getChatCallUseCase, monitorChatCallUpdatesUseCase)
        whenever(monitorChatCallUpdatesUseCase()).thenReturn(emptyFlow())
        underTest = MonitorParticipantsLimitWarningUseCase(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getChatCallUseCase = getChatCallUseCase,
            monitorChatCallUpdatesUseCase = monitorChatCallUpdatesUseCase,
        )
    }

    @Test
    fun `test that warning is false when the feature flag is disabled`() = runTest {
        stubFlag(false)

        underTest(chatId).test {
            assertThat(awaitItem()).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `test that warning is true when the active call has reached its user limit`() = runTest {
        stubFlag(true)
        stubInitialCall(call(limit = 100, participants = 100, status = ChatCallStatus.InProgress))

        underTest(chatId).test {
            assertThat(expectMostRecentItem()).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that warning is false when the active call is below its user limit`() = runTest {
        stubFlag(true)
        stubInitialCall(call(limit = 100, participants = 50, status = ChatCallStatus.InProgress))

        underTest(chatId).test {
            assertThat(expectMostRecentItem()).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that warning is false when there is no active call`() = runTest {
        stubFlag(true)
        getChatCallUseCase.stub { on { invoke(chatId) }.doReturn(null) }

        underTest(chatId).test {
            assertThat(expectMostRecentItem()).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that warning is false when the call at its limit is destroyed`() = runTest {
        stubFlag(true)
        stubInitialCall(call(limit = 100, participants = 100, status = ChatCallStatus.Destroyed))

        underTest(chatId).test {
            assertThat(expectMostRecentItem()).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that warning reacts to call updates for the same chat`() = runTest {
        stubFlag(true)
        stubInitialCall(call(limit = 100, participants = 50, status = ChatCallStatus.InProgress))
        val updatedCall = call(limit = 100, participants = 100, status = ChatCallStatus.InProgress)
        whenever(monitorChatCallUpdatesUseCase()).thenReturn(flowOf(updatedCall))

        underTest(chatId).test {
            assertThat(expectMostRecentItem()).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that warning ignores call updates for other chats`() = runTest {
        stubFlag(true)
        stubInitialCall(call(limit = 100, participants = 50, status = ChatCallStatus.InProgress))
        val otherChatCall = call(
            chatId = 999L,
            limit = 100,
            participants = 100,
            status = ChatCallStatus.InProgress,
        )
        whenever(monitorChatCallUpdatesUseCase()).thenReturn(flowOf(otherChatCall))

        underTest(chatId).test {
            assertThat(expectMostRecentItem()).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun stubFlag(enabled: Boolean) {
        getFeatureFlagValueUseCase.stub {
            on { invoke(ApiFeatures.CallUnlimitedProPlan) }.doReturn(enabled)
        }
    }

    private fun stubInitialCall(chatCall: ChatCall) {
        getChatCallUseCase.stub { on { invoke(chatId) }.doReturn(chatCall) }
    }

    private fun call(
        chatId: Long = this.chatId,
        limit: Int?,
        participants: Int?,
        status: ChatCallStatus,
    ) = mock<ChatCall> {
        on { this.chatId } doReturn chatId
        on { callUsersLimit } doReturn limit
        on { numParticipants } doReturn participants
        on { this.status } doReturn status
    }
}
