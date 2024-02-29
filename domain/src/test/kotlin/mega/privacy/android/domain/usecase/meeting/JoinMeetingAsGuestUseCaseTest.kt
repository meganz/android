package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.chat.CreateEphemeralAccountUseCase
import mega.privacy.android.domain.usecase.chat.InitGuestChatSessionUseCase
import mega.privacy.android.domain.usecase.chat.link.OpenChatPreviewUseCase
import mega.privacy.android.domain.usecase.login.ChatLogoutUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/**
 * Test class for [JoinMeetingAsGuestUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JoinMeetingAsGuestUseCaseTest {

    private lateinit var underTest: JoinMeetingAsGuestUseCase

    private val openChatPreviewUseCase = Mockito.mock<OpenChatPreviewUseCase>()
    private val initGuestChatSessionUseCase = Mockito.mock<InitGuestChatSessionUseCase>()
    private val chatLogoutUseCase = Mockito.mock<ChatLogoutUseCase>()
    private val createEphemeralAccountUseCase = Mockito.mock<CreateEphemeralAccountUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = JoinMeetingAsGuestUseCase(
            openChatPreviewUseCase = openChatPreviewUseCase,
            initGuestChatSessionUseCase = initGuestChatSessionUseCase,
            chatLogoutUseCase = chatLogoutUseCase,
            createEphemeralAccountUseCase = createEphemeralAccountUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            openChatPreviewUseCase,
            initGuestChatSessionUseCase,
            chatLogoutUseCase,
            createEphemeralAccountUseCase
        )
    }

    @Test
    fun `test that all methods are called`() = runTest {
        val meetingLink = "meetingLink"
        val guestFirstName = "firstName"
        val guestLastName = "lastName"
        underTest(meetingLink, guestFirstName, guestLastName)

        verify(openChatPreviewUseCase, times(2)).invoke(meetingLink)
        verify(chatLogoutUseCase).invoke()
        verify(initGuestChatSessionUseCase).invoke(false)
        verify(createEphemeralAccountUseCase).invoke(guestFirstName, guestLastName)

    }
}
