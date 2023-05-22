package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.usecase.GetNumUnreadUserAlertsUseCase
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.chat.GetNumUnreadChatsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNotificationCountUseCaseTest {

    private lateinit var underTest: GetNotificationCountUseCase

    private val rootNodeExistsUseCase = mock<RootNodeExistsUseCase>()
    private val getNumUnreadUserAlertsUseCase = mock<GetNumUnreadUserAlertsUseCase>()
    private val getIncomingContactRequestsUseCase = mock<GetIncomingContactRequestsUseCase>()
    private val getNumUnreadChatsUseCase = mock<GetNumUnreadChatsUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = GetNotificationCountUseCase(
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            getNumUnreadUserAlertsUseCase = getNumUnreadUserAlertsUseCase,
            getIncomingContactRequestsUseCase = getIncomingContactRequestsUseCase,
            getNumUnreadChatsUseCase = getNumUnreadChatsUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            rootNodeExistsUseCase,
            getNumUnreadUserAlertsUseCase,
            getIncomingContactRequestsUseCase,
            getNumUnreadChatsUseCase
        )
    }

    @ParameterizedTest(
        name = "when withChatNotifications is {0} and rootNodeExistsUseCase is {1} " +
                "and the unhandled notifications are: unread user alerts {2}, " +
                "incoming contact requests {3} and" +
                "unread chats {4}"
    )
    @MethodSource("provideParameters")
    fun `test that the notification count is correct`(
        withChatNotifications: Boolean,
        rootNodeExists: Boolean,
        numUnreadUserAlerts: Int,
        numIncomingContactRequests: Int,
        numUnreadChats: Int,
        expectedResult: Int,
    ) = runTest {
        whenever(rootNodeExistsUseCase()).thenReturn(rootNodeExists)
        whenever(getNumUnreadUserAlertsUseCase()).thenReturn(numUnreadUserAlerts)
        val incomingContactRequestsList = mock<List<ContactRequest>> {
            on { size }.thenReturn(numIncomingContactRequests)
        }
        whenever(getIncomingContactRequestsUseCase()).thenReturn(incomingContactRequestsList)
        whenever(getNumUnreadChatsUseCase()).thenReturn(numUnreadChats)

        assertEquals(underTest(withChatNotifications), expectedResult)
    }

    companion object {
        @JvmStatic
        private fun provideParameters(): Stream<Arguments?>? =
            Stream.of(
                Arguments.of(false, true, 1, 2, 4, 3),
                Arguments.of(false, true, 0, 2, 4, 2),
                Arguments.of(false, false, 1, 2, 4, 0),
                Arguments.of(false, false, 1, 2, 0, 0),
                Arguments.of(true, false, 1, 2, 4, 0),
                Arguments.of(true, false, 1, 0, 4, 0),
                Arguments.of(true, true, 1, 2, 4, 7),
                Arguments.of(true, true, 1, 2, 0, 3),
            )
    }
}