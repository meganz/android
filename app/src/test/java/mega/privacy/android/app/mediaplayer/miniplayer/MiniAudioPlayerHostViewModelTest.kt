package mega.privacy.android.app.mediaplayer.miniplayer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MiniAudioPlayerHostViewModelTest {

    private lateinit var underTest: MiniAudioPlayerHostViewModel

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    @BeforeEach
    fun setUp() {
        reset(getFeatureFlagValueUseCase)
    }

    private suspend fun stubFlags(revamp: Boolean, phaseTwo: Boolean) {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.AudioPlayerRevamp)).thenReturn(revamp)
        whenever(getFeatureFlagValueUseCase(ApiFeatures.AudioPlayerPhaseTwo)).thenReturn(phaseTwo)
    }

    private fun initViewModel() {
        underTest = MiniAudioPlayerHostViewModel(getFeatureFlagValueUseCase)
    }

    /**
     * The flags are read lazily, so nothing has been resolved until a host collects. Hosts rely on
     * this to render neither bar rather than the wrong one.
     */
    @Test
    fun `test that isComposeMiniPlayerEnabled is null until it is collected`() = runTest {
        stubFlags(revamp = true, phaseTwo = true)
        initViewModel()

        assertThat(underTest.isComposeMiniPlayerEnabled.value).isNull()
    }

    @Test
    fun `test that isComposeMiniPlayerEnabled is true when both flags are enabled`() = runTest {
        stubFlags(revamp = true, phaseTwo = true)
        initViewModel()

        assertThat(underTest.isComposeMiniPlayerEnabled.filterNotNull().first()).isTrue()
    }

    @Test
    fun `test that isComposeMiniPlayerEnabled is false when the revamp flag is disabled`() =
        runTest {
            stubFlags(revamp = false, phaseTwo = true)
            initViewModel()

            assertThat(underTest.isComposeMiniPlayerEnabled.filterNotNull().first()).isFalse()
        }

    @Test
    fun `test that isComposeMiniPlayerEnabled is false when the phase two flag is disabled`() =
        runTest {
            stubFlags(revamp = true, phaseTwo = false)
            initViewModel()

            assertThat(underTest.isComposeMiniPlayerEnabled.filterNotNull().first()).isFalse()
        }

    @Test
    fun `test that isComposeMiniPlayerEnabled is false when reading the flags fails`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.AudioPlayerRevamp))
            .thenThrow(RuntimeException("flag error"))
        initViewModel()

        assertThat(underTest.isComposeMiniPlayerEnabled.filterNotNull().first()).isFalse()
    }
}
