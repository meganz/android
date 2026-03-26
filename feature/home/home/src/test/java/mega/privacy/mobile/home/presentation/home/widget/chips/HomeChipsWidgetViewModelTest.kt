package mega.privacy.mobile.home.presentation.home.widget.chips

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@ExtendWith(CoroutineMainDispatcherExtension::class)
class HomeChipsWidgetViewModelTest {

    private lateinit var underTest: HomeChipsWidgetViewModel

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    @BeforeEach
    fun setUp() {
        stubFeatureFlag()
    }

    @AfterEach
    fun tearDown() {
        reset(getFeatureFlagValueUseCase)
    }

    private fun initViewModel() {
        underTest = HomeChipsWidgetViewModel(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )
    }

    private fun stubFeatureFlag(
        isMediaRevampPhase2Enabled: Boolean = false,
        isAudiosChipVisible: Boolean = false,
    ) {
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(ApiFeatures.MediaRevampPhase2) } doReturn isMediaRevampPhase2Enabled
            onBlocking { invoke(ApiFeatures.AudiosChipInHome) } doReturn isAudiosChipVisible
        }
    }

    @ParameterizedTest(name = "when the MediaRevampPhase2 is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that isMediaRevampPhase2Enabled is updated correctly`(
        isMediaRevampPhase2Enabled: Boolean
    ) = runTest {
        stubFeatureFlag(isMediaRevampPhase2Enabled = isMediaRevampPhase2Enabled)
        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem().isMediaRevampPhase2Enabled).isEqualTo(isMediaRevampPhase2Enabled)
        }
    }

    @ParameterizedTest(name = "when the isAudiosChipVisible is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that isAudiosChipVisible is updated correctly`(
        isAudiosChipVisible: Boolean,
    ) = runTest {
        stubFeatureFlag(isAudiosChipVisible = isAudiosChipVisible)
        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem().isAudiosChipVisible).isEqualTo(isAudiosChipVisible)
        }
    }
}
