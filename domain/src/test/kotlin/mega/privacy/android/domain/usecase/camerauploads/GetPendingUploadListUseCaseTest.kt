package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.CameraUploadMedia
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.MediaLocalPathExists
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import mega.privacy.android.domain.usecase.file.GetGPSCoordinatesUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.util.LinkedList

@OptIn(ExperimentalCoroutinesApi::class)
class GetPendingUploadListUseCaseTest {

    private lateinit var underTest: GetPendingUploadListUseCase

    private val getNodeFromCloudUseCase: GetNodeFromCloudUseCase = mock()
    private val getParentNodeUseCase: GetParentNodeUseCase = mock()
    private val getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase = mock()
    private val getSecondarySyncHandleUseCase: GetSecondarySyncHandleUseCase = mock()
    private val getFingerprintUseCase: GetFingerprintUseCase = mock()
    private val mediaLocalPathExists: MediaLocalPathExists = mock()
    private val shouldCompressVideo: ShouldCompressVideo = mock()
    private val getGPSCoordinatesUseCase: GetGPSCoordinatesUseCase = mock()
    private val isNodeInRubbishBin: IsNodeInRubbish = mock()
    private val getNodeGPSCoordinatesUseCase: GetNodeGPSCoordinatesUseCase = mock()

    private val uploadMedia = CameraUploadMedia("", 0)

    private val primaryPhoto = SyncRecord(
        0,
        File("").absolutePath,
        null,
        "local fingerprint",
        null,
        0,
        File("").name,
        1F,
        0F,
        SyncStatus.STATUS_PENDING.value,
        SyncRecordType.TYPE_PHOTO,
        null,
        isCopyOnly = false,
        isSecondary = false
    )

    private val secondaryPhoto = SyncRecord(
        0,
        File("").absolutePath,
        null,
        "local fingerprint",
        null,
        0,
        File("").name,
        1F,
        0F,
        SyncStatus.STATUS_PENDING.value,
        SyncRecordType.TYPE_PHOTO,
        null,
        isCopyOnly = false,
        isSecondary = true
    )

    private val compressPrimaryVideo = SyncRecord(
        0,
        File("").absolutePath,
        null,
        "local fingerprint",
        null,
        0,
        File("").name,
        1F,
        0F,
        SyncStatus.STATUS_TO_COMPRESS.value,
        SyncRecordType.TYPE_VIDEO,
        null,
        isCopyOnly = false,
        isSecondary = false
    )

    private val secondaryVideo = SyncRecord(
        0,
        File("").absolutePath,
        null,
        "local fingerprint",
        null,
        0,
        File("").name,
        1F,
        0F,
        SyncStatus.STATUS_PENDING.value,
        SyncRecordType.TYPE_VIDEO,
        null,
        isCopyOnly = false,
        isSecondary = true
    )

    @Before
    fun setUp() {
        underTest = GetPendingUploadListUseCase(
            getNodeFromCloudUseCase,
            getParentNodeUseCase,
            getPrimarySyncHandleUseCase,
            getSecondarySyncHandleUseCase,
            getFingerprintUseCase,
            mediaLocalPathExists,
            shouldCompressVideo,
            getGPSCoordinatesUseCase,
            isNodeInRubbishBin,
            getNodeGPSCoordinatesUseCase
        )
    }

    @Test
    fun `test that correct sync record list is returned if node does not exist for primary photo media`() =
        runTest {
            whenever(getFingerprintUseCase(any())).thenReturn("local fingerprint")
            whenever(getPrimarySyncHandleUseCase()).thenReturn(1L)
            whenever(getSecondarySyncHandleUseCase()).thenReturn(1L)
            whenever(shouldCompressVideo()).thenReturn(false)
            whenever(getNodeFromCloudUseCase("", NodeId(1L))).thenReturn(null)
            whenever(mediaLocalPathExists(any(), any())).thenReturn(false)
            whenever(getGPSCoordinatesUseCase(any(), any())).thenReturn(Pair(0F, 1F))
            whenever(getNodeGPSCoordinatesUseCase(NodeId(1L))).thenReturn(Pair(0.0, 0.0))
            val queue = LinkedList<CameraUploadMedia>()
            queue.add(uploadMedia)
            assertThat(underTest(queue, isSecondary = false, isVideo = false)).isEqualTo(
                listOf(
                    primaryPhoto
                )
            )
        }

    @Test
    fun `test that correct sync record list is returned if node does not exist for secondary photo media`() =
        runTest {
            val handle = 1234L
            val node = mock<FileNode> {
                on { this.id }.thenReturn(NodeId(handle))
            }
            whenever(getFingerprintUseCase(any())).thenReturn("local fingerprint")
            whenever(getPrimarySyncHandleUseCase()).thenReturn(1L)
            whenever(getSecondarySyncHandleUseCase()).thenReturn(1L)
            whenever(shouldCompressVideo()).thenReturn(false)
            whenever(getNodeFromCloudUseCase("", NodeId(1L))).thenReturn(null)
            whenever(getParentNodeUseCase(NodeId(handle))).thenReturn(node)
            whenever(mediaLocalPathExists(any(), any())).thenReturn(false)
            whenever(getGPSCoordinatesUseCase(any(), any())).thenReturn(Pair(0F, 1F))
            whenever(getNodeGPSCoordinatesUseCase(NodeId(1L))).thenReturn(Pair(0.0, 0.0))
            val queue = LinkedList<CameraUploadMedia>()
            queue.add(uploadMedia)
            assertThat(underTest(queue, isSecondary = true, isVideo = false)).isEqualTo(
                listOf(
                    secondaryPhoto
                )
            )
        }

    @Test
    fun `test that correct sync record list is returned if node does not exist for primary video media which needs to be compressed`() =
        runTest {
            val handle = 1234L
            val node = mock<FileNode> {
                on { this.id }.thenReturn(NodeId(handle))
            }
            whenever(getFingerprintUseCase(any())).thenReturn("local fingerprint")
            whenever(getPrimarySyncHandleUseCase()).thenReturn(1L)
            whenever(getSecondarySyncHandleUseCase()).thenReturn(1L)
            whenever(shouldCompressVideo()).thenReturn(true)
            whenever(getNodeFromCloudUseCase("", NodeId(1L))).thenReturn(null)
            whenever(mediaLocalPathExists(any(), any())).thenReturn(false)
            whenever(getParentNodeUseCase(NodeId(handle))).thenReturn(node)
            whenever(getGPSCoordinatesUseCase(any(), any())).thenReturn(Pair(0F, 1F))
            whenever(getNodeGPSCoordinatesUseCase(NodeId(1L))).thenReturn(Pair(0.0, 0.0))
            val queue = LinkedList<CameraUploadMedia>()
            queue.add(uploadMedia)
            assertThat(underTest(queue, isSecondary = false, isVideo = true)).isEqualTo(
                listOf(
                    compressPrimaryVideo
                )
            )
        }

    @Test
    fun `test that correct sync record list is returned if node does not exist for secondary video media which does not need to be compressed`() =
        runTest {
            whenever(getFingerprintUseCase(any())).thenReturn("local fingerprint")
            whenever(getPrimarySyncHandleUseCase()).thenReturn(1L)
            whenever(getSecondarySyncHandleUseCase()).thenReturn(1L)
            whenever(shouldCompressVideo()).thenReturn(false)
            whenever(getNodeFromCloudUseCase("", NodeId(1L))).thenReturn(null)
            whenever(mediaLocalPathExists(any(), any())).thenReturn(false)
            whenever(getGPSCoordinatesUseCase(any(), any())).thenReturn(Pair(0F, 1F))
            whenever(getNodeGPSCoordinatesUseCase(NodeId(1L))).thenReturn(Pair(0.0, 0.0))
            val queue = LinkedList<CameraUploadMedia>()
            queue.add(uploadMedia)
            assertThat(underTest(queue, isSecondary = true, isVideo = true)).isEqualTo(
                listOf(
                    secondaryVideo
                )
            )
        }

    @Test
    fun `test that empty sync record list is returned if node exists and parent node already exists for primary photo media`() =
        runTest {
            val handle = 1234L
            val node = mock<TypedFileNode> {
                on { this.id }.thenReturn(NodeId(handle))
            }
            whenever(getFingerprintUseCase(any())).thenReturn("local fingerprint")
            whenever(getPrimarySyncHandleUseCase()).thenReturn(1L)
            whenever(getSecondarySyncHandleUseCase()).thenReturn(1L)
            whenever(shouldCompressVideo()).thenReturn(false)
            whenever(getNodeFromCloudUseCase("", NodeId(1L))).thenReturn(node)
            whenever(mediaLocalPathExists(any(), any())).thenReturn(false)
            whenever(getParentNodeUseCase(NodeId(handle))).thenReturn(node)
            whenever(getGPSCoordinatesUseCase(any(), any())).thenReturn(Pair(0F, 1F))
            whenever(isNodeInRubbishBin(any())).thenReturn(false)
            whenever(getNodeGPSCoordinatesUseCase(NodeId(1L))).thenReturn(Pair(0.0, 0.0))
            val queue = LinkedList<CameraUploadMedia>()
            assertThat(underTest(queue, isSecondary = false, isVideo = false)).isEqualTo(
                emptyList<SyncRecord>()
            )
        }

    @Test
    fun `test that empty sync record list is returned if node is in rubbish bin`() =
        runTest {
            val handle = 1234L
            val node = mock<TypedFileNode> {
                on { this.id }.thenReturn(NodeId(handle))
            }
            whenever(getFingerprintUseCase(any())).thenReturn("local fingerprint")
            whenever(getPrimarySyncHandleUseCase()).thenReturn(1L)
            whenever(getSecondarySyncHandleUseCase()).thenReturn(1L)
            whenever(shouldCompressVideo()).thenReturn(false)
            whenever(getNodeFromCloudUseCase("", NodeId(1L))).thenReturn(node)
            whenever(mediaLocalPathExists(any(), any())).thenReturn(false)
            whenever(getParentNodeUseCase(NodeId(handle))).thenReturn(node)
            whenever(getGPSCoordinatesUseCase(any(), any())).thenReturn(Pair(0F, 1F))
            whenever(getNodeGPSCoordinatesUseCase(NodeId(1L))).thenReturn(Pair(0.0, 0.0))
            whenever(isNodeInRubbishBin(any())).thenReturn(true)
            val queue = LinkedList<CameraUploadMedia>()
            assertThat(underTest(queue, isSecondary = false, isVideo = false)).isEqualTo(
                emptyList<SyncRecord>()
            )
        }
}
