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
        val actual = underTest(pendingTransfer, size, exception)
        assertAll(
            { assertThat(actual.fileName).isEqualTo(fileName) },
            { assertThat(actual.type).isEqualTo(TransferType.DOWNLOAD) },
            { assertThat(actual.state).isEqualTo(TransferState.STATE_FAILED) },
            { assertThat(actual.size).isEqualTo(sizeString) },
            { assertThat(actual.handle).isEqualTo(nodeHandle) },
            { assertThat(actual.path).isEqualTo(fullPath) },
            { assertThat(actual.isOffline).isEqualTo(offline) },
            { assertThat(actual.timestamp).isEqualTo(now) },
            { assertThat(actual.error).isEqualTo(exceptionMessage) },
            { assertThat(actual.originalPath).isEqualTo(fullPath) },
            { assertThat(actual.appData).isEqualTo(appData) }
        )
    }
}
