package mega.privacy.android.feature.photos.presentation.albums.create

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.core.sharedcomponents.mapper.AlbumNameValidationExceptionMessageMapper
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.domain.usecase.media.MonitorMediaAlbumsUseCase
import mega.privacy.android.domain.usecase.media.ValidateAndCreateUserAlbumUseCase
import mega.privacy.android.domain.usecase.photos.GetNextDefaultAlbumNameUseCase
import mega.privacy.android.navigation.destination.CreateAlbumDialogResult
import mega.privacy.android.shared.resources.R as sharedResR
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel backing [CreateAlbumDialogM3].
 *
 * Owns the create-album flow: it keeps a live list of the user's album names so the suggested
 * name stays collision-free, and recomputes that suggestion at confirm time to narrow the window
 * for a duplicate name created concurrently on another device.
 */
@HiltViewModel
class CreateAlbumDialogViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val getNextDefaultAlbumNameUseCase: GetNextDefaultAlbumNameUseCase,
    private val validateAndCreateUserAlbumUseCase: ValidateAndCreateUserAlbumUseCase,
    monitorMediaAlbumsUseCase: MonitorMediaAlbumsUseCase,
    private val albumNameValidationExceptionMessageMapper: AlbumNameValidationExceptionMessageMapper,
) : ViewModel() {

    private val defaultName =
        context.getString(sharedResR.string.create_new_album_input_album_name_placeholder)
    private val userAlbumNames = MutableStateFlow<List<String>>(emptyList())
    private val events = MutableStateFlow(CreateAlbumEvents())

    val uiState: StateFlow<CreateAlbumDialogState> by lazy {
        combine(
            monitorMediaAlbumsUseCase()
                .catch {
                    Timber.e(it)
                    emit(emptyList())
                }
                .map { albums ->
                    albums
                        .filterIsInstance<MediaAlbum.User>()
                        .map { it.title }
                }
                .onEach { names ->
                    userAlbumNames.update { names }
                }
                .map { names ->
                    runCatching {
                        getNextDefaultAlbumNameUseCase(defaultName, names)
                    }.getOrDefault(defaultName)
                },
            events,
        ) { placeholder, currentEvents ->
            CreateAlbumDialogState(
                placeholder = placeholder,
                errorMessage = currentEvents.errorMessage,
                albumCreatedEvent = currentEvents.albumCreatedEvent,
            )
        }.asUiStateFlow(
            scope = viewModelScope,
            initialValue = CreateAlbumDialogState(placeholder = defaultName),
        )
    }

    private var createAlbumJob: Job? = null

    /**
     * Creates a new user album. A blank [input] falls back to a freshly computed default name
     * based on the latest known album list. On validation failure the mapped message is surfaced;
     * on success [CreateAlbumDialogState.albumCreatedEvent] is emitted.
     */
    fun createAlbum(input: String) {
        if (createAlbumJob?.isActive == true) return

        createAlbumJob = viewModelScope.launch {
            val finalName = input.trim().ifBlank {
                getNextDefaultAlbumNameUseCase(defaultName, userAlbumNames.value)
            }
            runCatching {
                validateAndCreateUserAlbumUseCase(finalName)
            }.onFailure { e ->
                Timber.e(e)
                if (e is AlbumNameValidationException) {
                    val message = albumNameValidationExceptionMessageMapper(e)
                    events.update { it.copy(errorMessage = triggered(message)) }
                }
            }.onSuccess { albumId ->
                events.update {
                    it.copy(
                        errorMessage = consumed(),
                        albumCreatedEvent = triggered(
                            CreateAlbumDialogResult(
                                albumId = albumId.id,
                                albumName = finalName,
                            )
                        ),
                    )
                }
            }
        }
    }

    fun resetErrorMessage() {
        events.update { it.copy(errorMessage = consumed()) }
    }

    fun resetAlbumCreatedEvent() {
        events.update { it.copy(albumCreatedEvent = consumed()) }
    }

    private data class CreateAlbumEvents(
        val errorMessage: StateEventWithContent<String> = consumed(),
        val albumCreatedEvent: StateEventWithContent<CreateAlbumDialogResult> = consumed(),
    )
}
