package mega.privacy.android.data.facade

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.domain.entity.call.AudioDevice
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppEventFacadeTest {

    private lateinit var underTest: AppEventGateway
    private lateinit var coroutineScope: CoroutineScope

    @BeforeAll
    fun setUp() {
        coroutineScope = TestScope(UnconfinedTestDispatcher())
        underTest = AppEventFacade(
            appScope = coroutineScope
        )
    }

    @Test
    fun `test that set SMS Verification Shown set the correct state`() = runTest {
        underTest.setSMSVerificationShown(true)
        assertThat(underTest.isSMSVerificationShown()).isTrue()
        underTest.setSMSVerificationShown(false)
        assertThat(underTest.isSMSVerificationShown()).isFalse()
    }

    @Test
    fun `test that is SMSVerification Shown default value is the correct one`() = runTest {
        assertThat(underTest.isSMSVerificationShown()).isFalse()
    }

    @Test
    fun `test that broadcast completed transfer fires an event`() = runTest {
        underTest.monitorCompletedTransfer.test {
            underTest.broadcastCompletedTransfer()

            val actual = awaitItem()
            assertThat(actual).isEqualTo(Unit)
        }
    }

    @Test
    fun `test that broadcast chat archived state fires an event`() = runTest {
        underTest.monitorChatArchived().test {
            underTest.broadcastChatArchived("Chat Title")
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `test that broadcast leave chat state fires an event`() = runTest {
        val chatId = 1234L
        underTest.monitorLeaveChat().test {
            underTest.broadcastLeaveChat(chatId)
            assertThat(awaitItem()).isEqualTo(chatId)
        }
    }

    @Test
    fun `test that broadcast refresh session fires an event`() = runTest {
        underTest.monitorRefreshSession().test {
            underTest.broadcastRefreshSession()
            assertThat(awaitItem()).isEqualTo(Unit)
        }
    }

    @Test
    fun `test that broadcast business account expired fires an event`() = runTest {
        underTest.monitorBusinessAccountExpired().test {
            underTest.broadcastBusinessAccountExpired()
            assertThat(awaitItem()).isEqualTo(Unit)
        }
    }

    @Test
    fun `test that update user data fires an event when it broadcast`() = runTest {
        underTest.monitorUpdateUserData().test {
            underTest.broadcastUpdateUserData()
            assertThat(expectMostRecentItem()).isEqualTo(Unit)
        }
    }

    @Test
    fun `test that broadcast call screen opened fires an event`() = runTest {
        underTest.monitorCallScreenOpened().test {
            underTest.broadcastCallScreenOpened(true)
            assertThat(awaitItem()).isEqualTo(true)
        }
    }

    @Test
    fun `test that broadcast audio output changed fires an event`() = runTest {
        underTest.monitorAudioOutput().test {
            underTest.broadcastAudioOutput(audioDevice = AudioDevice.SpeakerPhone)
            assertThat(awaitItem()).isEqualTo(AudioDevice.SpeakerPhone)
        }
    }
}
