package mega.privacy.android.app.consent

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.ump.FormError
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.consent.model.AdsConsentState
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.advertisements.SetGoogleConsentLoadedUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.shared.original.core.ui.utils.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AdsConsentViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val adConsentFlowWrapper: AdConsentFlowWrapper,
    private val setGoogleConsentLoadedUseCase: SetGoogleConsentLoadedUseCase,
) : ViewModel() {

    private val manualConsentFlow = MutableStateFlow(false)
    private val adConsentChannel =
        Channel<Boolean>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var adConsentJob: Job? = null

    val state: StateFlow<AdsConsentState> by lazy {
        flow {
            emit(getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag))
        }.flatMapConcat<Boolean, AdsConsentState> { isAdsEnabled ->
            if (isAdsEnabled) {
                combine(
                    manualConsentFlow,
                    adConsentChannel.receiveAsFlow()
                        .catch { Timber.e(it, "Error loading consent flow") }
                ) { manuallyConsented, canRequestConsent ->
                    if (manuallyConsented || !canRequestConsent) {
                        AdsConsentState.Data(
                            showConsentFormEvent = consumed,
                            adConsentHandledEvent = triggered,
                            adFeatureDisabled = consumed,
                        )
                    } else {
                        AdsConsentState.Data(
                            showConsentFormEvent = triggered,
                            adConsentHandledEvent = consumed,
                            adFeatureDisabled = consumed,
                        )
                    }
                }
            } else {
                flow {
                    emit(
                        AdsConsentState.Data(
                            showConsentFormEvent = consumed,
                            adConsentHandledEvent = consumed,
                            adFeatureDisabled = triggered,
                        )
                    )
                }
            }
        }.asUiStateFlow(viewModelScope, AdsConsentState.Loading)
    }

    fun onLoaded(activity: Activity) {
        adConsentJob = viewModelScope.launch {
            adConsentFlowWrapper.getCanRequestConsentFlow(activity).collect {
                adConsentChannel.send(it)
            }
        }
    }

    fun onUnLoaded() {
        adConsentJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        adConsentJob?.cancel()
    }

    fun onConsentFormEventHandled() {
        viewModelScope.launch {
            manualConsentFlow.emit(true)
        }
    }

    fun onConsentFormShown(error: FormError?) {
        if (error != null) {
            Timber.e("Error loading or showing consent form: ${error.message}")
        }
    }

    fun onAdConsentHandled() {
        viewModelScope.launch {
            setGoogleConsentLoadedUseCase(true)
        }
    }

}