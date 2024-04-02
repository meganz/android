package mega.privacy.android.domain.usecase.notifications

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.notifications.PromoNotification
import mega.privacy.android.domain.repository.NotificationsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPromoNotificationsUseCaseTest {
    private lateinit var underTest: GetPromoNotificationsUseCase
    private val notificationsRepository = mock<NotificationsRepository>()
    private val getEnabledNotificationsUseCase = mock<GetEnabledNotificationsUseCase>()
    private val getLastReadNotificationUseCase = mock<GetLastReadNotificationIdUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = GetPromoNotificationsUseCase(
            notificationsRepository = notificationsRepository,
            getEnabledNotificationsUseCase = getEnabledNotificationsUseCase,
            getLastReadNotificationUseCase = getLastReadNotificationUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            notificationsRepository,
            getEnabledNotificationsUseCase,
            getLastReadNotificationUseCase
        )
    }

    private fun getMockedPromoNotification(promoID: Long): PromoNotification =
        PromoNotification(
            promoID = promoID,
            title = "Title",
            description = "Description",
            iconURL = "https://www.mega.co.nz",
            imageURL = "https://www.mega.co.nz",
            startTimeStamp = 1,
            endTimeStamp = 1,
            actionName = "Action name",
            actionURL = "https://www.mega.co.nz"
        )

    private fun provideParameters() = Stream.of(
        Arguments.of(
            // enabledNotifications,
            // lastReadNotificationID,
            // returnedPromoNotificationsList,
            // expectedTotalPromoNotificationCount,
            // expectedNewPromoNotificationsCount
            emptyList<Int>(),
            0,
            emptyList<PromoNotification>(),
            0,
            0
        ),
        Arguments.of(
            // enabledNotifications,
            // lastReadNotificationID,
            // returnedPromoNotificationsList,
            // expectedTotalPromoNotificationCount,
            // expectedNewPromoNotificationsCount
            listOf(1),
            1,
            listOf(getMockedPromoNotification(1L)),
            1,
            0
        ),
        Arguments.of(
            // enabledNotifications,
            // lastReadNotificationID,
            // returnedPromoNotificationsList,
            // expectedTotalPromoNotificationCount,
            // expectedNewPromoNotificationsCount
            listOf(1, 2),
            1,
            listOf(
                getMockedPromoNotification(1L),
                getMockedPromoNotification(2L)
            ),
            2,
            1
        ),
        Arguments.of(
            // enabledNotifications,
            // lastReadNotificationID,
            // returnedPromoNotificationsList,
            // expectedTotalPromoNotificationCount,
            // expectedNewPromoNotificationsCount
            listOf(1, 2, 3),
            1,
            listOf(
                getMockedPromoNotification(1L),
                getMockedPromoNotification(2L),
                getMockedPromoNotification(3L)
            ),
            3,
            2
        ),
        Arguments.of(
            // enabledNotifications,
            // lastReadNotificationID,
            // returnedPromoNotificationsList,
            // expectedTotalPromoNotificationCount,
            // expectedNewPromoNotificationsCount
            listOf(1, 2, 3, 4),
            1,
            listOf(
                getMockedPromoNotification(1L),
                getMockedPromoNotification(2L),
                getMockedPromoNotification(3L),
                getMockedPromoNotification(4L)
            ),
            4,
            3
        ),
        Arguments.of(
            // enabledNotifications,
            // lastReadNotificationID,
            // returnedPromoNotificationsList,
            // expectedTotalPromoNotificationCount,
            // expectedNewPromoNotificationsCount
            listOf(1, 2, 3, 4, 5),
            1,
            listOf(
                getMockedPromoNotification(1L),
                getMockedPromoNotification(2L),
                getMockedPromoNotification(3L),
                getMockedPromoNotification(4L),
                getMockedPromoNotification(5L)
            ),
            5,
            4
        )

    )

    @ParameterizedTest(name = "when enabledNotifications = {0}, lastReadNotificationID = {1}, returned promo notifications list = {2} then expectedTotalPromoNotificationCount = {3} and expectedNewPromoNotificationsCount = {4}")
    @MethodSource("provideParameters")
    fun `test that the right list size of promo notifications is returned when getLastReadNotificationUseCase and getEnabledNotificationsUseCase return the right values`(
        enabledNotifications: List<Int>,
        lastReadNotificationID: Long,
        returnedPromoNotificationsList: List<PromoNotification>,
        expectedTotalPromoNotificationCount: Int,
        expectedNewPromoNotificationsCount: Int,
    ) = runTest {

        whenever(notificationsRepository.getPromoNotifications()).thenReturn(
            returnedPromoNotificationsList
        )
        whenever(getEnabledNotificationsUseCase()).thenReturn(enabledNotifications)
        whenever(getLastReadNotificationUseCase()).thenReturn(lastReadNotificationID)
        assertThat(underTest.invoke().size).isEqualTo(expectedTotalPromoNotificationCount)
        assertThat(underTest.invoke().count { it.isNew }).isEqualTo(
            expectedNewPromoNotificationsCount
        )
    }
}