package mega.privacy.android.feature.fileinfo.presentation

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.nodecomponents.mapper.NodeDestinationMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeLocation
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.usecase.GetAddressFromCoordinatesUseCase
import mega.privacy.android.domain.usecase.GetImageNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetNodePathByIdUseCase
import mega.privacy.android.domain.usecase.MonitorNodeUpdatesById
import mega.privacy.android.domain.usecase.node.GetNodeLocationByIdUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.domain.usecase.node.SetNodeDescriptionUseCase
import mega.privacy.android.domain.usecase.shares.GetContactItemFromInShareFolder
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailUriRequest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.GetFolderTreeInfo
import mega.privacy.android.domain.usecase.shares.GetNodeOutSharesUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPreviewUseCase
import java.io.File
import mega.privacy.android.feature.fileinfo.presentation.model.Coordinates
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.wheneverBlocking
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FileInfoViewModelTest {

    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val monitorNodeUpdatesById: MonitorNodeUpdatesById = mock()
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase = mock()
    private val isNodeInBackupsUseCase: IsNodeInBackupsUseCase = mock()
    private val getNodeAccessPermission: GetNodeAccessPermission = mock()
    private val fileTypeIconMapper: FileTypeIconMapper = mock()
    private val getNodePathByIdUseCase: GetNodePathByIdUseCase = mock()
    private val getNodeLocationByIdUseCase: GetNodeLocationByIdUseCase = mock()
    private val getImageNodeByIdUseCase: GetImageNodeByIdUseCase = mock()
    private val getAddressFromCoordinatesUseCase: GetAddressFromCoordinatesUseCase = mock()
    private val setNodeDescriptionUseCase: SetNodeDescriptionUseCase = mock()
    private val getNodeOutSharesUseCase: GetNodeOutSharesUseCase = mock()
    private val getContactItemFromInShareFolder: GetContactItemFromInShareFolder = mock()
    private val getFolderTreeInfo: GetFolderTreeInfo = mock()
    private val getPreviewUseCase: GetPreviewUseCase = mock()
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper = mock()
    private val nodeDestinationMapper: NodeDestinationMapper = mock()

    private val fileTypeInfo = UnknownFileTypeInfo(mimeType = "image/heic", extension = "heic")

    private lateinit var underTest: FileInfoViewModel

    private fun initViewModel(nodeHandle: Long = NODE_HANDLE) {
        underTest = FileInfoViewModel(
            getNodeByIdUseCase = getNodeByIdUseCase,
            monitorNodeUpdatesById = monitorNodeUpdatesById,
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            getNodeAccessPermission = getNodeAccessPermission,
            fileTypeIconMapper = fileTypeIconMapper,
            getNodePathByIdUseCase = getNodePathByIdUseCase,
            getNodeLocationByIdUseCase = getNodeLocationByIdUseCase,
            getImageNodeByIdUseCase = getImageNodeByIdUseCase,
            getAddressFromCoordinatesUseCase = getAddressFromCoordinatesUseCase,
            setNodeDescriptionUseCase = setNodeDescriptionUseCase,
            getNodeOutSharesUseCase = getNodeOutSharesUseCase,
            getContactItemFromInShareFolder = getContactItemFromInShareFolder,
            getFolderTreeInfo = getFolderTreeInfo,
            getPreviewUseCase = getPreviewUseCase,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            nodeDestinationMapper = nodeDestinationMapper,
            nodeHandle = nodeHandle,
        )
    }

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(
            getNodeByIdUseCase,
            monitorNodeUpdatesById,
            isNodeInRubbishBinUseCase,
            isNodeInBackupsUseCase,
            getNodeAccessPermission,
            fileTypeIconMapper,
            getNodePathByIdUseCase,
            getNodeLocationByIdUseCase,
            getImageNodeByIdUseCase,
            getAddressFromCoordinatesUseCase,
            setNodeDescriptionUseCase,
            getNodeOutSharesUseCase,
            getContactItemFromInShareFolder,
            getFolderTreeInfo,
            getPreviewUseCase,
            durationInSecondsTextMapper,
            nodeDestinationMapper,
        )
        whenever(monitorNodeUpdatesById(any())).thenReturn(emptyFlow())
        whenever(fileTypeIconMapper(any(), any())).thenReturn(FILE_ICON_RES)
        wheneverBlocking { getFolderTreeInfo(any()) } doReturn FolderTreeInfo(
            numberOfFiles = 0,
            numberOfFolders = 0,
            totalCurrentSizeInBytes = 0L,
            numberOfVersions = 0,
            sizeOfPreviousVersionsInBytes = 0L,
        )
    }

    private fun mockFileNode(
        name: String = "file.txt",
        size: Long = 1024L,
        creationTime: Long = 100L,
        modificationTime: Long = 200L,
        description: String? = "a description",
        tags: List<String>? = listOf("marketing", "2026"),
        isTakenDown: Boolean = false,
        versionCount: Int = 0,
        type: FileTypeInfo = fileTypeInfo,
    ): TypedFileNode = mock {
        on { id } doReturn NodeId(NODE_HANDLE)
        on { this.name } doReturn name
        on { this.size } doReturn size
        on { this.creationTime } doReturn creationTime
        on { this.modificationTime } doReturn modificationTime
        on { this.description } doReturn description
        on { this.tags } doReturn tags
        on { this.isTakenDown } doReturn isTakenDown
        on { this.versionCount } doReturn versionCount
        on { this.type } doReturn type
    }

    private fun mockFolderNode(
        name: String = "folder",
        creationTime: Long = 100L,
    ): TypedFolderNode = mock {
        on { id } doReturn NodeId(NODE_HANDLE)
        on { this.name } doReturn name
        on { this.creationTime } doReturn creationTime
        on { description } doReturn null
        on { tags } doReturn null
        on { isTakenDown } doReturn false
    }

    private fun mockImageNode(
        type: FileTypeInfo = StaticImageFileTypeInfo(mimeType = "image/jpeg", extension = "jpg"),
        latitude: Double = 52.09,
        longitude: Double = 5.12,
    ): ImageNode = mock {
        on { this.type } doReturn type
        on { this.latitude } doReturn latitude
        on { this.longitude } doReturn longitude
    }

    @Test
    fun `test that init loads file node metadata into state`() = runTest {
        val node = mockFileNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)

        initViewModel()
        advanceUntilIdle()

        with(underTest.uiState.value) {
            assertThat(isLoading).isFalse()
            assertThat(title).isEqualTo("file.txt")
            assertThat(isFile).isTrue()
            assertThat(iconRes).isEqualTo(FILE_ICON_RES)
            assertThat(thumbnailData).isEqualTo(ThumbnailRequest(NodeId(NODE_HANDLE)))
            assertThat(fileTypeExtension).isEqualTo("heic")
            assertThat(sizeInBytes).isEqualTo(1024L)
            assertThat(creationTime).isEqualTo(100L)
            assertThat(modificationTime).isEqualTo(200L)
            assertThat(descriptionText).isEqualTo("a description")
            assertThat(tags).containsExactly("marketing", "2026")
            assertThat(isTakenDown).isFalse()
            assertThat(accessPermission).isEqualTo(AccessPermission.OWNER)
        }
    }

    @Test
    fun `test that init sets isFile false and no modification time for a folder`() = runTest {
        val node = mockFolderNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)

        initViewModel()
        advanceUntilIdle()

        with(underTest.uiState.value) {
            assertThat(isFile).isFalse()
            assertThat(title).isEqualTo("folder")
            assertThat(fileTypeExtension).isNull()
            assertThat(thumbnailData).isNull()
            assertThat(iconRes).isNotNull()
            assertThat(sizeInBytes).isEqualTo(0L)
            assertThat(modificationTime).isNull()
            assertThat(descriptionText).isEmpty()
            assertThat(tags).isEmpty()
        }
    }

    @Test
    fun `test that init stops loading when node is not found`() = runTest {
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        with(underTest.uiState.value) {
            assertThat(isLoading).isFalse()
            assertThat(title).isEmpty()
        }
    }

    @Test
    fun `test that rubbish and backups flags are set from use cases`() = runTest {
        val node = mockFileNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(isNodeInRubbishBinUseCase(NodeId(NODE_HANDLE))).thenReturn(true)
        whenever(isNodeInBackupsUseCase(NODE_HANDLE)).thenReturn(true)

        initViewModel()
        advanceUntilIdle()

        with(underTest.uiState.value) {
            assertThat(isNodeInRubbish).isTrue()
            assertThat(isNodeInBackups).isTrue()
        }
    }

    @Test
    fun `test that access permission defaults to UNKNOWN when use case returns null`() = runTest {
        val node = mockFileNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.accessPermission).isEqualTo(AccessPermission.UNKNOWN)
    }

    @Test
    fun `test that a node update re-fetches node info`() = runTest {
        val oldNode = mockFileNode(name = "old.txt")
        val newNode = mockFileNode(name = "new.txt")
        whenever(monitorNodeUpdatesById(NodeId(NODE_HANDLE)))
            .thenReturn(flowOf(listOf(NodeChanges.Name)))
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE)))
            .thenReturn(oldNode)
            .thenReturn(newNode)

        initViewModel()
        advanceUntilIdle()

        verify(getNodeByIdUseCase, times(2)).invoke(NodeId(NODE_HANDLE))
        assertThat(underTest.uiState.value.title).isEqualTo("new.txt")
    }

    @Test
    fun `test that the source type and path folders and destinations are exposed for the location`() =
        runTest {
            val node = mockFileNode()
            val nodeLocation = NodeLocation(
                node = node,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                ancestorIds = listOf(NodeId(10L)),
            )
            val destinations = listOf<NavKey>(mock())
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            whenever(getNodePathByIdUseCase(NodeId(NODE_HANDLE)))
                .thenReturn("/Documents/Marketing/file.txt")
            whenever(getNodeLocationByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(nodeLocation)
            whenever(nodeDestinationMapper(nodeLocation)).thenReturn(destinations)

            initViewModel()
            advanceUntilIdle()

            with(underTest.uiState.value) {
                assertThat(nodeSourceType).isEqualTo(NodeSourceType.CLOUD_DRIVE)
                assertThat(locationFolders).containsExactly("Documents", "Marketing").inOrder()
                assertThat(locationDestinations).isEqualTo(destinations)
            }
        }

    @Test
    fun `test that the location folders are empty when the node sits in the root`() =
        runTest {
            val node = mockFileNode()
            val nodeLocation = NodeLocation(
                node = node,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                ancestorIds = emptyList(),
            )
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            whenever(getNodePathByIdUseCase(NodeId(NODE_HANDLE)))
                .thenReturn("/file.txt")
            whenever(getNodeLocationByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(nodeLocation)

            initViewModel()
            advanceUntilIdle()

            with(underTest.uiState.value) {
                assertThat(nodeSourceType).isEqualTo(NodeSourceType.CLOUD_DRIVE)
                assertThat(locationFolders).isEmpty()
            }
        }

    @Test
    fun `test that the owner email prefix is stripped from the incoming share location folders`() =
        runTest {
            val node = mockFileNode()
            val nodeLocation = NodeLocation(
                node = node,
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
                ancestorIds = listOf(NodeId(10L)),
            )
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            whenever(getNodePathByIdUseCase(NodeId(NODE_HANDLE)))
                .thenReturn("bob@mega.co.nz:Marketing/2026/file.txt")
            whenever(getNodeLocationByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(nodeLocation)

            initViewModel()
            advanceUntilIdle()

            with(underTest.uiState.value) {
                assertThat(nodeSourceType).isEqualTo(NodeSourceType.INCOMING_SHARES)
                assertThat(locationFolders).containsExactly("Marketing", "2026").inOrder()
            }
        }

    @Test
    fun `test that the source type is null when the location cannot be resolved`() =
        runTest {
            val node = mockFileNode()
            whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
            whenever(getNodePathByIdUseCase(NodeId(NODE_HANDLE))).thenReturn("")
            whenever(getNodeLocationByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(null)

            initViewModel()
            advanceUntilIdle()

            with(underTest.uiState.value) {
                assertThat(nodeSourceType).isNull()
                assertThat(locationFolders).isEmpty()
                assertThat(locationDestinations).isNull()
            }
        }

    @Test
    fun `test that map location is shown for a geo-tagged image owned by the user`() = runTest {
        val node = mockFileNode()
        val imageNode = mockImageNode(latitude = 52.09, longitude = 5.12)
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)
        whenever(getImageNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(imageNode)

        initViewModel()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.mapCoordinates)
            .isEqualTo(Coordinates(latitude = 52.09, longitude = 5.12))
    }

    @Test
    fun `test that the location caption is resolved from the coordinates`() = runTest {
        val node = mockFileNode()
        val imageNode = mockImageNode(latitude = 52.09, longitude = 5.12)
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)
        whenever(getImageNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(imageNode)
        whenever(getAddressFromCoordinatesUseCase(52.09, 5.12)).thenReturn("Utrecht, Netherlands")

        initViewModel()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.locationCaption).isEqualTo("Utrecht, Netherlands")
    }

    @Test
    fun `test that map location is hidden for a non-media node`() = runTest {
        val node = mockFileNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)
        whenever(getImageNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.mapCoordinates).isNull()
    }

    @Test
    fun `test that map location is shown for a geo-tagged video`() = runTest {
        val node = mockFileNode()
        val videoNode = mockImageNode(
            type = VideoFileTypeInfo(mimeType = "video/mp4", extension = "mp4", duration = 1.seconds),
            latitude = 52.09,
            longitude = 5.12,
        )
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)
        whenever(getImageNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(videoNode)

        initViewModel()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.mapCoordinates)
            .isEqualTo(Coordinates(latitude = 52.09, longitude = 5.12))
    }

    @Test
    fun `test that map location is hidden for an image without coordinates`() = runTest {
        val node = mockFileNode()
        val imageNode = mockImageNode(latitude = 0.0, longitude = 0.0)
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)
        whenever(getImageNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(imageNode)

        initViewModel()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.mapCoordinates).isNull()
    }

    @Test
    fun `test that map location is hidden when only one coordinate is set`() = runTest {
        val node = mockFileNode()
        val imageNode = mockImageNode(latitude = 52.09, longitude = 0.0)
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)
        whenever(getImageNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(imageNode)

        initViewModel()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.mapCoordinates).isNull()
    }

    @Test
    fun `test that map location is hidden for out-of-range coordinates`() = runTest {
        val node = mockFileNode()
        val imageNode = mockImageNode(latitude = 200.0, longitude = 5.12)
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)
        whenever(getImageNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(imageNode)

        initViewModel()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.mapCoordinates).isNull()
    }

    @Test
    fun `test that map location is hidden when the user is not the owner`() = runTest {
        val node = mockFileNode()
        val imageNode = mockImageNode(latitude = 52.09, longitude = 5.12)
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.READ)
        whenever(getImageNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(imageNode)

        initViewModel()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.mapCoordinates).isNull()
    }

    @Test
    fun `test that updateDescription sets the node description`() = runTest {
        val node = mockFileNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)

        initViewModel()
        advanceUntilIdle()
        underTest.updateDescription("a new description")
        advanceUntilIdle()

        verify(setNodeDescriptionUseCase).invoke(NodeId(NODE_HANDLE), "a new description")
    }

    @Test
    fun `test that description is editable for an owner outside rubbish and backups`() = runTest {
        val node = mockFileNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)

        initViewModel()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.canEditDescription).isTrue()
    }

    @Test
    fun `test that description is not editable with read access`() = runTest {
        val node = mockFileNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.READ)

        initViewModel()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.canEditDescription).isFalse()
    }

    @Test
    fun `test that description is not editable when the node is in the rubbish bin`() = runTest {
        val node = mockFileNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)
        whenever(isNodeInRubbishBinUseCase(NodeId(NODE_HANDLE))).thenReturn(true)

        initViewModel()
        advanceUntilIdle()

        assertThat(underTest.uiState.value.canEditDescription).isFalse()
    }

    @Test
    fun `test that init loads the shared contact count for an outgoing share folder`() = runTest {
        val node = mockFolderNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)
        whenever(getNodeOutSharesUseCase(NodeId(NODE_HANDLE))).thenReturn(List(3) { mock() })

        initViewModel()
        advanceUntilIdle()

        with(underTest.uiState.value) {
            assertThat(sharedContactCount).isEqualTo(3)
            assertThat(isOutgoingShare).isTrue()
        }
    }

    @Test
    fun `test that a folder with no out shares is not an outgoing share`() = runTest {
        val node = mockFolderNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)
        whenever(getNodeOutSharesUseCase(NodeId(NODE_HANDLE))).thenReturn(emptyList())

        initViewModel()
        advanceUntilIdle()

        with(underTest.uiState.value) {
            assertThat(sharedContactCount).isEqualTo(0)
            assertThat(isOutgoingShare).isFalse()
        }
    }

    @Test
    fun `test that init loads the owner for an incoming share folder`() = runTest {
        val node = mockFolderNode()
        val contactData = mock<ContactData> {
            on { alias } doReturn null
            on { fullName } doReturn "John Doe"
        }
        val owner = mock<ContactItem> {
            on { email } doReturn "owner@mail.com"
            on { this.contactData } doReturn contactData
        }
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.FULL)
        whenever(getContactItemFromInShareFolder(any(), any())).thenReturn(owner)

        initViewModel()
        advanceUntilIdle()

        with(underTest.uiState.value) {
            assertThat(ownerName).isEqualTo("John Doe")
            assertThat(ownerEmail).isEqualTo("owner@mail.com")
            assertThat(isIncomingShare).isTrue()
        }
    }

    @Test
    fun `test that a folder that is not an incoming share has no owner`() = runTest {
        val node = mockFolderNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)
        whenever(getContactItemFromInShareFolder(any(), any())).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        with(underTest.uiState.value) {
            assertThat(ownerEmail).isNull()
            assertThat(isIncomingShare).isFalse()
        }
    }

    @Test
    fun `test that init loads folder stats for a folder`() = runTest {
        val node = mockFolderNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)
        whenever(getFolderTreeInfo(node)).thenReturn(
            FolderTreeInfo(
                numberOfFiles = 3223,
                numberOfFolders = 540,
                totalCurrentSizeInBytes = 22_800L,
                numberOfVersions = 91,
                sizeOfPreviousVersionsInBytes = 1_260L,
            )
        )

        initViewModel()
        advanceUntilIdle()

        with(underTest.uiState.value) {
            assertThat(sizeInBytes).isEqualTo(22_800L)
            assertThat(numberOfFiles).isEqualTo(3223)
            assertThat(numberOfFolders).isEqualTo(540)
            assertThat(numberOfVersions).isEqualTo(91)
            assertThat(currentVersionsSizeInBytes).isEqualTo(22_800L)
            assertThat(previousVersionsSizeInBytes).isEqualTo(1_260L)
            assertThat(showFolderVersions).isTrue()
        }
    }

    @Test
    fun `test that init loads the version count for a file with versions`() = runTest {
        val node = mockFileNode(versionCount = 2)
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)

        initViewModel()
        advanceUntilIdle()

        with(underTest.uiState.value) {
            assertThat(versionCount).isEqualTo(2)
            assertThat(showFileVersions).isTrue()
        }
    }

    @Test
    fun `test that a file does not load folder versions`() = runTest {
        val node = mockFileNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)

        initViewModel()
        advanceUntilIdle()

        with(underTest.uiState.value) {
            assertThat(numberOfVersions).isEqualTo(0)
            assertThat(showFolderVersions).isFalse()
        }
    }

    @Test
    fun `test that a video node loads its duration and full-resolution preview`() = runTest {
        val videoType = VideoFileTypeInfo(mimeType = "video/mp4", extension = "mov", duration = 84.seconds)
        val node = mockFileNode(type = videoType)
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)
        val previewFile = File("/cache/preview.jpg")
        whenever(durationInSecondsTextMapper(84.seconds)).thenReturn("1:24")
        whenever(getPreviewUseCase(node)).thenReturn(previewFile)

        initViewModel()
        advanceUntilIdle()

        with(underTest.uiState.value) {
            assertThat(durationText).isEqualTo("1:24")
            assertThat(thumbnailData)
                .isEqualTo(ThumbnailUriRequest(UriPath.fromFile(previewFile)))
            assertThat(isMediaFile).isTrue()
        }
    }

    @Test
    fun `test that a non-media file has no duration and keeps its thumbnail`() = runTest {
        val node = mockFileNode()
        whenever(getNodeByIdUseCase(NodeId(NODE_HANDLE))).thenReturn(node)
        whenever(getNodeAccessPermission(NodeId(NODE_HANDLE))).thenReturn(AccessPermission.OWNER)

        initViewModel()
        advanceUntilIdle()

        with(underTest.uiState.value) {
            assertThat(durationText).isNull()
            assertThat(thumbnailData).isEqualTo(ThumbnailRequest(NodeId(NODE_HANDLE)))
            assertThat(isMediaFile).isFalse()
        }
        verifyNoInteractions(getPreviewUseCase)
    }

    private companion object {
        const val NODE_HANDLE = 99113034474275L
        const val FILE_ICON_RES = 12345
    }
}
