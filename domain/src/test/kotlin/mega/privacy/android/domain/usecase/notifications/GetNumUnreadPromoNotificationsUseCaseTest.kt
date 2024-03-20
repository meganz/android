package mega.privacy.android.domain.usecase.notifications

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.notifications.PromoNotification
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

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNumUnreadPromoNotificationsUseCaseTest {
    private lateinit var underTest: GetNumUnreadPromoNotificationsUseCase
    private val getPromoNotificationsUseCase = mock<GetPromoNotificationsUseCase>()
    private val getLastReadNotificationUseCase = mock<GetLastReadNotificationIDUseCase>()


    @BeforeAll
    fun setUp() {
        underTest = GetNumUnreadPromoNotificationsUseCase(
            getPromoNotificationsUseCase = getPromoNotificationsUseCase,
            getLastReadNotificationUseCase = getLastReadNotificationUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getPromoNotificationsUseCase,
            getLastReadNotificationUseCase
        )
    }

    private fun getMockedPromoId(promoID: Long): PromoNotification =
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
        Arguments.of(emptyList<PromoNotification>(), 0, 0),
        Arguments.of(
            listOf(
                getMockedPromoId(1)
            ), 1, 0
        ),
        Arguments.of(
            listOf(
                getMockedPromoId(1),
                getMockedPromoId(2)
            ), 1, 1
        ),
        Arguments.of(
            listOf(
                getMockedPromoId(1),
                getMockedPromoId(2),
                getMockedPromoId(3)
            ), 0, 3
        ),
        Arguments.of(
            listOf(
                getMockedPromoId(1),
                getMockedPromoId(2),
                getMockedPromoId(3)
            ), 3, 0
        ),
    )

    @ParameterizedTest
    @MethodSource("provideParameters")
    fun `test that calculateUnreadPromoNotifications return the correct number of unread promo notifications`(
        promoNotifications: List<PromoNotification>,
        lastReadNotificationId: Long,
        expectedUnreadPromoNotificationsCount: Int,
    ) = runTest {
        whenever(getPromoNotificationsUseCase()).thenReturn(promoNotifications)
        whenever(getLastReadNotificationUseCase()).thenReturn(lastReadNotificationId)
        Truth.assertThat(underTest.invoke()).isEqualTo(expectedUnreadPromoNotificationsCount)
    }
}