package mega.privacy.android.domain.usecase.chat.message.reactions

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.entity.chat.messages.reactions.ReactionUpdate
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorReactionUpdatesUseCaseTest {

    private lateinit var underTest: MonitorReactionUpdatesUseCase

    private val chatRepository = mock<ChatRepository>()
    private val chatMessageRepository = mock<ChatMessageRepository>()

    private val chatId = 123L
    private val myUserHandle = 234L
    private val userHandle = 456L

    @BeforeEach
    fun setup() {
        underTest = MonitorReactionUpdatesUseCase(chatRepository, chatMessageRepository)
    }

    @AfterEach
    fun resetMocks() {
        reset(chatRepository, chatMessageRepository)
    }

    @ParameterizedTest(name = " to {4}, I add a reaction={3} and outdated reactions size is {5}")
    @ArgumentsSource(MonitorReactionUpdatedArgumentsProvider::class)
    fun `test that reaction is updated when count increments `(
        reactionUpdate: ReactionUpdate,
        outdatedReactions: List<Reaction>,
        updatedReactions: List<Reaction>,
        iAddReaction: Boolean,
        updatedReactionCount: Int,
        outdatedReactionsCount: Int,
    ) = runTest {
        whenever(chatRepository.monitorReactionUpdates(chatId)).thenReturn(flowOf(reactionUpdate))
        with(reactionUpdate) {
            val hasMe = updatedReactions.find { it.reaction == reaction }?.hasMe ?: false
            val userHandles = buildList {
                if (hasMe) {
                    add(myUserHandle)
                    count - 1
                } else {
                    count
                }.let { repeat(it) { add(userHandle) } }
            }

            whenever(chatMessageRepository.getReactionUsers(chatId, msgId, reaction))
                .thenReturn(userHandles)
            whenever(chatRepository.getMyUserHandle()).thenReturn(myUserHandle)
            whenever(chatMessageRepository.getReactionsFromMessage(chatId, msgId))
                .thenReturn(outdatedReactions)
            whenever(
                chatMessageRepository.updateReactionsInMessage(chatId, msgId, updatedReactions)
            ).thenReturn(Unit)
            underTest.invoke(chatId)
            verify(chatMessageRepository).getReactionUsers(chatId, msgId, reaction)
            verify(chatRepository).getMyUserHandle()
            verify(chatMessageRepository).getReactionsFromMessage(chatId, msgId)
            verify(chatMessageRepository).updateReactionsInMessage(
                eq(chatId),
                eq(msgId),
                argWhere { list -> list.containsAll(updatedReactions) && list.size == updatedReactions.size }
            )

        }
    }

    @ParameterizedTest(name = " I reacted={3} and outdated reactions size is {4}")
    @ArgumentsSource(MonitorReactionRemovedArgumentsProvider::class)
    fun `test that reaction is removed when updated count is 0 `(
        reactionUpdate: ReactionUpdate,
        outdatedReactions: List<Reaction>,
        updatedReactions: List<Reaction>,
        iReacted: Boolean,
        outdatedReactionsSize: Int,
    ) = runTest {
        whenever(chatRepository.monitorReactionUpdates(chatId)).thenReturn(flowOf(reactionUpdate))
        with(reactionUpdate) {
            whenever(chatMessageRepository.getReactionsFromMessage(chatId, msgId))
                .thenReturn(outdatedReactions)
            whenever(
                chatMessageRepository.updateReactionsInMessage(chatId, msgId, updatedReactions)
            ).thenReturn(Unit)

            underTest.invoke(chatId)


            verify(chatMessageRepository).getReactionsFromMessage(chatId, msgId)
            verify(chatMessageRepository).updateReactionsInMessage(
                eq(chatId),
                eq(msgId),
                argWhere { list -> list.containsAll(updatedReactions) && list.size == updatedReactions.size }
            )
        }
    }

    internal class MonitorReactionUpdatedArgumentsProvider : ArgumentsProvider {

        private val reaction1 = "reaction1"
        private val reaction2 = "reaction2"
        private val msgId = 321L
        private val myUserHandle = 234L
        private val userHandle = 456L

        override fun provideArguments(context: ExtensionContext): Stream<out Arguments>? {
            return Stream.of(
                Arguments.of(
                    ReactionUpdate(msgId, reaction1, 2),
                    listOf(Reaction(reaction1, 1, listOf(myUserHandle), true)),
                    listOf(Reaction(reaction1, 2, listOf(myUserHandle, userHandle), true)),
                    true,
                    2,
                    1,
                ),
                Arguments.of(
                    ReactionUpdate(msgId, reaction1, 2),
                    listOf(Reaction(reaction1, 1, listOf(userHandle), false)),
                    listOf(Reaction(reaction1, 2, listOf(userHandle, userHandle), false)),
                    false,
                    2,
                    1,
                ),
                Arguments.of(
                    ReactionUpdate(msgId, reaction1, 1),
                    emptyList<Reaction>(),
                    listOf(Reaction(reaction1, 1, listOf(userHandle), false)),
                    false,
                    1,
                    0,
                ),
                Arguments.of(
                    ReactionUpdate(msgId, reaction1, 1),
                    emptyList<Reaction>(),
                    listOf(Reaction(reaction1, 1, listOf(myUserHandle), true)),
                    true,
                    1,
                    0,
                ),
                Arguments.of(
                    ReactionUpdate(msgId, reaction1, 2),
                    listOf(
                        Reaction(reaction1, 1, listOf(userHandle), false),
                        Reaction(reaction2, 1, listOf(userHandle), false)
                    ),
                    listOf(
                        Reaction(reaction1, 2, listOf(myUserHandle, userHandle), true),
                        Reaction(reaction2, 1, listOf(userHandle), false)
                    ),
                    true,
                    2,
                    2,
                ),
            )
        }
    }

    internal class MonitorReactionRemovedArgumentsProvider : ArgumentsProvider {

        private val reaction1 = "reaction1"
        private val reaction2 = "reaction2"
        private val msgId = 321L
        private val myUserHandle = 234L
        private val userHandle = 456L

        override fun provideArguments(context: ExtensionContext): Stream<out Arguments>? {
            return Stream.of(
                Arguments.of(
                    ReactionUpdate(msgId, reaction1, 0),
                    listOf(Reaction(reaction1, 1, listOf(userHandle), false)),
                    emptyList<Reaction>(),
                    false,
                    1,
                ),
                Arguments.of(
                    ReactionUpdate(msgId, reaction1, 0),
                    listOf(Reaction(reaction1, 1, listOf(myUserHandle), true)),
                    emptyList<Reaction>(),
                    true,
                    1,
                ),
                Arguments.of(
                    ReactionUpdate(msgId, reaction1, 0),
                    listOf(
                        Reaction(reaction1, 1, listOf(userHandle), false),
                        Reaction(reaction2, 1, listOf(userHandle), false)
                    ),
                    listOf(Reaction(reaction2, 1, listOf(userHandle), false)),
                    false,
                    2,
                ),
                Arguments.of(
                    ReactionUpdate(msgId, reaction1, 0),
                    listOf(
                        Reaction(reaction1, 1, listOf(myUserHandle), false),
                        Reaction(reaction2, 1, listOf(userHandle), false)
                    ),
                    listOf(Reaction(reaction2, 1, listOf(userHandle), false)),
                    true,
                    2,
                ),
            )
        }
    }
}