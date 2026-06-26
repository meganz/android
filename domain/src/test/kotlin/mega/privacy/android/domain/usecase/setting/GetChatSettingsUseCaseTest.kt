package mega.privacy.android.domain.usecase.setting

import com.google.common.truth.Truth.assertThat
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
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetChatSettingsUseCaseTest {
    private lateinit var underTest: GetChatSettingsUseCase

    private val chatRepository = mock<ChatRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetChatSettingsUseCase(chatRepository = chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that invoke returns the chat settings from the repository`() = runTest {
        val expected = ChatSettings()
        whenever(chatRepository.getChatSettings()).thenReturn(expected)

        assertThat(underTest()).isEqualTo(expected)
    }

    @Test
    fun `test that invoke returns null when the repository has no chat settings`() = runTest {
        whenever(chatRepository.getChatSettings()).thenReturn(null)

        assertThat(underTest()).isNull()
    }
}
