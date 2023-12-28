package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class JoinGuestChatCallUseCaseTest {
    private val createEphemeralAccountUseCase: CreateEphemeralAccountUseCase = mock()
    private val initGuestChatSessionUseCase: InitGuestChatSessionUseCase = mock()
    private val joinChatLinkUseCase: JoinChatLinkUseCase = mock()

    private lateinit var underTest: JoinGuestChatCallUseCase

    @Before
    fun setup() {
        underTest = JoinGuestChatCallUseCase(
            createEphemeralAccountUseCase,
            initGuestChatSessionUseCase,
            joinChatLinkUseCase
        )
    }

    @Test
    fun `test that all methods are called in correct order`() = runTest {
        val chatLink = "chatLink"
        val firstName = "firstName"
        val lastName = "lastName"

        underTest.invoke(chatLink, firstName, lastName)

        verify(initGuestChatSessionUseCase).invoke(false)
        verify(createEphemeralAccountUseCase).invoke(firstName, lastName)
        verify(joinChatLinkUseCase).invoke(chatLink)
    }

    @Test(expected = ChatRoomDoesNotExistException::class)
    fun `test that exception is thrown when chat room does not exist`() = runTest {
        val chatLink = "chatLink"
        val firstName = "firstName"
        val lastName = "lastName"

        whenever(joinChatLinkUseCase.invoke(chatLink)).thenThrow(ChatRoomDoesNotExistException())

        underTest.invoke(chatLink, firstName, lastName)
    }
}
