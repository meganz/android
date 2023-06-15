package test.mega.privacy.android.app.presentation.photos.albums.importlink

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.constants.StringsConstants.INVALID_CHARACTERS
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity.Companion.ALBUM_LINK
import mega.privacy.android.app.presentation.photos.albums.importlink.AlbumImportViewModel
import mega.privacy.android.app.presentation.photos.util.LegacyPublicAlbumPhotoProvider
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetUserAlbums
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.photos.GetProscribedAlbumNamesUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AlbumImportViewModelTest {
    private val mockHasCredentialsUseCase: HasCredentials = mock()

    private val mockGetUserAlbums: GetUserAlbums = mock()

    private val mockGetPublicAlbumUseCase: GetPublicAlbumUseCase = mock()

    private val mockLegacyPublicAlbumPhotoProvider: LegacyPublicAlbumPhotoProvider = mock()

    private val mockGetProscribedAlbumNamesUseCase: GetProscribedAlbumNamesUseCase = mock()

    private val mockGetStringFromStringResMapper: GetStringFromStringResMapper = mock()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher = StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that show error access dialog if link is null`() = runTest {
        // given
        whenever(mockHasCredentialsUseCase())
            .thenReturn(false)

        val underTest = AlbumImportViewModel(
            savedStateHandle = SavedStateHandle(),
            hasCredentialsUseCase = mockHasCredentialsUseCase,
            getUserAlbums = mockGetUserAlbums,
            getPublicAlbumUseCase = mockGetPublicAlbumUseCase,
            legacyPublicAlbumPhotoProvider = mockLegacyPublicAlbumPhotoProvider,
            getProscribedAlbumNamesUseCase = mockGetProscribedAlbumNamesUseCase,
            getStringFromStringResMapper = mockGetStringFromStringResMapper,
            defaultDispatcher = StandardTestDispatcher(),
        )

        // when
        underTest.initialize()

        // then
        underTest.stateFlow.test {
            repeat(2) { awaitItem() }

            val state = awaitItem()
            assertThat(state.showErrorAccessDialog).isTrue()
        }
    }

    @Test
    fun `test that show decryption key dialog if link not contains key`() = runTest {
        // given
        val link = "https://mega.nz/collection/handle"

        whenever(mockHasCredentialsUseCase())
            .thenReturn(false)

        val underTest = AlbumImportViewModel(
            savedStateHandle = SavedStateHandle(mapOf(ALBUM_LINK to link)),
            hasCredentialsUseCase = mockHasCredentialsUseCase,
            getUserAlbums = mockGetUserAlbums,
            getPublicAlbumUseCase = mockGetPublicAlbumUseCase,
            legacyPublicAlbumPhotoProvider = mockLegacyPublicAlbumPhotoProvider,
            getProscribedAlbumNamesUseCase = mockGetProscribedAlbumNamesUseCase,
            getStringFromStringResMapper = mockGetStringFromStringResMapper,
            defaultDispatcher = StandardTestDispatcher(),
        )

        // when
        underTest.initialize()

        // then
        underTest.stateFlow.test {
            repeat(2) { awaitItem() }

            val state = awaitItem()
            assertThat(state.showInputDecryptionKeyDialog).isTrue()
        }
    }

    @Test
    fun `test that get public album works properly`() = runTest {
        // given
        val link = "https://mega.nz/collection/handle#key"
        val album = mock<Album.UserAlbum>()

        whenever(mockHasCredentialsUseCase())
            .thenReturn(false)

        whenever(mockGetPublicAlbumUseCase(albumLink = AlbumLink(link)))
            .thenReturn(album to listOf())

        whenever(mockLegacyPublicAlbumPhotoProvider.getPublicPhotos(albumPhotoIds = listOf()))
            .thenReturn(listOf())

        val underTest = AlbumImportViewModel(
            savedStateHandle = SavedStateHandle(mapOf(ALBUM_LINK to link)),
            hasCredentialsUseCase = mockHasCredentialsUseCase,
            getUserAlbums = mockGetUserAlbums,
            getPublicAlbumUseCase = mockGetPublicAlbumUseCase,
            legacyPublicAlbumPhotoProvider = mockLegacyPublicAlbumPhotoProvider,
            getProscribedAlbumNamesUseCase = mockGetProscribedAlbumNamesUseCase,
            getStringFromStringResMapper = mockGetStringFromStringResMapper,
            defaultDispatcher = StandardTestDispatcher(),
        )

        // when
        underTest.initialize()

        // then
        underTest.stateFlow.test {
            repeat(2) { awaitItem() }

            val state = awaitItem()
            assertThat(state.album).isEqualTo(album)
        }
    }

    @Test
    fun `test that close decryption key dialog works properly`() = runTest {
        // given
        val underTest = AlbumImportViewModel(
            savedStateHandle = SavedStateHandle(),
            hasCredentialsUseCase = mockHasCredentialsUseCase,
            getUserAlbums = mockGetUserAlbums,
            getPublicAlbumUseCase = mockGetPublicAlbumUseCase,
            legacyPublicAlbumPhotoProvider = mockLegacyPublicAlbumPhotoProvider,
            getProscribedAlbumNamesUseCase = mockGetProscribedAlbumNamesUseCase,
            getStringFromStringResMapper = mockGetStringFromStringResMapper,
            defaultDispatcher = StandardTestDispatcher(),
        )

        // when
        underTest.closeInputDecryptionKeyDialog()

        // then
        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.showInputDecryptionKeyDialog).isFalse()
        }
    }

    @Test
    fun `test that select photo works properly`() = runTest {
        // given
        val photo = mock<Photo.Image>()

        val underTest = AlbumImportViewModel(
            savedStateHandle = SavedStateHandle(),
            hasCredentialsUseCase = mockHasCredentialsUseCase,
            getUserAlbums = mockGetUserAlbums,
            getPublicAlbumUseCase = mockGetPublicAlbumUseCase,
            legacyPublicAlbumPhotoProvider = mockLegacyPublicAlbumPhotoProvider,
            getProscribedAlbumNamesUseCase = mockGetProscribedAlbumNamesUseCase,
            getStringFromStringResMapper = mockGetStringFromStringResMapper,
            defaultDispatcher = StandardTestDispatcher(),
        )

        // when
        underTest.selectPhoto(photo)

        // then
        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.selectedPhotos.size).isEqualTo(1)
        }
    }

    @Test
    fun `test that unselect photo works properly`() = runTest {
        // given
        val photo = mock<Photo.Image>()

        val underTest = AlbumImportViewModel(
            savedStateHandle = SavedStateHandle(),
            hasCredentialsUseCase = mockHasCredentialsUseCase,
            getUserAlbums = mockGetUserAlbums,
            getPublicAlbumUseCase = mockGetPublicAlbumUseCase,
            legacyPublicAlbumPhotoProvider = mockLegacyPublicAlbumPhotoProvider,
            getProscribedAlbumNamesUseCase = mockGetProscribedAlbumNamesUseCase,
            getStringFromStringResMapper = mockGetStringFromStringResMapper,
            defaultDispatcher = StandardTestDispatcher(),
        )

        // when
        underTest.unselectPhoto(photo)

        // then
        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.selectedPhotos).doesNotContain(photo)
        }
    }

    @Test
    fun `test that clear selection works properly`() = runTest {
        // given
        val underTest = AlbumImportViewModel(
            savedStateHandle = SavedStateHandle(),
            hasCredentialsUseCase = mockHasCredentialsUseCase,
            getUserAlbums = mockGetUserAlbums,
            getPublicAlbumUseCase = mockGetPublicAlbumUseCase,
            legacyPublicAlbumPhotoProvider = mockLegacyPublicAlbumPhotoProvider,
            getProscribedAlbumNamesUseCase = mockGetProscribedAlbumNamesUseCase,
            getStringFromStringResMapper = mockGetStringFromStringResMapper,
            defaultDispatcher = StandardTestDispatcher(),
        )

        // when
        underTest.clearSelection()

        // then
        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.selectedPhotos).isEmpty()
        }
    }

    @Test
    fun `test that album name empty should show error`() = runTest {
        // given
        val underTest = AlbumImportViewModel(
            savedStateHandle = SavedStateHandle(),
            hasCredentialsUseCase = mockHasCredentialsUseCase,
            getUserAlbums = mockGetUserAlbums,
            getPublicAlbumUseCase = mockGetPublicAlbumUseCase,
            legacyPublicAlbumPhotoProvider = mockLegacyPublicAlbumPhotoProvider,
            getProscribedAlbumNamesUseCase = mockGetProscribedAlbumNamesUseCase,
            getStringFromStringResMapper = mockGetStringFromStringResMapper,
            defaultDispatcher = StandardTestDispatcher(),
        )

        whenever(mockGetStringFromStringResMapper(any()))
            .thenReturn("")

        // when
        underTest.validateAlbumName(albumName = "")

        // then
        underTest.stateFlow.drop(1).test {
            val state = awaitItem()
            assertThat(state.renameAlbumErrorMessage).isNotNull()
        }
    }

    @Test
    fun `test that album name contains invalid char should show error`() = runTest {
        // given
        val underTest = AlbumImportViewModel(
            savedStateHandle = SavedStateHandle(),
            hasCredentialsUseCase = mockHasCredentialsUseCase,
            getUserAlbums = mockGetUserAlbums,
            getPublicAlbumUseCase = mockGetPublicAlbumUseCase,
            legacyPublicAlbumPhotoProvider = mockLegacyPublicAlbumPhotoProvider,
            getProscribedAlbumNamesUseCase = mockGetProscribedAlbumNamesUseCase,
            getStringFromStringResMapper = mockGetStringFromStringResMapper,
            defaultDispatcher = StandardTestDispatcher(),
        )

        whenever(mockGetStringFromStringResMapper(any(), any()))
            .thenReturn("")

        // when
        underTest.validateAlbumName(albumName = INVALID_CHARACTERS)

        // then
        underTest.stateFlow.drop(1).test {
            val state = awaitItem()
            assertThat(state.renameAlbumErrorMessage).isNotNull()
        }
    }

    @Test
    fun `test that valid album name should not show error`() = runTest {
        // given
        val underTest = AlbumImportViewModel(
            savedStateHandle = SavedStateHandle(),
            hasCredentialsUseCase = mockHasCredentialsUseCase,
            getUserAlbums = mockGetUserAlbums,
            getPublicAlbumUseCase = mockGetPublicAlbumUseCase,
            legacyPublicAlbumPhotoProvider = mockLegacyPublicAlbumPhotoProvider,
            getProscribedAlbumNamesUseCase = mockGetProscribedAlbumNamesUseCase,
            getStringFromStringResMapper = mockGetStringFromStringResMapper,
            defaultDispatcher = StandardTestDispatcher(),
        )

        whenever(mockGetProscribedAlbumNamesUseCase())
            .thenReturn(listOf())

        whenever(mockGetStringFromStringResMapper(any(), any()))
            .thenReturn("")

        // when
        underTest.validateAlbumName(albumName = "My Album")

        // then
        underTest.stateFlow.drop(1).test {
            val state = awaitItem()
            assertThat(state.renameAlbumErrorMessage).isNull()
        }
    }

    @Test
    fun `test that close rename album dialog works properly`() = runTest {
        // given
        val underTest = AlbumImportViewModel(
            savedStateHandle = SavedStateHandle(),
            hasCredentialsUseCase = mockHasCredentialsUseCase,
            getUserAlbums = mockGetUserAlbums,
            getPublicAlbumUseCase = mockGetPublicAlbumUseCase,
            legacyPublicAlbumPhotoProvider = mockLegacyPublicAlbumPhotoProvider,
            getProscribedAlbumNamesUseCase = mockGetProscribedAlbumNamesUseCase,
            getStringFromStringResMapper = mockGetStringFromStringResMapper,
            defaultDispatcher = StandardTestDispatcher(),
        )

        // when
        underTest.closeRenameAlbumDialog()

        // then
        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.showRenameAlbumDialog).isFalse()
        }
    }
}
