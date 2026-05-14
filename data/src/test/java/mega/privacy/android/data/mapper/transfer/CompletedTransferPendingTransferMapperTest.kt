package mega.privacy.android.data.mapper.transfer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.wrapper.StringWrapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import mega.privacy.android.domain.entity.uri.UriPath
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompletedTransferPendingTransferMapperTest {
    private lateinit var underTest: CompletedTransferPendingTransferMapper

    private val deviceGateway = mock<DeviceGateway>()
    private val fileGateway = mock<FileGateway>()
    private val stringWrapper = mock<StringWrapper>()

    @BeforeAll
    fun setup() {
        underTest = CompletedTransferPendingTransferMapper(
            deviceGateway,
            fileGateway,
            stringWrapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            deviceGateway,
            fileGateway,
            stringWrapper,
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the completed transfer is mapped with the correct fields from pending transfer`(
        offline: Boolean,
    ) = runTest {
        val nodeHandle = 786L
        val size = 10 * 1024 * 1024L
        val sizeString = "10MB"
        val now = 123L
        val fileName = "file.txt"
        val offlinePath = "/offlinePath/"
        val path = if (offline) offlinePath else "/path/"
        val fullPath = "$path$fileName"
        val appData = listOf(mock<TransferAppData.ChatUpload>())
        val exceptionMessage = "Some Exception"
        val exception = RuntimeException(exceptionMessage)
        val pendingTransfer = mock<PendingTransfer> {
            on { this.uriPath } doReturn UriPath(fullPath)
            on { this.nodeIdentifier } doReturn
                    PendingTransferNodeIdentifier.CloudDriveNode(NodeId(nodeHandle))
            on { this.transferType } doReturn TransferType.DOWNLOAD
            on { this.appData } doReturn appData
            on { this.fileName } doReturn fileName
        }
        whenever(deviceGateway.now) doReturn (now)
        whenever(stringWrapper.getSizeString(size)) doReturn (sizeString)
        whenever(fileGateway.getOfflineFilesRootPath()) doReturn offlinePath
        val uniqueId = 987654321L
        whenever(pendingTransfer.transferUniqueId) doReturn uniqueId
        val actual = underTest(pendingTransfer, size, exception)
        assertAll(
            { assertThat(actual.fileName).isEqualTo(fileName) },
            { assertThat(actual.type).isEqualTo(TransferType.DOWNLOAD) },
            { assertThat(actual.state).isEqualTo(TransferState.STATE_FAILED) },
            { assertThat(actual.size).isEqualTo(sizeString) },
            { assertThat(actual.handle).isEqualTo(nodeHandle) },
            { assertThat(actual.parentHandle).isEqualTo(-1L) },
            { assertThat(actual.path).isEqualTo(fullPath) },
            { assertThat(actual.isOffline).isEqualTo(offline) },
            { assertThat(actual.timestamp).isEqualTo(now) },
            { assertThat(actual.error).isEqualTo(exceptionMessage) },
            { assertThat(actual.originalPath).isEqualTo(fullPath) },
            { assertThat(actual.appData).isEqualTo(appData) },
            { assertThat(actual.uniqueId).isEqualTo(uniqueId) },
            { assertThat(actual.totalBytes).isEqualTo(size) }
        )
    }

    @Test
    fun `test that an upload sets parentHandle from nodeIdentifier and clears handle`() = runTest {
        val parentNodeHandle = 786L
        val pendingTransfer = uploadPendingTransfer(
            parentNodeHandle = parentNodeHandle,
            uriPath = "/path/file.txt",
            fileName = "file.txt",
        )
        whenever(stringWrapper.getSizeString(0L)) doReturn "0B"

        val actual = underTest(pendingTransfer, 0L, RuntimeException())

        assertAll(
            { assertThat(actual.parentHandle).isEqualTo(parentNodeHandle) },
            { assertThat(actual.handle).isEqualTo(-1L) },
        )
    }

    @Test
    fun `test that fileName falls back to FileGateway when pending transfer fileName is null`() =
        runTest {
            val uriPath = "content://media/external/images/media/12345"
            val resolvedName = "IMG_20260310.jpg"
            val pendingTransfer = uploadPendingTransfer(
                parentNodeHandle = 1L,
                uriPath = uriPath,
                fileName = null,
            )
            whenever(stringWrapper.getSizeString(0L)) doReturn "0B"
            whenever(fileGateway.getFileNameFromUri(uriPath)) doReturn resolvedName

            val actual = underTest(pendingTransfer, 0L, RuntimeException())

            assertThat(actual.fileName).isEqualTo(resolvedName)
        }

    @Test
    fun `test that fileName falls back to uriPath basename when FileGateway returns null`() =
        runTest {
            val uriPath = "/storage/emulated/0/Pictures/photo.png"
            val pendingTransfer = uploadPendingTransfer(
                parentNodeHandle = 1L,
                uriPath = uriPath,
                fileName = null,
            )
            whenever(stringWrapper.getSizeString(0L)) doReturn "0B"
            whenever(fileGateway.getFileNameFromUri(uriPath)) doReturn null

            val actual = underTest(pendingTransfer, 0L, RuntimeException())

            assertThat(actual.fileName).isEqualTo("photo.png")
        }

    private fun uploadPendingTransfer(
        parentNodeHandle: Long,
        uriPath: String,
        fileName: String?,
    ) = mock<PendingTransfer> {
        on { this.uriPath } doReturn UriPath(uriPath)
        on { this.nodeIdentifier } doReturn
                PendingTransferNodeIdentifier.CloudDriveNode(NodeId(parentNodeHandle))
        on { this.transferType } doReturn TransferType.GENERAL_UPLOAD
        on { this.appData } doReturn emptyList()
        on { this.fileName } doReturn fileName
    }
}
