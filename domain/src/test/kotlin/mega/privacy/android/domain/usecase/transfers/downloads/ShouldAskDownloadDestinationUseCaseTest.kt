package mega.privacy.android.domain.usecase.transfers.downloads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShouldAskDownloadDestinationUseCaseTest {
    private lateinit var underTest: ShouldAskDownloadDestinationUseCase

    private val settingsRepository = mock<SettingsRepository>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setup() {

        underTest = ShouldAskDownloadDestinationUseCase(
            settingsRepository,
            fileSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            settingsRepository,
            fileSystemRepository,
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
            val destination = "destination"
            whenever(settingsRepository.isStorageAskAlways()).thenReturn(false)
            whenever(settingsRepository.getStorageDownloadLocation()).thenReturn(destination)
            whenever(fileSystemRepository.hasPersistedPermission(UriPath(destination), true))
                .thenReturn(true)
            whenever(fileSystemRepository.doesUriPathExist(UriPath(destination))).thenReturn(true)
            assertThat(underTest()).isFalse()
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that destination permission is not considered as valid when it doesn't have persisted write permission`(
        expected: Boolean,
    ) = runTest {
        val destination = "destination"
        whenever(settingsRepository.getStorageDownloadLocation()).thenReturn(destination)
        whenever(settingsRepository.isStorageAskAlways()).thenReturn(false)
        whenever(fileSystemRepository.hasPersistedPermission(UriPath(destination), true))
            .thenReturn(!expected)
        whenever(fileSystemRepository.doesUriPathExist(UriPath(destination))).thenReturn(true)
        assertThat(underTest()).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that destination is not considered as valid when it doesn't exist`(
        expected: Boolean,
    ) = runTest {
        val destination = "destination"
        whenever(settingsRepository.getStorageDownloadLocation()).thenReturn(destination)
        whenever(settingsRepository.isStorageAskAlways()).thenReturn(false)
        whenever(fileSystemRepository.hasPersistedPermission(UriPath(destination), true))
            .thenReturn(true)
        whenever(fileSystemRepository.doesUriPathExist(UriPath(destination))).thenReturn(!expected)
        assertThat(underTest()).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that persisted write permission is taken again when is valid`(
        expected: Boolean,
    ) = runTest {
        val destination = "destination"
        whenever(settingsRepository.getStorageDownloadLocation()).thenReturn(destination)
        whenever(settingsRepository.isStorageAskAlways()).thenReturn(false)
        whenever(fileSystemRepository.hasPersistedPermission(UriPath(destination), true))
            .thenReturn(true)
        whenever(fileSystemRepository.doesUriPathExist(UriPath(destination))).thenReturn(true)

        underTest()

        verify(fileSystemRepository).takePersistablePermission(UriPath(destination), true)
    }

}