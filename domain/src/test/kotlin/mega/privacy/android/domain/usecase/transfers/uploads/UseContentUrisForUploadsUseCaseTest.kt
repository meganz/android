package mega.privacy.android.domain.usecase.transfers.uploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.featuretoggle.DomainFeatures
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UseContentUrisForUploadsUseCaseTest {

    private lateinit var underTest: UseContentUrisForUploadsUseCase

    private val transferRepository = mock<TransferRepository>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    @BeforeAll
    fun setup() {
        underTest = UseContentUrisForUploadsUseCase(
            transferRepository,
            getFeatureFlagValueUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            transferRepository,
            getFeatureFlagValueUseCase,
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that false is returned when feature flag is false`(
        isForChat: Boolean,
    ) = runTest {
        whenever(getFeatureFlagValueUseCase(DomainFeatures.UseFileDescriptorForUploads)) doReturn false

        assertThat(underTest(isForChat)).isEqualTo(false)
    }

    @Test
    fun `test that false is returned when is for chat is true`() = runTest {
        whenever(getFeatureFlagValueUseCase(DomainFeatures.UseFileDescriptorForUploads)) doReturn true

        assertThat(underTest(true)).isEqualTo(false)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that value from repository is returned when feature flag is true and is not for chat`(
        allowTransfersWithContentUris: Boolean,
    ) = runTest {
        whenever(getFeatureFlagValueUseCase(DomainFeatures.UseFileDescriptorForUploads)) doReturn true
        whenever(transferRepository.allowTransfersWithContentUris()) doReturn allowTransfersWithContentUris

        assertThat(underTest(false)).isEqualTo(allowTransfersWithContentUris)
    }
}