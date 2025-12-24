package mega.privacy.android.feature.sync.ui.notification

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import mega.privacy.android.feature.sync.R as SyncR
import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.icon.pack.R
import mega.privacy.android.shared.resources.R as sharedResR
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
internal class SyncNotificationMapperTest {

    private lateinit var underTest: SyncNotificationMapper

    private val getDomainNameUseCase: GetDomainNameUseCase = mock()
    private val syncPendingIntentProvider: SyncPendingIntentProvider = mock()
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        underTest = SyncNotificationMapper(getDomainNameUseCase, syncPendingIntentProvider)
    }

    @Test
    fun `test that notification is created for stalled issue`() = runTest {
        val domainName = "mega.nz"
        whenever(getDomainNameUseCase()).thenReturn(domainName)

        val notificationMessage = SyncNotificationMessage(
            title = sharedResR.string.general_sync_notification_stalled_issues_title,
            text = sharedResR.string.general_sync_notification_stalled_issues_text,
            syncNotificationType = SyncNotificationType.STALLED_ISSUE,
            notificationDetails = NotificationDetails(path = "Test/Path", errorCode = null)
        )

        val notification = underTest(context, notificationMessage, singleActivity = true)

        assertThat(notification).isNotNull()
        assertThat(notification.channelId).isEqualTo(SyncNotificationManager.CHANNEL_ID)
        assertThat(notification.smallIcon.resId).isEqualTo(R.drawable.ic_stat_notify)
    }

    @Test
    fun `test that notification content intent is set correctly when singleActivity is false`() =
        runTest {
            val domainName = "mega.nz"
            whenever(getDomainNameUseCase()).thenReturn(domainName)

            val notificationMessage = SyncNotificationMessage(
                title = sharedResR.string.general_sync_notification_stalled_issues_title,
                text = sharedResR.string.general_sync_notification_stalled_issues_text,
                syncNotificationType = SyncNotificationType.STALLED_ISSUE,
                notificationDetails = NotificationDetails(path = "Test/Path", errorCode = null)
            )

            val notification = underTest(context, notificationMessage, singleActivity = false)

            assertThat(notification.contentIntent).isNotNull()
        }

    @Test
    fun `test that notification content intent is set correctly when singleActivity is true`() =
        runTest {
            val domainName = "mega.nz"
            whenever(getDomainNameUseCase()).thenReturn(domainName)

            val notificationMessage = SyncNotificationMessage(
                title = sharedResR.string.general_sync_notification_stalled_issues_title,
                text = sharedResR.string.general_sync_notification_stalled_issues_text,
                syncNotificationType = SyncNotificationType.STALLED_ISSUE,
                notificationDetails = NotificationDetails(path = "Test/Path", errorCode = null)
            )

            whenever(syncPendingIntentProvider(context, notificationMessage)).thenReturn(
                mock()
            )


            val notification = underTest(context, notificationMessage, singleActivity = false)
            assertThat(notification.contentIntent).isNotNull()

        }

    @Test
    fun `test that notification is auto cancel`() = runTest {
        val domainName = "mega.nz"
        whenever(getDomainNameUseCase()).thenReturn(domainName)

        val notificationMessage = SyncNotificationMessage(
            title = sharedResR.string.general_sync_notification_stalled_issues_title,
            text = sharedResR.string.general_sync_notification_stalled_issues_text,
            syncNotificationType = SyncNotificationType.STALLED_ISSUE,
            notificationDetails = NotificationDetails(path = "Test/Path", errorCode = null)
        )

        val notification = underTest(context, notificationMessage, singleActivity = true)

        assertThat(notification.flags and Notification.FLAG_AUTO_CANCEL).isNotEqualTo(0)
    }

    @Test
    fun `test that foreground notification is created correctly`() {
        val notification = underTest.createForegroundNotification(context)

        assertThat(notification).isNotNull()
        assertThat(notification.channelId).isEqualTo(SyncNotificationManager.SYNC_PROGRESS_CHANNEL_ID)
        assertThat(notification.smallIcon.resId).isEqualTo(R.drawable.ic_stat_notify)
    }

    @Test
    fun `test that foreground notification is ongoing`() {
        val notification = underTest.createForegroundNotification(context)

        assertThat(notification.flags and Notification.FLAG_ONGOING_EVENT).isNotEqualTo(0)
    }

    @Test
    fun `test that foreground notification has correct content`() {
        val notification = underTest.createForegroundNotification(context)

        val expectedTitle = context.getString(SyncR.string.sync)
        val expectedText = context.getString(SyncR.string.sync_list_sync_state_syncing)

        // Note: Notification text is stored in extras
        assertThat(notification.extras.getString(Notification.EXTRA_TITLE)).isEqualTo(expectedTitle)
        assertThat(notification.extras.getString(Notification.EXTRA_TEXT)).isEqualTo(expectedText)
    }
}
