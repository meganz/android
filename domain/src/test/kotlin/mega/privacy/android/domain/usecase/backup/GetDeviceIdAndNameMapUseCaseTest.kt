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

/**
 * Test class for [GetDeviceIdAndNameMapUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetDeviceIdAndNameMapUseCaseTest {

    private lateinit var underTest: GetDeviceIdAndNameMapUseCase

    private val backupRepository = mock<BackupRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetDeviceIdAndNameMapUseCase(backupRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(backupRepository)
    }

    @Test
    fun `test that get device id and name map is invoked`() = runTest {
        val deviceIdAndNameMap = mapOf("123456" to "Samsung", "789012" to "iPhone 14")
        whenever(backupRepository.getDeviceIdAndNameMap()).thenReturn(deviceIdAndNameMap)
        assertThat(underTest()).isEqualTo(deviceIdAndNameMap)
    }
}