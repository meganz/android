package mega.privacy.android.data.repository

import androidx.work.WorkInfo
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.SDCardGateway
import mega.privacy.android.data.gateway.TransfersPreferencesGateway
import mega.privacy.android.data.gateway.WorkManagerGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import mega.privacy.android.data.mapper.node.MegaNodeMapper
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants
import mega.privacy.android.data.mapper.transfer.CompletedTransferMapper
import mega.privacy.android.data.mapper.transfer.CompletedTransferPendingTransferMapper
import mega.privacy.android.data.mapper.transfer.InProgressTransferMapper
import mega.privacy.android.data.mapper.transfer.PausedTransferEventMapper
import mega.privacy.android.data.mapper.transfer.TransferAppDataStringMapper
import mega.privacy.android.data.mapper.transfer.TransferEventMapper
import mega.privacy.android.data.mapper.transfer.TransferMapper
import mega.privacy.android.data.mapper.transfer.active.ActiveTransferTotalsMapper
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.data.model.RequestEvent
import mega.privacy.android.data.repository.DefaultTransfersRepository.Companion.TRANSFERS_SD_TEMPORARY_FOLDER
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.ActiveTransferActionGroup
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.CompletedTransferState
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.InsertPendingTransferRequest
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.transfer.pending.UpdateAlreadyTransferredFilesCount
import mega.privacy.android.domain.entity.transfer.pending.UpdatePendingTransferState
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransferData
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.time.Duration.Companion.seconds

/**
 * Test class for [DefaultTransfersRepository]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultTransfersRepositoryTest {
    private lateinit var underTest: DefaultTransfersRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val transferEventMapper = mock<TransferEventMapper>()
    private val appEventGateway: AppEventGateway = mock()
    private val transferMapper: TransferMapper = mock()
    private val transferAppDataStringMapper: TransferAppDataStringMapper = mock()
    private val pausedTransferEventMapper = mock<PausedTransferEventMapper>()
    private val completedTransferMapper = mock<CompletedTransferMapper>()
    private val completedTransferPendingTransferMapper =
        mock<CompletedTransferPendingTransferMapper>()
    private val localStorageGateway: MegaLocalStorageGateway = mock()
    private val workerManagerGateway = mock<WorkManagerGateway>()
    private val megaLocalRoomGateway = mock<MegaLocalRoomGateway>()
    private val cancelTokenProvider = mock<CancelTokenProvider>()
    private val activeTransferTotalsMapper = mock<ActiveTransferTotalsMapper>()
    private val megaNodeMapper = mock<MegaNodeMapper>()
    private val sdCardGateway = mock<SDCardGateway>()
    private val deviceGateway = mock<DeviceGateway>()
    private val inProgressTransferMapper = mock<InProgressTransferMapper>()
    private val monitorFetchNodesFinishUseCase = mock<MonitorFetchNodesFinishUseCase>()
    private val transfersPreferencesGateway = mock<TransfersPreferencesGateway>()
    private val cacheGateway = mock<CacheGateway>()
    private val megaApiFolderGateway = mock<MegaApiFolderGateway>()
    private val megaChatApiGateway = mock<MegaChatApiGateway>()

    private val testScope = CoroutineScope(UnconfinedTestDispatcher())

    @BeforeAll
    fun setUp() = runTest {
        underTest = createDefaultTransfersRepository()
    }

    private suspend fun createDefaultTransfersRepository(paused: Boolean = false): DefaultTransfersRepository {
        //need to stub this method as it's called on init
        whenever(localStorageGateway.getTransferQueueStatus()).thenReturn(paused)
        whenever(monitorFetchNodesFinishUseCase()).thenReturn(flowOf(true))
        stubPauseTransfers(paused)
        return DefaultTransfersRepository(
            megaApiGateway = megaApiGateway,
            megaChatApiGateway = megaChatApiGateway,
            megaApiFolderGateway = megaApiFolderGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            transferEventMapper = transferEventMapper,
            appEventGateway = appEventGateway,
            transferMapper = transferMapper,
            transferAppDataStringMapper = transferAppDataStringMapper,
            pausedTransferEventMapper = pausedTransferEventMapper,
            activeTransferTotalsMapper = activeTransferTotalsMapper,
            completedTransferMapper = completedTransferMapper,
            completedTransferPendingTransferMapper = completedTransferPendingTransferMapper,
            localStorageGateway = localStorageGateway,
            workerManagerGateway = workerManagerGateway,
            megaLocalRoomGateway = megaLocalRoomGateway,
            cancelTokenProvider = cancelTokenProvider,
            scope = testScope,
            megaNodeMapper = megaNodeMapper,
            sdCardGateway = sdCardGateway,
            deviceGateway = deviceGateway,
            inProgressTransferMapper = inProgressTransferMapper,
            monitorFetchNodesFinishUseCase = monitorFetchNodesFinishUseCase,
            transfersPreferencesGateway = { transfersPreferencesGateway },
            cacheGateway = cacheGateway
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiGateway,
            megaChatApiGateway,
            megaApiFolderGateway,
            transferEventMapper,
            appEventGateway,
            transferMapper,
            pausedTransferEventMapper,
            localStorageGateway,
            workerManagerGateway,
            megaLocalRoomGateway,
            cancelTokenProvider,
            completedTransferMapper,
            completedTransferPendingTransferMapper,
            megaNodeMapper,
            sdCardGateway,
            deviceGateway,
            inProgressTransferMapper,
            cacheGateway,
            transferAppDataStringMapper,
            activeTransferTotalsMapper,
            monitorFetchNodesFinishUseCase,
            transfersPreferencesGateway,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UploadDownloadTests {

        private fun mockStartUpload() = megaApiGateway.startUpload(
            localPath = any(),
            parentNode = any(),
            fileName = anyOrNull(),
            modificationTime = any(),
            appData = anyOrNull(),
            isSourceTemporary = any(),
            shouldStartFirst = any(),
            cancelToken = anyOrNull(),
            listener = any(),
        )

        private fun startUploadFlow(appData: List<TransferAppData>? = null) =
            underTest.startUpload(
                localPath = "test local path",
                parentNodeId = NodeId(1L),
                fileName = "test filename",
                modificationTime = 123456789L,
                appData = appData,
                isSourceTemporary = false,
                shouldStartFirst = false,
            )

        private fun mockStartChatUpload() = megaApiGateway.startUploadForChat(
            localPath = any(),
            parentNode = any(),
            fileName = anyOrNull(),
            appData = anyOrNull(),
            isSourceTemporary = any(),
            listener = any(),
        )

        private fun startChatUploadFlow(appData: List<TransferAppData.ChatUploadAppData> = chatAppData) =
            underTest.startUploadForChat(
                localPath = "test local path",
                parentNodeId = NodeId(1L),
                fileName = "test filename",
                appData = appData,
                isSourceTemporary = false,
            )

        private val chatAppData = listOf(TransferAppData.ChatUpload(1L))
        private val appData = listOf(TransferAppData.CameraUpload)

        private fun mockStartDownload() = megaApiGateway.startDownload(
            node = anyOrNull(),
            localPath = anyOrNull(),
            fileName = anyOrNull(),
            appData = anyOrNull(),
            cancelToken = anyOrNull(),
            collisionCheck = anyOrNull(),
            collisionResolution = anyOrNull(),
            startFirst = anyOrNull(),
            listener = any(),
        )

        private fun startDownloadFlow() =
            underTest.startDownload(
                node = mock<TypedFileNode>(),
                localPath = "test local path",
                appData = null,
                shouldStartFirst = false,
            )

        private fun provideUploadAndDownloadParameters() = listOf(
            Arguments.of(
                { mockStartUpload() }, { startUploadFlow() }, true
            ),
            Arguments.of(
                { mockStartDownload() }, { startDownloadFlow() }, false
            ),
            Arguments.of(
                { mockStartChatUpload() }, { startChatUploadFlow() }, true
            ),
        )

        @ParameterizedTest
        @MethodSource("provideUploadAndDownloadParameters")
        fun `test that OnTransferStart is returned when the upload and download begins`(
            mockStart: () -> Unit, startFlow: () -> Flow<TransferEvent>, isUpload: Boolean,
        ) = runTest {
            whenever(mockStart()).thenAnswer {
                (it.arguments.last() as OptionalMegaTransferListenerInterface).onTransferStart(
                    api = mock(),
                    transfer = mock(),
                )
            }
            if (isUpload) {
                whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
            } else {
                whenever(megaNodeMapper(any())).thenReturn(mock())
            }
            val expected = mock<TransferEvent.TransferStartEvent>()
            whenever(transferEventMapper.invoke(any())).thenReturn(expected)
            startFlow().test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }

        @ParameterizedTest
        @MethodSource("provideUploadAndDownloadParameters")
        fun `test that OnTransferFinished is returned when the upload and download is finished`(
            mockStart: () -> Unit, startFlow: () -> Flow<TransferEvent>, isUpload: Boolean,
        ) = runTest {
            whenever(mockStart()).thenAnswer {
                (it.arguments.last() as OptionalMegaTransferListenerInterface).onTransferFinish(
                    api = mock(),
                    transfer = mock(),
                    e = mock { on { errorCode }.thenReturn(MegaError.API_OK) },
                )
            }
            if (isUpload) {
                whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
            } else {
                whenever(megaNodeMapper(any())).thenReturn(mock())
            }
            val expected = mock<TransferEvent.TransferFinishEvent>()
            whenever(transferEventMapper.invoke(any())).thenReturn(expected)
            startFlow().test {
                assertThat(awaitItem()).isEqualTo(expected)
                awaitComplete()
            }
        }

        @ParameterizedTest
        @MethodSource("provideUploadAndDownloadParameters")
        fun `test that OnTransferFinished is returned when the download and upload is finished`(
            mockStart: () -> Unit, startFlow: () -> Flow<TransferEvent>, isUpload: Boolean,
        ) = runTest {
            whenever(mockStart()).thenAnswer {
                (it.arguments.last() as OptionalMegaTransferListenerInterface).onTransferFinish(
                    api = mock(),
                    transfer = mock(),
                    e = mock { on { errorCode }.thenReturn(MegaError.API_OK) },
                )
            }
            if (isUpload) {
                whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
            } else {
                whenever(megaNodeMapper(any())).thenReturn(mock())
            }
            val expected = mock<TransferEvent.TransferFinishEvent>()
            whenever(transferEventMapper.invoke(any())).thenReturn(expected)
            startFlow().test {
                assertThat(awaitItem()).isEqualTo(expected)
                awaitComplete()
            }
        }

        @ParameterizedTest
        @MethodSource("provideUploadAndDownloadParameters")
        fun `test that OnTransferUpdate is returned when the ongoing upload has been updated`(
            mockStart: () -> Unit, startFlow: () -> Flow<TransferEvent>, isUpload: Boolean,
        ) = runTest {
            whenever(mockStart()).thenAnswer {
                (it.arguments.last() as OptionalMegaTransferListenerInterface).onTransferUpdate(
                    api = mock(),
                    transfer = mock(),
                )
            }
            if (isUpload) {
                whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
            } else {
                whenever(megaNodeMapper(any())).thenReturn(mock())
            }
            val expected = mock<TransferEvent.TransferUpdateEvent>()
            whenever(transferEventMapper.invoke(argThat { this is GlobalTransfer.OnTransferUpdate }))
                .thenReturn(expected)
            startFlow().test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }

        @ParameterizedTest
        @MethodSource("provideUploadAndDownloadParameters")
        fun `test that onFolderTransferUpdate is returned when the ongoing upload receives an onFolderTransferUpdate`(
            mockStart: () -> Unit, startFlow: () -> Flow<TransferEvent>, isUpload: Boolean,
        ) = runTest {
            whenever(mockStart()).thenAnswer {
                (it.arguments.last() as OptionalMegaTransferListenerInterface).onFolderTransferUpdate(
                    api = mock(),
                    transfer = mock(),
                    stage = 0,
                    folderCount = 0,
                    createdFolderCount = 0,
                    fileCount = 0,
                    currentFolder = "",
                    currentFileLeafName = ""
                )
            }
            if (isUpload) {
                whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
            } else {
                whenever(megaNodeMapper(any())).thenReturn(mock())
            }
            val expected = mock<TransferEvent.TransferUpdateEvent>()
            whenever(transferEventMapper.invoke(argThat { this is GlobalTransfer.OnFolderTransferUpdate }))
                .thenReturn(expected)
            startFlow().test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }

        @ParameterizedTest
        @MethodSource("provideUploadAndDownloadParameters")
        fun `test that OnTransferTemporaryError is returned when the upload experiences a temporary error`(
            mockStart: () -> Unit, startFlow: () -> Flow<TransferEvent>, isUpload: Boolean,
        ) = runTest {
            whenever(mockStart()).thenAnswer {
                (it.arguments.last() as OptionalMegaTransferListenerInterface).onTransferTemporaryError(
                    api = mock(),
                    transfer = mock(),
                    e = mock { on { errorCode }.thenReturn(MegaError.API_OK + 1) },
                )
            }
            if (isUpload) {
                whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
            } else {
                whenever(megaNodeMapper(any())).thenReturn(mock())
            }
            val expected = mock<TransferEvent.TransferTemporaryErrorEvent>()
            whenever(transferEventMapper.invoke(any())).thenReturn(expected)
            startFlow().test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }

        @ParameterizedTest
        @MethodSource("provideUploadAndDownloadParameters")
        fun `test that OnTransferData is returned when the upload data is being read`(
            mockStart: () -> Unit, startFlow: () -> Flow<TransferEvent>, isUpload: Boolean,
        ) = runTest {
            whenever(mockStart()).thenAnswer {
                (it.arguments.last() as OptionalMegaTransferListenerInterface).onTransferData(
                    api = mock(),
                    transfer = mock(),
                    buffer = byteArrayOf(),
                )
            }
            if (isUpload) {
                whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
            } else {
                whenever(megaNodeMapper(any())).thenReturn(mock())
            }
            val expected = mock<TransferEvent.TransferDataEvent>()
            whenever(transferEventMapper.invoke(any())).thenReturn(expected)
            startFlow().test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }

        @Test
        fun `test that app data is sent to gateway when upload is started`() = runTest {
            whenever(mockStartUpload()).thenAnswer {
                (it.arguments.last() as OptionalMegaTransferListenerInterface).onTransferData(
                    api = mock(),
                    transfer = mock(),
                    buffer = byteArrayOf(),
                )
            }
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
            whenever(transferEventMapper.invoke(any())).thenReturn(mock<TransferEvent.TransferDataEvent>())
            val expected = "appData"
            whenever(transferAppDataStringMapper.invoke(appData)).thenReturn(expected)
            startUploadFlow(appData).test {
                assertThat(awaitItem()).isNotNull()
            }
            verify(megaApiGateway).startUpload(
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                eq(expected),
                any(),
                any(),
                anyOrNull(),
                any()
            )
        }

        @Test
        fun `test that app data is sent to gateway when upload to chat is started`() = runTest {
            whenever(mockStartChatUpload()).thenAnswer {
                (it.arguments.last() as OptionalMegaTransferListenerInterface).onTransferData(
                    api = mock(),
                    transfer = mock(),
                    buffer = byteArrayOf(),
                )
            }
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
            whenever(transferEventMapper.invoke(any())).thenReturn(mock<TransferEvent.TransferDataEvent>())
            val expected = "appData"
            whenever(transferAppDataStringMapper.invoke(chatAppData)).thenReturn(expected)

            startChatUploadFlow().test {
                assertThat(awaitItem()).isNotNull()
            }

            verify(megaApiGateway).startUploadForChat(
                any(),
                any(),
                anyOrNull(),
                eq(expected),
                any(),
                any()
            )
        }

        @Test
        fun `test that an exception is thrown when upload to chat is started with empty app data`() =
            runTest {
                whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())

                startChatUploadFlow(emptyList()).test {
                    awaitError()
                }
            }

        @Test
        fun `test that an exception is thrown when upload to chat is started and node is not found`() =
            runTest {
                whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(null)

                startChatUploadFlow().test {
                    awaitError()
                }
            }
    }

    @Test
    fun `test that cancelTransferByTag returns success when MegaApi returns API_OK`() =
        runTest {
            val transferTag = 1000

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }

            val megaRequest = mock<MegaRequest>()

            whenever(megaApiGateway.cancelTransferByTag(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.cancelTransferByTag(transferTag)
        }

    @Test
    fun `test that cancelTransferByTag finishes with general MegaException when MegaApi returns error other than API_OK`() =
        runTest {
            val transferTag = 1000

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EFAILED)
            }

            val megaRequest = mock<MegaRequest>()

            whenever(megaApiGateway.cancelTransferByTag(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }
            assertThrows<MegaException> {
                underTest.cancelTransferByTag(transferTag)
            }
        }

    @Test
    fun `test that moveTransferToFirstByTag returns success when MegaApi returns API_OK`() =
        runTest {
            val transferTag = 1000

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }

            val megaRequest = mock<MegaRequest>()

            whenever(megaApiGateway.moveTransferToFirstByTag(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.moveTransferToFirstByTag(transferTag)
        }

    @Test
    fun `test that moveTransferToFirstByTag finishes with general MegaException when MegaApi returns error other than API_OK`() =
        runTest {
            val transferTag = 1000

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EFAILED)
            }

            val megaRequest = mock<MegaRequest>()

            whenever(megaApiGateway.moveTransferToFirstByTag(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }
            assertThrows<MegaException> {
                underTest.moveTransferToFirstByTag(transferTag)
            }
        }

    @Test
    fun `test that moveTransferToLastByTag returns success when MegaApi returns API_OK`() =
        runTest {
            val transferTag = 1000

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }

            val megaRequest = mock<MegaRequest>()

            whenever(megaApiGateway.moveTransferToLastByTag(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.moveTransferToLastByTag(transferTag)
        }

    @Test
    fun `test that moveTransferToLastByTag finishes with general MegaException when MegaApi returns error other than API_OK`() =
        runTest {
            val transferTag = 1000

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EFAILED)
            }

            val megaRequest = mock<MegaRequest>()

            whenever(megaApiGateway.moveTransferToLastByTag(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }
            assertThrows<MegaException> {
                underTest.moveTransferToLastByTag(transferTag)
            }
        }

    @Test
    fun `test that moveTransferBeforeByTag returns success when MegaApi returns API_OK`() =
        runTest {
            val transferTag = 1000
            val previousTransferTag = 1001

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }

            val megaRequest = mock<MegaRequest>()

            whenever(megaApiGateway.moveTransferBeforeByTag(any(), any(), any())).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.moveTransferBeforeByTag(transferTag, previousTransferTag)
        }

    @Test
    fun `test that moveTransferBeforeByTag finishes with general MegaException when MegaApi returns error other than API_OK`() =
        runTest {
            val transferTag = 1000
            val previousTransferTag = 1001

            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EFAILED)
            }

            val megaRequest = mock<MegaRequest>()

            whenever(megaApiGateway.moveTransferBeforeByTag(any(), any(), any())).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            assertThrows<MegaException> {
                underTest.moveTransferBeforeByTag(transferTag, previousTransferTag)
            }
        }

    @Test
    fun `test that getTransferByTag return value when call api returns object`() = runTest {
        val transferTag = 1
        val megaTransfer = mock<MegaTransfer>()
        val transfer = mock<Transfer>()
        whenever(megaApiGateway.getTransfersByTag(transferTag)).thenReturn(megaTransfer)
        whenever(transferMapper(megaTransfer)).thenReturn(transfer)
        assertThat(underTest.getTransferByTag(transferTag)).isEqualTo(transfer)
    }

    @Test
    fun `test that getTransferByTag return null when call api returns null`() = runTest {
        val transferTag = 1
        whenever(megaApiGateway.getTransfersByTag(transferTag)).thenReturn(null)
        assertThat(underTest.getTransferByTag(transferTag)).isNull()
    }

    @Test
    fun `test that getInProgressTransfers empty when both getTransferData numDownloads and numUploads equal zero`() =
        runTest {
            val data = mock<MegaTransferData> {
                on { numDownloads }.thenReturn(0)
                on { numUploads }.thenReturn(0)
            }
            whenever(megaApiGateway.getTransferData()).thenReturn(data)
            assertThat(underTest.getInProgressTransfers()).isEmpty()
        }

    @Test
    fun `test that getInProgressTransfers returns correctly when both getTransferData numDownloads and numUploads differ zero`() =
        runTest {
            val data = mock<MegaTransferData> {
                on { numDownloads }.thenReturn(5)
                on { numUploads }.thenReturn(5)
            }
            whenever(megaApiGateway.getTransferData()).thenReturn(data)
            whenever(transferMapper.invoke(any())).thenReturn(mock())
            whenever(megaApiGateway.getTransfersByTag(any())).thenReturn(mock())
            assertThat(underTest.getInProgressTransfers()).hasSize(data.numDownloads + data.numUploads)
        }

    @Test
    fun `test that addCompletedTransfers call local storage gateway addCompletedTransfers and app event gateway broadcastCompletedTransfer with error`() =
        runTest {
            val transfer = mock<Transfer> {
                on { it.state } doReturn TransferState.STATE_FAILED
            }
            val error = mock<MegaException>()
            val expected = listOf(mock<CompletedTransfer>())
            val path = "path"
            val event = mock<TransferEvent.TransferFinishEvent> {
                on { it.transfer } doReturn transfer
                on { it.error } doReturn error
            }
            whenever(completedTransferMapper(transfer, error, path)).thenReturn(expected.first())
            underTest.addCompletedTransfers(mapOf(event to path))
            verify(megaLocalRoomGateway).addCompletedTransfers(expected)
            verify(appEventGateway).broadcastCompletedTransfer(CompletedTransferState.Error)
        }

    @Test
    fun `test that addCompletedTransfers call local storage gateway addCompletedTransfers and app event gateway broadcastCompletedTransfer with completed`() =
        runTest {
            val transfer = mock<Transfer> {
                on { it.state } doReturn TransferState.STATE_COMPLETED
            }
            val error = mock<MegaException>()
            val expected = listOf(mock<CompletedTransfer>())
            val path = "path"
            val event = mock<TransferEvent.TransferFinishEvent> {
                on { it.transfer } doReturn transfer
                on { it.error } doReturn error
            }
            whenever(completedTransferMapper(transfer, error, path)).thenReturn(expected.first())
            underTest.addCompletedTransfers(mapOf(event to path))
            verify(megaLocalRoomGateway).addCompletedTransfers(expected)
            verify(appEventGateway).broadcastCompletedTransfer(CompletedTransferState.Completed)
        }

    @Test
    fun `test that addCompletedTransferFromFailedPendingTransfer call local storage gateway addCompletedTransfer and app event gateway broadcastCompletedTransfer with the mapped transfer`() =
        runTest {
            val transfer = mock<PendingTransfer>()
            val size = 100L
            val error = mock<MegaException>()
            val expected = mock<CompletedTransfer>()
            whenever(completedTransferPendingTransferMapper(transfer, size, error))
                .thenReturn(expected)
            underTest.addCompletedTransferFromFailedPendingTransfer(transfer, size, error)
            verify(megaLocalRoomGateway).addCompletedTransfer(expected)
            verify(appEventGateway).broadcastCompletedTransfer(CompletedTransferState.Error)
        }

    @Test
    fun `test that addCompletedTransferFromFailedPendingTransfers call local storage gateway addCompletedTransfers and app event gateway broadcastCompletedTransfer with the mapped transfers`() =
        runTest {
            val transfers = (0..10).map { mock<PendingTransfer>() }
            val error = mock<MegaException>()
            val expected = transfers.map { pendingTransfer ->
                mock<CompletedTransfer>().also {
                    whenever(completedTransferPendingTransferMapper(pendingTransfer, 0L, error))
                        .thenReturn(it)
                }
            }
            underTest.addCompletedTransferFromFailedPendingTransfers(transfers, error)
            verify(megaLocalRoomGateway).addCompletedTransfers(expected)
            verify(appEventGateway).broadcastCompletedTransfer(CompletedTransferState.Error)
        }

    @Test
    fun `test that addCompletedTransfersIfNotExist if there are no completed transfers in data base`() =
        runTest {
            val transfer1 = mock<Transfer>()
            val transfer2 = mock<Transfer>()
            val completedTransfer1 = getCompletedTransfer("transfer1")
            val completedTransfer2 = getCompletedTransfer("transfer2")

            whenever(megaLocalRoomGateway.getCompletedTransfers()).thenReturn(flowOf(emptyList()))
            whenever(completedTransferMapper(transfer1, null)).thenReturn(completedTransfer1)
            whenever(completedTransferMapper(transfer2, null)).thenReturn(completedTransfer2)

            underTest.addCompletedTransfersIfNotExist(listOf(transfer1, transfer2))

            verify(megaLocalRoomGateway).getCompletedTransfers()
            verify(completedTransferMapper).invoke(transfer1, null)
            verify(completedTransferMapper).invoke(transfer2, null)
            verify(megaLocalRoomGateway)
                .addCompletedTransfers(listOf(completedTransfer1, completedTransfer2))
        }

    @Test
    fun `test that addCompletedTransfersIfNotExist invokes correctly if some of the received transfers are not in data base`() =
        runTest {
            val transfer1 = mock<Transfer>()
            val transfer2 = mock<Transfer>()
            val completedTransfer1 = getCompletedTransfer("transfer1")
            val completedTransfer2 = getCompletedTransfer("transfer2")
            val completedTransfer3 = getCompletedTransfer("transfer3")

            whenever(megaLocalRoomGateway.getCompletedTransfers())
                .thenReturn(flowOf(listOf(completedTransfer1, completedTransfer3)))
            whenever(completedTransferMapper(transfer1, null)).thenReturn(completedTransfer1)
            whenever(completedTransferMapper(transfer2, null)).thenReturn(completedTransfer2)

            underTest.addCompletedTransfersIfNotExist(listOf(transfer1, transfer2))

            verify(megaLocalRoomGateway).getCompletedTransfers()
            verify(completedTransferMapper).invoke(transfer1, null)
            verify(completedTransferMapper).invoke(transfer2, null)
            verify(megaLocalRoomGateway).addCompletedTransfers(listOf(completedTransfer2))
        }

    @Test
    fun `test that monitorCompletedTransfer returns the result of app event gateway monitorCompletedTransfer`() =
        runTest {
            val expected = CompletedTransferState.Completed
            whenever(appEventGateway.monitorCompletedTransfer).thenReturn(flowOf(expected))
            underTest.monitorCompletedTransfer().test {
                assertThat(awaitItem()).isEqualTo(expected)
                awaitComplete()
            }
        }

    @Test
    fun `test that insertOrUpdateActiveTransfer gateway is called when insertOrUpdateActiveTransfer is called`() =
        runTest {
            val activeTransfer = mock<ActiveTransfer>()
            underTest.insertOrUpdateActiveTransfer(activeTransfer)
            verify(megaLocalRoomGateway).insertOrUpdateActiveTransfer(activeTransfer)
        }

    @Test
    fun `test that isCompletedTransfersEmpty returns false if completed transfers db contains items`() =
        runTest {
            whenever(megaLocalRoomGateway.getCompletedTransfersCount()).thenReturn(1)
            assertThat(underTest.isCompletedTransfersEmpty()).isFalse()
        }

    @Test
    fun `test that isCompletedTransfersEmpty returns true if completed transfers db does not contain items`() =
        runTest {
            whenever(megaLocalRoomGateway.getCompletedTransfersCount()).thenReturn(0)
            assertThat(underTest.isCompletedTransfersEmpty()).isTrue()
        }

    @ParameterizedTest(name = "pauseTransfers: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that pauseTransfers returns success when MegaApi returns API_OK`(isPause: Boolean) =
        runTest {
            stubPauseTransfers(isPause)

            assertThat(underTest.pauseTransfers(isPause)).isEqualTo(isPause)
        }

    @Test
    fun `test that pauseTransfers finishes with general MegaException when MegaApi returns error other than API_OK`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EFAILED)
            }

            val megaRequest = mock<MegaRequest>()

            whenever(megaApiGateway.pauseTransfers(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            assertThrows<MegaException> {
                underTest.pauseTransfers(true)
            }
        }

    @ParameterizedTest(name = "pauseTransferByTag: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that pauseTransferByTag returns success when MegaApi returns API_OK`(isPause: Boolean) =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }

            val megaRequest = mock<MegaRequest> {
                on { flag }.thenReturn(isPause)
            }

            whenever(megaApiGateway.pauseTransferByTag(any(), any(), any())).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            assertThat(underTest.pauseTransferByTag(1, isPause)).isEqualTo(isPause)
        }

    @Test
    fun `test that pauseTransferByTag finishes with general MegaException when MegaApi returns error other than API_OK`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EFAILED)
            }

            val megaRequest = mock<MegaRequest>()

            whenever(megaApiGateway.pauseTransferByTag(any(), any(), any())).thenAnswer {
                ((it.arguments[2]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            assertThrows<MegaException> {
                underTest.pauseTransferByTag(1, true)
            }
        }

    @Test
    fun `test that deleteAllCompletedTransfers room gateway is called when deleteAllCompletedTransfers is called`(
    ) = runTest {
        underTest.deleteAllCompletedTransfers()
        verify(megaLocalRoomGateway).deleteAllCompletedTransfers()
    }

    @Test
    fun `test that getCompletedTransfersByState room gateway is called when getFailedOrCanceledTransfers is called`() =
        runTest {
            underTest.getFailedOrCanceledTransfers()
            verify(megaLocalRoomGateway).getCompletedTransfersByState(
                listOf(
                    MegaTransfer.STATE_FAILED,
                    MegaTransfer.STATE_CANCELLED
                )
            )
        }

    @Test
    fun `test that deleteCompletedTransfersByState room gateway is called when deleteFailedOrCanceledTransfers is called`() =
        runTest {
            underTest.deleteFailedOrCanceledTransfers()
            verify(megaLocalRoomGateway).deleteCompletedTransfersByState(
                listOf(
                    MegaTransfer.STATE_FAILED,
                    MegaTransfer.STATE_CANCELLED
                )
            )
        }

    @Test
    fun `test that function invoke correctly when deleteCompletedTransfer is called`() =
        runTest {
            val completedTransfer = mock<CompletedTransfer>()
            underTest.deleteCompletedTransfer(completedTransfer, false)
            verify(megaLocalRoomGateway).deleteCompletedTransfer(any())
        }


    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that paused transfers initial value is changed when MegaLocalStorageGateway indicates it should be paused`(
        expected: Boolean,
    ) = runTest {
        //creating a new instance of DefaultTransfersRepository because monitorPausedTransfers is cached
        val flow = createDefaultTransfersRepository(expected).monitorPausedTransfers()
        assertThat(flow.value).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that monitor paused transfers is updated when pauseTransfers is updated`(
        expected: Boolean,
    ) = runTest {
        //creating a new instance of DefaultTransfersRepository because monitorPausedTransfers is cached
        val underTest = createDefaultTransfersRepository(!expected)
        val flow = underTest.monitorPausedTransfers()
        assertThat(flow.value).isEqualTo(!expected) //just to be sure the value will be updated after emitting a new value
        stubPauseTransfers(expected)
        underTest.pauseTransfers(expected)
        assertThat(flow.value).isEqualTo(expected)
    }

    @Test
    fun `test that monitorTransferEvents emits transfer events`() = runTest {
        val start = GlobalTransfer.OnTransferStart(mock())
        val update = GlobalTransfer.OnTransferUpdate(mock())
        val folderUpdate = GlobalTransfer.OnFolderTransferUpdate(mock(), 1, 1, 1, 1, "", "")
        val finish = GlobalTransfer.OnTransferFinish(mock(), mock())
        val startEvent = TransferEvent.TransferStartEvent(mock())
        val updateEvent = TransferEvent.TransferUpdateEvent(mock())
        val folderUpdateEvent = TransferEvent.FolderTransferUpdateEvent(
            mock(),
            TransferStage.STAGE_NONE,
            1,
            1,
            1,
            "",
            ""
        )
        val finishEvent = TransferEvent.TransferFinishEvent(mock(), mock())
        val globalTransferEventsFlow = flowOf(start, update, folderUpdate, finish)
        whenever(transferEventMapper(start)).thenReturn(startEvent)
        whenever(transferEventMapper(update)).thenReturn(updateEvent)
        whenever(transferEventMapper(folderUpdate)).thenReturn(folderUpdateEvent)
        whenever(transferEventMapper(finish)).thenReturn(finishEvent)
        whenever(megaApiGateway.globalRequestEvents).thenReturn(emptyFlow())
        whenever(megaApiGateway.globalTransfer).thenReturn(globalTransferEventsFlow)
        underTest.monitorTransferEvents().test {
            assertThat(awaitItem()).isEqualTo(startEvent)
            assertThat(awaitItem()).isEqualTo(updateEvent)
            assertThat(awaitItem()).isEqualTo(folderUpdateEvent)
            assertThat(awaitItem()).isEqualTo(finishEvent)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorTransferEvents emits transfer paused events`() = runTest {
        val pause = RequestEvent.OnRequestFinish(mock(), mock())
        val resume = RequestEvent.OnRequestFinish(mock(), mock())
        val pauseEvent = TransferEvent.TransferPaused(mock(), true)
        val resumeEvent = TransferEvent.TransferPaused(mock(), false)
        val globalRequestEventsFlow = flowOf(pause, resume)
        whenever(pausedTransferEventMapper(eq(pause), any())).thenReturn(pauseEvent)
        whenever(pausedTransferEventMapper(eq(resume), any())).thenReturn(resumeEvent)
        whenever(megaApiGateway.globalRequestEvents).thenReturn(globalRequestEventsFlow)
        whenever(megaApiGateway.globalTransfer).thenReturn(emptyFlow())
        underTest.monitorTransferEvents().test {
            assertThat(awaitItem()).isEqualTo(pauseEvent)
            assertThat(awaitItem()).isEqualTo(resumeEvent)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that getCompletedTransferById invokes when getCompletedTransferById is called`() =
        runTest {
            val id = 1
            underTest.getCompletedTransferById(id)
            verify(megaLocalRoomGateway).getCompletedTransferById(id)
        }

    @Test
    fun `test that getOrCreateSDCardCacheFolder returns gateway value`() =
        runTest {
            val result = File("path")
            whenever(sdCardGateway.getOrCreateCacheFolder(TRANSFERS_SD_TEMPORARY_FOLDER))
                .thenReturn(result)
            assertThat(underTest.getOrCreateSDCardTransfersCacheFolder()).isEqualTo(result)
        }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PendingCounters {

        @Test
        fun `test that getNumPendingCameraUploads returns correctly`() = runTest {
            stubUploadTransfers()
            assertThat(underTest.getNumPendingCameraUploads()).isEqualTo(2)
        }

        @Test
        fun `test that getNumPendingPausedCameraUploads returns correctly`() = runTest {
            stubUploadTransfers()
            assertThat(underTest.getNumPendingPausedCameraUploads()).isEqualTo(1)
        }

        private fun stubUploadTransfers() = runTest {
            val megaTransfers =
                (1..3).flatMap { type ->
                    listOf(true, false).flatMap { paused ->
                        listOf(true, false).map { finished ->
                            val megaTransfer = mock<MegaTransfer>()
                            whenever(megaTransfer.isFinished).thenReturn(finished)
                            whenever(megaTransfer.state).thenReturn(if (paused) MegaTransfer.STATE_PAUSED else MegaTransfer.STATE_ACTIVE)
                            whenever(megaTransfer.appData).thenReturn(
                                when (type) {
                                    2 -> AppDataTypeConstants.CameraUpload.sdkTypeValue
                                    3 -> AppDataTypeConstants.ChatUpload.sdkTypeValue
                                    else -> null
                                }
                            )
                            megaTransfer
                        }
                    }
                }
            whenever(megaApiGateway.getTransfers(MegaTransfer.TYPE_UPLOAD))
                .thenReturn(megaTransfers)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ActiveTransfersTest {
        val transfer = mock<Transfer>()

        @BeforeEach
        internal fun resetMocks() {
            reset(transfer)
        }

        @Test
        fun `test that getActiveTransferByTag gateway result is returned when getActiveTransferByTag is called`() =
            runTest {
                val expected = mock<ActiveTransfer>()
                whenever(megaLocalRoomGateway.getActiveTransferByTag(1)).thenReturn(expected)
                val actual = underTest.getActiveTransferByTag(1)
                assertThat(actual).isEqualTo(expected)
            }

        @ParameterizedTest
        @EnumSource(TransferType::class)
        fun `test that getActiveTransfersByType gateway result is returned when getActiveTransfersByType is called`(
            transferType: TransferType,
        ) =
            runTest {
                val expected = mock<List<ActiveTransfer>>()
                val flow = flowOf(expected)
                whenever(megaLocalRoomGateway.getActiveTransfersByType(transferType))
                    .thenReturn(flow)
                val actual = underTest.getActiveTransfersByType(transferType).first()
                assertThat(actual).isEqualTo(expected)
            }

        @ParameterizedTest
        @EnumSource(TransferType::class)
        fun `test that getActiveTransfersByType gateway result is returned when getCurrentActiveTransfersByType is called`(
            transferType: TransferType,
        ) =
            runTest {
                val expected = mock<List<ActiveTransfer>>()
                whenever(megaLocalRoomGateway.getCurrentActiveTransfersByType(transferType))
                    .thenReturn(expected)
                val actual = underTest.getCurrentActiveTransfersByType(transferType)
                assertThat(actual).isEqualTo(expected)
            }

        @Test
        fun `test that getActiveTransfers gateway result is returned when getCurrentActiveTransfers is called`() =
            runTest {
                val expected = mock<List<ActiveTransfer>>()
                whenever(megaLocalRoomGateway.getCurrentActiveTransfers())
                    .thenReturn(expected)
                val actual = underTest.getCurrentActiveTransfers()
                assertThat(actual).isEqualTo(expected)
            }

        @Test
        fun `test that insertOrUpdateActiveTransfer gateway is called when insertOrUpdateActiveTransfer is called`() =
            runTest {
                val activeTransfer = mock<ActiveTransfer>()
                underTest.insertOrUpdateActiveTransfer(activeTransfer)
                verify(megaLocalRoomGateway).insertOrUpdateActiveTransfer(activeTransfer)
            }

        @Test
        fun `test that insertOrUpdateActiveTransfers gateway is called when insertOrUpdateActiveTransfers is called`() =
            runTest {
                val activeTransfers = mock<List<ActiveTransfer>>()
                underTest.insertOrUpdateActiveTransfers(activeTransfers)
                verify(megaLocalRoomGateway).insertOrUpdateActiveTransfers(activeTransfers)
            }

        @ParameterizedTest
        @EnumSource(TransferType::class)
        fun `test that deleteAllActiveTransfersByType gateway is called when deleteAllActiveTransfersByType is called`(
            transferType: TransferType,
        ) = runTest {
            underTest.deleteAllActiveTransfersByType(transferType)
            verify(megaLocalRoomGateway).deleteAllActiveTransfersByType(transferType)
        }

        @Test
        fun `test that deleteAllActiveTransfers gateway is called when deleteAllActiveTransfers is called`() =
            runTest {
                underTest.deleteAllActiveTransfers()
                verify(megaLocalRoomGateway).deleteAllActiveTransfers()
            }

        @Test
        fun `test that setActiveTransferAsFinishedByTag gateway is called when setActiveTransferAsFinishedByTag is called`(
        ) = runTest {
            val tags = mock<List<Int>>()
            underTest.setActiveTransferAsCancelledByTag(tags)
            verify(megaLocalRoomGateway).setActiveTransferAsCancelledByTag(tags)
        }

        @ParameterizedTest
        @EnumSource(TransferType::class)
        fun `test that getActiveTransferTotalsByType gateway result is returned when getActiveTransferTotalsByType is called`(
            transferType: TransferType,
        ) = runTest {
            val expected = mock<ActiveTransferTotals>()
            val list = mock<List<ActiveTransfer>>()
            val flow = flowOf(list)
            whenever(megaLocalRoomGateway.getActiveTransfersByType(transferType))
                .thenReturn(flow)
            whenever(activeTransferTotalsMapper(eq(transferType), eq(list), any(), anyOrNull()))
                .thenReturn(expected)
            val actual = underTest.getActiveTransferTotalsByType(transferType).first()
            assertThat(actual).isEqualTo(expected)
        }

        @ParameterizedTest
        @EnumSource(TransferType::class)
        fun `test that previous groups are send to activeTransferTotalsMapper after first emission`(
            transferType: TransferType,
        ) = runTest {
            val actionGroups = mock<List<ActiveTransferTotals.ActionGroup>>()
            val firstActiveTransferTotals = mock<ActiveTransferTotals> {
                on { this.actionGroups } doReturn actionGroups
            }
            val secondActiveTransferTotals = mock<ActiveTransferTotals>()
            val firstList = listOf(mock<ActiveTransfer>())
            val secondList = listOf(mock<ActiveTransfer>(), mock<ActiveTransfer>())
            val flow = MutableStateFlow(firstList)
            whenever(megaLocalRoomGateway.getActiveTransfersByType(transferType))
                .thenReturn(flow)
            whenever(
                activeTransferTotalsMapper(
                    type = transferType,
                    list = firstList,
                    transferredBytes = emptyMap(),
                    previousActionGroups = null
                )
            ) doReturn firstActiveTransferTotals
            whenever(
                activeTransferTotalsMapper(
                    type = transferType,
                    list = secondList,
                    transferredBytes = emptyMap(),
                    previousActionGroups = actionGroups //this comes from first emission
                )
            ) doReturn secondActiveTransferTotals
            underTest.getActiveTransferTotalsByType(transferType).test {
                assertThat(awaitItem()).isEqualTo(firstActiveTransferTotals)
                flow.emit(secondList)
                assertThat(awaitItem()).isEqualTo(secondActiveTransferTotals)
            }
        }

        @ParameterizedTest
        @EnumSource(TransferType::class)
        fun `test that getCurrentActiveTransferTotalsByType gateway result is returned when getCurrentActiveTransferTotalsByType is called`(
            transferType: TransferType,
        ) = runTest {
            val list = mock<List<ActiveTransfer>>()
            val expected = mock<ActiveTransferTotals>()
            whenever(megaLocalRoomGateway.getCurrentActiveTransfersByType(transferType))
                .thenReturn(list)
            whenever(activeTransferTotalsMapper(eq(transferType), eq(list), any(), anyOrNull()))
                .thenReturn(expected)
            val actual = underTest.getCurrentActiveTransferTotalsByType(transferType)
            assertThat(actual).isEqualTo(expected)
        }

        @ParameterizedTest
        @EnumSource(TransferType::class)
        fun `test that updateTransferredBytes adds currentTransferred bytes and deleteAllActiveTransfersByType clears the values`(
            transferType: TransferType,
        ) = runTest {
            testCurrentActiveTransferTotals(
                transferType = transferType,
                expectedMap = { transfer ->
                    mapOf(transfer.tag to transfer.transferredBytes)
                },
                callToTest = {
                    underTest.updateTransferredBytes(listOf(transfer))
                }
            )
        }

        @ParameterizedTest
        @EnumSource(TransferType::class)
        fun `test that updateTransferredBytes doesn't update when the new value is 0 bytes`(
            transferType: TransferType,
        ) = runTest {
            testCurrentActiveTransferTotals(
                transferType = transferType,
                expectedMap = { transfer ->
                    mapOf(transfer.tag to transfer.transferredBytes)
                },
                callToTest = {
                    val transferZero = mock<Transfer>()
                    stubActiveTransfer(transferZero, transferType, transferredBytes = 0L)

                    underTest.updateTransferredBytes(listOf(transfer))
                    underTest.updateTransferredBytes(listOf(transferZero))
                }
            )
        }

        @ParameterizedTest
        @EnumSource(TransferType::class)
        fun `test that updateTransferredBytes with non zero transferred bytes emits a new value`(
            transferType: TransferType,
        ) = runTest {
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn transferType
                on { this.transferredBytes } doReturn 34857L
            }
            val expected = mock<ActiveTransferTotals>()
            val list = mock<List<ActiveTransfer>>()
            val flow = flowOf(list)
            whenever(megaLocalRoomGateway.getActiveTransfersByType(transferType))
                .thenReturn(flow)
            whenever(activeTransferTotalsMapper(eq(transferType), eq(list), any(), anyOrNull()))
                .thenReturn(expected)

            underTest.getActiveTransferTotalsByType(transferType).test {
                awaitItem() //initial
                underTest.updateTransferredBytes(listOf(transfer))
                val actual = awaitItem()
                assertThat(actual).isEqualTo(expected)
                cancelAndIgnoreRemainingEvents()
            }
            underTest.deleteAllActiveTransfersByType(transferType)
        }

        /**
         * As getCurrentActiveTransferTotalsByType is based on a state flow, we need to reset this state to make testing stateless
         * This is a convenient function to test changes on this state and then reset it to its initial empty value.
         */
        private suspend fun testCurrentActiveTransferTotals(
            transferType: TransferType,
            expectedMap: (Transfer) -> Map<Int, Long>,
            callToTest: suspend () -> Unit,
        ) {
            stubActiveTransfer(transfer, transferType)
            val list = mock<List<ActiveTransfer>>()
            whenever(megaLocalRoomGateway.getCurrentActiveTransfersByType(transferType))
                .thenReturn(list)

            // test updateTransferredBytes
            callToTest()
            underTest.getCurrentActiveTransferTotalsByType(transferType)
            //here we check that the mapper is called with the proper expectedMap
            val map = expectedMap(transfer)
            verify(activeTransferTotalsMapper).invoke(
                eq(transferType),
                eq(list),
                eq(map),
                anyOrNull()
            )

            // test deleteAllActiveTransfersByType so we also clear the cached values
            underTest.deleteAllActiveTransfersByType(transferType)
            underTest.getCurrentActiveTransferTotalsByType(transferType)
            //here we check that the mapper is called with the proper expectedMap
            verify(activeTransferTotalsMapper).invoke(
                eq(transferType),
                eq(list),
                eq(emptyMap()),
                anyOrNull()
            )
        }

        private fun stubActiveTransfer(
            transfer: Transfer,
            transferType: TransferType,
            transferredBytes: Long = 900L,
        ) {
            val total = 1024L
            val tag = 1

            whenever(transfer.transferType).thenReturn(transferType)
            whenever(transfer.transferredBytes).thenReturn(transferredBytes)
            whenever(transfer.totalBytes).thenReturn(total)
            whenever(transfer.tag).thenReturn(tag)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class WorkerTests {
        @Test
        fun `test that workerManagerGateway enqueueDownloadsWorkerRequest is called when startDownloadWorker is called`() =
            runTest {
                underTest.startDownloadWorker()
                verify(workerManagerGateway).enqueueDownloadsWorkerRequest()
            }

        @ParameterizedTest
        @EnumSource(WorkInfo.State::class)
        fun `test that monitorIsDownloadsWorkerEnqueued returns workerManagerGateway values`(state: WorkInfo.State) =
            runTest {
                val workInfo = mock<WorkInfo> {
                    on { this.state }.thenReturn(state)
                }
                whenever(workerManagerGateway.monitorDownloadsStatusInfo())
                    .thenReturn(flowOf(listOf(workInfo)))
                underTest.monitorIsDownloadsWorkerEnqueued().test {
                    if (state == WorkInfo.State.ENQUEUED) {
                        assertThat(awaitItem()).isTrue()
                    } else {
                        assertThat(awaitItem()).isFalse()
                    }
                    cancelAndIgnoreRemainingEvents()
                }

            }

        @ParameterizedTest
        @EnumSource(WorkInfo.State::class)
        fun `test that monitorIsDownloadsWorkerFinished returns workerManagerGateway values`(state: WorkInfo.State) =
            runTest {
                val workInfo = mock<WorkInfo> {
                    on { this.state }.thenReturn(state)
                }
                whenever(workerManagerGateway.monitorDownloadsStatusInfo())
                    .thenReturn(flowOf(listOf(workInfo)))
                underTest.monitorIsDownloadsWorkerFinished().test {
                    if (state.isFinished) {
                        assertThat(awaitItem()).isTrue()
                    } else {
                        assertThat(awaitItem()).isFalse()
                    }
                    cancelAndIgnoreRemainingEvents()
                }

            }

        @Test
        fun `test that workerManagerGateway enqueueUploadsWorkerRequest is called when startUploadsWorker is called`() =
            runTest {
                underTest.startUploadsWorker()
                verify(workerManagerGateway).enqueueUploadsWorkerRequest()
            }

        @ParameterizedTest
        @EnumSource(WorkInfo.State::class)
        fun `test that monitorIsUploadsWorkerEnqueued returns workerManagerGateway values`(state: WorkInfo.State) =
            runTest {
                val workInfo = mock<WorkInfo> {
                    on { this.state }.thenReturn(state)
                }
                whenever(workerManagerGateway.monitorUploadsStatusInfo())
                    .thenReturn(flowOf(listOf(workInfo)))
                underTest.monitorIsUploadsWorkerEnqueued().test {
                    if (state == WorkInfo.State.ENQUEUED) {
                        assertThat(awaitItem()).isTrue()
                    } else {
                        assertThat(awaitItem()).isFalse()
                    }
                    cancelAndIgnoreRemainingEvents()
                }

            }

        @ParameterizedTest
        @EnumSource(WorkInfo.State::class)
        fun `test that monitorUploadsStatusInfo returns workerManagerGateway values`(state: WorkInfo.State) =
            runTest {
                val workInfo = mock<WorkInfo> {
                    on { this.state }.thenReturn(state)
                }
                whenever(workerManagerGateway.monitorUploadsStatusInfo())
                    .thenReturn(flowOf(listOf(workInfo)))
                underTest.monitorIsUploadsWorkerFinished().test {
                    if (state.isFinished) {
                        assertThat(awaitItem()).isTrue()
                    } else {
                        assertThat(awaitItem()).isFalse()
                    }
                    cancelAndIgnoreRemainingEvents()
                }

            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DownloadCountersTest {

        @ParameterizedTest(name = " megaApi call returns {0}")
        @ValueSource(ints = [0, 7, 200])
        fun `test that getCurrentDownloadSpeed returns correctly if`(
            speed: Int,
        ) = runTest {
            whenever(megaApiGateway.currentDownloadSpeed).thenReturn(speed)
            assertThat(underTest.getCurrentDownloadSpeed()).isEqualTo(speed)
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [26, 27, 28, 29])
    fun `test that allowUserToSetDownloadDestination returns true when sdk is lower than Android 11`(
        sdk: Int,
    ) =
        runTest {
            whenever(deviceGateway.getSdkVersionInt()).thenReturn(sdk)
            assertThat(underTest.allowUserToSetDownloadDestination()).isTrue()
        }

    @ParameterizedTest
    @ValueSource(ints = [30, 31, 32, 33, 34])
    fun `test that allowUserToSetDownloadDestination returns false when sdk is equal or higher than Android 11`(
        sdk: Int,
    ) =
        runTest {
            whenever(deviceGateway.getSdkVersionInt()).thenReturn(sdk)
            assertThat(underTest.allowUserToSetDownloadDestination()).isFalse()
        }

    private fun stubPauseTransfers(isPause: Boolean) {
        val megaError = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_OK)
        }

        val megaRequest = mock<MegaRequest> {
            on { flag }.thenReturn(isPause)
        }

        whenever(megaApiGateway.pauseTransfers(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                megaRequest,
                megaError,
            )
        }
    }

    @Test
    fun `test that monitorInProgressTransfers emits empty map when no in progress transfers has been added`() =
        runTest {
            setUp()
            underTest.monitorInProgressTransfers().test {
                assertThat(awaitItem()).isEqualTo(emptyMap<Int, InProgressTransfer>())
            }
        }

    @Test
    fun `test that monitorInProgressTransfers emits a map with a transfer after call updateInProgressTransfer`() =
        runTest {
            val tag = 1
            val transfer = mock<Transfer> {
                on { this.tag } doReturn tag
            }
            val expected = mock<InProgressTransfer.Upload> {
                on { this.tag } doReturn tag
            }

            setUp()
            whenever(inProgressTransferMapper(transfer)).thenReturn(expected)

            underTest.monitorInProgressTransfers().test {
                assertThat(awaitItem()).isEqualTo(emptyMap<Int, InProgressTransfer>())
                underTest.updateInProgressTransfer(transfer)
                assertThat(awaitItem()).isEqualTo(mapOf(Pair(tag, expected)))
            }
        }

    @Test
    fun `test that in progress transfers flow is updated correctly when updateInProgressTransfers and removeInProgressTransfers are called`() =
        runTest {
            val tag = 5
            val transfer = mock<Transfer> {
                on { it.tag } doReturn tag
            }
            val inProgressTransfer = mock<InProgressTransfer.Download> {
                on { it.tag } doReturn tag
            }
            whenever(inProgressTransferMapper(transfer)).thenReturn(inProgressTransfer)
            setUp()
            underTest.monitorInProgressTransfers().test {
                assertThat(awaitItem()).isEmpty()
                underTest.updateInProgressTransfers(listOf(transfer))

                assertThat(awaitItem()).containsExactly(tag, inProgressTransfer)
                underTest.removeInProgressTransfers(setOf(tag))

                assertThat(awaitItem()).isEmpty()
            }
        }

    @Test
    fun `test that cancelTransfers is invoked for each direction when cancelTransfers is invoked`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }
            whenever(megaApiGateway.cancelTransfers(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaError,
                )
            }

            underTest.cancelTransfers()

            verify(megaApiGateway).cancelTransfers(eq(MegaTransfer.TYPE_UPLOAD), any())
            verify(megaApiGateway).cancelTransfers(eq(MegaTransfer.TYPE_DOWNLOAD), any())
        }

    @Test
    fun `test that resetPauseTransfers invokes `() = runTest {
        underTest.resetPauseTransfers()
        val flow1 = underTest.monitorPausedTransfers()
        assertThat(flow1.value).isFalse()
        verify(localStorageGateway).setTransferQueueStatus(false)
        val flow2 = underTest.monitorAskedResumeTransfers()
        assertThat(flow2.value).isFalse()
    }

    @Test
    fun `test that getPendingTransfersByTag gateway result is returned when getPendingTransfersByTag is called`() =
        runTest {
            val tag = 15
            val expected = mock<PendingTransfer>()
            whenever(megaLocalRoomGateway.getPendingTransfersByTag(tag)).thenReturn(expected)
            val actual = underTest.getPendingTransfersByTag(tag)
            assertThat(actual).isEqualTo(expected)
        }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that monitorPendingTransfersByType gateway result is returned when monitorPendingTransfersByType is called`(
        transferType: TransferType,
    ) = runTest {
        val expected = flowOf(listOf(mock<PendingTransfer>()))
        whenever(megaLocalRoomGateway.monitorPendingTransfersByType(transferType)).thenReturn(
            expected
        )
        val actual = underTest.monitorPendingTransfersByType(transferType)
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that getPendingTransfersByType gateway result is returned when getPendingTransfersByType is called`(
        transferType: TransferType,
    ) = runTest {
        val expected = listOf(mock<PendingTransfer>())
        whenever(megaLocalRoomGateway.getPendingTransfersByType(transferType)).thenReturn(expected)
        val actual = underTest.getPendingTransfersByType(transferType)
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(PendingTransferState::class)
    fun `test that getPendingTransfersByState gateway result is returned when getPendingTransfersByState is called`(
        state: PendingTransferState,
    ) = runTest {
        val expected = listOf(mock<PendingTransfer>())
        whenever(megaLocalRoomGateway.getPendingTransfersByState(state)).thenReturn(expected)
        val actual = underTest.getPendingTransfersByState(state)
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(PendingTransferState::class)
    fun `test that monitorPendingTransfersByTypeAndState gateway result is returned when monitorPendingTransfersByTypeAndState is called`(
        pendingTransferState: PendingTransferState,
    ) = runTest {
        val expected = flowOf(listOf(mock<PendingTransfer>()))
        whenever(
            megaLocalRoomGateway
                .monitorPendingTransfersByTypeAndState(TransferType.DOWNLOAD, pendingTransferState)
        )
            .thenReturn(expected)
        val actual = underTest
            .monitorPendingTransfersByTypeAndState(TransferType.DOWNLOAD, pendingTransferState)
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(PendingTransferState::class)
    fun `test that getPendingTransfersByTypeAndState gateway result is returned when getPendingTransfersByTypeAndState is called`(
        pendingTransferState: PendingTransferState,
    ) = runTest {
        val expected = listOf(mock<PendingTransfer>())
        whenever(
            megaLocalRoomGateway
                .getPendingTransfersByTypeAndState(TransferType.DOWNLOAD, pendingTransferState)
        )
            .thenReturn(expected)
        val actual = underTest
            .getPendingTransfersByTypeAndState(TransferType.DOWNLOAD, pendingTransferState)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that insertPendingTransfers gateway is called with the correct parameters when insertPendingTransfers is called`() =
        runTest {
            val expected = listOf(mock<InsertPendingTransferRequest>())

            underTest.insertPendingTransfers(expected)

            verify(megaLocalRoomGateway).insertPendingTransfers(expected)
        }

    @Test
    fun `test that updatePendingTransfer gateway is called with the correct parameters when updatePendingTransfer is called`() =
        runTest {
            val updatePendingTransferRequests = mock<UpdatePendingTransferState>()

            underTest.updatePendingTransfer(updatePendingTransferRequests)

            verify(megaLocalRoomGateway).updatePendingTransfers(updatePendingTransferRequests)
        }

    @Test
    fun `test that updatePendingTransfers gateway is called with the correct parameters when updatePendingTransfers is called`() =
        runTest {
            val updatePendingTransferRequests1 = mock<UpdatePendingTransferState>()
            val updatePendingTransferRequests2 = mock<UpdateAlreadyTransferredFilesCount>()

            underTest.updatePendingTransfers(
                listOf(updatePendingTransferRequests1, updatePendingTransferRequests2)
            )

            verify(megaLocalRoomGateway).updatePendingTransfers(
                updatePendingTransferRequests1,
                updatePendingTransferRequests2
            )
        }

    @Test
    fun `test that transfersPreferencesGateway invokes setRequestFilesPermissionDenied when setRequestFilesPermissionDenied is called`() =
        runTest {
            underTest.setRequestFilesPermissionDenied()
            verify(transfersPreferencesGateway).setRequestFilesPermissionDenied()
        }

    @Test
    fun `test that monitorRequestFilesPermissionDenied returns what transfersPreferencesGateway monitorRequestFilesPermissionDenied returns`() =
        runTest {
            val flow = flowOf(true)
            whenever(transfersPreferencesGateway.monitorRequestFilesPermissionDenied())
                .thenReturn(flow)
            underTest.monitorRequestFilesPermissionDenied().test {
                assertThat(awaitItem()).isEqualTo(true)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that transfersPreferencesGateway invokes clearPreferences when clearPreferences is called`() =
        runTest {
            underTest.clearPreferences()
            verify(transfersPreferencesGateway).clearPreferences()
        }

    @Test
    fun `test that getBandwidthOverQuotaDelay invokes and returns correctly`() = runTest {
        val expected = 123L

        whenever(megaApiGateway.getBandwidthOverQuotaDelay()).thenReturn(expected)

        assertThat(underTest.getBandwidthOverQuotaDelay()).isEqualTo(expected.seconds)
    }

    @Test
    fun `test that insert Active Transfer Group returns room gateway result`() = runTest {
        val expected = 123L
        val activeTransferActionGroup = mock<ActiveTransferActionGroup>()
        whenever(megaLocalRoomGateway.insertActiveTransferGroup(activeTransferActionGroup))
            .thenReturn(expected)

        assertThat(underTest.insertActiveTransferGroup(activeTransferActionGroup)).isEqualTo(
            expected
        )
    }

    @Test
    fun `test that get Active Transfer Group returns room gateway entity`() = runTest {
        val groupId = 435834379
        val expected = mock<ActiveTransferActionGroup>()
        whenever(megaLocalRoomGateway.getActiveTransferGroup(groupId)).thenReturn(expected)

        assertThat(underTest.getActiveTransferGroupById(groupId)).isEqualTo(expected)
    }

    private fun getCompletedTransfer(fileName: String) = CompletedTransfer(
        fileName = fileName,
        type = 0,
        state = 6,
        size = "234Kb",
        handle = 1L,
        path = "parentPath",
        isOffline = false,
        timestamp = 123L,
        error = "error",
        originalPath = "localPath",
        parentHandle = 2L,
        appData = null,
    )
}
