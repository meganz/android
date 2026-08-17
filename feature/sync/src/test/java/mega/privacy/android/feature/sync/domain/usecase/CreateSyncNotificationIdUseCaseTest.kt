package mega.privacy.android.feature.sync.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.usecase.notifcation.CreateSyncNotificationIdUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CreateSyncNotificationIdUseCaseTest {
    private lateinit var underTest: CreateSyncNotificationIdUseCase

    @BeforeAll
    fun setup() {
        underTest = CreateSyncNotificationIdUseCase()
    }

    @Test
    fun `test that the use case generates a stable value for the same notification`() = runTest {
        val notification = SyncNotificationMessage(
            title = 1,
            text = 2,
            syncNotificationType = SyncNotificationType.STALLED_ISSUE,
            notificationDetails = NotificationDetails(
                path = "/path",
                errorCode = null,
                issueId = "issue-id",
            ),
        )

        val firstInvocation = underTest(notification)
        val secondInvocation = underTest(notification)

        assertThat(firstInvocation).isEqualTo(secondInvocation)
    }
}
