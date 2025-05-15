package mega.privacy.android.domain.usecase.notifications

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.PermissionRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShouldShowNotificationPermissionUseCaseTest {
    private lateinit var underTest: ShouldShowNotificationPermissionUseCase
    private val permissionRepository: PermissionRepository = mock()
    private val currentTimeProvider: () -> Long = mock()

    @BeforeAll
    fun setup() {
        underTest =
            ShouldShowNotificationPermissionUseCase(permissionRepository, currentTimeProvider)
    }

    @AfterEach
    fun resetMocks() {
        reset(
            permissionRepository,
            currentTimeProvider,
        )
    }

    @Test
    fun `test that invoke returns false when timestamp is null`() = runTest {
        whenever(permissionRepository.monitorNotificationPermissionShownTimestamp())
            .thenReturn(flowOf(null))

        assertThat(underTest()).isFalse()
    }

    @Test
    fun `test that invoke returns true when timestamp is more than 2 days ago`() = runTest {
        val currentTime = System.currentTimeMillis()
        val timestamp = currentTime - (3 * 24 * 60 * 60 * 1000L)
        whenever(permissionRepository.monitorNotificationPermissionShownTimestamp())
            .thenReturn(flowOf(timestamp))
        whenever(currentTimeProvider()).thenReturn(currentTime)

        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test that invoke returns false when timestamp is less than 2 days ago`() = runTest {
        val currentTime = System.currentTimeMillis()
        val timestamp = currentTime - (1 * 24 * 60 * 60 * 1000L)
        whenever(permissionRepository.monitorNotificationPermissionShownTimestamp())
            .thenReturn(flowOf(timestamp))
        whenever(currentTimeProvider()).thenReturn(currentTime)

        assertThat(underTest()).isFalse()
    }

    @Test
    fun `test that invoke returns false when timestamp is exactly 2 days ago`() = runTest {
        val currentTime = System.currentTimeMillis()
        val timestamp = currentTime - (2 * 24 * 60 * 60 * 1000L)
        whenever(permissionRepository.monitorNotificationPermissionShownTimestamp())
            .thenReturn(flowOf(timestamp))
        whenever(currentTimeProvider()).thenReturn(currentTime)

        assertThat(underTest()).isFalse()
    }
}