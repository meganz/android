package mega.privacy.android.feature.sync.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.SyncShownNotificationEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.data.gateway.notification.SyncNotificationGateway
import mega.privacy.android.feature.sync.data.mapper.notification.GenericErrorToNotificationMessageMapper
import mega.privacy.android.feature.sync.data.mapper.notification.StalledIssuesToNotificationMessageMapper
import mega.privacy.android.feature.sync.data.mapper.notification.SyncShownNotificationEntityToSyncNotificationMessageMapper
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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
    private val dbEntityToDomainMapper: SyncShownNotificationEntityToSyncNotificationMessageMapper =
        mock()
    private val unconfinedTestDispatcher = UnconfinedTestDispatcher(scheduler)

    private val underTest = SyncNotificationRepositoryImpl(
        syncNotificationGateway = syncNotificationGateway,
        stalledIssuesToNotificationMessageMapper = stalledIssuesToNotificationMessageMapper,
        genericErrorToNotificationMessageMapper = genericErrorToNotificationMessageMapper,
        dbEntityToDomainMapper,
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

    @ParameterizedTest
    @EnumSource(SyncNotificationType::class)
    fun `test that getDisplayedNotificationsByType returns notification with correct type`(
        notificationType: SyncNotificationType,
    ) =
        runTest {
            val notificationEntities = listOf(
                SyncShownNotificationEntity(notificationType = notificationType.name)
            )
            val notificationMessage = SyncNotificationMessage(
                title = "Notification title",
                text = "Notification text",
                syncNotificationType = notificationType,
                notificationDetails = NotificationDetails(path = "Path", errorCode = 0)
            )
            whenever(syncNotificationGateway.getNotificationByType(notificationType.name)).thenReturn(
                notificationEntities
            )
            whenever(dbEntityToDomainMapper(notificationEntities.first())).thenReturn(
                notificationMessage
            )

            val result = underTest.getDisplayedNotificationsByType(notificationType)

            assertThat(result.first()).isEqualTo(notificationMessage)
        }

    @ParameterizedTest
    @EnumSource(SyncNotificationType::class)
    fun `test that setDisplayedNotification sets notification with correct type`() =
        runTest {
            val notificationMessage = SyncNotificationMessage(
                title = "Notification title",
                text = "Notification text",
                syncNotificationType = SyncNotificationType.STALLED_ISSUE,
                notificationDetails = NotificationDetails(path = "Path", errorCode = null)
            )
            val notificationEntity =
                SyncShownNotificationEntity(notificationType = notificationMessage.syncNotificationType.name)
            whenever(dbEntityToDomainMapper(notificationMessage)).thenReturn(
                notificationEntity
            )

            underTest.setDisplayedNotification(notificationMessage)

            verify(syncNotificationGateway).setNotificationShown(notificationEntity)
        }

    @ParameterizedTest
    @EnumSource(SyncNotificationType::class)
    fun `test that deleteDisplayedNotificationByType deletes notification with correct type`(
        notificationType: SyncNotificationType,
    ) =
        runTest {
            underTest.deleteDisplayedNotificationByType(notificationType)

            verify(syncNotificationGateway).deleteNotificationByType(notificationType.name)
        }

    @Test
    fun `test that getBatteryLowNotification returns a generic error notification message`() =
        runTest {
            val notificationMessage: SyncNotificationMessage = mock()
            whenever(genericErrorToNotificationMessageMapper(SyncNotificationType.BATTERY_LOW)).thenReturn(
                notificationMessage
            )

            val result = underTest.getBatteryLowNotification()

            assertThat(result).isEqualTo(notificationMessage)
        }

    @Test
    fun `test that getUserNotOnWifiNotificationShown returns a generic error notification message`() =
        runTest {
            val notificationMessage: SyncNotificationMessage = mock()
            whenever(genericErrorToNotificationMessageMapper(SyncNotificationType.NOT_CONNECTED_TO_WIFI)).thenReturn(
                notificationMessage
            )

            val result = underTest.getUserNotOnWifiNotification()

            assertThat(result).isEqualTo(notificationMessage)
        }

    @Test
    fun `test that getSyncErrorsNotification returns a generic error notification message`() =
        runTest {
            val notificationMessage = SyncNotificationMessage(
                title = "Notification title",
                text = "Notification text",
                syncNotificationType = SyncNotificationType.ERROR,
                notificationDetails = NotificationDetails(path = "somePath", errorCode = 1)
            )
            val syncs = listOf(
                FolderPair(
                    id = 1,
                    syncType = SyncType.TYPE_TWOWAY,
                    pairName = "someName",
                    localFolderPath = "somePath",
                    remoteFolder = RemoteFolder(id = NodeId(1L), name = "someName"),
                    syncStatus = SyncStatus.SYNCED,
                    syncError = SyncError.UNKNOWN_ERROR
                )
            )
            whenever(
                genericErrorToNotificationMessageMapper(
                    SyncNotificationType.ERROR,
                    errorCode = 1,
                    issuePath = "somePath"
                )
            ).thenReturn(
                notificationMessage
            )

            val result = underTest.getSyncErrorsNotification(syncs)

            assertThat(result).isEqualTo(notificationMessage)
        }

    @Test
    fun `test that getSyncStalledIssuesNotification returns a stalled issue error notification message`() =
        runTest {
            val notificationMessage: SyncNotificationMessage = mock()
            val stalledIssues = listOf(
                StalledIssue(
                    syncId = 1,
                    nodeIds = listOf(NodeId(1)),
                    issueType = StallIssueType.FileIssue,
                    conflictName = "someConflict",
                    localPaths = listOf("somePath"),
                    nodeNames = listOf("someNode")
                )
            )
            whenever(
                stalledIssuesToNotificationMessageMapper(
                    stalledIssues.first().localPaths.first()
                )
            ).thenReturn(notificationMessage)

            val result = underTest.getSyncStalledIssuesNotification(stalledIssues)

            assertThat(result).isEqualTo(notificationMessage)
        }
}