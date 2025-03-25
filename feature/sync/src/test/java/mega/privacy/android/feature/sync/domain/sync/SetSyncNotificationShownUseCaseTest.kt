package mega.privacy.android.feature.sync.domain.sync

import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.repository.SyncNotificationRepository
import mega.privacy.android.feature.sync.domain.usecase.notifcation.SetSyncNotificationShownUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetSyncNotificationShownUseCaseTest {

    private val syncNotificationRepository: SyncNotificationRepository = mock()

    private val underTest = SetSyncNotificationShownUseCase(syncNotificationRepository)

    @Test
    fun `test that set notification shown invokes repository set notification shown method`() =
        runTest {
            val syncNotificationMessage: SyncNotificationMessage = mock()
            val notificationId = 1234

            underTest(
                syncNotificationMessage = syncNotificationMessage,
                notificationId = notificationId,
            )

            verify(syncNotificationRepository).setDisplayedNotification(
                notification = syncNotificationMessage,
                notificationId = notificationId,
            )
        }
}