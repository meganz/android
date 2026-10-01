package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.usecase.GetNumUnreadUserAlertsUseCase
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.contactrequest.GetIncomingContactRequestsUseCase
import mega.privacy.android.domain.usecase.chat.GetNumUnreadChatsUseCase
import mega.privacy.android.domain.usecase.notifications.GetFeatureNotificationCountUseCase
import mega.privacy.android.domain.usecase.notifications.GetNumUnreadPromoNotificationsUseCase
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNotificationCountUseCaseTest {

    private lateinit var underTest: GetNotificationCountUseCase

    private val rootNodeExistsUseCase = mock<RootNodeExistsUseCase>()
    private val getNumUnreadUserAlertsUseCase = mock<GetNumUnreadUserAlertsUseCase>()
    private val getIncomingContactRequestsUseCase = mock<GetIncomingContactRequestsUseCase>()
    private val getNumUnreadChatsUseCase = mock<GetNumUnreadChatsUseCase>()
    private val getFeatureNotificationCountUseCase = mock<GetFeatureNotificationCountUseCase>()
    private val getNumUnreadPromoNotificationsUseCase =
        mock<GetNumUnreadPromoNotificationsUseCase>()
    private val featureNotifications = setOf(getFeatureNotificationCountUseCase)


    @BeforeAll
    fun setUp() {
        underTest = GetNotificationCountUseCase(
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            getNumUnreadUserAlertsUseCase = getNumUnreadUserAlertsUseCase,
            getIncomingContactRequestsUseCase = getIncomingContactRequestsUseCase,
            getNumUnreadChatsUseCase = getNumUnreadChatsUseCase,
            getNumUnreadPromoNotificationsUseCase = getNumUnreadPromoNotificationsUseCase,
            featureNotifications = featureNotifications,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            rootNodeExistsUseCase,
            getNumUnreadUserAlertsUseCase,
            getIncomingContactRequestsUseCase,
            getNumUnreadChatsUseCase,
            getFeatureNotificationCountUseCase,
            getNumUnreadPromoNotificationsUseCase,
        )
    }

    @ParameterizedTest(
        name = "when withChatNotifications is {0} and promo notifications are {1} and rootNodeExistsUseCase is {2} " +
                "and the unhandled notifications are: unread user alerts {3}, " +
                "incoming contact requests {4} unread chats {5} and featureNotifications are {6}"
    )
    @MethodSource("provideParameters")
    fun `test that the notification count is correct`(
        withChatNotifications: Boolean,
        unreadPromoNotificationsCount: Int,
        rootNodeExists: Boolean,
        numUnreadUserAlerts: Int,
        numIncomingContactRequests: Int,
        numUnreadChats: Int,
        featureNotification: Int,
        expectedResult: Int,
    ) = runTest {
        whenever(getNumUnreadPromoNotificationsUseCase()).thenReturn(unreadPromoNotificationsCount)
        whenever(rootNodeExistsUseCase()).thenReturn(rootNodeExists)
        whenever(getNumUnreadUserAlertsUseCase()).thenReturn(numUnreadUserAlerts)
        whenever(getFeatureNotificationCountUseCase()).thenReturn(featureNotification)
        val incomingContactRequestsList = mock<List<ContactRequest>> {
            on { size }.thenReturn(numIncomingContactRequests)
        }
        whenever(getIncomingContactRequestsUseCase()).thenReturn(incomingContactRequestsList)
        whenever(getNumUnreadChatsUseCase()).thenReturn(flowOf(numUnreadChats))

        assertEquals(
            underTest(withChatNotifications),
            expectedResult
        )
    }

    companion object {
        @JvmStatic
        private fun provideParameters(): Stream<Arguments?>? =
            Stream.of(
                Arguments.of(false, 0, true, 1, 2, 4, 0, 3),
                Arguments.of(true, 0, true, 1, 2, 4, 0, 7),
                Arguments.of(false, 0, false, 1, 2, 4, 0, 0),
                Arguments.of(true, 0, false, 1, 2, 4, 0, 0),
                Arguments.of(false, 1, true, 1, 2, 4, 0, 4),
                Arguments.of(true, 1, true, 1, 2, 4, 0, 8),
                Arguments.of(false, 1, false, 1, 2, 4, 0, 0),
                Arguments.of(true, 1, false, 1, 2, 4, 0, 0),
            )
    }
}