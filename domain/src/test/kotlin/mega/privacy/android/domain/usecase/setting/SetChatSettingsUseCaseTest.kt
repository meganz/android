package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetChatSettingsUseCaseTest {
    private lateinit var underTest: SetChatSettingsUseCase

    private val chatRepository = mock<ChatRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetChatSettingsUseCase(chatRepository = chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that invoke sets the chat settings on the repository`() = runTest {
        val chatSettings = ChatSettings()

        underTest(chatSettings)

        verify(chatRepository).setChatSettings(chatSettings)
    }
}
