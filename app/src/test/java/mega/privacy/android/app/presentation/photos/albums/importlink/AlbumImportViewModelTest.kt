package mega.privacy.android.app.presentation.photos.albums.importlink

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentConsumed
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.TimberJUnit5Extension
import mega.privacy.android.app.constants.StringsConstants.INVALID_CHARACTERS
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity.Companion.ALBUM_LINK
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.photos.Album.UserAlbum
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.usecase.GetUserAlbums
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeFromSerializedDataUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.photos.DownloadPublicAlbumPhotoPreviewUseCase
import mega.privacy.android.domain.usecase.photos.DownloadPublicAlbumPhotoThumbnailUseCase
import mega.privacy.android.domain.usecase.photos.GetProscribedAlbumNamesUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumNodesDataUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumPhotoUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumUseCase
import mega.privacy.android.domain.usecase.photos.ImportPublicAlbumUseCase
import mega.privacy.android.domain.usecase.photos.IsAlbumLinkValidUseCase
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(TimberJUnit5Extension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AlbumImportViewModelTest {
    private lateinit var underTest: AlbumImportViewModel

    private val mockSavedStateHandle: SavedStateHandle = mock()

    private val mockHasCredentialsUseCaseUseCase: HasCredentialsUseCase = mock()

    private val mockGetUserAlbums: GetUserAlbums = mock()

    private val mockGetPublicAlbumUseCase: GetPublicAlbumUseCase = mock()

    private val mockGetPublicAlbumPhotoUseCase: GetPublicAlbumPhotoUseCase = mock()

    private val mockDownloadPublicAlbumPhotoPreviewUseCase: DownloadPublicAlbumPhotoPreviewUseCase =
        mock()

    private val mockDownloadPublicAlbumPhotoThumbnailUseCase: DownloadPublicAlbumPhotoThumbnailUseCase =
        mock()

    private val mockMonitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()

    private val mockGetProscribedAlbumNamesUseCase: GetProscribedAlbumNamesUseCase = mock()

    private val mockGetStringFromStringResMapper: GetStringFromStringResMapper = mock()

    private val mockImportPublicAlbumUseCase: ImportPublicAlbumUseCase = mock()

    private val mockIsAlbumLinkValidUseCase: IsAlbumLinkValidUseCase = mock()

    private val mockMonitorConnectivityUseCase: MonitorConnectivityUseCase = mock()

    private val mockGetPublicNodeFromSerializedDataUseCase: GetPublicNodeFromSerializedDataUseCase =
        mock()

    private val getPublicAlbumNodesDataUseCase: GetPublicAlbumNodesDataUseCase = mock()

    @BeforeEach
    fun setup() {
        whenever(mockMonitorConnectivityUseCase()).thenReturn(flowOf(false))
        runBlocking { whenever(mockHasCredentialsUseCaseUseCase()).thenReturn(false) }
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = AlbumImportViewModel(
            savedStateHandle = mockSavedStateHandle,
            hasCredentialsUseCase = mockHasCredentialsUseCaseUseCase,
            getUserAlbums = mockGetUserAlbums,
            getPublicAlbumUseCase = mockGetPublicAlbumUseCase,
            getPublicAlbumPhotoUseCase = mockGetPublicAlbumPhotoUseCase,
            downloadPublicAlbumPhotoPreviewUseCase = mockDownloadPublicAlbumPhotoPreviewUseCase,
            downloadPublicAlbumPhotoThumbnailUseCase = mockDownloadPublicAlbumPhotoThumbnailUseCase,
            monitorAccountDetailUseCase = mockMonitorAccountDetailUseCase,
            getProscribedAlbumNamesUseCase = mockGetProscribedAlbumNamesUseCase,
            getStringFromStringResMapper = mockGetStringFromStringResMapper,
            importPublicAlbumUseCase = mockImportPublicAlbumUseCase,
            isAlbumLinkValidUseCase = mockIsAlbumLinkValidUseCase,
            monitorConnectivityUseCase = mockMonitorConnectivityUseCase,
            defaultDispatcher = StandardTestDispatcher(),
            getPublicNodeFromSerializedDataUseCase = mockGetPublicNodeFromSerializedDataUseCase,
            getPublicAlbumNodesDataUseCase = getPublicAlbumNodesDataUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            mockSavedStateHandle,
            mockHasCredentialsUseCaseUseCase,
            mockGetUserAlbums,
            mockGetPublicAlbumUseCase,
            mockGetPublicAlbumPhotoUseCase,
            mockDownloadPublicAlbumPhotoThumbnailUseCase,
            mockMonitorAccountDetailUseCase,
            mockGetProscribedAlbumNamesUseCase,
            mockGetStringFromStringResMapper,
            mockImportPublicAlbumUseCase,
            mockIsAlbumLinkValidUseCase,
            mockMonitorConnectivityUseCase,
            mockGetPublicNodeFromSerializedDataUseCase,
            getPublicAlbumNodesDataUseCase,
        )
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
            val state = awaitItem()
            assertThat(state.showErrorAccessDialog).isTrue()
        }
    }

    @Test
    fun `test that show decryption key dialog if link not contains key`() = runTest {
        val link = "https://mega.app/collection/handle"

        whenever(mockSavedStateHandle.get<String>(ALBUM_LINK))
            .thenReturn(link)

        whenever(mockHasCredentialsUseCaseUseCase())
            .thenReturn(false)

        // when
        underTest.initialize()

        // then
        underTest.stateFlow.drop(1).test {
            val state = awaitItem()
            assertThat(state.showInputDecryptionKeyDialog).isTrue()
        }
    }

    @Test
    fun `test that get public album works properly`() = runTest {
        // given
        val link = "https://mega.app/collection/handle#key"
        val album = mock<UserAlbum>()

        whenever(mockSavedStateHandle.get<String>(ALBUM_LINK))
            .thenReturn(link)

        whenever(mockHasCredentialsUseCaseUseCase())
            .thenReturn(false)

        whenever(mockGetPublicAlbumUseCase(albumLink = AlbumLink(link)))
            .thenReturn(album to listOf())

        whenever(mockGetPublicAlbumPhotoUseCase(albumPhotoIds = listOf()))
            .thenReturn(listOf())

        // when
        underTest.initialize()

        // then
        underTest.stateFlow.drop(1).test {
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

    @Test
    fun `test that start download triggers the correct download event when start download is triggered`() =
        runTest {
            val handle = 1L
            val photo = mock<Photo.Image> {
                on { id } doReturn handle
            }
            val serializedData = "serializedNode"
            val megaNode = mock<MegaNode> {
                on { serialize() } doReturn serializedData
            }
            underTest.selectPhoto(photo)

            val node = mock<PublicLinkFile>()
            whenever(mockGetPublicNodeFromSerializedDataUseCase(megaNode.serialize()))
                .thenReturn(node)
            whenever(getPublicAlbumNodesDataUseCase())
                .thenReturn(mapOf(NodeId(handle) to serializedData))
            underTest.stateFlow.test {
                awaitItem() //initial
                underTest.startDownload()
                val actual = awaitItem().downloadEvent
                assertThat(actual).isInstanceOf(StateEventWithContentTriggered::class.java)
                val content = (actual as StateEventWithContentTriggered).content
                assertThat(content).isInstanceOf(TransferTriggerEvent.StartDownloadNode::class.java)
                val triggerEvent = content as TransferTriggerEvent.StartDownloadNode
                assertThat(triggerEvent.nodes).containsExactly(node)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that selection is cleared when start download is invoked and DownloadWorker feature flag is true`() =
        runTest {
            stubSelectedTypedNode()
            underTest.stateFlow.test {
                underTest.startDownload()
                assertThat(awaitItem().selectedPhotos).isNotEmpty()
                underTest.clearSelection()
                val actual =
                    (cancelAndConsumeRemainingEvents().last() as Event.Item).value.selectedPhotos
                assertThat(actual).isEmpty()
            }
        }

    @Test
    fun `test that download event is consumed properly`() =
        runTest {
            stubSelectedTypedNode()
            underTest.stateFlow.test {
                awaitItem() //initial
                underTest.startDownload()
                assertThat(awaitItem().downloadEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
                awaitItem() //clear selection
                underTest.consumeDownloadEvent()
                assertThat(awaitItem().downloadEvent).isInstanceOf(StateEventWithContentConsumed::class.java)
            }
        }

    @Test
    fun `test that handleSharedAlbumLink handles link without sub-handle`() = runTest {
        val album = mock<UserAlbum>()
        val albumLink = "https://mega.app/collection/handle#key!"
        setupAlbumWithLink(albumLink, album, emptyList())

        underTest.initialize()

        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.folderSubHandle).isNull()
        }
    }

    @Test
    fun `test that handleSharedAlbumLink handles link with empty sub-handle`() = runTest {
        val album = mock<UserAlbum>()
        val albumLink = "https://mega.app/collection/handle#key!"
        setupAlbumWithLink(albumLink, album, emptyList())

        underTest.initialize()

        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.folderSubHandle).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that handleSharedAlbumLink handles null link`() = runTest {
        whenever(mockSavedStateHandle.get<String>(ALBUM_LINK))
            .thenReturn(null)

        underTest.initialize()

        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.folderSubHandle).isNull()
        }
    }

    @Test
    fun `test that handleSharedAlbumLink handles blank link`() = runTest {
        whenever(mockSavedStateHandle.get<String>(ALBUM_LINK))
            .thenReturn("")

        underTest.initialize()

        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.folderSubHandle).isNull()
        }
    }

    @Test
    fun `test that openFile triggers file opening event`() = runTest {
        val photo = mock<Photo.Image>()

        underTest.openFile(photo)

        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.openFileNodeEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            val content = (state.openFileNodeEvent as StateEventWithContentTriggered).content
            assertThat(content).isEqualTo(photo)
        }
    }

    @Test
    fun `test that openFile event can be triggered multiple times`() = runTest {
        val photo1 = mock<Photo.Image>()
        val photo2 = mock<Photo.Image>()

        underTest.stateFlow.test {
            awaitItem()

            underTest.openFile(photo1)
            val state1 = awaitItem()
            assertThat(state1.openFileNodeEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            val content1 = (state1.openFileNodeEvent as StateEventWithContentTriggered).content
            assertThat(content1).isEqualTo(photo1)

            underTest.openFile(photo2)
            val state2 = awaitItem()
            assertThat(state2.openFileNodeEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            val content2 = (state2.openFileNodeEvent as StateEventWithContentTriggered).content
            assertThat(content2).isEqualTo(photo2)
        }
    }

    @Test
    fun `test that album with sub-handle opens specific photo on initialization`() = runTest {
        val albumLink = "https://mega.app/collection/handle#key!subHandle123"
        val album = mock<UserAlbum>()
        val photo1 = mock<Photo.Image> {
            on { base64Id } doReturn "differentHandle"
        }
        val photo2 = mock<Photo.Image> {
            on { base64Id } doReturn "subHandle123"
        }
        val photos = listOf(photo1, photo2)

        setupAlbumWithLink(albumLink, album, photos)

        initUnderTest()

        underTest.stateFlow.drop(1).test {
            val state = awaitItem()
            assertThat(state.folderSubHandle).isEqualTo("subHandle123")

            val finalState = awaitItem()
            assertThat(finalState.openFileNodeEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            val content = (finalState.openFileNodeEvent as StateEventWithContentTriggered).content
            assertThat(content).isEqualTo(photo2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that album without matching sub-handle does not open any photo`() = runTest {
        val albumLink = "https://mega.app/collection/handle#key!nonexistentHandle"
        val album = mock<UserAlbum>()
        val photo1 = mock<Photo.Image> {
            on { base64Id } doReturn "handle1"
        }
        val photo2 = mock<Photo.Image> {
            on { base64Id } doReturn "handle2"
        }
        val photos = listOf(photo1, photo2)

        setupAlbumWithLink(albumLink, album, photos)

        initUnderTest()

        underTest.stateFlow.drop(1).test {
            val state = awaitItem()
            assertThat(state.folderSubHandle).isEqualTo("nonexistentHandle")

            val finalState = awaitItem()
            assertThat(finalState.openFileNodeEvent).isInstanceOf(StateEventWithContentConsumed::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun setupAlbumWithLink(
        albumLink: String,
        album: UserAlbum,
        photos: List<Photo>,
    ) {
        whenever(mockSavedStateHandle.get<String>(ALBUM_LINK)).thenReturn(albumLink)
        whenever(mockHasCredentialsUseCaseUseCase()).thenReturn(false)
        whenever(mockGetPublicAlbumUseCase(albumLink = AlbumLink(albumLink)))
            .thenReturn(album to listOf())
        whenever(mockGetPublicAlbumPhotoUseCase(albumPhotoIds = listOf()))
            .thenReturn(photos)
    }

    private suspend fun stubSelectedTypedNode(): TypedNode {
        val megaNode = stubSelectedMegaNode()
        val node = mock<PublicLinkFile>()
        whenever(mockGetPublicNodeFromSerializedDataUseCase(megaNode.serialize()))
            .thenReturn(node)
        return node
    }

    private fun stubSelectedMegaNode(): MegaNode {
        val handle = 1L
        val photo = mock<Photo.Image> {
            on { id } doReturn handle
        }
        val serializedData = "serializedNode"
        val megaNode = mock<MegaNode> {
            on { serialize() } doReturn serializedData
        }
        underTest.selectPhoto(photo)
        return megaNode
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
