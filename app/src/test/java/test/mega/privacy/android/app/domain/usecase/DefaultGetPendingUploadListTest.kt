package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultGetPendingUploadList
import mega.privacy.android.app.domain.usecase.GetFingerprint
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetNodeFromCloud
import mega.privacy.android.app.domain.usecase.GetParentMegaNode
import mega.privacy.android.app.domain.usecase.GetPendingUploadList
import mega.privacy.android.app.domain.usecase.GetPrimarySyncHandle
import mega.privacy.android.app.domain.usecase.GetSecondarySyncHandle
import mega.privacy.android.data.mapper.SyncRecordTypeIntMapper
import mega.privacy.android.domain.entity.CameraUploadMedia
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.usecase.GetGPSCoordinates
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.MediaLocalPathExists
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import mega.privacy.android.domain.usecase.UpdateCameraUploadTimeStamp
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.util.LinkedList

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetPendingUploadListTest {

    private lateinit var underTest: GetPendingUploadList

    private val getNodeFromCloud: GetNodeFromCloud = mock()
    private val getNodeByHandle: GetNodeByHandle = mock()
    private val getParentMegaNode: GetParentMegaNode = mock()
    private val getPrimarySyncHandle: GetPrimarySyncHandle = mock()
    private val getSecondarySyncHandle: GetSecondarySyncHandle = mock()
    private val updateTimeStamp: UpdateCameraUploadTimeStamp = mock()
    private val getFingerprint: GetFingerprint = mock()
    private val mediaLocalPathExists: MediaLocalPathExists = mock()
    private val shouldCompressVideo: ShouldCompressVideo = mock()
    private val getGPSCoordinates: GetGPSCoordinates = mock()
    private val syncRecordTypeIntMapper: SyncRecordTypeIntMapper = mock()
    private val isNodeInRubbishBin: IsNodeInRubbish = mock()

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
        1,
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
        1,
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
        2,
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
        2,
        null,
        isCopyOnly = false,
        isSecondary = true
    )

    @Before
    fun setUp() {
        underTest = DefaultGetPendingUploadList(
            getNodeFromCloud,
            getNodeByHandle,
            getParentMegaNode,
            getPrimarySyncHandle,
            getSecondarySyncHandle,
            updateTimeStamp,
            getFingerprint,
            mediaLocalPathExists,
            shouldCompressVideo,
            getGPSCoordinates,
            syncRecordTypeIntMapper,
            isNodeInRubbishBin
        )
    }

    @Test
    fun `test that correct sync record list is returned if node does not exist for primary photo media`() =
        runTest {
            val result = mock<MegaNode> {}
            whenever(getFingerprint(any())).thenReturn("local fingerprint")
            whenever(getPrimarySyncHandle()).thenReturn(1L)
            whenever(getSecondarySyncHandle()).thenReturn(1L)
            whenever(shouldCompressVideo()).thenReturn(false)
            whenever(getNodeByHandle(any())).thenReturn(result)
            whenever(getNodeFromCloud(any(), any())).thenReturn(null)
            whenever(mediaLocalPathExists(any(), any())).thenReturn(false)
            whenever(getGPSCoordinates(any(), any())).thenReturn(Pair(0F, 1F))
            whenever(syncRecordTypeIntMapper(any())).thenReturn(1)
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
            val result = mock<MegaNode> {}
            whenever(getFingerprint(any())).thenReturn("local fingerprint")
            whenever(getPrimarySyncHandle()).thenReturn(1L)
            whenever(getSecondarySyncHandle()).thenReturn(1L)
            whenever(shouldCompressVideo()).thenReturn(false)
            whenever(getNodeByHandle(any())).thenReturn(result)
            whenever(getNodeFromCloud(any(), any())).thenReturn(null)
            whenever(mediaLocalPathExists(any(), any())).thenReturn(false)
            whenever(getGPSCoordinates(any(), any())).thenReturn(Pair(0F, 1F))
            whenever(syncRecordTypeIntMapper(any())).thenReturn(1)
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
            val result = mock<MegaNode> {}
            whenever(getFingerprint(any())).thenReturn("local fingerprint")
            whenever(getPrimarySyncHandle()).thenReturn(1L)
            whenever(getSecondarySyncHandle()).thenReturn(1L)
            whenever(shouldCompressVideo()).thenReturn(true)
            whenever(getNodeByHandle(any())).thenReturn(result)
            whenever(getNodeFromCloud(any(), any())).thenReturn(null)
            whenever(mediaLocalPathExists(any(), any())).thenReturn(false)
            whenever(getGPSCoordinates(any(), any())).thenReturn(Pair(0F, 1F))
            whenever(syncRecordTypeIntMapper(any())).thenReturn(2)
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
            val result = mock<MegaNode> {}
            whenever(getFingerprint(any())).thenReturn("local fingerprint")
            whenever(getPrimarySyncHandle()).thenReturn(1L)
            whenever(getSecondarySyncHandle()).thenReturn(1L)
            whenever(shouldCompressVideo()).thenReturn(false)
            whenever(getNodeByHandle(any())).thenReturn(result)
            whenever(getNodeFromCloud(any(), any())).thenReturn(null)
            whenever(mediaLocalPathExists(any(), any())).thenReturn(false)
            whenever(getGPSCoordinates(any(), any())).thenReturn(Pair(0F, 1F))
            whenever(syncRecordTypeIntMapper(any())).thenReturn(2)
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
            val result = mock<MegaNode> {}
            whenever(result.handle).thenReturn(1L)
            whenever(getFingerprint(any())).thenReturn("local fingerprint")
            whenever(getPrimarySyncHandle()).thenReturn(1L)
            whenever(getSecondarySyncHandle()).thenReturn(1L)
            whenever(shouldCompressVideo()).thenReturn(false)
            whenever(getNodeByHandle(any())).thenReturn(result)
            whenever(getNodeFromCloud(any(), any())).thenReturn(result)
            whenever(mediaLocalPathExists(any(), any())).thenReturn(false)
            whenever(getParentMegaNode(any())).thenReturn(result)
            whenever(getGPSCoordinates(any(), any())).thenReturn(Pair(0F, 1F))
            whenever(isNodeInRubbishBin(any())).thenReturn(false)
            val queue = LinkedList<CameraUploadMedia>()
            queue.add(uploadMedia)
            assertThat(underTest(queue, isSecondary = false, isVideo = false)).isEqualTo(
                emptyList<SyncRecord>()
            )
        }

    @Test
    fun `test that empty sync record list is returned if node is in rubbish bin`() =
        runTest {
            val result = mock<MegaNode> {}
            whenever(result.handle).thenReturn(1L)
            whenever(getFingerprint(any())).thenReturn("local fingerprint")
            whenever(getPrimarySyncHandle()).thenReturn(1L)
            whenever(getSecondarySyncHandle()).thenReturn(1L)
            whenever(shouldCompressVideo()).thenReturn(false)
            whenever(getNodeByHandle(any())).thenReturn(result)
            whenever(getNodeFromCloud(any(), any())).thenReturn(result)
            whenever(mediaLocalPathExists(any(), any())).thenReturn(false)
            whenever(getParentMegaNode(any())).thenReturn(result)
            whenever(getGPSCoordinates(any(), any())).thenReturn(Pair(0F, 1F))
            whenever(isNodeInRubbishBin(any())).thenReturn(true)
            val queue = LinkedList<CameraUploadMedia>()
            queue.add(uploadMedia)
            assertThat(underTest(queue, isSecondary = false, isVideo = false)).isEqualTo(
                emptyList<SyncRecord>()
            )
        }
}
