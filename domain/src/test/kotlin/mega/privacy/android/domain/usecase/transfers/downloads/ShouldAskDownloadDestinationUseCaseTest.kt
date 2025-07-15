package mega.privacy.android.domain.usecase.transfers.downloads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.GetStorageDownloadDefaultPathUseCase
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
    private val getStorageDownloadDefaultPathUseCase = mock<GetStorageDownloadDefaultPathUseCase>()

    @BeforeAll
    fun setup() {
        underTest = ShouldAskDownloadDestinationUseCase(
            settingsRepository,
            fileSystemRepository,
            getStorageDownloadDefaultPathUseCase
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            settingsRepository,
            fileSystemRepository,
            getStorageDownloadDefaultPathUseCase,
        )

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that use case returns true when the destination is not set`(
        askAlwaysSetting: Boolean,
    ) = runTest {
        whenever(settingsRepository.getDownloadLocation()).thenReturn(null)
        whenever(settingsRepository.isAskForDownloadLocation()).thenReturn(askAlwaysSetting)
        assertThat(underTest()).isTrue()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that use case returns true when ask always is true`(
        destinationSet: Boolean,
    ) = runTest {
        whenever(settingsRepository.getDownloadLocation()).thenReturn(if (destinationSet) "destination" else null)
        whenever(settingsRepository.isAskForDownloadLocation()).thenReturn(true)
        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test that use case returns false when the destination is set and valid and ask always is false`() =
        runTest {
            val destination = "destination"
            whenever(settingsRepository.isAskForDownloadLocation()).thenReturn(false)
            whenever(settingsRepository.getDownloadLocation()).thenReturn(destination)
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
        whenever(settingsRepository.getDownloadLocation()).thenReturn(destination)
        whenever(settingsRepository.isAskForDownloadLocation()).thenReturn(false)
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
        whenever(settingsRepository.getDownloadLocation()).thenReturn(destination)
        whenever(settingsRepository.isAskForDownloadLocation()).thenReturn(false)
        whenever(fileSystemRepository.hasPersistedPermission(UriPath(destination), true))
            .thenReturn(true)
        whenever(fileSystemRepository.doesUriPathExist(UriPath(destination))).thenReturn(!expected)
        assertThat(underTest()).isEqualTo(expected)
    }

    @Test
    fun `test that destination settings are reset when file doesn't have permission anymore`() =
        runTest {
            val destination = "destination"
            whenever(settingsRepository.getDownloadLocation()).thenReturn(destination)
            whenever(settingsRepository.isAskForDownloadLocation()).thenReturn(false)
            whenever(fileSystemRepository.hasPersistedPermission(UriPath(destination), true))
                .thenReturn(false)
            whenever(fileSystemRepository.doesUriPathExist(UriPath(destination))).thenReturn(true)
            underTest()
            verify(settingsRepository).setShouldPromptToSaveDestination(true)
            verify(settingsRepository).setAskForDownloadLocation(true)
            verify(settingsRepository).setDownloadLocation(null)
        }

    @Test
    fun `test that destination settings are reset to true when file doesn't exist anymore`() =
        runTest {
            val destination = "destination"
            whenever(settingsRepository.getDownloadLocation()).thenReturn(destination)
            whenever(settingsRepository.isAskForDownloadLocation()).thenReturn(false)
            whenever(fileSystemRepository.hasPersistedPermission(UriPath(destination), true))
                .thenReturn(true)
            whenever(fileSystemRepository.doesUriPathExist(UriPath(destination))).thenReturn(false)
            underTest()
            verify(settingsRepository).setShouldPromptToSaveDestination(true)
            verify(settingsRepository).setAskForDownloadLocation(true)
            verify(settingsRepository).setDownloadLocation(null)
        }


    @Test
    fun `test that destination is considered valid when it doesn't have permission but is the default path`() =
        runTest {
            val destination = "/defaultDestination"
            whenever(getStorageDownloadDefaultPathUseCase()).thenReturn(destination)
            whenever(settingsRepository.getDownloadLocation()).thenReturn(destination)
            whenever(settingsRepository.isAskForDownloadLocation()).thenReturn(false)
            whenever(fileSystemRepository.hasPersistedPermission(UriPath(destination), true))
                .thenReturn(false)
            whenever(fileSystemRepository.doesUriPathExist(UriPath(destination))).thenReturn(true)
            assertThat(underTest()).isFalse()
        }

    @Test
    fun `test that destination is considered valid when it doesn't exist but is the default path`() =
        runTest {
            val destination = "/defaultDestination"
            whenever(getStorageDownloadDefaultPathUseCase()).thenReturn(destination)
            whenever(settingsRepository.getDownloadLocation()).thenReturn(destination)
            whenever(settingsRepository.isAskForDownloadLocation()).thenReturn(false)
            whenever(fileSystemRepository.hasPersistedPermission(UriPath(destination), true))
                .thenReturn(true)
            whenever(fileSystemRepository.doesUriPathExist(UriPath(destination))).thenReturn(false)
            assertThat(underTest()).isFalse()
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that persisted write permission is taken again when is valid`(
        expected: Boolean,
    ) = runTest {
        val destination = "destination"
        whenever(settingsRepository.getDownloadLocation()).thenReturn(destination)
        whenever(settingsRepository.isAskForDownloadLocation()).thenReturn(false)
        whenever(fileSystemRepository.hasPersistedPermission(UriPath(destination), true))
            .thenReturn(true)
        whenever(fileSystemRepository.doesUriPathExist(UriPath(destination))).thenReturn(true)

        underTest()

        verify(fileSystemRepository).takePersistablePermission(UriPath(destination), true)
    }

}