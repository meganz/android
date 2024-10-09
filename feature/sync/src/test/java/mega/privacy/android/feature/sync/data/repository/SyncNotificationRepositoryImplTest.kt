package mega.privacy.android.feature.sync.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.SyncShownNotificationEntity
import mega.privacy.android.feature.sync.data.gateway.notification.SyncNotificationGateway
import mega.privacy.android.feature.sync.data.mapper.notification.GenericErrorToNotificationMessageMapper
import mega.privacy.android.feature.sync.data.mapper.notification.StalledIssuesToNotificationMessageMapper
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncNotificationRepositoryImplTest {

    private val syncNotificationGateway: SyncNotificationGateway = mock()
    private val stalledIssuesToNotificationMessageMapper: StalledIssuesToNotificationMessageMapper =
        mock()
    private val genericErrorToNotificationMessageMapper: GenericErrorToNotificationMessageMapper =
        mock()
    private val scheduler = TestCoroutineScheduler()
    private val unconfinedTestDispatcher = UnconfinedTestDispatcher(scheduler)

    private val underTest = SyncNotificationRepositoryImpl(
        syncNotificationGateway = syncNotificationGateway,
        stalledIssuesToNotificationMessageMapper = stalledIssuesToNotificationMessageMapper,
        genericErrorToNotificationMessageMapper = genericErrorToNotificationMessageMapper,
        ioDispatcher = unconfinedTestDispatcher
    )

    @BeforeEach
    fun resetMocks() {
        reset(
            syncNotificationGateway,
            stalledIssuesToNotificationMessageMapper,
            genericErrorToNotificationMessageMapper,
        )
    }

    @Test
    fun `test that isBatteryLowNotificationShown returns true if battery low was shown`() =
        runTest {
            whenever(syncNotificationGateway.getNotificationByType("BATTERY_LOW")).thenReturn(
                listOf(
                    mock()
                )
            )

            val result = underTest.isBatteryLowNotificationShown()

            assertThat(result).isTrue()
        }

    @Test
    fun `test that isBatteryLowNotificationShown returns false if battery low was not shown`() =
        runTest {
            whenever(syncNotificationGateway.getNotificationByType("BATTERY_LOW")).thenReturn(
                emptyList()
            )

            val result = underTest.isBatteryLowNotificationShown()

            assertThat(result).isFalse()
        }

    @Test
    fun `test that getBatteryLowNotification returns a generic error notification message`() =
        runTest {
            val notificationMessage: SyncNotificationMessage = mock()
            whenever(genericErrorToNotificationMessageMapper()).thenReturn(notificationMessage)

            val result = underTest.getBatteryLowNotification()

            assertThat(result).isEqualTo(notificationMessage)
        }

    @Test
    fun `test that setBatteryLowNotificationShown sets the notification as shown`() = runTest {
        underTest.setBatteryLowNotificationShown(true)

        verify(syncNotificationGateway).setNotificationShown(
            SyncShownNotificationEntity(notificationType = "BATTERY_LOW")
        )
    }

    @Test
    fun `test that setBatteryLowNotificationShown sets the notification as not shown`() = runTest {
        underTest.setBatteryLowNotificationShown(false)

        verify(syncNotificationGateway).deleteNotificationByType("BATTERY_LOW")
    }

    @Test
    fun `test that isUserNotOnWifiNotificationShown returns true if user not on wifi notification was shown`() =
        runTest {
            whenever(syncNotificationGateway.getNotificationByType("NOT_CONNECTED_TO_WIFI")).thenReturn(
                listOf(
                    mock()
                )
            )

            val result = underTest.isUserNotOnWifiNotificationShown()

            assertThat(result).isTrue()
        }

    @Test
    fun `test that isUserNotOnWifiNotificationShown returns false if user not on wifi notification was not shown`() =
        runTest {
            whenever(syncNotificationGateway.getNotificationByType("NOT_CONNECTED_TO_WIFI")).thenReturn(
                emptyList()
            )

            val result = underTest.isUserNotOnWifiNotificationShown()

            assertThat(result).isFalse()
        }

    @Test
    fun `test that getUserNotOnWifiNotificationShown returns a generic error notification message`() =
        runTest {
            val notificationMessage: SyncNotificationMessage = mock()
            whenever(genericErrorToNotificationMessageMapper()).thenReturn(notificationMessage)

            val result = underTest.getUserNotOnWifiNotification()

            assertThat(result).isEqualTo(notificationMessage)
        }

    @Test
    fun `test that setUserNotOnWifiNotificationShown sets the notification as shown`() = runTest {
        underTest.setUserNotOnWifiNotificationShown(true)

        verify(syncNotificationGateway).setNotificationShown(
            SyncShownNotificationEntity(notificationType = "NOT_CONNECTED_TO_WIFI")
        )
    }

    @Test
    fun `test that setUserNotOnWifiNotificationShown sets the notification as not shown`() =
        runTest {
            underTest.setUserNotOnWifiNotificationShown(false)

            verify(syncNotificationGateway).deleteNotificationByType("NOT_CONNECTED_TO_WIFI")
        }

    @Test
    fun `test that isSyncErrorsNotificationShown returns true if error notification was shown`() =
        runTest {
            whenever(syncNotificationGateway.getNotificationByType("ERROR")).thenReturn(
                listOf(
                    mock()
                )
            )

            val result = underTest.isSyncErrorsNotificationShown(emptyList())

            assertThat(result).isTrue()
        }

    @Test
    fun `test that isSyncErrorsNotificationShown returns false if error notification was not shown`() =
        runTest {
            whenever(syncNotificationGateway.getNotificationByType("ERROR")).thenReturn(
                emptyList()
            )

            val result = underTest.isSyncErrorsNotificationShown(emptyList())

            assertThat(result).isFalse()
        }

    @Test
    fun `test that getSyncErrorsNotification returns a generic error notification message`() =
        runTest {
            val notificationMessage: SyncNotificationMessage = mock()
            whenever(genericErrorToNotificationMessageMapper()).thenReturn(notificationMessage)

            val result = underTest.getSyncErrorsNotification()

            assertThat(result).isEqualTo(notificationMessage)
        }

    @Test
    fun `test that setSyncErrorsNotificationShown sets the notification as shown`() = runTest {
        underTest.setSyncErrorsNotificationShown(listOf(mock()), shown = true)

        verify(syncNotificationGateway).setNotificationShown(
            SyncShownNotificationEntity(notificationType = "ERROR")
        )
    }

    @Test
    fun `test that setSyncErrorsNotificationShown sets the notification as not shown`() =
        runTest {
            underTest.setSyncErrorsNotificationShown(emptyList(), shown = false)

            verify(syncNotificationGateway).deleteNotificationByType("ERROR")
        }

    @Test
    fun `test that isSyncStalledIssuesNotificationShown returns true if stalled issue notification was shown`() =
        runTest {
            whenever(syncNotificationGateway.getNotificationByType("STALLED_ISSUE")).thenReturn(
                listOf(
                    mock()
                )
            )

            val result = underTest.isSyncStalledIssuesNotificationShown(emptyList())

            assertThat(result).isTrue()
        }

    @Test
    fun `test that isSyncStalledIssuesNotificationShown returns false if stalled issue notification was not shown`() =
        runTest {
            whenever(syncNotificationGateway.getNotificationByType("STALLED_ISSUE")).thenReturn(
                emptyList()
            )

            val result = underTest.isSyncStalledIssuesNotificationShown(emptyList())

            assertThat(result).isFalse()
        }

    @Test
    fun `test that getSyncStalledIssuesNotification returns a stalled issue error notification message`() =
        runTest {
            val notificationMessage: SyncNotificationMessage = mock()
            whenever(stalledIssuesToNotificationMessageMapper()).thenReturn(notificationMessage)

            val result = underTest.getSyncStalledIssuesNotification()

            assertThat(result).isEqualTo(notificationMessage)
        }

    @Test
    fun `test that setSyncStalledIssuesNotificationShown sets the notification as shown`() =
        runTest {
            underTest.setSyncStalledIssuesNotificationShown(listOf(mock()), shown = true)

            verify(syncNotificationGateway).setNotificationShown(
                SyncShownNotificationEntity(notificationType = "STALLED_ISSUE")
            )
        }

    @Test
    fun `test that setSyncStalledIssuesNotificationShown sets the notification as not shown`() =
        runTest {
            underTest.setSyncStalledIssuesNotificationShown(emptyList(), shown = false)

            verify(syncNotificationGateway).deleteNotificationByType("STALLED_ISSUE")
        }
}