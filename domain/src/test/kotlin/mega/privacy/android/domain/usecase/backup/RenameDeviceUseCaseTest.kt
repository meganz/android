package mega.privacy.android.domain.usecase.backup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.BackupRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [RenameDeviceUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RenameDeviceUseCaseTest {

    private lateinit var underTest: RenameDeviceUseCase

    private val backupRepository = mock<BackupRepository>()

    @BeforeAll
    fun setUp() {
        underTest = RenameDeviceUseCase(backupRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(backupRepository)
    }

    @Test
    fun `test that rename device is invoked`() = runTest {
        val deviceId = "123-456"
        val deviceName = "New Device Name"

        underTest.invoke(
            deviceId = deviceId,
            deviceName = deviceName,
        )
        verify(backupRepository).renameDevice(
            deviceId = deviceId,
            deviceName = deviceName,
        )
    }
}