package mega.privacy.android.feature.sync.ui.notification

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.usecase.notifcation.CreateSyncNotificationIdUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.shared.resources.R as sharedResR
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncNotificationManagerTest {

    private lateinit var underTest: SyncNotificationManager

    private val context: Context = mock()
    private val notificationManagerCompat: NotificationManagerCompat = mock()
    private val createSyncNotificationIdUseCase: CreateSyncNotificationIdUseCase = mock()
    private val syncNotificationMapper: SyncNotificationMapper = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()


    @BeforeAll
    fun setUp() {
        underTest = SyncNotificationManager(
            notificationManagerCompat,
            syncNotificationMapper,
            createSyncNotificationIdUseCase,
            getFeatureFlagValueUseCase,
        )
    }

    @AfterEach
    fun clear() {
        reset(
            notificationManagerCompat,
            syncNotificationMapper,
            createSyncNotificationIdUseCase,
            getFeatureFlagValueUseCase
        )
    }

    @Test
    fun `test that sync notification manager invokes manager compat with correct notification`() =
        runTest {
            val notificationId = 11234
            whenever(createSyncNotificationIdUseCase()).thenReturn(notificationId)
            val notificationMessage = SyncNotificationMessage(
                title = sharedResR.string.general_sync_notification_stalled_issues_title,
                text = sharedResR.string.general_sync_notification_stalled_issues_text,
                syncNotificationType = SyncNotificationType.STALLED_ISSUE,
                notificationDetails = NotificationDetails(path = "Path", errorCode = null)
            )
            val notification: Notification = mock()
            whenever(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)).thenReturn(false)
            whenever(syncNotificationMapper(context, notificationMessage, false)).thenReturn(
                notification
            )

            underTest.show(context, notificationMessage)

            verify(notificationManagerCompat).notify(notificationId, notification)
        }

    @Test
    fun `test that sync notification manager invokes manager compat with correct notification when single activity enabled`() =
        runTest {
            val notificationId = 11234
            whenever(createSyncNotificationIdUseCase()).thenReturn(notificationId)
            val notificationMessage = SyncNotificationMessage(
                title = sharedResR.string.general_sync_notification_stalled_issues_title,
                text = sharedResR.string.general_sync_notification_stalled_issues_text,
                syncNotificationType = SyncNotificationType.STALLED_ISSUE,
                notificationDetails = NotificationDetails(path = "Path", errorCode = null)
            )
            val notification: Notification = mock()
            whenever(getFeatureFlagValueUseCase(AppFeatures.SingleActivity)).thenReturn(true)
            whenever(syncNotificationMapper(context, notificationMessage, true)).thenReturn(
                notification
            )

            underTest.show(context, notificationMessage)

            verify(notificationManagerCompat).notify(notificationId, notification)
        }

    @Test
    fun `test that sync notification manager cancel notification`() = runTest {
        val notificationId = 11234

        underTest.cancelNotification(notificationId)

        verify(notificationManagerCompat).cancel(notificationId)
    }

    @Test
    fun `test that sync notification manager returns false if notification is not displayed`() {
        whenever(notificationManagerCompat.activeNotifications).thenReturn(emptyList())

        val result = underTest.isSyncNotificationDisplayed()

        assertThat(result).isFalse()
    }


    @Test
    fun `test that sync notification manager returns true if notification is displayed`() {
        val channelId = SyncNotificationManager.CHANNEL_ID
        val statusBarNotification: StatusBarNotification = mock()
        val notification: Notification = mock()
        whenever(statusBarNotification.notification).thenReturn(notification)
        whenever(notification.channelId).doReturn(channelId)
        whenever(notificationManagerCompat.activeNotifications).thenReturn(
            listOf(
                statusBarNotification
            )
        )

        val result = underTest.isSyncNotificationDisplayed()

        assertThat(result).isTrue()
    }

    @Test
    fun `test that createForegroundNotification returns notification from mapper`() {
        val notification: Notification = mock()
        whenever(syncNotificationMapper.createForegroundNotification(context)).thenReturn(
            notification
        )

        val result = underTest.createForegroundNotification(context)

        assertThat(result).isEqualTo(notification)
        verify(syncNotificationMapper).createForegroundNotification(context)
    }
}
