package test.mega.privacy.android.app.presentation.photos.albums.importlink

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.constants.StringsConstants.INVALID_CHARACTERS
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity.Companion.ALBUM_LINK
import mega.privacy.android.app.presentation.photos.albums.importlink.AlbumImportViewModel
import mega.privacy.android.app.presentation.photos.util.LegacyPublicAlbumPhotoNodeProvider
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album.UserAlbum
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetUserAlbums
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.photos.DownloadPublicAlbumPhotoPreviewUseCase
import mega.privacy.android.domain.usecase.photos.DownloadPublicAlbumPhotoThumbnailUseCase
import mega.privacy.android.domain.usecase.photos.GetProscribedAlbumNamesUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumPhotoUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumUseCase
import mega.privacy.android.domain.usecase.photos.ImportPublicAlbumUseCase
import mega.privacy.android.domain.usecase.photos.IsAlbumLinkValidUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AlbumImportViewModelTest {
    private lateinit var underTest: AlbumImportViewModel

    private val mockSavedStateHandle: SavedStateHandle = mock()

    private val mockHasCredentialsUseCaseUseCase: HasCredentialsUseCase = mock()

    private val mockGetUserAlbums: GetUserAlbums = mock()

    private val mockGetPublicAlbumUseCase: GetPublicAlbumUseCase = mock()

    private val mockGetPublicAlbumPhotoUseCase: GetPublicAlbumPhotoUseCase = mock()

    private val mockLegacyPublicAlbumPhotoNodeProvider: LegacyPublicAlbumPhotoNodeProvider = mock()

    private val mockDownloadPublicAlbumPhotoPreviewUseCase: DownloadPublicAlbumPhotoPreviewUseCase = mock()

    private val mockDownloadPublicAlbumPhotoThumbnailUseCase: DownloadPublicAlbumPhotoThumbnailUseCase = mock()

    private val mockMonitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()

    private val mockGetProscribedAlbumNamesUseCase: GetProscribedAlbumNamesUseCase = mock()

    private val mockGetStringFromStringResMapper: GetStringFromStringResMapper = mock()

    private val mockImportPublicAlbumUseCase: ImportPublicAlbumUseCase = mock()

    private val mockIsAlbumLinkValidUseCase: IsAlbumLinkValidUseCase = mock()

    private val mockMonitorConnectivityUseCase: MonitorConnectivityUseCase = mock()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher = StandardTestDispatcher())
        whenever(mockMonitorConnectivityUseCase()).thenReturn(flowOf(false))

        underTest = AlbumImportViewModel(
            savedStateHandle = mockSavedStateHandle,
            hasCredentialsUseCase = mockHasCredentialsUseCaseUseCase,
            getUserAlbums = mockGetUserAlbums,
            getPublicAlbumUseCase = mockGetPublicAlbumUseCase,
            getPublicAlbumPhotoUseCase = mockGetPublicAlbumPhotoUseCase,
            legacyPublicAlbumPhotoNodeProvider = mockLegacyPublicAlbumPhotoNodeProvider,
            downloadPublicAlbumPhotoPreviewUseCase = mockDownloadPublicAlbumPhotoPreviewUseCase,
            downloadPublicAlbumPhotoThumbnailUseCase = mockDownloadPublicAlbumPhotoThumbnailUseCase,
            monitorAccountDetailUseCase = mockMonitorAccountDetailUseCase,
            getProscribedAlbumNamesUseCase = mockGetProscribedAlbumNamesUseCase,
            getStringFromStringResMapper = mockGetStringFromStringResMapper,
            importPublicAlbumUseCase = mockImportPublicAlbumUseCase,
            isAlbumLinkValidUseCase = mockIsAlbumLinkValidUseCase,
            monitorConnectivityUseCase = mockMonitorConnectivityUseCase,
            defaultDispatcher = StandardTestDispatcher(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that show error access dialog if link is null`() = runTest {
        // given
        whenever(mockHasCredentialsUseCaseUseCase())
            .thenReturn(false)

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

        whenever(mockSavedStateHandle.get<String>(ALBUM_LINK))
            .thenReturn(link)

        whenever(mockHasCredentialsUseCaseUseCase())
            .thenReturn(false)

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
        val album = mock<UserAlbum>()

        whenever(mockSavedStateHandle.get<String>(ALBUM_LINK))
            .thenReturn(link)

        whenever(mockHasCredentialsUseCaseUseCase())
            .thenReturn(false)

        whenever(mockGetPublicAlbumUseCase(albumLink = AlbumLink(link)))
            .thenReturn(album to listOf())

        whenever(mockGetPublicAlbumPhotoUseCase(albumPhotoIds = listOf()))
            .thenReturn(listOf())

        whenever(mockLegacyPublicAlbumPhotoNodeProvider.loadNodeCache(albumPhotoIds = listOf()))
            .thenReturn(Unit)

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
        whenever(mockGetStringFromStringResMapper(any()))
            .thenReturn("")

        // when
        underTest.validateAlbumName(albumName = "")

        // then
        underTest.stateFlow.test {
            repeat(1) { awaitItem() }

            val state = awaitItem()
            assertThat(state.renameAlbumErrorMessage).isNotNull()
        }
    }

    @Test
    fun `test that album name contains invalid char should show error`() = runTest {
        // given
        whenever(mockGetStringFromStringResMapper(any(), any()))
            .thenReturn("")

        // when
        underTest.validateAlbumName(albumName = INVALID_CHARACTERS)

        // then
        underTest.stateFlow.test {
            repeat(1) { awaitItem() }

            val state = awaitItem()
            assertThat(state.renameAlbumErrorMessage).isNotNull()
        }
    }

    @Test
    fun `test that valid album name should not show error`() = runTest {
        // given
        whenever(mockGetProscribedAlbumNamesUseCase())
            .thenReturn(listOf())

        whenever(mockGetStringFromStringResMapper(any(), any()))
            .thenReturn("")

        // when
        underTest.validateAlbumName(albumName = "My Album")

        // then
        underTest.stateFlow.test {
            repeat(1) { awaitItem() }

            val state = awaitItem()
            assertThat(state.renameAlbumErrorMessage).isNull()
        }
    }

    @Test
    fun `test that close rename album dialog works properly`() = runTest {
        // when
        underTest.closeRenameAlbumDialog()

        // then
        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.showRenameAlbumDialog).isFalse()
        }
    }

    @Test
    fun `test that conflict album name should show rename album dialog`() = runTest {
        // given
        val conflictAlbumName = "My Album"

        val album = mock<UserAlbum> {
            on { title }.thenReturn(conflictAlbumName)
        }
        val photos = listOf<Photo>()

        underTest.localAlbumNames = setOf(conflictAlbumName)

        // when
        underTest.validateImportConstraint(album, photos)

        // then
        underTest.stateFlow.test {
            repeat(1) { awaitItem() }

            val state = awaitItem()
            assertThat(state.showRenameAlbumDialog).isTrue()
        }
    }

    @Test
    fun `test that import album works properly`() = runTest {
        // given
        underTest.isNetworkConnected = true

        whenever(mockGetStringFromStringResMapper(any(), any()))
            .thenReturn("")

        // when
        underTest.importAlbum(targetParentFolderNodeId = NodeId(-1L))

        // then
        underTest.stateFlow.test {
            repeat(2) { awaitItem() }

            val state = awaitItem()
            assertThat(state.importAlbumMessage).isNotNull()
        }
    }

    @Test
    fun `test that clear import message works properly`() = runTest {
        // when
        underTest.clearImportAlbumMessage()

        // then
        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.importAlbumMessage).isNull()
        }
    }

    @Test
    fun `test that exceeds storage should show storage exceeds dialog`() = runTest {
        // given
        val album = mock<UserAlbum>()
        val photos = listOf<Photo>(
            mock<Photo.Image> {
                on { size }.thenReturn(100L)
            },
            mock<Photo.Image> {
                on { size }.thenReturn(200L)
            },
            mock<Photo.Image> {
                on { size }.thenReturn(300L)
            }
        )

        underTest.availableStorage = 500L

        // when
        underTest.validateImportConstraint(album, photos)

        // then
        underTest.stateFlow.test {
            repeat(1) { awaitItem() }

            val state = awaitItem()
            assertThat(state.showStorageExceededDialog).isTrue()
        }
    }

    @Test
    fun `test that close storage exceeded dialog works properly`() = runTest {
        // when
        underTest.closeStorageExceededDialog()

        // then
        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.showStorageExceededDialog).isFalse()
        }
    }
}
