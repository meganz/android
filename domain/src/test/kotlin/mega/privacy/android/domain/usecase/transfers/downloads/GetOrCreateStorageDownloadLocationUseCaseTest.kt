package mega.privacy.android.domain.usecase.transfers.downloads

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetOrCreateStorageDownloadLocationUseCaseTest {
    private lateinit var underTest: GetOrCreateStorageDownloadLocationUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetOrCreateStorageDownloadLocationUseCase(
            settingsRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository)
    }

    @Test
    fun `test that repository path is returned when it is not null`() = runTest {
        whenever(settingsRepository.getStorageDownloadLocation()).thenReturn(DEVICE_PATH)
        val actual = underTest()
        Truth.assertThat(actual).isEqualTo(DEVICE_PATH)
        verify(settingsRepository).getStorageDownloadLocation() // verify it's called only once
    }

    @Test
    fun `test that repository path is set to default when it is null`() = runTest {
        whenever(settingsRepository.getStorageDownloadLocation()).thenReturn(null)
        underTest()
        verify(settingsRepository).setDefaultStorageDownloadLocation()
    }

    @Test
    fun `test that repository path is checked again after set to default when it is null`() =
        runTest {
            whenever(settingsRepository.getStorageDownloadLocation())
                .thenReturn(null)
                .thenReturn(DEVICE_PATH)
            val actual = underTest()
            Truth.assertThat(actual).isEqualTo(DEVICE_PATH)
        }

    companion object {
        private const val DEVICE_PATH = "sdcard/download/MEGA download"
    }
}