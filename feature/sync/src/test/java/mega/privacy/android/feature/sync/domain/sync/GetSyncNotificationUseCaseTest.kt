package mega.privacy.android.feature.sync.domain.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
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
    fun `test that use case returns null when sync notification is currently displayed`() =
        runTest {
            val isSyncNotificationCurrentlyDisplayed = true
            val isBatteryLow = false
            val isUserOnWifi = true
            val isSyncOnlyByWifi = true
            val syncs = emptyList<FolderPair>()
            val stalledIssues = emptyList<StalledIssue>()

            val result = underTest(
                isSyncNotificationCurrentlyDisplayed,
                isBatteryLow,
                isUserOnWifi,
                isSyncOnlyByWifi,
                syncs,
                stalledIssues
            )

            assertThat(result).isNull()
        }

    @Test
    fun `test that use case returns sync notification when battery is low and notification was not shown`() =
        runTest {
            val isSyncNotificationCurrentlyDisplayed = false
            val isBatteryLow = true
            val isUserOnWifi = true
            val isSyncOnlyByWifi = true
            val syncs = emptyList<FolderPair>()
            val stalledIssues = emptyList<StalledIssue>()
            val notification: SyncNotificationMessage = mock()
            whenever(syncNotificationRepository.isBatteryLowNotificationShown()).thenReturn(false)
            whenever(syncNotificationRepository.getBatteryLowNotification()).thenReturn(notification)

            val result = underTest(
                isSyncNotificationCurrentlyDisplayed,
                isBatteryLow,
                isUserOnWifi,
                isSyncOnlyByWifi,
                syncs,
                stalledIssues
            )

            assertThat(result).isEqualTo(notification)
            verify(syncNotificationRepository).setBatteryLowNotificationShown(true)
        }

    @Test
    fun `test that use case returns null when battery is low and notification was shown`() =
        runTest {
            val isSyncNotificationCurrentlyDisplayed = false
            val isBatteryLow = true
            val isUserOnWifi = true
            val isSyncOnlyByWifi = true
            val syncs = emptyList<FolderPair>()
            val stalledIssues = emptyList<StalledIssue>()
            whenever(syncNotificationRepository.isBatteryLowNotificationShown()).thenReturn(true)

            val result = underTest(
                isSyncNotificationCurrentlyDisplayed,
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
            val isSyncNotificationCurrentlyDisplayed = false
            val isBatteryLow = false
            val isUserOnWifi = false
            val isSyncOnlyByWifi = true
            val syncs = emptyList<FolderPair>()
            val stalledIssues = emptyList<StalledIssue>()
            val notification: SyncNotificationMessage = mock()
            whenever(syncNotificationRepository.isUserNotOnWifiNotificationShown()).thenReturn(false)
            whenever(syncNotificationRepository.getUserNotOnWifiNotification()).thenReturn(
                notification
            )

            val result = underTest(
                isSyncNotificationCurrentlyDisplayed,
                isBatteryLow,
                isUserOnWifi,
                isSyncOnlyByWifi,
                syncs,
                stalledIssues
            )

            assertThat(result).isEqualTo(notification)
            verify(syncNotificationRepository).setUserNotOnWifiNotificationShown(true)
        }

    @Test
    fun `test that use case returns null when user is not on wifi and notification was not shown`() =
        runTest {
            val isSyncNotificationCurrentlyDisplayed = false
            val isBatteryLow = false
            val isUserOnWifi = false
            val isSyncOnlyByWifi = true
            val syncs = emptyList<FolderPair>()
            val stalledIssues = emptyList<StalledIssue>()
            whenever(syncNotificationRepository.isUserNotOnWifiNotificationShown()).thenReturn(false)

            val result = underTest(
                isSyncNotificationCurrentlyDisplayed,
                isBatteryLow,
                isUserOnWifi,
                isSyncOnlyByWifi,
                syncs,
                stalledIssues
            )

            assertThat(result).isNull()
        }

    @Test
    fun `test that use case returns sync notification when sync errors are present and notification was not shown`() =
        runTest {
            val isSyncNotificationCurrentlyDisplayed = false
            val isBatteryLow = false
            val isUserOnWifi = true
            val isSyncOnlyByWifi = true
            val syncs = listOf(mock<FolderPair> { on { syncError } doReturn mock() })
            val stalledIssues = emptyList<StalledIssue>()
            val notification: SyncNotificationMessage = mock()
            whenever(syncNotificationRepository.isSyncErrorsNotificationShown(syncs)).thenReturn(
                false
            )
            whenever(syncNotificationRepository.getSyncErrorsNotification()).thenReturn(notification)

            val result = underTest(
                isSyncNotificationCurrentlyDisplayed,
                isBatteryLow,
                isUserOnWifi,
                isSyncOnlyByWifi,
                syncs,
                stalledIssues
            )

            assertThat(result).isEqualTo(notification)
            verify(syncNotificationRepository).setSyncErrorsNotificationShown(syncs)
        }

    @Test
    fun `test that use case returns null when sync errors are present and notification was shown`() =
        runTest {
            val isSyncNotificationCurrentlyDisplayed = false
            val isBatteryLow = false
            val isUserOnWifi = true
            val isSyncOnlyByWifi = true
            val syncs = listOf(mock<FolderPair> { on { syncError } doReturn mock() })
            val stalledIssues = emptyList<StalledIssue>()
            whenever(syncNotificationRepository.isSyncErrorsNotificationShown(syncs)).thenReturn(
                true
            )

            val result = underTest(
                isSyncNotificationCurrentlyDisplayed,
                isBatteryLow,
                isUserOnWifi,
                isSyncOnlyByWifi,
                syncs,
                stalledIssues
            )

            assertThat(result).isEqualTo(null)
        }

    @Test
    fun `test that use case returns sync notification when sync stalled issues are present and notification was not shown`() =
        runTest {
            val isSyncNotificationCurrentlyDisplayed = false
            val isBatteryLow = false
            val isUserOnWifi = true
            val isSyncOnlyByWifi = true
            val syncs = emptyList<FolderPair>()
            val stalledIssues = listOf(mock<StalledIssue>())
            val notification: SyncNotificationMessage = mock()
            whenever(syncNotificationRepository.isSyncStalledIssuesNotificationShown(stalledIssues)).thenReturn(
                false
            )
            whenever(syncNotificationRepository.getSyncStalledIssuesNotification()).thenReturn(
                notification
            )

            val result = underTest(
                isSyncNotificationCurrentlyDisplayed,
                isBatteryLow,
                isUserOnWifi,
                isSyncOnlyByWifi,
                syncs,
                stalledIssues
            )

            assertThat(result).isEqualTo(notification)
            verify(syncNotificationRepository).setSyncStalledIssuesNotificationShown(stalledIssues)
        }

    @Test
    fun `test that use case returns null when sync stalled issues are present and notification was shown`() =
        runTest {
            val isSyncNotificationCurrentlyDisplayed = false
            val isBatteryLow = false
            val isUserOnWifi = true
            val isSyncOnlyByWifi = true
            val syncs = emptyList<FolderPair>()
            val stalledIssues = listOf(mock<StalledIssue>())
            whenever(syncNotificationRepository.isSyncStalledIssuesNotificationShown(stalledIssues)).thenReturn(
                true
            )

            val result = underTest(
                isSyncNotificationCurrentlyDisplayed,
                isBatteryLow,
                isUserOnWifi,
                isSyncOnlyByWifi,
                syncs,
                stalledIssues
            )

            assertThat(result).isEqualTo(null)
        }
}