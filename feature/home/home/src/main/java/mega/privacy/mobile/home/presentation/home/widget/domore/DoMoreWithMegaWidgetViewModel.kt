package mega.privacy.mobile.home.presentation.home.widget.domore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.camerauploads.HasCameraSyncEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.mobile.home.presentation.home.widget.domore.model.DoMoreWithMegaUiState
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for [DoMoreWithMegaWidget].
 *
 * Collects every [DoMoreWithMegaItem] contributed via Dagger `@IntoSet`, sorts them by
 * their order, and gates the whole section behind the
 * [ApiFeatures.DoMoreWithMEGA] remote feature flag. It also tracks whether Camera uploads is
 * enabled (and whether it was ever enabled) so the Camera uploads shortcut can route to either
 * its settings or the permissions onboarding screen.
 */
@HiltViewModel
class DoMoreWithMegaWidgetViewModel @Inject constructor(
    items: Set<@JvmSuppressWildcards DoMoreWithMegaItem>,
    isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val hasCameraSyncEnabledUseCase: HasCameraSyncEnabledUseCase,
) : ViewModel() {

    private val sortedItems = items.sortedBy { it.identifier.ordinal }

    val uiState: StateFlow<DoMoreWithMegaUiState> =
        isCameraUploadsEnabledUseCase.monitorCameraUploadsEnabled
            .catch { Timber.e(it) }
            .map { isCameraUploadsEnabled ->
                DoMoreWithMegaUiState(
                    items = sortedItems,
                    isCameraUploadsEnabled = isCameraUploadsEnabled,
                    hasPreviouslyEnabledCameraUploads = runCatching { hasCameraSyncEnabledUseCase() }
                        .onFailure { Timber.e(it) }
                        .getOrDefault(false),
                )
            }
            .asUiStateFlow(
                scope = viewModelScope,
                initialValue = DoMoreWithMegaUiState(items = sortedItems),
            )
}
