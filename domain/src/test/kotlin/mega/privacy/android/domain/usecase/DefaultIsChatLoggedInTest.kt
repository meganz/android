package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultIsChatLoggedInTest {
    private lateinit var underTest: IsChatLoggedIn

    private val chatRepository = mock<ChatRepository>()

    @Before
    fun setUp() {
        underTest = DefaultIsChatLoggedIn(chatRepository = chatRepository)
    }

    @Test
    fun `test that default initial value is true`() = runTest {
        chatRepository.stub {
            onBlocking { notifyChatLogout() }.thenReturn(emptyFlow())
        }
        underTest().test {
            assertThat(awaitItem()).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test that the logout notification returns false`() = runTest {
        chatRepository.stub {
            onBlocking { notifyChatLogout() }.thenReturn(flowOf(true))
        }
        underTest().test {
            assertThat(awaitItem()).isTrue()
            assertThat(awaitItem()).isFalse()
            awaitComplete()
        }
    }
}