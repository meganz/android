package mega.privacy.android.domain.usecase.backup

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.BackupRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetDeviceIdUseCaseTest {

    private lateinit var underTest: GetDeviceIdUseCase

    private val backupRepository = mock<BackupRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetDeviceIdUseCase(backupRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(backupRepository)
    }

    @Test
    fun `test that get device id is invoked`() = runTest {
        val deviceId = "123-456"
        whenever(backupRepository.getDeviceId()).thenReturn(deviceId)
        assertThat(underTest()).isEqualTo(deviceId)
    }
}