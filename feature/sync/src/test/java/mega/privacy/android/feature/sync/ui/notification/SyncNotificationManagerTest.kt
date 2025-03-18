package mega.privacy.android.feature.sync.ui.notification

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.usecase.notifcation.CreateSyncNotificationIdUseCase
import mega.privacy.android.shared.resources.R as sharedResR
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
internal class SyncNotificationManagerTest {

    private lateinit var underTest: SyncNotificationManager

    private val context: Context = mock()
    private val notificationManagerCompat: NotificationManagerCompat = mock()
    private val createSyncNotificationIdUseCase: CreateSyncNotificationIdUseCase = mock()
    private val syncNotificationMapper: SyncNotificationMapper = mock()


    @Before
    fun setUp() {
        underTest = SyncNotificationManager(
            notificationManagerCompat,
            syncNotificationMapper,
            createSyncNotificationIdUseCase
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
            whenever(syncNotificationMapper(context, notificationMessage)).thenReturn(notification)

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
}