package mega.privacy.android.data.facade

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import nz.mega.sdk.MegaTransfer
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
    fun `test that broadcast camera upload progress fires an event`() = runTest {
        val expected = Pair(50, 25)
        underTest.monitorCameraUploadProgress.test {
            underTest.broadcastCameraUploadProgress(expected.first, expected.second)

            val actual = awaitItem()
            assertThat(actual).isEqualTo(actual)
        }
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
        val expected = CompletedTransfer(
            fileName = "",
            type = MegaTransfer.TYPE_UPLOAD,
            state = MegaTransfer.STATE_COMPLETED,
            size = "",
            handle = 0L,
            isOffline = false,
            path = "",
            timestamp = 0L,
            error = "",
            originalPath = "",
            parentHandle = 0L
        )
        underTest.monitorCompletedTransfer.test {
            underTest.broadcastCompletedTransfer(expected)

            val actual = awaitItem()
            assertThat(actual).isEqualTo(expected)
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
}
