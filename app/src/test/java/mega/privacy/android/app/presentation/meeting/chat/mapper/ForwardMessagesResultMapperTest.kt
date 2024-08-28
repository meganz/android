package mega.privacy.android.app.presentation.meeting.chat.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.meeting.chat.mapper.ForwardMessagesResultMapper
import mega.privacy.android.app.presentation.meeting.chat.model.ForwardMessagesToChatsResult
import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ForwardMessagesResultMapperTest {

    private lateinit var underTest: ForwardMessagesResultMapper

    @BeforeAll
    fun setUp() {
        underTest = ForwardMessagesResultMapper()
    }

    @ParameterizedTest(name = " when there results {0}, message count is {1} the expected is {2}")
    @ArgumentsSource(ForwardMessagesArgumentsProvider::class)
    fun `test that mapper returns correctly`(
        results: List<ForwardResult>,
        messagesCount: Int,
        expected: ForwardMessagesToChatsResult,
    ) {
        val result = underTest(results, messagesCount)
        assertThat(result).isEqualTo(expected)
    }

    internal class ForwardMessagesArgumentsProvider : ArgumentsProvider {

        private val chatId = 123L
        private val otherChatId = 456L

        override fun provideArguments(context: ExtensionContext): Stream<out Arguments>? {
            return Stream.of(
                Arguments.of(
                    listOf(ForwardResult.Success(chatId)),
                    1,
                    ForwardMessagesToChatsResult.AllSucceeded(chatId, 1)
                ),
                Arguments.of(
                    listOf(ForwardResult.GeneralError),
                    1,
                    ForwardMessagesToChatsResult.AllFailed(1)
                ),
                Arguments.of(
                    listOf(ForwardResult.ErrorNotAvailable), 1,
                    ForwardMessagesToChatsResult.AllNotAvailable(1)
                ),
                Arguments.of(
                    listOf(ForwardResult.Success(chatId), ForwardResult.Success(otherChatId)),
                    1,
                    ForwardMessagesToChatsResult.AllSucceeded(null, 1)
                ),
                Arguments.of(
                    listOf(ForwardResult.Success(chatId), ForwardResult.GeneralError),
                    1,
                    ForwardMessagesToChatsResult.SomeFailed(chatId, 1)
                ),
                Arguments.of(
                    listOf(ForwardResult.Success(chatId), ForwardResult.ErrorNotAvailable),
                    1,
                    ForwardMessagesToChatsResult.SomeNotAvailable(chatId, 1)
                ),
                Arguments.of(
                    listOf(ForwardResult.GeneralError, ForwardResult.GeneralError),
                    1,
                    ForwardMessagesToChatsResult.AllFailed(1)
                ),
                Arguments.of(
                    listOf(ForwardResult.ErrorNotAvailable, ForwardResult.ErrorNotAvailable),
                    1,
                    ForwardMessagesToChatsResult.AllNotAvailable(1)
                ),
                Arguments.of(
                    listOf(ForwardResult.GeneralError, ForwardResult.ErrorNotAvailable),
                    1,
                    ForwardMessagesToChatsResult.AllFailed(1)
                ),
                Arguments.of(
                    listOf(ForwardResult.Success(chatId), ForwardResult.Success(chatId)),
                    2,
                    ForwardMessagesToChatsResult.AllSucceeded(chatId, 2)
                ),
                Arguments.of(
                    listOf(
                        ForwardResult.Success(chatId),
                        ForwardResult.Success(otherChatId),
                        ForwardResult.Success(chatId),
                        ForwardResult.Success(otherChatId),
                    ),
                    2,
                    ForwardMessagesToChatsResult.AllSucceeded(null, 2)
                ),
                Arguments.of(
                    listOf(ForwardResult.Success(chatId), ForwardResult.GeneralError),
                    2,
                    ForwardMessagesToChatsResult.SomeFailed(chatId, 1)
                ),
                Arguments.of(
                    listOf(ForwardResult.Success(chatId), ForwardResult.ErrorNotAvailable),
                    2,
                    ForwardMessagesToChatsResult.SomeNotAvailable(chatId, 1)
                ),
                Arguments.of(
                    listOf(ForwardResult.GeneralError, ForwardResult.GeneralError),
                    2,
                    ForwardMessagesToChatsResult.AllFailed(2)
                ),
                Arguments.of(
                    listOf(ForwardResult.ErrorNotAvailable, ForwardResult.ErrorNotAvailable),
                    2,
                    ForwardMessagesToChatsResult.AllNotAvailable(2)
                ),
                Arguments.of(
                    listOf(ForwardResult.GeneralError, ForwardResult.ErrorNotAvailable),
                    2,
                    ForwardMessagesToChatsResult.AllFailed(2)
                ),
                Arguments.of(
                    listOf(
                        ForwardResult.Success(chatId),
                        ForwardResult.Success(chatId),
                        ForwardResult.Success(chatId)
                    ),
                    3,
                    ForwardMessagesToChatsResult.AllSucceeded(chatId, 3)
                ),
                Arguments.of(
                    listOf(
                        ForwardResult.Success(chatId),
                        ForwardResult.Success(otherChatId),
                        ForwardResult.Success(chatId),
                        ForwardResult.Success(otherChatId),
                        ForwardResult.Success(chatId),
                        ForwardResult.Success(otherChatId),
                    ),
                    3,
                    ForwardMessagesToChatsResult.AllSucceeded(null, 3)
                ),
                Arguments.of(
                    listOf(
                        ForwardResult.Success(chatId),
                        ForwardResult.Success(chatId),
                        ForwardResult.GeneralError,
                    ),
                    3,
                    ForwardMessagesToChatsResult.SomeFailed(chatId, 1)
                ),
                Arguments.of(
                    listOf(
                        ForwardResult.Success(chatId),
                        ForwardResult.Success(otherChatId),
                        ForwardResult.Success(chatId),
                        ForwardResult.Success(otherChatId),
                        ForwardResult.GeneralError,
                        ForwardResult.Success(otherChatId),
                    ),
                    3,
                    ForwardMessagesToChatsResult.SomeFailed(null, 1)
                ),
                Arguments.of(
                    listOf(
                        ForwardResult.Success(chatId),
                        ForwardResult.Success(otherChatId),
                        ForwardResult.Success(chatId),
                        ForwardResult.Success(otherChatId),
                        ForwardResult.GeneralError,
                        ForwardResult.GeneralError,
                    ),
                    3,
                    ForwardMessagesToChatsResult.SomeFailed(null, 2)
                ),
                Arguments.of(
                    listOf(
                        ForwardResult.Success(chatId),
                        ForwardResult.Success(chatId),
                        ForwardResult.ErrorNotAvailable,
                    ),
                    3,
                    ForwardMessagesToChatsResult.SomeNotAvailable(chatId, 1)
                ),
                Arguments.of(
                    listOf(
                        ForwardResult.Success(chatId),
                        ForwardResult.Success(otherChatId),
                        ForwardResult.Success(chatId),
                        ForwardResult.Success(otherChatId),
                        ForwardResult.ErrorNotAvailable,
                        ForwardResult.Success(otherChatId),
                    ),
                    3,
                    ForwardMessagesToChatsResult.SomeNotAvailable(null, 1)
                ),
                Arguments.of(
                    listOf(
                        ForwardResult.Success(chatId),
                        ForwardResult.Success(otherChatId),
                        ForwardResult.Success(chatId),
                        ForwardResult.Success(otherChatId),
                        ForwardResult.ErrorNotAvailable,
                        ForwardResult.ErrorNotAvailable,
                    ),
                    3,
                    ForwardMessagesToChatsResult.SomeNotAvailable(null, 2)
                ),
                Arguments.of(
                    listOf(
                        ForwardResult.GeneralError,
                        ForwardResult.GeneralError,
                        ForwardResult.GeneralError,
                    ),
                    3,
                    ForwardMessagesToChatsResult.AllFailed(3)
                ),
                Arguments.of(
                    listOf(
                        ForwardResult.GeneralError,
                        ForwardResult.GeneralError,
                        ForwardResult.GeneralError,
                        ForwardResult.GeneralError,
                        ForwardResult.GeneralError,
                        ForwardResult.GeneralError,
                    ),
                    3,
                    ForwardMessagesToChatsResult.AllFailed(3)
                ),
                Arguments.of(
                    listOf(
                        ForwardResult.ErrorNotAvailable,
                        ForwardResult.ErrorNotAvailable,
                        ForwardResult.ErrorNotAvailable,
                    ),
                    3,
                    ForwardMessagesToChatsResult.AllNotAvailable(3)
                ),
                Arguments.of(
                    listOf(
                        ForwardResult.ErrorNotAvailable,
                        ForwardResult.ErrorNotAvailable,
                        ForwardResult.ErrorNotAvailable,
                        ForwardResult.ErrorNotAvailable,
                        ForwardResult.ErrorNotAvailable,
                        ForwardResult.ErrorNotAvailable,
                    ),
                    3,
                    ForwardMessagesToChatsResult.AllNotAvailable(3)
                ),
                Arguments.of(
                    listOf(
                        ForwardResult.GeneralError,
                        ForwardResult.ErrorNotAvailable,
                        ForwardResult.ErrorNotAvailable,
                    ),
                    3,
                    ForwardMessagesToChatsResult.AllFailed(3)
                ),
                Arguments.of(
                    listOf(
                        ForwardResult.GeneralError,
                        ForwardResult.ErrorNotAvailable,
                        ForwardResult.ErrorNotAvailable,
                        ForwardResult.GeneralError,
                        ForwardResult.ErrorNotAvailable,
                        ForwardResult.ErrorNotAvailable,
                    ),
                    3,
                    ForwardMessagesToChatsResult.AllFailed(3)
                ),
            )
        }
    }
}