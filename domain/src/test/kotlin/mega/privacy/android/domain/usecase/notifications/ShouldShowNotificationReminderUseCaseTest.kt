package mega.privacy.android.domain.usecase.notifications

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.PermissionRepository
import mega.privacy.android.domain.usecase.permisison.HasNotificationPermissionUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShouldShowNotificationReminderUseCaseTest {
    private lateinit var underTest: ShouldShowNotificationReminderUseCase
    private val permissionRepository: PermissionRepository = mock()
    private val hasNotificationPermissionUseCase: HasNotificationPermissionUseCase = mock()
    private val currentTimeProvider: () -> Long = mock()

    @BeforeAll
    fun setup() {
        underTest = ShouldShowNotificationReminderUseCase(
            permissionRepository,
            hasNotificationPermissionUseCase,
            currentTimeProvider
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            permissionRepository,
            hasNotificationPermissionUseCase,
            currentTimeProvider,
        )
    }

    @Test
    fun `test that invoke returns false when timestamp is null`() = runTest {
        whenever(permissionRepository.monitorNotificationPermissionShownTimestamp())
            .thenReturn(flowOf(null))
        whenever(hasNotificationPermissionUseCase()).thenReturn(false)

        assertThat(underTest()).isFalse()
    }

    @Test
    fun `test that invoke returns false when user has notification permission (irrespective of timestamp)`() =
        runTest {
            val currentTime = System.currentTimeMillis()
            val timestamp1 = currentTime - (3 * 24 * 60 * 60 * 1000L) // 3 days ago
            whenever(permissionRepository.monitorNotificationPermissionShownTimestamp())
                .thenReturn(flowOf(timestamp1))
            whenever(hasNotificationPermissionUseCase()).thenReturn(true)
            whenever(currentTimeProvider()).thenReturn(currentTime)

            assertThat(underTest()).isFalse()

            val timestamp2 = currentTime - (3 * 24 * 60 * 60 * 1000L) // 1 day ago
            whenever(permissionRepository.monitorNotificationPermissionShownTimestamp())
                .thenReturn(flowOf(timestamp2))

            assertThat(underTest()).isFalse()
        }

    @Test
    fun `test that invoke returns true when user does not have permission and timestamp is more than 2 days ago`() =
        runTest {
            val currentTime = System.currentTimeMillis()
            val timestamp = currentTime - (3 * 24 * 60 * 60 * 1000L) //3 days ago
            whenever(permissionRepository.monitorNotificationPermissionShownTimestamp())
                .thenReturn(flowOf(timestamp))
            whenever(hasNotificationPermissionUseCase()).thenReturn(false)
            whenever(currentTimeProvider()).thenReturn(currentTime)

            assertThat(underTest()).isTrue()
        }

    @Test
    fun `test that invoke returns false when user does not have permission and timestamp is less than 2 days ago`() =
        runTest {
            val currentTime = System.currentTimeMillis()
            val timestamp = currentTime - (1 * 24 * 60 * 60 * 1000L) //1 day ago
            whenever(permissionRepository.monitorNotificationPermissionShownTimestamp())
                .thenReturn(flowOf(timestamp))
            whenever(hasNotificationPermissionUseCase()).thenReturn(false)
            whenever(currentTimeProvider()).thenReturn(currentTime)

            assertThat(underTest()).isFalse()
        }

    @Test
    fun `test that invoke returns false when user does not have permission and timestamp is exactly 2 days ago`() =
        runTest {
            val currentTime = System.currentTimeMillis()
            val timestamp = currentTime - (2 * 24 * 60 * 60 * 1000L) //2 days ago
            whenever(permissionRepository.monitorNotificationPermissionShownTimestamp())
                .thenReturn(flowOf(timestamp))
            whenever(hasNotificationPermissionUseCase()).thenReturn(false)
            whenever(currentTimeProvider()).thenReturn(currentTime)

            assertThat(underTest()).isFalse()
        }
}