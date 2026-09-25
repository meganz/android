package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SetUserTypingStatusUseCaseTest {
    private lateinit var underTest: SetUserTypingStatusUseCase

    private val chatParticipantsRepository = mock<ChatParticipantsRepository>()
    private val getCurrentTimeInMilliSeconds = mock<() -> Long>()

    @BeforeEach
    fun setUp() {
        reset(
            chatParticipantsRepository,
            getCurrentTimeInMilliSeconds,
        )
    }

    private fun initUnderTest(scope: CoroutineScope) {
        underTest =
            SetUserTypingStatusUseCase(
                chatParticipantsRepository = chatParticipantsRepository,
                scope = scope,
                getCurrentTimeInMilliSeconds = getCurrentTimeInMilliSeconds,
            )

        getCurrentTimeInMilliSeconds.stub {
            onGeneric { invoke() }.doReturn(1)
        }
    }

    @Test
    fun `test that start typing function is called with correct chat id`() = runTest {
        val expectedChatId = 12345L
        initUnderTest(this)

        underTest(isUserTyping = true, chatId = expectedChatId)

        verify(chatParticipantsRepository).setUserStartTyping(expectedChatId)
    }

    @Test
    fun `test that stop typing function is called with correct chat id`() = runTest {
        val expectedChatId = 12345L
        initUnderTest(this)

        underTest(isUserTyping = false, chatId = expectedChatId)

        verify(chatParticipantsRepository).setUserStopTyping(expectedChatId)
    }


    @Test
    fun `test that stop typing is sent 5 seconds after the typing notification`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val expectedChatId = 12345L
        initUnderTest(this)

        underTest(isUserTyping = true, chatId = expectedChatId)
        verify(chatParticipantsRepository).setUserStartTyping(expectedChatId)
        advanceTimeBy(6.seconds)

        verify(chatParticipantsRepository, times(1)).setUserStopTyping(expectedChatId)

        Dispatchers.resetMain()
    }

    @Test
    fun `test that stop typing is only sent 5 seconds after the last typing notification if multiple typing events`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val expectedChatId = 12345L
            var typingTime = 1L
            initUnderTest(this)

            getCurrentTimeInMilliSeconds.stub {
                onGeneric { invoke() }.doAnswer {
                    typingTime.also {
                        typingTime += 2.seconds.inWholeMilliseconds
                    }
                }
            }

            underTest(isUserTyping = true, chatId = expectedChatId)
            verify(chatParticipantsRepository).setUserStartTyping(expectedChatId)

            advanceTimeBy(2.seconds)

            verify(chatParticipantsRepository, never()).setUserStopTyping(any())
            underTest(isUserTyping = true, chatId = expectedChatId)
            verify(chatParticipantsRepository, times(1)).setUserStartTyping(expectedChatId)

            advanceTimeBy(2.seconds)

            verify(chatParticipantsRepository, never()).setUserStopTyping(any())
            underTest(isUserTyping = true, chatId = expectedChatId)
            verify(chatParticipantsRepository, times(2)).setUserStartTyping(expectedChatId)

            advanceTimeBy(6.seconds)

            verify(chatParticipantsRepository, times(1)).setUserStopTyping(expectedChatId)

            Dispatchers.resetMain()
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that stop typing is called if a new chat id is sent`(isTyping: Boolean) = runTest {
        val chatOne = 12345L
        val chatTwo = 123456L
        initUnderTest(this)

        underTest(isUserTyping = true, chatId = chatOne)
        verify(chatParticipantsRepository, times(1)).setUserStartTyping(chatOne)
        underTest(isUserTyping = isTyping, chatId = chatTwo)
        verify(chatParticipantsRepository).setUserStopTyping(chatOne)
        if (isTyping) {
            verify(chatParticipantsRepository).setUserStartTyping(chatTwo)
        } else {
            verify(chatParticipantsRepository).setUserStopTyping(chatTwo)
        }
    }

    @Test
    fun `test that typing events are only sent every 4 seconds`() = runTest {
        val expectedChatId = 12345L
        val initialTime = 1L
        val second = initialTime + 3.seconds.inWholeMilliseconds
        val third = initialTime + 4.seconds.inWholeMilliseconds

        initUnderTest(this)

        getCurrentTimeInMilliSeconds.stub {
            onGeneric { invoke() }.doReturn(initialTime, second, third)
        }

        underTest(isUserTyping = true, chatId = expectedChatId)
        verify(chatParticipantsRepository).setUserStartTyping(expectedChatId)
        underTest(isUserTyping = true, chatId = expectedChatId)
        verify(chatParticipantsRepository, times(1)).setUserStartTyping(expectedChatId)
        underTest(isUserTyping = true, chatId = expectedChatId)
        verify(chatParticipantsRepository, times(2)).setUserStartTyping(expectedChatId)
    }


}