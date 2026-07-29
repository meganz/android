package mega.privacy.android.feature.photos.presentation.albums.create

import android.content.Context
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.sharedcomponents.mapper.AlbumNameValidationExceptionMessageMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.domain.usecase.media.MonitorUserAlbumNamesUseCase
import mega.privacy.android.domain.usecase.media.ValidateAndCreateUserAlbumUseCase
import mega.privacy.android.domain.usecase.photos.GetNextDefaultAlbumNameUseCase
import mega.privacy.android.navigation.destination.CreateAlbumDialogResult
import mega.privacy.android.shared.resources.R as sharedResR
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class CreateAlbumDialogViewModelTest {

    private lateinit var underTest: CreateAlbumDialogViewModel

    private val context = mock<Context>()
    private val getNextDefaultAlbumNameUseCase = mock<GetNextDefaultAlbumNameUseCase>()
    private val validateAndCreateUserAlbumUseCase = mock<ValidateAndCreateUserAlbumUseCase>()
    private val monitorUserAlbumNamesUseCase = mock<MonitorUserAlbumNamesUseCase>()
    private val albumNameValidationExceptionMessageMapper =
        mock<AlbumNameValidationExceptionMessageMapper>()

    private val defaultName = "New album"

    @BeforeEach
    fun resetMocks() {
        reset(
            context,
            getNextDefaultAlbumNameUseCase,
            validateAndCreateUserAlbumUseCase,
            monitorUserAlbumNamesUseCase,
            albumNameValidationExceptionMessageMapper,
        )
        whenever(context.getString(sharedResR.string.create_new_album_input_album_name_placeholder))
            .thenReturn(defaultName)
        whenever(monitorUserAlbumNamesUseCase()).thenReturn(flowOf(emptyList()))
        whenever(getNextDefaultAlbumNameUseCase(any(), any())).thenReturn(defaultName)
    }

    private fun initViewModel() {
        underTest = CreateAlbumDialogViewModel(
            context = context,
            getNextDefaultAlbumNameUseCase = getNextDefaultAlbumNameUseCase,
            validateAndCreateUserAlbumUseCase = validateAndCreateUserAlbumUseCase,
            monitorUserAlbumNamesUseCase = monitorUserAlbumNamesUseCase,
            albumNameValidationExceptionMessageMapper = albumNameValidationExceptionMessageMapper,
        )
    }

    // state is lazily started (asUiStateFlow / WhileSubscribed), so keep an active collector
    // for the duration of the test to run the upstream that computes the placeholder. The
    // collector runs on an unconfined dispatcher so it starts eagerly without advancing time.
    private fun TestScope.initAndCollect() {
        initViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            underTest.uiState.collect {}
        }
    }

    @Test
    fun `test that placeholder reflects the suggestion computed from the monitored albums`() =
        runTest {
            whenever(monitorUserAlbumNamesUseCase())
                .thenReturn(flowOf(listOf(defaultName)))
            whenever(getNextDefaultAlbumNameUseCase(defaultName, listOf(defaultName)))
                .thenReturn("New album (1)")

            initAndCollect()

            assertThat(underTest.uiState.value.placeholder).isEqualTo("New album (1)")
        }

    @Test
    fun `test that createAlbum re-fetches the default name from the latest albums when input is blank`() =
        runTest {
            whenever(monitorUserAlbumNamesUseCase())
                .thenReturn(flowOf(listOf(defaultName)))
            whenever(getNextDefaultAlbumNameUseCase(defaultName, listOf(defaultName)))
                .thenReturn("New album (1)")
            whenever(validateAndCreateUserAlbumUseCase(any())).thenReturn(AlbumId(10L))

            initAndCollect()

            underTest.createAlbum("   ")

            verify(getNextDefaultAlbumNameUseCase, times(2))
                .invoke(defaultName, listOf(defaultName))
            verify(validateAndCreateUserAlbumUseCase).invoke("New album (1)")
        }

    @Test
    fun `test that createAlbum uses the trimmed input verbatim when it is not blank`() = runTest {
        whenever(validateAndCreateUserAlbumUseCase(any())).thenReturn(AlbumId(10L))

        initAndCollect()

        underTest.createAlbum("  Holiday  ")

        verify(validateAndCreateUserAlbumUseCase).invoke("Holiday")
    }

    @Test
    fun `test that createAlbum emits the album created event on success`() = runTest {
        whenever(validateAndCreateUserAlbumUseCase("Holiday")).thenReturn(AlbumId(42L))

        initAndCollect()

        underTest.createAlbum("Holiday")

        val event = underTest.uiState.value.albumCreatedEvent
        assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
        assertThat((event as StateEventWithContentTriggered).content)
            .isEqualTo(CreateAlbumDialogResult(albumId = 42L, albumName = "Holiday"))
    }

    @Test
    fun `test that createAlbum surfaces a mapped error message when name validation fails`() =
        runTest {
            whenever(validateAndCreateUserAlbumUseCase(any()))
                .thenAnswer { throw AlbumNameValidationException.Exists }
            whenever(albumNameValidationExceptionMessageMapper(AlbumNameValidationException.Exists))
                .thenReturn("Already exists")

            initAndCollect()

            underTest.createAlbum("Existing")

            val error = underTest.uiState.value.errorMessage
            assertThat(error).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((error as StateEventWithContentTriggered).content)
                .isEqualTo("Already exists")
        }

    @Test
    fun `test that a non validation failure does not surface an error message`() = runTest {
        whenever(validateAndCreateUserAlbumUseCase(any()))
            .thenAnswer { throw RuntimeException("boom") }

        initAndCollect()

        underTest.createAlbum("Whatever")

        verifyNoInteractions(albumNameValidationExceptionMessageMapper)
        assertThat(underTest.uiState.value.errorMessage).isEqualTo(consumed())
    }

    @Test
    fun `test that resetErrorMessage consumes the error message`() = runTest {
        whenever(validateAndCreateUserAlbumUseCase(any()))
            .thenAnswer { throw AlbumNameValidationException.Exists }
        whenever(albumNameValidationExceptionMessageMapper(AlbumNameValidationException.Exists))
            .thenReturn("Already exists")

        initAndCollect()
        underTest.createAlbum("Existing")

        underTest.resetErrorMessage()

        assertThat(underTest.uiState.value.errorMessage).isEqualTo(consumed())
    }

    @Test
    fun `test that resetAlbumCreatedEvent consumes the album created event`() = runTest {
        whenever(validateAndCreateUserAlbumUseCase(any())).thenReturn(AlbumId(42L))

        initAndCollect()
        underTest.createAlbum("Holiday")

        underTest.resetAlbumCreatedEvent()

        assertThat(underTest.uiState.value.albumCreatedEvent).isEqualTo(consumed())
    }
}
