package mega.privacy.android.domain.usecase.setting

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorChatSettingsUseCaseTest {
    private lateinit var underTest: MonitorChatSettingsUseCase

    private val chatRepository = mock<ChatRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorChatSettingsUseCase(chatRepository = chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that invoke emits the chat settings from the repository`() = runTest {
        val expected = ChatSettings()
        chatRepository.stub {
            on { monitorChatSettings() }.thenReturn(flowOf(expected))
        }

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke emits null when the repository has no chat settings`() = runTest {
        chatRepository.stub {
            on { monitorChatSettings() }.thenReturn(flowOf(null))
        }

        underTest().test {
            assertThat(awaitItem()).isNull()
            awaitComplete()
        }
    }
}
