package mega.privacy.android.domain.usecase.featureflag

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FeatureFlagRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ClearPersistedFeatureFlagsUseCaseTest {

    private val featureFlagRepository = mock<FeatureFlagRepository>()

    private val underTest = ClearPersistedFeatureFlagsUseCase(
        featureFlagRepository = featureFlagRepository,
    )

    @Test
    fun `test that invoke clears the persisted snapshot through the repository`() = runTest {
        underTest()

        verify(featureFlagRepository).clearPersistedSnapshot()
    }
}
