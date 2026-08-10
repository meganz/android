package mega.privacy.mobile.home.presentation.home.widget.domore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.camerauploads.HasCameraSyncEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.destination.AlbumContentNavKey
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
    private val navigationEventQueue: NavigationEventQueue,
    @ApplicationScope private val applicationScope: CoroutineScope,
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

    /**
     * Opens the content of a newly created album.
     *
     * Emitted through the [NavigationEventQueue] on the [applicationScope] rather than via the
     * widget's [mega.privacy.android.navigation.contract.NavigationHandler]: the "See album"
     * snackbar action can fire after the Home composition (and this ViewModel) have been torn down
     * and rebuilt, which would leave the captured handler pointing at a detached back stack.
     */
    fun openCreatedAlbum(albumId: Long) {
        applicationScope.launch {
            navigationEventQueue.emit(AlbumContentNavKey(id = albumId, type = "custom"))
        }
    }
}
