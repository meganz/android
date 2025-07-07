package mega.privacy.android.feature.sync.domain.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
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
import mega.privacy.android.feature.sync.ui.notification.SyncNotificationManager
import mega.privacy.android.shared.resources.R as sharedResR
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetSyncNotificationUseCaseTest {

    private lateinit var underTest: GetSyncNotificationUseCase

    private val syncNotificationRepository: SyncNotificationRepository = mock()
    private val syncNotificationManager: SyncNotificationManager = mock()

    @BeforeAll
    fun setUp() {
        underTest = GetSyncNotificationUseCase(
            syncNotificationRepository = syncNotificationRepository,
            syncNotificationManager = syncNotificationManager,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            syncNotificationRepository,
            syncNotificationManager,
        )
    }

    @Test
    fun `test that use case returns null when no sync set up`() = runTest {
        val isBatteryLow = false
        val isUserOnWifi = true
        val isSyncOnlyByWifi = true
        val syncs = emptyList<FolderPair>()
        val stalledIssues = emptyList<StalledIssue>()

        whenever(syncNotificationRepository.getDisplayedNotificationsIdsByType(any()))
            .thenReturn(emptyList())

        val result = underTest(
            isBatteryLow,
            isUserOnWifi,
            isSyncOnlyByWifi,
            syncs,
            stalledIssues
        )

        assertThat(result).isEqualTo(null)
    }

    @Test
    fun `test that use case returns sync notification when battery is low and notification was not shown`() =
        runTest {
            val isBatteryLow = true
            val isUserOnWifi = true
            val isSyncOnlyByWifi = true
            val syncs = listOf(mock<FolderPair> { on { syncError } doReturn mock() })
            val stalledIssues = emptyList<StalledIssue>()
            val notification: SyncNotificationMessage = mock()
            whenever(syncNotificationRepository.getDisplayedNotificationsByType(SyncNotificationType.BATTERY_LOW)).thenReturn(
                emptyList()
            )
            whenever(
                syncNotificationRepository.getDisplayedNotificationsIdsByType(any())
            ).thenReturn(emptyList())
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
            val syncs = listOf(mock<FolderPair>())
            val stalledIssues = emptyList<StalledIssue>()
            whenever(syncNotificationRepository.getDisplayedNotificationsByType(SyncNotificationType.BATTERY_LOW)).thenReturn(
                listOf(mock())
            )
            whenever(
                syncNotificationRepository.getDisplayedNotificationsIdsByType(SyncNotificationType.BATTERY_LOW)
            ).thenReturn(listOf(1234))

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
            val syncs = listOf(mock<FolderPair> { on { syncError } doReturn mock() })
            val stalledIssues = emptyList<StalledIssue>()
            val notification: SyncNotificationMessage = mock()
            whenever(syncNotificationRepository.getDisplayedNotificationsByType(SyncNotificationType.NOT_CONNECTED_TO_WIFI)).thenReturn(
                emptyList()
            )
            whenever(
                syncNotificationRepository.getDisplayedNotificationsIdsByType(any())
            ).thenReturn(emptyList())
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
            val syncs = listOf(mock<FolderPair>())
            val stalledIssues = emptyList<StalledIssue>()
            whenever(syncNotificationRepository.getDisplayedNotificationsByType(SyncNotificationType.NOT_CONNECTED_TO_WIFI)).thenReturn(
                listOf(mock())
            )
            whenever(
                syncNotificationRepository.getDisplayedNotificationsIdsByType(any())
            ).thenReturn(listOf(1234))

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
            whenever(
                syncNotificationRepository.getDisplayedNotificationsIdsByType(any())
            ).thenReturn(emptyList())
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
                    remoteFolder = RemoteFolder(NodeId(123L), "remotePath"),
                    syncStatus = SyncStatus.SYNCED,
                    syncError = SyncError.ACTIVE_SYNC_SAME_PATH
                )
            )
            val stalledIssues = emptyList<StalledIssue>()
            whenever(syncNotificationRepository.getDisplayedNotificationsByType(SyncNotificationType.ERROR)).thenReturn(
                listOf(
                    SyncNotificationMessage(
                        title = sharedResR.string.general_sync_notification_generic_error_title,
                        text = sharedResR.string.general_sync_notification_generic_error_text,
                        syncNotificationType = SyncNotificationType.ERROR,
                        notificationDetails = NotificationDetails(
                            "localPath",
                            SyncError.ACTIVE_SYNC_SAME_PATH.ordinal
                        )
                    )
                )
            )
            whenever(
                syncNotificationRepository.getDisplayedNotificationsIdsByType(any())
            ).thenReturn(emptyList())
            whenever(
                syncNotificationRepository.getDisplayedNotificationsIdsByType(SyncNotificationType.ERROR)
            ).thenReturn(listOf(1234))

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
            val syncs =
                listOf(mock<FolderPair> { on { syncError } doReturn SyncError.NO_SYNC_ERROR })
            val stalledIssues = listOf(mock<StalledIssue>())
            val notification: SyncNotificationMessage = mock()
            whenever(
                syncNotificationRepository.getDisplayedNotificationsByType(SyncNotificationType.STALLED_ISSUE)
            ).thenReturn(emptyList())
            whenever(
                syncNotificationRepository.getDisplayedNotificationsIdsByType(any())
            ).thenReturn(emptyList())
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
                        title = sharedResR.string.general_sync_notification_stalled_issues_title,
                        text = sharedResR.string.general_sync_notification_stalled_issues_text,
                        syncNotificationType = SyncNotificationType.STALLED_ISSUE,
                        notificationDetails = NotificationDetails(
                            "localPath",
                            SyncError.NO_SYNC_ERROR.ordinal
                        )
                    )
                )
            )
            whenever(
                syncNotificationRepository.getDisplayedNotificationsIdsByType(any())
            ).thenReturn(emptyList())
            whenever(
                syncNotificationRepository.getDisplayedNotificationsIdsByType(SyncNotificationType.STALLED_ISSUE)
            ).thenReturn(listOf(1234))

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

        whenever(
            syncNotificationRepository.getDisplayedNotificationsIdsByType(any())
        ).thenReturn(emptyList())

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

    @ParameterizedTest
    @EnumSource(SyncNotificationType::class)
    fun `test that use case dismisses notification when bad conditions are solved`(
        notificationType: SyncNotificationType,
    ) =
        runTest {
            val isBatteryLow = false
            val isUserOnWifi = true
            val isSyncOnlyByWifi = true
            val syncs =
                listOf(mock<FolderPair> { on { syncError } doReturn SyncError.NO_SYNC_ERROR })
            val stalledIssues = emptyList<StalledIssue>()
            val notificationId = 1234
            whenever(syncNotificationRepository.getDisplayedNotificationsByType(notificationType))
                .thenReturn(listOf(mock()))
            whenever(syncNotificationRepository.getDisplayedNotificationsIdsByType(any()))
                .thenReturn(emptyList())
            whenever(syncNotificationRepository.getDisplayedNotificationsIdsByType(notificationType))
                .thenReturn(listOf(notificationId))

            val result = underTest(
                isBatteryLow,
                isUserOnWifi,
                isSyncOnlyByWifi,
                syncs,
                stalledIssues
            )

            assertThat(result).isNull()
            if (notificationType != SyncNotificationType.CHANGE_SYNC_ROOT && notificationType != SyncNotificationType.NOT_CHARGING) {
                verify(syncNotificationManager).cancelNotification(notificationId)
                verify(syncNotificationRepository).deleteDisplayedNotificationByType(
                    notificationType
                )
            }
        }
}
