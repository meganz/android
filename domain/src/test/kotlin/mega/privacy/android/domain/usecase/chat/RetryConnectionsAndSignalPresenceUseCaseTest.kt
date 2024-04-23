package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatPresenceConfig
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.RetryPendingConnectionsUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RetryConnectionsAndSignalPresenceUseCaseTest {

    private val chatRepository: ChatRepository = mock()
    private val retryPendingConnectionsUseCase: RetryPendingConnectionsUseCase = mock()

    private lateinit var underTest: RetryConnectionsAndSignalPresenceUseCase

    @BeforeEach
    fun setUp() {
        underTest = RetryConnectionsAndSignalPresenceUseCase(
            chatRepository = chatRepository,
            retryPendingConnectionsUseCase = retryPendingConnectionsUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            chatRepository,
            retryPendingConnectionsUseCase
        )
    }

    @ParameterizedTest
    @MethodSource("provideChatPresenceConfig")
    fun `test that the correct boolean value is returned when chat presence config is not null and not pending`(
        config: ChatPresenceConfig?,
        expected: Boolean,
    ) = runTest {
        whenever(chatRepository.getChatPresenceConfig()) doReturn config

        val actual = underTest()

        verify(chatRepository).getChatPresenceConfig()
        assertThat(actual).isEqualTo(expected)

        if (expected) {
            verify(chatRepository).signalPresenceActivity()
        }
    }

    private fun provideChatPresenceConfig() = Stream.of(
        Arguments.of(
            newChatPresenceConfig(),
            true
        ),
        Arguments.of(
            null,
            false
        ),
        Arguments.of(
            newChatPresenceConfig(withIsPending = true),
            false
        )
    )

    private fun newChatPresenceConfig(
        withOnlineStatus: UserChatStatus = UserChatStatus.Offline,
        withIsAutoAwayEnabled: Boolean = false,
        withAutoAwayTimeout: Long = 0L,
        withIsPersist: Boolean = false,
        withIsPending: Boolean = false,
        withIsLastGreenVisible: Boolean = false,
    ) = ChatPresenceConfig(
        onlineStatus = withOnlineStatus,
        isAutoAwayEnabled = withIsAutoAwayEnabled,
        autoAwayTimeout = withAutoAwayTimeout,
        isPersist = withIsPersist,
        isPending = withIsPending,
        isLastGreenVisible = withIsLastGreenVisible
    )
}
