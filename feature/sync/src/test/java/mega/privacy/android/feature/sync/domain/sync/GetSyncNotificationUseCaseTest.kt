package mega.privacy.android.feature.sync.domain.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.repository.SyncNotificationRepository
import mega.privacy.android.feature.sync.domain.usecase.notifcation.GetSyncNotificationUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetSyncNotificationUseCaseTest {

    private lateinit var underTest: GetSyncNotificationUseCase

    private val syncNotificationRepository: SyncNotificationRepository = mock()

    @BeforeAll
    fun setUp() {
        underTest = GetSyncNotificationUseCase(syncNotificationRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncNotificationRepository)
    }

    @Test
    fun `test that use case returns sync notification when battery is low and notification was not shown`() =
        runTest {
            val isBatteryLow = true
            val isUserOnWifi = true
            val isSyncOnlyByWifi = true
            val syncs = emptyList<FolderPair>()
            val stalledIssues = emptyList<StalledIssue>()
            val notification: SyncNotificationMessage = mock()
            whenever(syncNotificationRepository.getDisplayedNotificationsByType(SyncNotificationType.BATTERY_LOW)).thenReturn(
                emptyList()
            )
            whenever(syncNotificationRepository.getBatteryLowNotification()).thenReturn(notification)

            val result = underTest(
                isBatteryLow,
                isUserOnWifi,
                isSyncOnlyByWifi,
                syncs,
                stalledIssues
            )

            assertThat(result).isEqualTo(notification)
        }

    @Test
    fun `test that use case returns null when battery is low and notification was shown`() =
        runTest {
            val isBatteryLow = true
            val isUserOnWifi = true
            val isSyncOnlyByWifi = true
            val syncs = emptyList<FolderPair>()
            val stalledIssues = emptyList<StalledIssue>()
            whenever(syncNotificationRepository.getDisplayedNotificationsByType(SyncNotificationType.BATTERY_LOW)).thenReturn(
                listOf(mock())
            )

            val result = underTest(
                isBatteryLow,
                isUserOnWifi,
                isSyncOnlyByWifi,
                syncs,
                stalledIssues
            )

            assertThat(result).isNull()
        }

    @Test
    fun `test that use case returns sync notification when user is not on wifi and notification was not shown`() =
        runTest {
            val isBatteryLow = false
            val isUserOnWifi = false
            val isSyncOnlyByWifi = true
            val syncs = emptyList<FolderPair>()
            val stalledIssues = emptyList<StalledIssue>()
            val notification: SyncNotificationMessage = mock()
            whenever(syncNotificationRepository.getDisplayedNotificationsByType(SyncNotificationType.NOT_CONNECTED_TO_WIFI)).thenReturn(
                emptyList()
            )
            whenever(syncNotificationRepository.getUserNotOnWifiNotification()).thenReturn(
                notification
            )

            val result = underTest(
                isBatteryLow,
                isUserOnWifi,
                isSyncOnlyByWifi,
                syncs,
                stalledIssues
            )

            assertThat(result).isEqualTo(notification)
            verify(syncNotificationRepository).deleteDisplayedNotificationByType(
                SyncNotificationType.BATTERY_LOW
            )
        }

    @Test
    fun `test that use case returns null when user is not on wifi and notification was not shown`() =
        runTest {
            val isBatteryLow = false
            val isUserOnWifi = false
            val isSyncOnlyByWifi = true
            val syncs = emptyList<FolderPair>()
            val stalledIssues = emptyList<StalledIssue>()
            whenever(syncNotificationRepository.getDisplayedNotificationsByType(SyncNotificationType.NOT_CONNECTED_TO_WIFI)).thenReturn(
                listOf(mock())
            )

            val result = underTest(
                isBatteryLow,
                isUserOnWifi,
                isSyncOnlyByWifi,
                syncs,
                stalledIssues
            )

            verify(syncNotificationRepository).deleteDisplayedNotificationByType(
                SyncNotificationType.BATTERY_LOW
            )
            assertThat(result).isNull()
        }

    @Test
    fun `test that use case returns sync notification when sync errors are present and notification was not shown`() =
        runTest {
            val isBatteryLow = false
            val isUserOnWifi = true
            val isSyncOnlyByWifi = true
            val syncs = listOf(mock<FolderPair> { on { syncError } doReturn mock() })
            val stalledIssues = emptyList<StalledIssue>()
            val notification: SyncNotificationMessage = mock()
            whenever(syncNotificationRepository.getDisplayedNotificationsByType(SyncNotificationType.ERROR)).thenReturn(
                emptyList()
            )
            whenever(syncNotificationRepository.getSyncErrorsNotification(syncs)).thenReturn(
                notification
            )

            val result = underTest(
                isBatteryLow,
                isUserOnWifi,
                isSyncOnlyByWifi,
                syncs,
                stalledIssues
            )

            assertThat(result).isEqualTo(notification)
            verify(syncNotificationRepository).deleteDisplayedNotificationByType(
                SyncNotificationType.BATTERY_LOW
            )
            verify(syncNotificationRepository).deleteDisplayedNotificationByType(
                SyncNotificationType.NOT_CONNECTED_TO_WIFI
            )
        }

    @Test
    fun `test that use case returns null when sync errors are present and notification was shown`() =
        runTest {
            val isBatteryLow = false
            val isUserOnWifi = true
            val isSyncOnlyByWifi = true
            val syncs = listOf(
                FolderPair(
                    id = 1,
                    syncType = SyncType.TYPE_TWOWAY,
                    pairName = "pairName",
                    localFolderPath = "localPath",
                    remoteFolder = RemoteFolder(123L, "remotePath"),
                    syncStatus = SyncStatus.SYNCED,
                    syncError = SyncError.ACTIVE_SYNC_SAME_PATH
                )
            )
            val stalledIssues = emptyList<StalledIssue>()
            whenever(syncNotificationRepository.getDisplayedNotificationsByType(SyncNotificationType.ERROR)).thenReturn(
                listOf(
                    SyncNotificationMessage(
                        title = "",
                        text = "",
                        syncNotificationType = SyncNotificationType.ERROR,
                        notificationDetails = NotificationDetails(
                            "localPath",
                            SyncError.ACTIVE_SYNC_SAME_PATH.ordinal
                        )
                    )
                )
            )

            val result = underTest(
                isBatteryLow,
                isUserOnWifi,
                isSyncOnlyByWifi,
                syncs,
                stalledIssues
            )

            verify(syncNotificationRepository).deleteDisplayedNotificationByType(
                SyncNotificationType.BATTERY_LOW
            )
            verify(syncNotificationRepository).deleteDisplayedNotificationByType(
                SyncNotificationType.NOT_CONNECTED_TO_WIFI
            )
            assertThat(result).isEqualTo(null)
        }

    @Test
    fun `test that use case returns sync notification when sync stalled issues are present and notification was not shown`() =
        runTest {
            val isBatteryLow = false
            val isUserOnWifi = true
            val isSyncOnlyByWifi = true
            val syncs = emptyList<FolderPair>()
            val stalledIssues = listOf(mock<StalledIssue>())
            val notification: SyncNotificationMessage = mock()
            whenever(
                syncNotificationRepository.getDisplayedNotificationsByType(SyncNotificationType.STALLED_ISSUE)
            ).thenReturn(
                emptyList()
            )
            whenever(syncNotificationRepository.getSyncStalledIssuesNotification(stalledIssues)).thenReturn(
                notification
            )

            val result = underTest(
                isBatteryLow,
                isUserOnWifi,
                isSyncOnlyByWifi,
                syncs,
                stalledIssues
            )

            assertThat(result).isEqualTo(notification)
            verify(syncNotificationRepository).deleteDisplayedNotificationByType(
                SyncNotificationType.BATTERY_LOW
            )
            verify(syncNotificationRepository).deleteDisplayedNotificationByType(
                SyncNotificationType.NOT_CONNECTED_TO_WIFI
            )
            verify(syncNotificationRepository).deleteDisplayedNotificationByType(
                SyncNotificationType.ERROR
            )
        }

    @Test
    fun `test that use case returns null when sync stalled issues are present and notification was shown`() =
        runTest {
            val isBatteryLow = false
            val isUserOnWifi = true
            val isSyncOnlyByWifi = true
            val syncs = emptyList<FolderPair>()
            val stalledIssues = listOf(mock<StalledIssue>())
            whenever(
                syncNotificationRepository.getDisplayedNotificationsByType(SyncNotificationType.STALLED_ISSUE)
            ).thenReturn(
                listOf(
                    SyncNotificationMessage(
                        title = "",
                        text = "",
                        syncNotificationType = SyncNotificationType.ERROR,
                        notificationDetails = NotificationDetails(
                            "localPath",
                            SyncError.NO_SYNC_ERROR.ordinal
                        )
                    )
                )
            )

            val result = underTest(
                isBatteryLow,
                isUserOnWifi,
                isSyncOnlyByWifi,
                syncs,
                stalledIssues
            )

            verify(syncNotificationRepository).deleteDisplayedNotificationByType(
                SyncNotificationType.BATTERY_LOW
            )
            verify(syncNotificationRepository).deleteDisplayedNotificationByType(
                SyncNotificationType.NOT_CONNECTED_TO_WIFI
            )
            verify(syncNotificationRepository).deleteDisplayedNotificationByType(
                SyncNotificationType.ERROR
            )
            assertThat(result).isEqualTo(null)
        }

    @Test
    fun `test that use case returns null when no sync issues are present`() = runTest {
        val isBatteryLow = false
        val isUserOnWifi = true
        val isSyncOnlyByWifi = true
        val syncs = emptyList<FolderPair>()
        val stalledIssues = emptyList<StalledIssue>()

        val result = underTest(
            isBatteryLow,
            isUserOnWifi,
            isSyncOnlyByWifi,
            syncs,
            stalledIssues
        )

        verify(syncNotificationRepository).deleteDisplayedNotificationByType(
            SyncNotificationType.BATTERY_LOW
        )
        verify(syncNotificationRepository).deleteDisplayedNotificationByType(
            SyncNotificationType.NOT_CONNECTED_TO_WIFI
        )
        verify(syncNotificationRepository).deleteDisplayedNotificationByType(
            SyncNotificationType.ERROR
        )
        verify(syncNotificationRepository).deleteDisplayedNotificationByType(
            SyncNotificationType.STALLED_ISSUE
        )
        assertThat(result).isEqualTo(null)
    }
}