package mega.privacy.android.domain.usecase.notifications

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.notifications.PromoNotification
import mega.privacy.android.domain.repository.NotificationsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPromoNotificationsUseCaseTest {
    private lateinit var underTest: GetPromoNotificationsUseCase
    private val notificationsRepository = mock<NotificationsRepository>()
    private val getEnabledNotificationsUseCase = mock<GetEnabledNotificationsUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = GetPromoNotificationsUseCase(
            notificationsRepository = notificationsRepository,
            getEnabledNotificationsUseCase = getEnabledNotificationsUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            notificationsRepository,
            getEnabledNotificationsUseCase
        )
    }

    private fun getMockedPromoNotificationsList(): List<PromoNotification> {
        val mockedList = mutableListOf<PromoNotification>()
        for (i in 1L..5L) {
            mockedList += PromoNotification(
                promoID = i,
                title = "Title",
                description = "Description",
                iconURL = "https://www.mega.co.nz",
                imageURL = "https://www.mega.co.nz",
                startTimeStamp = 1,
                endTimeStamp = 1,
                actionName = "Action name",
                actionURL = "https://www.mega.co.nz"
            )
        }
        return mockedList
    }

    @Test
    fun `test that invoke returns the list of promo notifications`() = runTest {
        val promoNotificationsList =
            getMockedPromoNotificationsList()
        val expectedList = promoNotificationsList.sortedByDescending { it.promoID }
        whenever(notificationsRepository.getPromoNotifications()).thenReturn(promoNotificationsList)
        whenever(getEnabledNotificationsUseCase()).thenReturn(listOf(3, 1, 4, 2, 5))

        println(promoNotificationsList)
        println(expectedList)
        assertThat(underTest.invoke()).isEqualTo(expectedList)
    }
}