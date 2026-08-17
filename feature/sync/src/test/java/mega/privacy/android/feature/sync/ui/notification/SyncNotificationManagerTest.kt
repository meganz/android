package mega.privacy.android.feature.sync.ui.notification

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.usecase.notifcation.CreateSyncNotificationIdUseCase
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.shared.resources.R as sharedResR
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncNotificationManagerTest {

    private lateinit var underTest: SyncNotificationManager

    private val context: Context = mock()
    private val notificationManagerCompat: NotificationManagerCompat = mock()
    private val createSyncNotificationIdUseCase: CreateSyncNotificationIdUseCase = mock()
    private val syncNotificationMapper: SyncNotificationMapper = mock()
    private val syncPermissionsManager: SyncPermissionsManager = mock()


    @BeforeEach
    fun setUp() {
        underTest = SyncNotificationManager(
            context,
            notificationManagerCompat,
            syncNotificationMapper,
            createSyncNotificationIdUseCase,
            syncPermissionsManager,
        )
        whenever(syncPermissionsManager.isNotificationsPermissionGranted()).thenReturn(true)
    }

    @AfterEach
    fun clear() {
        reset(
            notificationManagerCompat,
            syncNotificationMapper,
            createSyncNotificationIdUseCase,
            syncPermissionsManager,
        )
    }

    @Test
    fun `test that sync notification manager invokes manager compat with correct notification`() =
        runTest {
            val notificationId = 11234
            val notificationMessage = SyncNotificationMessage(
                title = sharedResR.string.general_sync_notification_stalled_issues_title,
                text = sharedResR.string.general_sync_notification_stalled_issues_text,
                syncNotificationType = SyncNotificationType.STALLED_ISSUE,
                notificationDetails = NotificationDetails(path = "Path", errorCode = null)
            )
            whenever(createSyncNotificationIdUseCase(notificationMessage)).thenReturn(notificationId)
            val notification: Notification = mock()
            whenever(syncNotificationMapper(context, notificationMessage)).thenReturn(
                notification
            )

            underTest.show(notificationMessage)

            verify(notificationManagerCompat).notify(notificationId, notification)
        }

    @Test
    fun `test that sync notification manager cancel notification`() = runTest {
        val notificationId = 11234

        underTest.cancelNotification(notificationId)

        verify(notificationManagerCompat).cancel(notificationId)
    }

    @Test
    fun `test that show returns null without notification permission`() = runTest {
        whenever(syncPermissionsManager.isNotificationsPermissionGranted()).thenReturn(false)

        val notificationMessage = SyncNotificationMessage(
            title = sharedResR.string.general_sync_notification_stalled_issues_title,
            text = sharedResR.string.general_sync_notification_stalled_issues_text,
            syncNotificationType = SyncNotificationType.STALLED_ISSUE,
            notificationDetails = NotificationDetails(path = "Path", errorCode = null),
        )

        val result = underTest.show(notificationMessage)

        assertThat(result).isNull()
        verifyNoInteractions(notificationManagerCompat, syncNotificationMapper)
    }

    @Test
    fun `test that show returns null when the permission is revoked before posting`() = runTest {
        val notificationId = 11234
        val notificationMessage = SyncNotificationMessage(
            title = sharedResR.string.general_sync_notification_stalled_issues_title,
            text = sharedResR.string.general_sync_notification_stalled_issues_text,
            syncNotificationType = SyncNotificationType.STALLED_ISSUE,
            notificationDetails = NotificationDetails(path = "Path", errorCode = null),
        )
        val notification: Notification = mock()
        whenever(createSyncNotificationIdUseCase(notificationMessage)).thenReturn(notificationId)
        whenever(syncNotificationMapper(context, notificationMessage)).thenReturn(notification)
        whenever(notificationManagerCompat.notify(notificationId, notification))
            .thenThrow(SecurityException("Permission revoked"))

        val result = underTest.show(notificationMessage)

        assertThat(result).isNull()
    }

    @Test
    fun `test that createForegroundNotification returns notification from mapper`() {
        val notification: Notification = mock()
        whenever(syncNotificationMapper.createForegroundNotification(context)).thenReturn(
            notification
        )

        val result = underTest.createForegroundNotification()

        assertThat(result).isEqualTo(notification)
        verify(syncNotificationMapper).createForegroundNotification(context)
    }
}
