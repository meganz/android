package mega.privacy.android.app.presentation.notification

import androidx.compose.ui.unit.sp
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.notification.NotificationViewModel
import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.presentation.notification.model.mapper.NotificationMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.ContactChangeContactEstablishedAlert
import mega.privacy.android.domain.entity.IncomingPendingContactRequestAlert
import mega.privacy.android.domain.entity.notifications.PromoNotification
import mega.privacy.android.domain.usecase.AcknowledgeUserAlertsUseCase
import mega.privacy.android.domain.usecase.MonitorUserAlertsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.notifications.GetPromoNotificationsUseCase
import mega.privacy.android.domain.usecase.notifications.SetLastReadNotificationUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationViewModelTest {
    private lateinit var underTest: NotificationViewModel

    private val monitorUserAlertsUseCase = mock<MonitorUserAlertsUseCase> {
        onBlocking { invoke() }.thenReturn(
            emptyFlow()
        )
    }

    private val acknowledgeUserAlertsUseCase = mock<AcknowledgeUserAlertsUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getPromoNotificationsUseCase = mock<GetPromoNotificationsUseCase>()
    private val setLastReadNotificationUseCase = mock<SetLastReadNotificationUseCase>()
    private val notificationMapper = mock<NotificationMapper>()


    @BeforeEach
    fun resetMocks() {
        reset(
            monitorUserAlertsUseCase,
            acknowledgeUserAlertsUseCase,
            getFeatureFlagValueUseCase,
            getPromoNotificationsUseCase,
            setLastReadNotificationUseCase,
            notificationMapper
        )
    }

    private fun initViewModel() {
        underTest = NotificationViewModel(
            acknowledgeUserAlertsUseCase = acknowledgeUserAlertsUseCase,
            monitorUserAlertsUseCase = monitorUserAlertsUseCase,
            getPromoNotificationsUseCase = getPromoNotificationsUseCase,
            setLastReadNotificationUseCase = setLastReadNotificationUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            notificationMapper = notificationMapper,
        )
    }

    @Test
    fun `test that initial value is an empty list and scroll to top is false`() = runTest {

        whenever(monitorUserAlertsUseCase()).thenReturn(mock())
        whenever(getFeatureFlagValueUseCase(any())).thenReturn(true)
        whenever(getPromoNotificationsUseCase()).thenReturn(mock())
        whenever(notificationMapper(any())).thenReturn(mock())

        initViewModel()
        underTest.state.test {
            val (normalNotifications, promoNotifications, scrollToTop) = awaitItem()
            assertWithMessage("Initial notification list should be empty").that(normalNotifications)
                .isEmpty()
            assertWithMessage("Initial promo notification list should be empty").that(
                promoNotifications
            ).isEmpty()
            assertWithMessage("Initial scroll to top should be false").that(scrollToTop).isFalse()
        }
    }

    @Test
    fun `test that subsequent values are returned`() = runTest {

        val expectedNotification = Notification(
            sectionTitle = { "" },
            sectionColour = 0,
            sectionIcon = null,
            title = { "" },
            titleTextSize = 16.sp,
            description = { "" },
            schedMeetingNotification = null,
            dateText = { "" },
            isNew = true,
            backgroundColor = { "#D3D3D3" },
            separatorMargin = { 0 },
        ) {}

        val expectedPromoNotification = PromoNotification(
            promoID = 1,
            title = "Title",
            description = "Description",
            iconURL = "https://www.mega.co.nz",
            imageURL = "https://www.mega.co.nz",
            startTimeStamp = 1,
            endTimeStamp = 1,
            actionName = "Action name",
            actionURL = "https://www.mega.co.nz"
        )

        val alert = mock<IncomingPendingContactRequestAlert>()
        whenever(monitorUserAlertsUseCase()).thenReturn(flowOf(listOf(alert)))
        whenever(getFeatureFlagValueUseCase(any())).thenReturn(true)
        whenever(getPromoNotificationsUseCase()).thenReturn(listOf(expectedPromoNotification))
        whenever(notificationMapper(alert)).thenReturn(expectedNotification)

        initViewModel()

        underTest.state.drop(1).test {
            val state = awaitItem()
            assertWithMessage("Expected returned user alerts").that(state.notifications)
                .containsExactly(expectedNotification)
            assertWithMessage("Expected returned promo notification").that(state.promoNotifications)
                .containsExactly(expectedPromoNotification)
        }
    }

    @Test
    fun `test that should scroll is updated to true if new items appear`() = runTest {
        val initialAlert = mock<IncomingPendingContactRequestAlert>()
        val newAlert = mock<ContactChangeContactEstablishedAlert>()
        val initialNotification = Notification(
            sectionTitle = { "" },
            sectionColour = 0,
            sectionIcon = null,
            title = { "Initial" },
            titleTextSize = 16.sp,
            description = { "" },
            schedMeetingNotification = null,
            dateText = { "" },
            isNew = true,
            backgroundColor = { "#D3D3D3" },
            separatorMargin = { 0 },
        ) {}
        val newNotification = initialNotification.copy(title = { "New title" })
        whenever(notificationMapper(initialAlert)).thenReturn(initialNotification)
        whenever(notificationMapper(newAlert)).thenReturn(newNotification)
        whenever(getFeatureFlagValueUseCase(any())).thenReturn(true)
        whenever(getPromoNotificationsUseCase()).thenReturn(emptyList())

        whenever(monitorUserAlertsUseCase()).thenReturn(
            flowOf(
                listOf(initialAlert),
                listOf(newAlert, initialAlert)
            )
        )

        initViewModel()

        underTest.state.drop(1).test {
            val (_, _, scrollToTop) = awaitItem()
            assertWithMessage("Initial scroll value should be false").that(scrollToTop).isFalse()
            val (_, _, scrollUpdate) = awaitItem()
            assertWithMessage("Subsequent scroll value should be true").that(scrollUpdate).isTrue()
        }
    }

    @Test
    fun `test that notifications are acknowledged once loaded`() {
        underTest.onNotificationsLoaded()
        scheduler.advanceUntilIdle()
        verifyBlocking(acknowledgeUserAlertsUseCase, AcknowledgeUserAlertsUseCase::invoke)
    }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that promo notifications are fetched based on feature flag value`(featureFlag: Boolean) =
        runTest {
            whenever(notificationMapper(any())).thenReturn(mock())
            whenever(getFeatureFlagValueUseCase(any())).thenReturn(featureFlag)
            whenever(getPromoNotificationsUseCase()).thenReturn(emptyList())
            val alert = mock<IncomingPendingContactRequestAlert>()
            whenever(monitorUserAlertsUseCase()).thenReturn(flowOf(listOf(alert)))
            initViewModel()
            scheduler.advanceUntilIdle()
            if (featureFlag) {
                verifyBlocking(getPromoNotificationsUseCase, GetPromoNotificationsUseCase::invoke)
            } else {
                verifyBlocking(
                    getPromoNotificationsUseCase,
                    times(0),
                    GetPromoNotificationsUseCase::invoke
                )
            }
        }


    private fun providePromoNotificationsParameters() = Stream.of(
        Arguments.of(emptyList<PromoNotification>(), false, 0),
        Arguments.of(
            listOf(
                PromoNotification(
                    promoID = 3,
                    title = "Title",
                    description = "Description",
                    iconURL = "https://www.mega.co.nz",
                    imageURL = "https://www.mega.co.nz",
                    startTimeStamp = 1,
                    endTimeStamp = 1,
                    actionName = "Action name",
                    actionURL = "https://www.mega.co.nz"
                ),
                PromoNotification(
                    promoID = 2,
                    title = "Title",
                    description = "Description",
                    iconURL = "https://www.mega.co.nz",
                    imageURL = "https://www.mega.co.nz",
                    startTimeStamp = 1,
                    endTimeStamp = 1,
                    actionName = "Action name",
                    actionURL = "https://www.mega.co.nz"
                )
            ), true, 3
        )
    )

    @ParameterizedTest(name = "test that setLastReadNotificationUseCase invocation is: {1} when getPromoNotificationsUseCase returns {0} and that the last read promo notification id is {2}")
    @MethodSource("providePromoNotificationsParameters")
    fun `test that setLastReadNotificationUseCase is invoked based on getPromoNotificationsUseCase returns the right list`(
        promoNotificationsList: List<PromoNotification>,
        expectedInvocations: Boolean,
        expectedLastReadPromoNotificationId: Long,
    ) =
        runTest {
            whenever(notificationMapper(any())).thenReturn(mock())
            whenever(getFeatureFlagValueUseCase(any())).thenReturn(true)
            whenever(getPromoNotificationsUseCase()).thenReturn(promoNotificationsList)
            val alert = mock<IncomingPendingContactRequestAlert>()
            whenever(monitorUserAlertsUseCase()).thenReturn(flowOf(listOf(alert)))
            initViewModel()
            scheduler.advanceUntilIdle()
            underTest.onNotificationsLoaded()
            scheduler.advanceUntilIdle()

            if (expectedInvocations) {
                verifyBlocking(setLastReadNotificationUseCase) {
                    invoke(any())
                }
                underTest.state.test {
                    val value = awaitItem().promoNotifications.firstOrNull()?.promoID
                    assertThat(value).isEqualTo(expectedLastReadPromoNotificationId)

                }
            } else {
                verify(setLastReadNotificationUseCase, times(0)).invoke(
                    any()
                )
            }
        }

    companion object {
        private val scheduler = TestCoroutineScheduler()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher(scheduler))
    }
}