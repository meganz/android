package mega.privacy.android.data.mapper.chat.update

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.mapper.chat.ChatMessageMapper
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.room.update.ChatRoomMessageUpdate
import mega.privacy.android.domain.entity.chat.room.update.HistoryTruncatedByRetentionTime
import mega.privacy.android.domain.entity.chat.room.update.MessageLoaded
import mega.privacy.android.domain.entity.chat.room.update.MessageReceived
import mega.privacy.android.domain.entity.chat.room.update.MessageUpdate
import nz.mega.sdk.MegaChatMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatRoomMessageUpdateMapperTest {
    private lateinit var underTest: ChatRoomMessageUpdateMapper
    private val chatMessage = mock<ChatMessage>()

    private val chatMessageMapper = mock<ChatMessageMapper> {
        onBlocking { invoke(any()) } doReturn chatMessage
    }

    @BeforeEach
    internal fun setUp() {
        underTest = ChatRoomMessageUpdateMapper(chatMessageMapper = chatMessageMapper)
    }

    @ParameterizedTest(name = "test that {0} is mapped to {1}")
    @MethodSource("provideMessageUpdates")
    internal fun `test that message updates are mapped`(
        input: ChatRoomUpdate,
        expected: ChatRoomMessageUpdate,
    ) = runTest {
        assertThat(underTest(input)).isEqualTo(expected)
    }

    @ParameterizedTest(name = "test that {0} is mapped to null")
    @MethodSource("provideNonMessageUpdates")
    internal fun `test that non message updates return null`(
        input: ChatRoomUpdate,
    ) = runTest {
        assertThat(underTest(input)).isNull()
    }

    private fun provideMessageUpdates(): Stream<Arguments>? {
        val megaMessage = mock<MegaChatMessage>()
        return Stream.of(
            Arguments.of(mock<ChatRoomUpdate.OnHistoryTruncatedByRetentionTime> {
                on { msg }.thenReturn(
                    megaMessage
                )
            }, HistoryTruncatedByRetentionTime(chatMessage)),
            Arguments.of(
                mock<ChatRoomUpdate.OnMessageLoaded> { on { msg }.thenReturn(megaMessage) },
                MessageLoaded(chatMessage)
            ),
            Arguments.of(
                mock<ChatRoomUpdate.OnMessageReceived> { on { msg }.thenReturn(megaMessage) },
                MessageReceived(chatMessage)
            ),
            Arguments.of(
                mock<ChatRoomUpdate.OnMessageUpdate> { on { msg }.thenReturn(megaMessage) },
                MessageUpdate(chatMessage)
            ),
        )
    }

    private fun provideNonMessageUpdates() = Stream.of(
        mock<ChatRoomUpdate.OnChatRoomUpdate>(),
        mock<ChatRoomUpdate.OnHistoryReloaded>(),
        mock<ChatRoomUpdate.OnReactionUpdate>(),
    )
}