package mega.privacy.mobile.home.presentation.home.widget.domore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.core.sharedcomponents.mapper.AlbumNameValidationExceptionMessageMapper
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.camerauploads.HasCameraSyncEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.media.MonitorMediaAlbumsUseCase
import mega.privacy.android.domain.usecase.media.ValidateAndCreateUserAlbumUseCase
import mega.privacy.android.domain.usecase.photos.GetNextDefaultAlbumNameUseCase
import mega.privacy.mobile.home.presentation.home.widget.domore.model.CreatedAlbum
import mega.privacy.mobile.home.presentation.home.widget.domore.model.DoMoreWithMegaUiState
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for [DoMoreWithMegaWidget].
 *
 * Collects every [DoMoreWithMegaItem] contributed via Dagger `@IntoSet`, sorts them by
 * their order, and gates the whole section behind the
 * [ApiFeatures.DoMoreWithMEGA] remote feature flag. It also owns the "Create album"
 * shortcut flow: showing the name dialog and creating the album via
 * [ValidateAndCreateUserAlbumUseCase].
 * [ApiFeatures.DoMoreWithMEGA] remote feature flag. It also tracks whether Camera uploads is
 * enabled (and whether it was ever enabled) so the Camera uploads shortcut can route to either
 * its settings or the permissions onboarding screen.
 */
@HiltViewModel
class DoMoreWithMegaWidgetViewModel @Inject constructor(
    private val items: Set<@JvmSuppressWildcards DoMoreWithMegaItem>,
    private val validateAndCreateUserAlbumUseCase: ValidateAndCreateUserAlbumUseCase,
    private val albumNameValidationExceptionMessageMapper: AlbumNameValidationExceptionMessageMapper,
    private val monitorMediaAlbumsUseCase: MonitorMediaAlbumsUseCase,
    private val getNextDefaultAlbumNameUseCase: GetNextDefaultAlbumNameUseCase,
    isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val hasCameraSyncEnabledUseCase: HasCameraSyncEnabledUseCase,
) : ViewModel() {
    private val sortedItems = items.sortedBy { it.identifier.ordinal }

    private val createAlbumState = MutableStateFlow(CreateAlbumState())

    val uiState: StateFlow<DoMoreWithMegaUiState> =
        combine(
            isCameraUploadsEnabledUseCase.monitorCameraUploadsEnabled.catch { Timber.e(it) },
            createAlbumState,
        ) { isCameraUploadsEnabled, createAlbum ->
            DoMoreWithMegaUiState(
                items = sortedItems,
                isCameraUploadsEnabled = isCameraUploadsEnabled,
                hasPreviouslyEnabledCameraUploads = runCatching { hasCameraSyncEnabledUseCase() }
                    .onFailure { Timber.e(it) }
                    .getOrDefault(false),
                createAlbumErrorMessage = createAlbum.createAlbumErrorMessage,
                albumCreatedEvent = createAlbum.albumCreatedEvent,
            )
        }.asUiStateFlow(
            scope = viewModelScope,
            initialValue = DoMoreWithMegaUiState(items = sortedItems),
        )

    private var createAlbumJob: Job? = null
    private val userAlbumNames = MutableStateFlow<List<String>>(emptyList())

    init {
        monitorUserAlbumNames()
    }

    private fun monitorUserAlbumNames() {
        monitorMediaAlbumsUseCase()
            .catch { Timber.e(it) }
            .onEach { albums ->
                userAlbumNames.update {
                    albums.filterIsInstance<MediaAlbum.User>().map { it.title }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Returns a non-colliding default album name based on [defaultName] and the existing
     * user albums, so the dialog can pre-fill a sensible suggestion (e.g. "New album (2)").
     */
    fun getPresetNewAlbumName(defaultName: String): String =
        runCatching {
            getNextDefaultAlbumNameUseCase(defaultName, userAlbumNames.value)
        }.getOrDefault("")

    /**
     * Validates [name] and creates a new user album. On a validation failure the mapped
     * message is surfaced in the dialog; on success [DoMoreWithMegaUiState.albumCreatedEvent]
     * is emitted.
     */
    fun createAlbum(name: String) {
        if (createAlbumJob?.isActive == true) return

        createAlbumJob = viewModelScope.launch {
            runCatching {
                validateAndCreateUserAlbumUseCase(name)
            }.onFailure { e ->
                Timber.e(e)
                if (e is AlbumNameValidationException) {
                    val message = albumNameValidationExceptionMessageMapper(e)
                    createAlbumState.update { it.copy(createAlbumErrorMessage = triggered(message)) }
                }
            }.onSuccess { albumId ->
                createAlbumState.update {
                    it.copy(
                        createAlbumErrorMessage = consumed(),
                        albumCreatedEvent = triggered(CreatedAlbum(id = albumId, name = name)),
                    )
                }
            }
        }
    }

    /**
     * Clears the current create-album error message.
     */
    fun resetCreateAlbumErrorMessage() {
        createAlbumState.update { it.copy(createAlbumErrorMessage = consumed()) }
    }

    /**
     * Consumes the album-created event once its snackbar has been shown.
     */
    fun resetAlbumCreatedEvent() {
        createAlbumState.update { it.copy(albumCreatedEvent = consumed()) }
    }

    private data class CreateAlbumState(
        val createAlbumErrorMessage: StateEventWithContent<String> = consumed(),
        val albumCreatedEvent: StateEventWithContent<CreatedAlbum> = consumed(),
    )
}
