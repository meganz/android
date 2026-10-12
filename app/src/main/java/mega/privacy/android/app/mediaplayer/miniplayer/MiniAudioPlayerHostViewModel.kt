package mega.privacy.android.app.mediaplayer.miniplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Decides which mini player a host renders.
 */
@HiltViewModel
class MiniAudioPlayerHostViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    /**
     * Whether the Compose mini player should be used, or `null` until the flags have been read.
     *
     * Both flags have to be on. The Compose bar only connects to a revamped session, so with the
     * revamp off the hosts still need the Media3 bar to serve legacy playback; and the bar is
     * second-phase work, so it stays off until that phase ships even once the revamp is on.
     *
     * Hosts render nothing while this is `null`. Defaulting to the legacy bar instead would
     * connect it to the session and then swap it for the Compose one a frame later.
     */
    val isComposeMiniPlayerEnabled: StateFlow<Boolean?> by lazy(LazyThreadSafetyMode.NONE) {
        flow {
            emit(
                getFeatureFlagValueUseCase(ApiFeatures.AudioPlayerRevamp)
                        && getFeatureFlagValueUseCase(ApiFeatures.AudioPlayerPhaseTwo)
            )
        }
            .catch {
                Timber.e(it, "Failed to read the audio player feature flags")
                emit(false)
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)
    }
}
