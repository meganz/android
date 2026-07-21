package mega.privacy.mobile.home.presentation.home.widget.domore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
    private val items: Set<@JvmSuppressWildcards DoMoreWithMegaItem>,
    isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val hasCameraSyncEnabledUseCase: HasCameraSyncEnabledUseCase,
) : ViewModel() {
    private val sortedItems = items.sortedBy { it.identifier.ordinal }

    private val visibleItems: Flow<List<DoMoreWithMegaItem>> =
        if (sortedItems.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(
                sortedItems.map { item ->
                    item.monitorVisibility
                        .catch { Timber.e(it); emit(true) }
                        .map { visible -> item to visible }
                }
            ) { pairs ->
                pairs.filter { it.second }.map { it.first }
            }
        }

    val uiState: StateFlow<DoMoreWithMegaUiState> =
        combine(
            visibleItems,
            isCameraUploadsEnabledUseCase.monitorCameraUploadsEnabled.catch { Timber.e(it) },
        ) { visibleItems, isCameraUploadsEnabled ->
            DoMoreWithMegaUiState(
                items = visibleItems,
                isCameraUploadsEnabled = isCameraUploadsEnabled,
                hasPreviouslyEnabledCameraUploads = runCatching { hasCameraSyncEnabledUseCase() }
                    .onFailure { Timber.e(it) }
                    .getOrDefault(false),
            )
        }.asUiStateFlow(
            scope = viewModelScope,
            initialValue = DoMoreWithMegaUiState(),
        )
}
