package mega.privacy.android.feature.sync.ui.notification

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.usecase.notifcation.CreateSyncNotificationIdUseCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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
                title = "Notification title",
                text = "Notification text",
                syncNotificationType = SyncNotificationType.STALLED_ISSUE,
                path = "Path",
                errorCode = null
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
}