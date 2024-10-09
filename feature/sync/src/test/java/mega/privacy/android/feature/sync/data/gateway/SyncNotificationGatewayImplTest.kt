package mega.privacy.android.feature.sync.data.gateway

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.dao.SyncShownNotificationDao
import mega.privacy.android.data.database.entity.SyncShownNotificationEntity
import mega.privacy.android.feature.sync.data.gateway.notification.SyncNotificationGatewayImpl
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncNotificationGatewayImplTest {

    private val syncShownNotificationDao: SyncShownNotificationDao = mock()
    private val underTest = SyncNotificationGatewayImpl(syncShownNotificationDao)

    @AfterEach
    fun tearDown() {
        reset(syncShownNotificationDao)
    }


    @Test
    fun `test that set notification shown invokes dao set notification shown method`() = runTest {
        val entity = SyncShownNotificationEntity(
            id = 321,
            notificationType = "error",
        )

        underTest.setNotificationShown(entity)

        verify(syncShownNotificationDao).insertSyncNotification(entity)
    }

    @Test
    fun `test that get notification by type fetches notification from dao`() = runTest {
        val notificationType = SyncNotificationType.STALLED_ISSUE.name
        val syncNotifications = listOf(
            SyncShownNotificationEntity(
                id = 321,
                notificationType = notificationType,
            )
        )
        whenever((syncShownNotificationDao.getSyncNotificationByType(notificationType))).thenReturn(
            syncNotifications
        )

        val result = underTest.getNotificationByType(notificationType)

        assertThat(result).isEqualTo(syncNotifications)
    }

    @Test
    fun `test delete notification by type deletes notification from dao`() = runTest {
        val notificationType = SyncNotificationType.STALLED_ISSUE.name

        underTest.deleteNotificationByType(notificationType)

        verify(syncShownNotificationDao).deleteSyncNotificationByType(notificationType)
    }
}