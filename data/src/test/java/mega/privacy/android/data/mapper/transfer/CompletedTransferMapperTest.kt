package mega.privacy.android.data.mapper.transfer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.wrapper.StringWrapper
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.SettingNotFoundException
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaTransfer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.math.BigInteger
import java.util.stream.Stream
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class CompletedTransferMapperTest {
    private lateinit var underTest: CompletedTransferMapper

    private val megaApiGateway: MegaApiGateway = mock()
    private val deviceGateway: DeviceGateway = mock()
    private val fileGateway: FileGateway = mock()
    private val stringWrapper: StringWrapper = mock()
    private val transferTypeIntMapper: TransferTypeIntMapper = mock()
    private val transferStateIntMapper: TransferStateIntMapper = mock()

    @BeforeAll
    fun setup() {
        underTest = CompletedTransferMapper(
            megaApiGateway = megaApiGateway,
            deviceGateway = deviceGateway,
            fileGateway = fileGateway,
            stringWrapper = stringWrapper,
            transferTypeIntMapper = transferTypeIntMapper,
            transferStateIntMapper = transferStateIntMapper,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiGateway,
            deviceGateway,
            fileGateway,
            stringWrapper,
            transferTypeIntMapper,
            transferStateIntMapper,
        )
    }

    @Test
    fun `test that completed transfer mapper maps correctly`() = runTest {
        val transfer = mockTransfer()
        val size = "10MB"
        val now = 123L
        whenever(deviceGateway.now).thenReturn(now)
        whenever(stringWrapper.getSizeString(any())).thenReturn(size)
        whenever(transferTypeIntMapper(TransferType.GENERAL_UPLOAD)).thenReturn(MegaTransfer.TYPE_UPLOAD)
        whenever(transferStateIntMapper(TransferState.STATE_COMPLETED)).thenReturn(MegaTransfer.STATE_COMPLETED)
        val actual = underTest(transfer, null)
        assertThat(actual.fileName).isEqualTo(transfer.fileName)
        assertThat(actual.type).isEqualTo(MegaTransfer.TYPE_UPLOAD)
        assertThat(actual.state).isEqualTo(MegaTransfer.STATE_COMPLETED)
        assertThat(actual.size).isEqualTo(size)
        assertThat(actual.handle).isEqualTo(transfer.nodeHandle)
        assertThat(actual.timestamp).isEqualTo(now)
        assertThat(actual.originalPath).isEqualTo(transfer.localPath)
        assertThat(actual.parentHandle).isEqualTo(transfer.parentHandle)
    }

    @ParameterizedTest(name = "when exception is {0}")
    @MethodSource("provideExceptionParams")
    fun `test that completed transfer mapper maps correctly when there is an exception`(exception: MegaException) =
        runTest {
            val transfer = mockTransfer()
            val size = "10MB"
            val error = "Error Storage Quota"
            whenever(stringWrapper.getSizeString(any())).thenReturn(size)
            whenever(stringWrapper.getErrorStorageQuota()).thenReturn(error)
            whenever(stringWrapper.getErrorStringResource(exception)).thenReturn(error)
            val actual = underTest(transfer, exception)
            assertThat(actual.error).isEqualTo(error)
        }

    @ParameterizedTest(name = "invoked with path {0} and mapped to {1}")
    @MethodSource("provideDownloadParams")
    fun `test that offline download transfer is mapped correctly when invoked`(
        path: String,
        expected: String,
    ) =
        runTest {
            val directoryPath = "/data/user/0/mega.privacy.android.app/files"
            val offlineDirectory = "MEGA Offline"
            val transfer = mockTransfer(
                transferType = TransferType.DOWNLOAD,
                parentPath = "$directoryPath/$offlineDirectory/$path/"
            )
            val mock1 = mock<MegaNode> {
                on { handle }.thenReturn(1L)
            }
            val mock2 = mock<MegaNode> {
                on { handle }.thenReturn(2L)
            }
            val mock3 = mock<MegaNode> {
                on { handle }.thenReturn(3L)
            }
            whenever(megaApiGateway.getParentNode(mock1)).thenReturn(mock2)
            whenever(megaApiGateway.getParentNode(mock2)).thenReturn(mock3)
            whenever(megaApiGateway.getParentNode(mock3)).thenReturn(null)
            val nodes = listOf(mock1, mock2, mock3)
            whenever(stringWrapper.getSizeString(any())).thenReturn("10MB")
            whenever(megaApiGateway.getMegaNodeByHandle(nodes.first().handle)).thenReturn(nodes.first())
            whenever(fileGateway.getOfflineFilesBackupsRootPath()).thenReturn("$directoryPath/$offlineDirectory/in")
            whenever(fileGateway.getOfflineFilesRootPath()).thenReturn("$directoryPath/$offlineDirectory")
            whenever(fileGateway.getAbsolutePath("$directoryPath/$offlineDirectory/3.jpg")).thenReturn(
                "$directoryPath/$offlineDirectory/3.jpg"
            )
            whenever(stringWrapper.getSavedForOfflineNew()).thenReturn("Offline")
            val actual = underTest(transfer, null)
            assertThat(actual.path).isEqualTo(expected)
        }

    @Test
    fun `test that download transfer is mapped correctly when invoked`() =
        runTest {
            val directoryPath = "/storage/emulated/0/Download/Mega Downloads"
            val offlineDirectoryPath = "/data/user/0/mega.privacy.android.app/files"
            val offlineDirectory = "MEGA Offline"
            val transfer = mockTransfer(
                transferType = TransferType.DOWNLOAD,
                parentPath = directoryPath
            )
            whenever(stringWrapper.getSizeString(any())).thenReturn("10MB")
            whenever(fileGateway.getOfflineFilesRootPath()).thenReturn("$offlineDirectoryPath/$offlineDirectory")
            val actual = underTest(transfer, null)
            assertThat(actual.type).isEqualTo(MegaTransfer.TYPE_DOWNLOAD)
            assertThat(actual.path).isEqualTo(directoryPath)
        }

    @ParameterizedTest(name = "invoked with path {0} and isInShare {1}")
    @MethodSource("provideUploadParams")
    fun `test that upload transfer is mapped correctly when invoked`(
        path: String,
        isInShare: Boolean,
        rootNodeHandle: Long,
        section: String,
        expected: String,
    ) =
        runTest {
            val transfer = mockTransfer(
                transferType = TransferType.GENERAL_UPLOAD,
            )
            val node1 = mock<MegaNode> {
                on { handle }.thenReturn(4)
                on { it.isInShare }.thenReturn(isInShare)
            }
            val node2 = mock<MegaNode> {
                on { handle }.thenReturn(2L)
            }
            val node3 = mock<MegaNode> {
                on { handle }.thenReturn(rootNodeHandle)
            }
            whenever(stringWrapper.getSizeString(any())).thenReturn("10MB")
            whenever(transferTypeIntMapper(TransferType.GENERAL_UPLOAD))
                .thenReturn(MegaTransfer.TYPE_UPLOAD)

            whenever(megaApiGateway.getMegaNodeByHandle(transfer.parentHandle)).thenReturn(node1)
            whenever(megaApiGateway.getNodePath(node1)).thenReturn(path)

            whenever(megaApiGateway.getParentNode(node1)).thenReturn(node2)
            whenever(megaApiGateway.getParentNode(node2)).thenReturn(node3)
            whenever(megaApiGateway.getParentNode(node3)).thenReturn(null)
            val rootNode = mock<MegaNode> {
                on { handle }.thenReturn(4L)
            }
            whenever(megaApiGateway.getRootNode()).thenReturn(rootNode)
            val rubbishBinNode = mock<MegaNode> {
                on { handle }.thenReturn(5L)
            }
            whenever(megaApiGateway.getRubbishBinNode()).thenReturn(rubbishBinNode)

            whenever(stringWrapper.getCloudDriveSection()).thenReturn("Cloud drive")
            whenever(stringWrapper.getRubbishBinSection()).thenReturn("Rubbish bin")
            whenever(stringWrapper.getTitleIncomingSharesExplorer()).thenReturn("Incoming shares")

            val actual = underTest(transfer, null)
            assertThat(actual.type).isEqualTo(MegaTransfer.TYPE_UPLOAD)
            assertThat(actual.path).isEqualTo(expected)
        }

    private fun provideDownloadParams() =
        Stream.of(
            Arguments.of("Camera Uploads", "Offline/Camera Uploads"),
            Arguments.of("Media Uploads", "Offline/Media Uploads"),
            Arguments.of("in", "Offline"),
        )

    private fun provideExceptionParams() =
        Stream.of(
            Arguments.of(QuotaExceededMegaException(errorCode = 1)),
            Arguments.of(SettingNotFoundException(errorCode = 2)),
        )

    private fun provideUploadParams() =
        Stream.of(
            Arguments.of("/Camera Uploads", false, 4L, "Cloud drive", "Cloud drive/Camera Uploads"),
            Arguments.of("/Rubbishbin/", false, 5L, "Rubbish bin", "Rubbish bin/Rubbish"),
            Arguments.of(
                "/:incoming shared",
                true,
                6L,
                "Incoming shares",
                "Incoming shares/incoming shared"
            ),
            Arguments.of("/Other/", false, 6L, "Other", ""),
        )

    private fun mockTransfer(
        transferType: TransferType? = null,
        parentPath: String? = null,
    ): Transfer {
        return mock {
            on { it.transferType }.thenReturn(transferType ?: TransferType.GENERAL_UPLOAD)
            on { it.transferredBytes }.thenReturn(Random.nextLong())
            on { it.totalBytes }.thenReturn(Random.nextLong())
            on { it.localPath }.thenReturn("/path/to/local")
            on { it.parentPath }.thenReturn(parentPath ?: "parentPath")
            on { it.nodeHandle }.thenReturn(1L)
            on { it.parentHandle }.thenReturn(4)
            on { it.fileName }.thenReturn("myFileName")
            on { it.stage }.thenReturn(TransferStage.STAGE_SCANNING)
            on { it.tag }.thenReturn(Random.nextInt())
            on { it.speed }.thenReturn(Random.nextLong())
            on { it.isForeignOverQuota }.thenReturn(true)
            on { it.isStreamingTransfer }.thenReturn(true)
            on { it.isFinished }.thenReturn(Random.nextBoolean())
            on { it.isFolderTransfer }.thenReturn(Random.nextBoolean())
            on { it.transferAppData }.thenReturn(listOf(TransferAppData.CameraUpload))
            on { it.state }.thenReturn(TransferState.STATE_COMPLETED)
            on { it.priority }.thenReturn(BigInteger.ONE)
            on { it.notificationNumber }.thenReturn(Random.nextLong())
        }
    }
}
