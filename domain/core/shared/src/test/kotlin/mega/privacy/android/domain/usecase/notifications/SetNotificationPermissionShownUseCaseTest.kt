package mega.privacy.android.domain.usecase.notifications

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.PermissionRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

class SetNotificationPermissionShownUseCaseTest {
    @Test
    fun `test that invoke sets the notification permission shown timestamp`() =
        runTest {
            val currentTime = System.currentTimeMillis()
            val permissionRepository: PermissionRepository = mock()
            val underTest = SetNotificationPermissionShownUseCase(permissionRepository) {
                currentTime
            }

            underTest()

            verify(permissionRepository).setNotificationPermissionShownTimestamp(currentTime)
        }
}