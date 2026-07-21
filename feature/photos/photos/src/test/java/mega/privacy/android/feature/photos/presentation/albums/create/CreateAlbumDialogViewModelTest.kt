package mega.privacy.android.feature.photos.presentation.albums.create

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.sharedcomponents.mapper.AlbumNameValidationExceptionMessageMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.domain.usecase.media.MonitorMediaAlbumsUseCase
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
    private val monitorMediaAlbumsUseCase = mock<MonitorMediaAlbumsUseCase>()
    private val albumNameValidationExceptionMessageMapper =
        mock<AlbumNameValidationExceptionMessageMapper>()

    private val defaultName = "New album"

    @BeforeEach
    fun resetMocks() {
        reset(
            context,
            getNextDefaultAlbumNameUseCase,
            validateAndCreateUserAlbumUseCase,
            monitorMediaAlbumsUseCase,
            albumNameValidationExceptionMessageMapper,
        )
        whenever(context.getString(sharedResR.string.create_new_album_input_album_name_placeholder))
            .thenReturn(defaultName)
        whenever(monitorMediaAlbumsUseCase()).thenReturn(flowOf(emptyList()))
        whenever(getNextDefaultAlbumNameUseCase(any(), any())).thenReturn(defaultName)
    }

    private fun initViewModel() {
        underTest = CreateAlbumDialogViewModel(
            context = context,
            getNextDefaultAlbumNameUseCase = getNextDefaultAlbumNameUseCase,
            validateAndCreateUserAlbumUseCase = validateAndCreateUserAlbumUseCase,
            monitorMediaAlbumsUseCase = monitorMediaAlbumsUseCase,
            albumNameValidationExceptionMessageMapper = albumNameValidationExceptionMessageMapper,
        )
    }

    @Test
    fun `test that placeholder reflects the suggestion computed from the monitored albums`() =
        runTest {
            whenever(monitorMediaAlbumsUseCase())
                .thenReturn(flowOf(listOf(userAlbum(1L, defaultName))))
            whenever(getNextDefaultAlbumNameUseCase(defaultName, listOf(defaultName)))
                .thenReturn("New album (1)")

            initViewModel()

            underTest.uiState.test {
                assertThat(expectMostRecentItem().placeholder).isEqualTo("New album (1)")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that createAlbum re-fetches the default name from the latest albums when input is blank`() =
        runTest {
            whenever(monitorMediaAlbumsUseCase())
                .thenReturn(flowOf(listOf(userAlbum(1L, defaultName))))
            whenever(getNextDefaultAlbumNameUseCase(defaultName, listOf(defaultName)))
                .thenReturn("New album (1)")
            whenever(validateAndCreateUserAlbumUseCase(any())).thenReturn(AlbumId(10L))

            initViewModel()

            underTest.uiState.test {
                underTest.createAlbum("   ")
                cancelAndIgnoreRemainingEvents()
            }

            verify(getNextDefaultAlbumNameUseCase, times(2))
                .invoke(defaultName, listOf(defaultName))
            verify(validateAndCreateUserAlbumUseCase).invoke("New album (1)")
        }

    @Test
    fun `test that createAlbum uses the trimmed input verbatim when it is not blank`() = runTest {
        whenever(validateAndCreateUserAlbumUseCase(any())).thenReturn(AlbumId(10L))

        initViewModel()

        underTest.uiState.test {
            underTest.createAlbum("  Holiday  ")
            cancelAndIgnoreRemainingEvents()
        }

        verify(validateAndCreateUserAlbumUseCase).invoke("Holiday")
    }

    @Test
    fun `test that createAlbum emits the album created event on success`() = runTest {
        whenever(validateAndCreateUserAlbumUseCase("Holiday")).thenReturn(AlbumId(42L))

        initViewModel()

        underTest.uiState.test {
            underTest.createAlbum("Holiday")
            val event = expectMostRecentItem().albumCreatedEvent
            assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((event as StateEventWithContentTriggered).content)
                .isEqualTo(CreateAlbumDialogResult(albumId = 42L, albumName = "Holiday"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that createAlbum surfaces a mapped error message when name validation fails`() =
        runTest {
            whenever(validateAndCreateUserAlbumUseCase(any()))
                .thenAnswer { throw AlbumNameValidationException.Exists }
            whenever(albumNameValidationExceptionMessageMapper(AlbumNameValidationException.Exists))
                .thenReturn("Already exists")

            initViewModel()

            underTest.uiState.test {
                underTest.createAlbum("Existing")
                val error = expectMostRecentItem().errorMessage
                assertThat(error).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((error as StateEventWithContentTriggered).content)
                    .isEqualTo("Already exists")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that a non validation failure does not surface an error message`() = runTest {
        whenever(validateAndCreateUserAlbumUseCase(any()))
            .thenAnswer { throw RuntimeException("boom") }

        initViewModel()

        underTest.uiState.test {
            underTest.createAlbum("Whatever")
            assertThat(expectMostRecentItem().errorMessage).isEqualTo(consumed())
            cancelAndIgnoreRemainingEvents()
        }

        verifyNoInteractions(albumNameValidationExceptionMessageMapper)
    }

    @Test
    fun `test that resetErrorMessage consumes the error message`() = runTest {
        whenever(validateAndCreateUserAlbumUseCase(any()))
            .thenAnswer { throw AlbumNameValidationException.Exists }
        whenever(albumNameValidationExceptionMessageMapper(AlbumNameValidationException.Exists))
            .thenReturn("Already exists")

        initViewModel()

        underTest.uiState.test {
            underTest.createAlbum("Existing")
            assertThat(expectMostRecentItem().errorMessage)
                .isInstanceOf(StateEventWithContentTriggered::class.java)

            underTest.resetErrorMessage()
            assertThat(expectMostRecentItem().errorMessage).isEqualTo(consumed())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that resetAlbumCreatedEvent consumes the album created event`() = runTest {
        whenever(validateAndCreateUserAlbumUseCase(any())).thenReturn(AlbumId(42L))

        initViewModel()

        underTest.uiState.test {
            underTest.createAlbum("Holiday")
            assertThat(expectMostRecentItem().albumCreatedEvent)
                .isInstanceOf(StateEventWithContentTriggered::class.java)

            underTest.resetAlbumCreatedEvent()
            assertThat(expectMostRecentItem().albumCreatedEvent).isEqualTo(consumed())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun userAlbum(id: Long, title: String) = MediaAlbum.User(
        id = AlbumId(id),
        title = title,
        cover = null,
        creationTime = 0L,
        modificationTime = 0L,
        isExported = false,
    )
}
