package mega.privacy.android.app.presentation.permissions

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class PermissionsViewModelTest {
    private lateinit var underTest: PermissionsViewModel

    private val defaultAccountRepository: AccountRepository = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun resetMocks() {
        reset(defaultAccountRepository, getFeatureFlagValueUseCase)
        Dispatchers.resetMain()
    }

    private fun init() {
        underTest = PermissionsViewModel(
            defaultAccountRepository = defaultAccountRepository,
            ioDispatcher = testDispatcher,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that onboarding flags are set correctly`(isEnabled: Boolean) = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.OnboardingRevamp))
            .thenReturn(isEnabled)

        init()

        underTest.uiState.test {
            assertThat(awaitItem().isOnboardingRevampEnabled).isEqualTo(isEnabled)
        }
    }
}