package mega.privacy.android.domain.usecase.transfers.downloads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.featuretoggle.DomainFeatures
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
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
    private val transferRepository = mock<TransferRepository>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setup() {

        underTest = ShouldAskDownloadDestinationUseCase(
            settingsRepository,
            transferRepository,
            fileSystemRepository,
            getFeatureFlagValueUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(
            settingsRepository,
            transferRepository,
            fileSystemRepository,
            getFeatureFlagValueUseCase,
        )

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that use case returns true when user is allowed to set the destination and is not set`(
        askAlwaysSetting: Boolean,
    ) =
        runTest {
            whenever(getFeatureFlagValueUseCase(DomainFeatures.AllowToChooseDownloadDestination))
                .thenReturn(false)
            whenever(settingsRepository.getStorageDownloadLocation()).thenReturn(null)
            whenever(transferRepository.allowUserToSetDownloadDestination()).thenReturn(true)
            whenever(settingsRepository.isStorageAskAlways()).thenReturn(askAlwaysSetting)
            assertThat(underTest()).isTrue()
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that use case returns true when user is not allowed to set the destination but feature flag is true`(
        askAlwaysSetting: Boolean,
    ) =
        runTest {
            whenever(getFeatureFlagValueUseCase(DomainFeatures.AllowToChooseDownloadDestination))
                .thenReturn(true)
            whenever(settingsRepository.getStorageDownloadLocation()).thenReturn(null)

            whenever(settingsRepository.isStorageAskAlways()).thenReturn(askAlwaysSetting)
            assertThat(underTest()).isTrue()
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that use case returns true when user is allowed to set the destination and ask always is true`(
        destinationSet: Boolean,
    ) =
        runTest {
            whenever(getFeatureFlagValueUseCase(DomainFeatures.AllowToChooseDownloadDestination))
                .thenReturn(false)
            whenever(settingsRepository.getStorageDownloadLocation()).thenReturn(if (destinationSet) "destination" else null)
            whenever(transferRepository.allowUserToSetDownloadDestination()).thenReturn(true)
            whenever(settingsRepository.isStorageAskAlways()).thenReturn(true)
            assertThat(underTest()).isTrue()
        }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that use case returns false when user is not allowed to set the destination`(
        destinationSet: Boolean,
    ) =
        runTest {
            whenever(getFeatureFlagValueUseCase(DomainFeatures.AllowToChooseDownloadDestination))
                .thenReturn(false)
            whenever(settingsRepository.getStorageDownloadLocation()).thenReturn(if (destinationSet) "destination" else null)
            whenever(transferRepository.allowUserToSetDownloadDestination()).thenReturn(false)
            assertThat(underTest()).isFalse()
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that use case returns false when the destination is set and ask always is false`(
        userAllowed: Boolean,
    ) =
        runTest {
            whenever(getFeatureFlagValueUseCase(DomainFeatures.AllowToChooseDownloadDestination))
                .thenReturn(false)
            val destination = "destination"
            whenever(settingsRepository.getStorageDownloadLocation()).thenReturn(destination)
            whenever(transferRepository.allowUserToSetDownloadDestination()).thenReturn(userAllowed)
            whenever(settingsRepository.isStorageAskAlways()).thenReturn(false)
            whenever(fileSystemRepository.hasPersistedPermission(UriPath(destination), true))
                .thenReturn(true)
            assertThat(underTest()).isFalse()
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that destination permission is not considered as valid when it doesn't have persisted write permission`(
        expected: Boolean,
    ) = runTest {
        whenever(getFeatureFlagValueUseCase(DomainFeatures.AllowToChooseDownloadDestination))
            .thenReturn(false)
        val destination = "destination"
        whenever(settingsRepository.getStorageDownloadLocation()).thenReturn(destination)
        whenever(transferRepository.allowUserToSetDownloadDestination()).thenReturn(true)
        whenever(settingsRepository.isStorageAskAlways()).thenReturn(false)
        whenever(fileSystemRepository.hasPersistedPermission(UriPath(destination), true))
            .thenReturn(!expected)
        assertThat(underTest()).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that persisted write permission is taken again when is valid`(
        expected: Boolean,
    ) = runTest {
        whenever(getFeatureFlagValueUseCase(DomainFeatures.AllowToChooseDownloadDestination))
            .thenReturn(false)
        val destination = "destination"
        whenever(settingsRepository.getStorageDownloadLocation()).thenReturn(destination)
        whenever(transferRepository.allowUserToSetDownloadDestination()).thenReturn(true)
        whenever(settingsRepository.isStorageAskAlways()).thenReturn(false)
        whenever(fileSystemRepository.hasPersistedPermission(UriPath(destination), true))
            .thenReturn(true)

        underTest()

        verify(fileSystemRepository).takePersistablePermission(UriPath(destination), true)
    }

}