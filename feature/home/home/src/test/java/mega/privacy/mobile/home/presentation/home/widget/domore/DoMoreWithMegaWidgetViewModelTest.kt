package mega.privacy.mobile.home.presentation.home.widget.domore

import androidx.compose.ui.graphics.vector.ImageVector
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.sharedcomponents.mapper.AlbumNameValidationExceptionMessageMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.domain.usecase.camerauploads.HasCameraSyncEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.media.MonitorMediaAlbumsUseCase
import mega.privacy.android.domain.usecase.media.ValidateAndCreateUserAlbumUseCase
import mega.privacy.android.domain.usecase.photos.GetNextDefaultAlbumNameUseCase
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.mobile.home.presentation.home.widget.domore.model.CreatedAlbum
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class DoMoreWithMegaWidgetViewModelTest {

    private lateinit var underTest: DoMoreWithMegaWidgetViewModel

    private val validateAndCreateUserAlbumUseCase = mock<ValidateAndCreateUserAlbumUseCase>()
    private val albumNameValidationExceptionMessageMapper =
        mock<AlbumNameValidationExceptionMessageMapper>()
    private val monitorMediaAlbumsUseCase = mock<MonitorMediaAlbumsUseCase>()
    private val getNextDefaultAlbumNameUseCase = mock<GetNextDefaultAlbumNameUseCase>()
    private val isCameraUploadsEnabledUseCase = mock<IsCameraUploadsEnabledUseCase>()
    private val hasCameraSyncEnabledUseCase = mock<HasCameraSyncEnabledUseCase>()

    @BeforeEach
    fun resetMocks() {
        reset(
            validateAndCreateUserAlbumUseCase,
            albumNameValidationExceptionMessageMapper,
            monitorMediaAlbumsUseCase,
            getNextDefaultAlbumNameUseCase,
        )
        whenever(monitorMediaAlbumsUseCase()).thenReturn(emptyFlow())
    }

    private fun initViewModel(
        items: Set<DoMoreWithMegaItem> = emptySet(),
        isCameraUploadsEnabled: Boolean = false,
        hasPreviouslyEnabledCameraUploads: Boolean = false,
    ) {
        whenever(isCameraUploadsEnabledUseCase.monitorCameraUploadsEnabled)
            .thenReturn(flowOf(isCameraUploadsEnabled))
        wheneverBlocking { hasCameraSyncEnabledUseCase() }
            .thenReturn(hasPreviouslyEnabledCameraUploads)
        underTest = DoMoreWithMegaWidgetViewModel(
            items = items,
            validateAndCreateUserAlbumUseCase = validateAndCreateUserAlbumUseCase,
            albumNameValidationExceptionMessageMapper = albumNameValidationExceptionMessageMapper,
            monitorMediaAlbumsUseCase = monitorMediaAlbumsUseCase,
            getNextDefaultAlbumNameUseCase = getNextDefaultAlbumNameUseCase,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            hasCameraSyncEnabledUseCase = hasCameraSyncEnabledUseCase,
        )
    }

    @Test
    fun `test that uiState emits all provided items`() = runTest {
        val items = DoMoreWithMegaItem.Identifier.entries.map { fakeItem(it) }.toSet()
        initViewModel(items)

        underTest.uiState.test {
            assertThat(awaitItem().items.map { it.identifier })
                .containsExactlyElementsIn(DoMoreWithMegaItem.Identifier.entries)
                .inOrder()
        }
    }

    @Test
    fun `test that uiState emits empty items when no items are provided`() = runTest {
        initViewModel(emptySet())

        underTest.uiState.test {
            assertThat(awaitItem().items).isEmpty()
        }
    }

    @Test
    fun `test that uiState emits isCameraUploadsEnabled true when camera uploads is enabled`() =
        runTest {
            initViewModel(emptySet(), isCameraUploadsEnabled = true)
            backgroundScope.launch { underTest.uiState.collect {} }
            advanceUntilIdle()

            assertThat(underTest.uiState.value.isCameraUploadsEnabled).isTrue()
        }

    @Test
    fun `test that uiState emits isCameraUploadsEnabled false when camera uploads is disabled`() =
        runTest {
            initViewModel(emptySet(), isCameraUploadsEnabled = false)
            backgroundScope.launch { underTest.uiState.collect {} }
            advanceUntilIdle()

            assertThat(underTest.uiState.value.isCameraUploadsEnabled).isFalse()
        }

    @Test
    fun `test that uiState emits hasPreviouslyEnabledCameraUploads true when camera uploads was previously enabled`() =
        runTest {
            initViewModel(emptySet(), hasPreviouslyEnabledCameraUploads = true)
            backgroundScope.launch { underTest.uiState.collect {} }
            advanceUntilIdle()

            assertThat(underTest.uiState.value.hasPreviouslyEnabledCameraUploads).isTrue()
        }

    @Test
    fun `test that uiState emits hasPreviouslyEnabledCameraUploads false when camera uploads was never enabled`() =
        runTest {
            initViewModel(emptySet(), hasPreviouslyEnabledCameraUploads = false)
            backgroundScope.launch { underTest.uiState.collect {} }
            advanceUntilIdle()

            assertThat(underTest.uiState.value.hasPreviouslyEnabledCameraUploads).isFalse()
        }

    @Test
    fun `test that createAlbum invokes the use case with the given name`() = runTest {
        initViewModel()
        whenever(validateAndCreateUserAlbumUseCase(any())).thenReturn(AlbumId(1L))

        underTest.createAlbum("My album")

        verify(validateAndCreateUserAlbumUseCase)("My album")
    }

    @Test
    fun `test that createAlbum emits the album created event when creation succeeds`() = runTest {
        initViewModel()
        whenever(validateAndCreateUserAlbumUseCase(any())).thenReturn(AlbumId(42L))

        underTest.createAlbum("My album")

        underTest.uiState.test {
            val event = awaitItem().albumCreatedEvent
            assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((event as StateEventWithContentTriggered).content)
                .isEqualTo(CreatedAlbum(id = AlbumId(42L), name = "My album"))
        }
    }

    @Test
    fun `test that resetAlbumCreatedEvent consumes the album created event`() = runTest {
        initViewModel()
        whenever(validateAndCreateUserAlbumUseCase(any())).thenReturn(AlbumId(42L))
        underTest.createAlbum("My album")

        underTest.resetAlbumCreatedEvent()

        underTest.uiState.test {
            assertThat(awaitItem().albumCreatedEvent).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that createAlbum surfaces an error message when name validation fails`() = runTest {
        initViewModel()
        whenever(validateAndCreateUserAlbumUseCase(any()))
            .thenAnswer { throw AlbumNameValidationException.Exists }
        whenever(albumNameValidationExceptionMessageMapper(AlbumNameValidationException.Exists))
            .thenReturn("Already exists")

        underTest.createAlbum("Existing")

        underTest.uiState.test {
            val error = awaitItem().createAlbumErrorMessage
            assertThat(error).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((error as StateEventWithContentTriggered).content).isEqualTo("Already exists")
        }
    }

    @Test
    fun `test that resetCreateAlbumErrorMessage consumes the error message`() = runTest {
        initViewModel()
        whenever(validateAndCreateUserAlbumUseCase(any()))
            .thenAnswer { throw AlbumNameValidationException.Exists }
        whenever(albumNameValidationExceptionMessageMapper(AlbumNameValidationException.Exists))
            .thenReturn("Already exists")
        underTest.createAlbum("Existing")

        underTest.resetCreateAlbumErrorMessage()

        underTest.uiState.test {
            assertThat(awaitItem().createAlbumErrorMessage).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that a non validation failure does not surface an error message`() = runTest {
        initViewModel()
        whenever(validateAndCreateUserAlbumUseCase(any()))
            .thenAnswer { throw RuntimeException("boom") }

        underTest.createAlbum("Whatever")

        verifyNoInteractions(albumNameValidationExceptionMessageMapper)
        underTest.uiState.test {
            assertThat(awaitItem().createAlbumErrorMessage).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that getPresetNewAlbumName delegates to the use case with the monitored album names`() =
        runTest {
            val albums = listOf(
                userAlbum(1L, "New album"),
                userAlbum(2L, "Holiday"),
            )
            whenever(monitorMediaAlbumsUseCase()).thenReturn(flowOf(albums))
            whenever(getNextDefaultAlbumNameUseCase("New album", listOf("New album", "Holiday")))
                .thenReturn("New album (1)")
            initViewModel()

            val result = underTest.getPresetNewAlbumName("New album")

            assertThat(result).isEqualTo("New album (1)")
            verify(getNextDefaultAlbumNameUseCase)("New album", listOf("New album", "Holiday"))
        }

    @Test
    fun `test that getPresetNewAlbumName uses an empty name list when no albums exist`() = runTest {
        whenever(getNextDefaultAlbumNameUseCase("New album", emptyList())).thenReturn("New album")
        initViewModel()

        val result = underTest.getPresetNewAlbumName("New album")

        assertThat(result).isEqualTo("New album")
        verify(getNextDefaultAlbumNameUseCase)("New album", emptyList())
    }

    private fun userAlbum(id: Long, title: String) = MediaAlbum.User(
        id = AlbumId(id),
        title = title,
        creationTime = 0L,
        modificationTime = 0L,
        isExported = false,
        cover = null,
    )

    private fun fakeItem(identifier: DoMoreWithMegaItem.Identifier) = object : DoMoreWithMegaItem {
        override val identifier: DoMoreWithMegaItem.Identifier = identifier
        override val icon: ImageVector = IconPack.Medium.Thin.Outline.Camera
        override val labelRes: Int = 0
    }
}
