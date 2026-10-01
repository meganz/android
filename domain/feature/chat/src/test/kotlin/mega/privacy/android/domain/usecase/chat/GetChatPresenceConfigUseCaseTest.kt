package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatPresenceConfig
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetChatPresenceConfigUseCaseTest {
    private val chatRepository: ChatRepository = mock()

    private lateinit var underTest: GetChatPresenceConfigUseCase

    @BeforeAll
    fun setup() {
        underTest = GetChatPresenceConfigUseCase(chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that invoke returns the presence config from the repository`() = runTest {
        val config = ChatPresenceConfig(
            onlineStatus = UserChatStatus.Online,
            isAutoAwayEnabled = true,
            autoAwayTimeout = 300L,
            isPersist = false,
            isPending = false,
            isLastGreenVisible = true,
        )
        whenever(chatRepository.getChatPresenceConfig()).thenReturn(config)

        assertThat(underTest()).isEqualTo(config)
    }

    @Test
    fun `test that invoke returns null when the repository has no presence config`() = runTest {
        whenever(chatRepository.getChatPresenceConfig()).thenReturn(null)

        assertThat(underTest()).isNull()
    }
}
