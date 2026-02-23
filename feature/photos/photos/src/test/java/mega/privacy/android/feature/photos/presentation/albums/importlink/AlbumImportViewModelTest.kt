package mega.privacy.android.feature.photos.presentation.albums.importlink

import android.content.Context
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.photos.Album.UserAlbum
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.usecase.GetUserAlbums
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.account.GetCurrentStorageStateUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeFromSerializedDataUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.photos.GetProscribedAlbumNamesUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumNodesDataUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumPhotoUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumUseCase
import mega.privacy.android.domain.usecase.photos.ImportPublicAlbumUseCase
import mega.privacy.android.domain.usecase.photos.IsAlbumLinkValidUseCase
import mega.privacy.android.feature.photos.mapper.PhotoUiStateMapper
import mega.privacy.android.feature.photos.model.PhotoUiState
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

private const val INVALID_CHARACTERS = "\" * / : < > ? \\ |"

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AlbumImportViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var underTest: AlbumImportViewModel

    private val mockHasCredentialsUseCase: HasCredentialsUseCase = mock()
    private val mockGetUserAlbums: GetUserAlbums = mock()
    private val mockGetPublicAlbumUseCase: GetPublicAlbumUseCase = mock()
    private val mockGetPublicAlbumPhotoUseCase: GetPublicAlbumPhotoUseCase = mock()
    private val mockMonitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val mockGetProscribedAlbumNamesUseCase: GetProscribedAlbumNamesUseCase = mock()
    private val mockImportPublicAlbumUseCase: ImportPublicAlbumUseCase = mock()
    private val mockIsAlbumLinkValidUseCase: IsAlbumLinkValidUseCase = mock()
    private val mockMonitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val mockGetPublicNodeFromSerializedDataUseCase: GetPublicNodeFromSerializedDataUseCase =
        mock()
    private val mockGetPublicAlbumNodesDataUseCase: GetPublicAlbumNodesDataUseCase = mock()
    private val mockGetCurrentStorageStateUseCase: GetCurrentStorageStateUseCase = mock()
    private val mockContext: Context = mock()
    private val mockPhotoUiStateMapper: PhotoUiStateMapper = mock()

    @BeforeEach
    fun setup() {
        whenever(mockMonitorConnectivityUseCase()).thenReturn(flowOf(false))
        runBlocking { whenever(mockHasCredentialsUseCase()).thenReturn(false) }
        whenever(mockContext.getString(any())).thenReturn("")
        whenever(mockContext.getString(any(), anyOrNull())).thenReturn("")
        initUnderTest()
    }

    private fun initUnderTest(albumLink: String? = null) {
        underTest = AlbumImportViewModel(
            photoUiStateMapper = mockPhotoUiStateMapper,
            hasCredentialsUseCase = mockHasCredentialsUseCase,
            getUserAlbums = mockGetUserAlbums,
            getPublicAlbumUseCase = mockGetPublicAlbumUseCase,
            getPublicAlbumPhotoUseCase = mockGetPublicAlbumPhotoUseCase,
            getProscribedAlbumNamesUseCase = mockGetProscribedAlbumNamesUseCase,
            monitorAccountDetailUseCase = mockMonitorAccountDetailUseCase,
            importPublicAlbumUseCase = mockImportPublicAlbumUseCase,
            isAlbumLinkValidUseCase = mockIsAlbumLinkValidUseCase,
            monitorConnectivityUseCase = mockMonitorConnectivityUseCase,
            getPublicNodeFromSerializedDataUseCase = mockGetPublicNodeFromSerializedDataUseCase,
            getPublicAlbumNodesDataUseCase = mockGetPublicAlbumNodesDataUseCase,
            getCurrentStorageStateUseCase = mockGetCurrentStorageStateUseCase,
            context = mockContext,
            defaultDispatcher = testDispatcher,
            albumLink = albumLink,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            mockHasCredentialsUseCase,
            mockGetUserAlbums,
            mockGetPublicAlbumUseCase,
            mockGetPublicAlbumPhotoUseCase,
            mockMonitorAccountDetailUseCase,
            mockGetProscribedAlbumNamesUseCase,
            mockImportPublicAlbumUseCase,
            mockIsAlbumLinkValidUseCase,
            mockMonitorConnectivityUseCase,
            mockGetPublicNodeFromSerializedDataUseCase,
            mockGetPublicAlbumNodesDataUseCase,
        )
    }

    @Test
    fun `test that show error access dialog if link is null`() = runTest {
        initUnderTest(albumLink = null)

        underTest.stateFlow.drop(2).test {
            assertThat(awaitItem().showErrorAccessDialog).isTrue()
        }
    }

    @Test
    fun `test that show decryption key dialog if link not contains key`() = runTest {
        val link = "https://mega.app/collection/handle"

        whenever(mockHasCredentialsUseCase()).thenReturn(false)

        initUnderTest(albumLink = link)

        underTest.stateFlow.drop(2).test {
            assertThat(awaitItem().showInputDecryptionKeyDialog).isTrue()
        }
    }

    @Test
    fun `test that get public album works properly`() = runTest {
        val link = "https://mega.app/collection/handle#key"
        val album = mock<UserAlbum>()
        val photo = mock<Photo.Image>()
        val photoUiState = mock<PhotoUiState.Image>()

        whenever(mockHasCredentialsUseCase()).thenReturn(false)

        whenever(mockGetPublicAlbumUseCase(albumLink = AlbumLink(link)))
            .thenReturn(album to listOf())

        whenever(mockGetPublicAlbumPhotoUseCase(albumPhotoIds = listOf()))
            .thenReturn(listOf(photo))

        whenever(mockPhotoUiStateMapper(photo)).thenReturn(photoUiState)

        initUnderTest(albumLink = link)

        underTest.stateFlow.drop(2).test {
            assertThat(awaitItem().album).isEqualTo(album)
        }
    }

    @Test
    fun `test that close decryption key dialog works properly`() = runTest {
        underTest.closeInputDecryptionKeyDialog()

        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.showInputDecryptionKeyDialog).isFalse()
        }
    }

    @Test
    fun `test that select photo works properly`() = runTest {
        val photo = createPhotoUiState(id = 1L)

        underTest.selectPhoto(photo)

        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.selectedPhotos.size).isEqualTo(1)
        }
    }

    @Test
    fun `test that unselect photo works properly`() = runTest {
        val photo = createPhotoUiState(id = 1L)

        underTest.unselectPhoto(photo)

        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.selectedPhotos).doesNotContain(photo)
        }
    }

    @Test
    fun `test that clear selection works properly`() = runTest {
        underTest.clearSelection()

        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.selectedPhotos).isEmpty()
        }
    }

    @Test
    fun `test that album name empty should show error`() = runTest {
        whenever(mockContext.getString(any())).thenReturn("error")

        underTest.validateAlbumName(albumName = "")

        underTest.stateFlow.test {
            repeat(1) { awaitItem() }

            val state = awaitItem()
            assertThat(state.renameAlbumErrorMessage).isNotNull()
        }
    }

    @Test
    fun `test that album name with dot should show error`() = runTest {
        whenever(mockContext.getString(any())).thenReturn(".")

        underTest.validateAlbumName(albumName = ".")

        underTest.stateFlow.test {
            repeat(1) { awaitItem() }

            val state = awaitItem()
            assertThat(state.renameAlbumErrorMessage).isNotNull()
        }
    }

    @Test
    fun `test that album name with double dot should show error`() = runTest {
        whenever(mockContext.getString(any())).thenReturn(".")

        underTest.validateAlbumName(albumName = "..")


        underTest.stateFlow.test {
            repeat(1) { awaitItem() }

            val state = awaitItem()
            assertThat(state.renameAlbumErrorMessage).isNotNull()
        }
    }

    @Test
    fun `test that album name contains invalid char should show error`() = runTest {
        whenever(mockContext.getString(any(), anyOrNull())).thenReturn("error")

        underTest.validateAlbumName(albumName = INVALID_CHARACTERS)


        underTest.stateFlow.test {
            repeat(1) { awaitItem() }

            val state = awaitItem()
            assertThat(state.renameAlbumErrorMessage).isNotNull()
        }
    }

    @Test
    fun `test that valid album name should not show error`() = runTest {
        whenever(mockGetProscribedAlbumNamesUseCase()).thenReturn(listOf())

        whenever(mockContext.getString(any(), anyOrNull())).thenReturn("")

        underTest.validateAlbumName(albumName = "My Album")

        underTest.stateFlow.test {
            repeat(1) { awaitItem() }

            val state = awaitItem()
            assertThat(state.renameAlbumErrorMessage).isNull()
        }
    }

    @Test
    fun `test that close rename album dialog works properly`() = runTest {
        underTest.closeRenameAlbumDialog()

        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.showRenameAlbumDialog).isFalse()
        }
    }

    @Test
    fun `test that conflict album name should show rename album dialog`() = runTest {
        val conflictAlbumName = "My Album"

        val album = mock<UserAlbum> {
            on { title }.thenReturn(conflictAlbumName)
        }
        val photos = listOf<PhotoUiState>()

        underTest.localAlbumNames = setOf(conflictAlbumName)

        underTest.validateImportConstraint(album, photos)

        underTest.stateFlow.test {
            repeat(1) { awaitItem() }

            val state = awaitItem()
            assertThat(state.showRenameAlbumDialog).isTrue()
        }
    }

    @Test
    fun `test that import album works properly`() = runTest {
        underTest.isNetworkConnected = true

        whenever(mockContext.getString(any(), anyOrNull())).thenReturn("")

        underTest.importAlbum(targetParentFolderNodeId = NodeId(-1L))

        underTest.stateFlow.test {
            repeat(2) { awaitItem() }

            val state = awaitItem()
            assertThat(state.importAlbumMessage).isNotNull()
        }
    }

    @Test
    fun `test that clear import message works properly`() = runTest {
        underTest.clearImportAlbumMessage()

        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.importAlbumMessage).isNull()
        }
    }

    @Test
    fun `test that exceeds storage should show storage exceeds dialog`() = runTest {
        val album = mock<UserAlbum>()
        val photos = listOf(
            createPhotoUiState(id = 1L, size = 100L),
            createPhotoUiState(id = 2L, size = 200L),
            createPhotoUiState(id = 3L, size = 300L),
        )

        underTest.availableStorage = 500L

        underTest.validateImportConstraint(album, photos)

        underTest.stateFlow.test {
            repeat(1) { awaitItem() }

            val state = awaitItem()
            assertThat(state.showStorageExceededDialog).isTrue()
        }
    }

    @Test
    fun `test that close storage exceeded dialog works properly`() = runTest {
        underTest.closeStorageExceededDialog()

        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.showStorageExceededDialog).isFalse()
        }
    }

    @Test
    fun `test that start download triggers the correct download event when start download is triggered`() =
        runTest {
            val handle = 1L
            val photo = createPhotoUiState(id = handle)
            val serializedData = "serializedNode"
            val megaNode = mock<MegaNode> {
                on { serialize() } doReturn serializedData
            }
            underTest.selectPhoto(photo)

            val node = mock<PublicLinkFile>()
            whenever(mockGetPublicNodeFromSerializedDataUseCase(serializedData))
                .thenReturn(node)
            whenever(mockGetPublicAlbumNodesDataUseCase())
                .thenReturn(mapOf(NodeId(handle) to serializedData))

            underTest.stateFlow.test {
                awaitItem()
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
    fun `test that selection is cleared when start download is invoked`() = runTest {
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
    fun `test that download event is consumed properly`() = runTest {
        stubSelectedTypedNode()
        underTest.stateFlow.test {
            awaitItem()
            underTest.startDownload()
            assertThat(awaitItem().downloadEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
            awaitItem()
            underTest.consumeDownloadEvent()
            assertThat(awaitItem().downloadEvent).isInstanceOf(StateEventWithContentConsumed::class.java)
        }
    }

    @Test
    fun `test that handleSharedAlbumLink handles link without sub-handle`() = runTest {
        val album = mock<UserAlbum>()
        val albumLink = "https://mega.app/collection/handle#key!"

        whenever(mockHasCredentialsUseCase()).thenReturn(false)
        whenever(mockGetPublicAlbumUseCase(albumLink = AlbumLink(albumLink)))
            .thenReturn(album to listOf())
        whenever(mockGetPublicAlbumPhotoUseCase(albumPhotoIds = listOf()))
            .thenReturn(emptyList())

        initUnderTest(albumLink = albumLink)

        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.folderSubHandle).isNull()
        }
    }

    @Test
    fun `test that handleSharedAlbumLink handles null link`() = runTest {
        initUnderTest(albumLink = null)

        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.folderSubHandle).isNull()
        }
    }

    @Test
    fun `test that handleSharedAlbumLink handles blank link`() = runTest {
        initUnderTest(albumLink = "")

        underTest.stateFlow.test {
            val state = awaitItem()
            assertThat(state.folderSubHandle).isNull()
        }
    }

    @Test
    fun `test that openFile triggers file opening event`() = runTest {
        val photo = createPhotoUiState(id = 1L)

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
        val photo1 = createPhotoUiState(id = 1L)
        val photo2 = createPhotoUiState(id = 2L)

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
        val photo1 = createPhotoUiState(id = 1L, base64Id = "differentHandle")
        val photo2 = createPhotoUiState(id = 2L, base64Id = "subHandle123")
        val photos = listOf(photo1, photo2)

        whenever(mockHasCredentialsUseCase()).thenReturn(false)
        whenever(mockGetPublicAlbumUseCase(albumLink = AlbumLink(albumLink)))
            .thenReturn(album to listOf())
        whenever(mockGetPublicAlbumPhotoUseCase(albumPhotoIds = listOf()))
            .thenReturn(listOf(mock<Photo.Image>(), mock<Photo.Image>()))
        whenever(mockPhotoUiStateMapper(any())).thenReturn(photo1, photo2)

        initUnderTest(albumLink = albumLink)

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
        val photo1 = createPhotoUiState(id = 1L, base64Id = "handle1")
        val photo2 = createPhotoUiState(id = 2L, base64Id = "handle2")

        whenever(mockHasCredentialsUseCase()).thenReturn(false)
        whenever(mockGetPublicAlbumUseCase(albumLink = AlbumLink(albumLink)))
            .thenReturn(album to listOf())
        whenever(mockGetPublicAlbumPhotoUseCase(albumPhotoIds = listOf()))
            .thenReturn(listOf(mock<Photo.Image>(), mock<Photo.Image>()))
        whenever(mockPhotoUiStateMapper(any())).thenReturn(photo1, photo2)

        initUnderTest(albumLink = albumLink)

        underTest.stateFlow.drop(1).test {
            val state = awaitItem()
            assertThat(state.folderSubHandle).isEqualTo("nonexistentHandle")

            val finalState = awaitItem()
            assertThat(finalState.openFileNodeEvent).isInstanceOf(StateEventWithContentConsumed::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun stubSelectedTypedNode(): TypedNode {
        val handle = 1L
        val photo = createPhotoUiState(id = handle)
        val serializedData = "serializedNode"
        val megaNode = mock<MegaNode> {
            on { serialize() } doReturn serializedData
        }
        val node = mock<PublicLinkFile>()
        whenever(mockGetPublicNodeFromSerializedDataUseCase(serializedData))
            .thenReturn(node)
        whenever(mockGetPublicAlbumNodesDataUseCase())
            .thenReturn(mapOf(NodeId(handle) to serializedData))
        underTest.selectPhoto(photo)
        return node
    }

    private fun createPhotoUiState(
        id: Long,
        size: Long = 0L,
        base64Id: String? = null,
    ): PhotoUiState.Image {
        return PhotoUiState.Image(
            id = id,
            albumPhotoId = null,
            parentId = 0L,
            name = "photo",
            isFavourite = false,
            creationTime = LocalDateTime.now(),
            modificationTime = LocalDateTime.now(),
            thumbnailFilePath = null,
            previewFilePath = null,
            fileTypeInfo = StaticImageFileTypeInfo("image/jpeg", "jpg"),
            size = size,
            isTakenDown = false,
            isSensitive = false,
            isSensitiveInherited = false,
            base64Id = base64Id,
        )
    }

    companion object {
        private val testDispatcher = StandardTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}
