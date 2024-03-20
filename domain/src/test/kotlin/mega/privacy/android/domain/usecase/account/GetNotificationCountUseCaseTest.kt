package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.notifications.PromoNotification
import mega.privacy.android.domain.usecase.GetNumUnreadUserAlertsUseCase
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.chat.GetNumUnreadChatsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.notifications.GetFeatureNotificationCountUseCase
import mega.privacy.android.domain.usecase.notifications.GetPromoNotificationsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
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
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getPromoNotificationsUseCase = mock<GetPromoNotificationsUseCase>()
    private val featureNotifications = setOf(getFeatureNotificationCountUseCase)


    @BeforeAll
    fun setUp() {
        underTest = GetNotificationCountUseCase(
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            getNumUnreadUserAlertsUseCase = getNumUnreadUserAlertsUseCase,
            getIncomingContactRequestsUseCase = getIncomingContactRequestsUseCase,
            getNumUnreadChatsUseCase = getNumUnreadChatsUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getPromoNotificationsUseCase = getPromoNotificationsUseCase,
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
            getFeatureFlagValueUseCase,
            getPromoNotificationsUseCase,
        )
    }

    @ParameterizedTest(
        name = "when withChatNotifications is {0} and PromoNotifications feature is {1} and promo notifications are {2} and rootNodeExistsUseCase is {3} " +
                "and the unhandled notifications are: unread user alerts {4}, " +
                "incoming contact requests {5} unread chats {6} and featureNotifications are {7}"
    )
    @MethodSource("provideParameters")
    fun `test that the notification count is correct`(
        withChatNotifications: Boolean,
        isPromoNotificationsFeatureEnabled: Boolean,
        promoNotificationsList: List<PromoNotification>,
        rootNodeExists: Boolean,
        numUnreadUserAlerts: Int,
        numIncomingContactRequests: Int,
        numUnreadChats: Int,
        featureNotification: Int,
        expectedResult: Int,
    ) = runTest {
        whenever(getFeatureFlagValueUseCase(any())).thenReturn(isPromoNotificationsFeatureEnabled)
        whenever(getPromoNotificationsUseCase()).thenReturn(promoNotificationsList)
        whenever(rootNodeExistsUseCase()).thenReturn(rootNodeExists)
        whenever(getNumUnreadUserAlertsUseCase()).thenReturn(numUnreadUserAlerts)
        whenever(getFeatureNotificationCountUseCase()).thenReturn(featureNotification)
        val incomingContactRequestsList = mock<List<ContactRequest>> {
            on { size }.thenReturn(numIncomingContactRequests)
        }
        whenever(getIncomingContactRequestsUseCase()).thenReturn(incomingContactRequestsList)
        whenever(getNumUnreadChatsUseCase()).thenReturn(flowOf(numUnreadChats))

        assertEquals(
            underTest(withChatNotifications, mock()),
            expectedResult
        )
    }

    companion object {
        val promoNotification = PromoNotification(
            promoID = 1,
            title = "Title",
            description = "Description",
            imageName = "Image name",
            imageURL = "https://www.mega.co.nz",
            startTimeStamp = 1,
            endTimeStamp = 1,
            actionName = "Action name",
            actionURL = "https://www.mega.co.nz"
        )

        @JvmStatic
        private fun provideParameters(): Stream<Arguments?>? =
            Stream.of(
                Arguments.of(false, false, emptyList<PromoNotification>(), true, 1, 2, 4, 0, 3),
                Arguments.of(true, false, emptyList<PromoNotification>(), true, 1, 2, 4, 0, 7),
                Arguments.of(false, true, emptyList<PromoNotification>(), true, 1, 2, 4, 0, 3),
                Arguments.of(true, true, emptyList<PromoNotification>(), true, 1, 2, 4, 0, 7),
                Arguments.of(false, false, emptyList<PromoNotification>(), false, 1, 2, 4, 0, 0),
                Arguments.of(true, false, emptyList<PromoNotification>(), false, 1, 2, 4, 0, 0),
                Arguments.of(false, true, emptyList<PromoNotification>(), false, 1, 2, 4, 0, 0),
                Arguments.of(true, true, emptyList<PromoNotification>(), false, 1, 2, 4, 0, 0),
                Arguments.of(false, false, listOf(promoNotification), true, 1, 2, 4, 0, 3),
                Arguments.of(true, false, listOf(promoNotification), true, 1, 2, 4, 0, 7),
                Arguments.of(false, true, listOf(promoNotification), true, 1, 2, 4, 0, 4),
                Arguments.of(true, true, listOf(promoNotification), true, 1, 2, 4, 0, 8),
                Arguments.of(false, false, listOf(promoNotification), false, 1, 2, 4, 0, 0),
                Arguments.of(true, false, listOf(promoNotification), false, 1, 2, 4, 0, 0),
                Arguments.of(false, true, listOf(promoNotification), false, 1, 2, 4, 0, 0),
                Arguments.of(true, true, listOf(promoNotification), false, 1, 2, 4, 0, 0),
            )
    }
}