package mega.privacy.android.domain.usecase.logout

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.featureflag.ClearPersistedFeatureFlagsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ClearPersistedFeatureFlagsLogoutTaskTest {
    private lateinit var underTest: ClearPersistedFeatureFlagsLogoutTask

    private val clearPersistedFeatureFlagsUseCase = mock<ClearPersistedFeatureFlagsUseCase>()

    @BeforeEach
    internal fun setUp() {
        underTest = ClearPersistedFeatureFlagsLogoutTask(
            clearPersistedFeatureFlagsUseCase = clearPersistedFeatureFlagsUseCase,
        )
    }

    @Test
    internal fun `test that persisted feature flags are cleared on logout success`() = runTest {
        underTest.onLogoutSuccess()

        verify(clearPersistedFeatureFlagsUseCase).invoke()
    }
}
