package mega.privacy.android.domain.usecase.transfers.downloads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShouldAskDownloadDestinationUseCaseTest {
    private lateinit var underTest: ShouldAskDownloadDestinationUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setup() {

        underTest = ShouldAskDownloadDestinationUseCase(
            settingsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            settingsRepository,
        )

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that use case returns true when the destination is not set`(
        askAlwaysSetting: Boolean,
    ) = runTest {
        whenever(settingsRepository.getStorageDownloadLocation()).thenReturn(null)
        whenever(settingsRepository.isStorageAskAlways()).thenReturn(askAlwaysSetting)
        assertThat(underTest()).isTrue()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that use case returns true when ask always is true`(
        destinationSet: Boolean,
    ) = runTest {
        whenever(settingsRepository.getStorageDownloadLocation()).thenReturn(if (destinationSet) "destination" else null)
        whenever(settingsRepository.isStorageAskAlways()).thenReturn(true)
        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test that use case returns false when the destination is set and ask always is false`() =
        runTest {
            whenever(settingsRepository.isStorageAskAlways()).thenReturn(false)
            whenever(settingsRepository.getStorageDownloadLocation()).thenReturn("destination")
            assertThat(underTest()).isFalse()
        }
}