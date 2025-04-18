package mega.privacy.android.feature.sync.domain.sync.notification

import com.google.common.truth.Truth
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.repository.SyncNotificationRepository
import mega.privacy.android.feature.sync.domain.usecase.notifcation.GetSyncIssueNotificationByTypeUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetSyncIssueNotificationByTypeUseCaseTest {

    private lateinit var underTest: GetSyncIssueNotificationByTypeUseCase
    private val syncNotificationRepository: SyncNotificationRepository = mock()

    @BeforeEach
    fun setUp() {
        underTest = GetSyncIssueNotificationByTypeUseCase(syncNotificationRepository)
    }


    @ParameterizedTest
    @EnumSource(SyncNotificationType::class)
    fun `test that it returns notification when invoked`(syncNotificationType: SyncNotificationType) {
        val expected = mock<SyncNotificationMessage>()
        whenever(syncNotificationRepository.getSyncIssueNotificationByType(syncNotificationType)).thenReturn(
            expected
        )
        val result = underTest(syncNotificationType)
        Truth.assertThat(result).isEqualTo(expected)
    }
}
