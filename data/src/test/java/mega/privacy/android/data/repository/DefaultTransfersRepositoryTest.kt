package mega.privacy.android.data.repository

import androidx.work.WorkInfo
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cache.MapCache
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.WorkManagerGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
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
import mega.privacy.android.data.mapper.transfer.upload.MegaUploadOptionsMapper
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.data.model.RequestEvent
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.pitag.PitagTarget
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.times
import mega.privacy.android.domain.entity.transfer.ActiveTransferActionGroup
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
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
import nz.mega.sdk.MegaNode
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
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Test class for [DefaultTransfersRepository]
 */
@ExperimentalCoroutinesApi
@OptIn(ExperimentalTime::class)
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
    private val deviceGateway = mock<DeviceGateway>()
    private val inProgressTransferMapper = mock<InProgressTransferMapper>()
    private val monitorFetchNodesFinishUseCase = mock<MonitorFetchNodesFinishUseCase>()
    private val megaUploadOptionsMapper = mock<MegaUploadOptionsMapper>()
    private val displayPathFromUriCache = mock<MapCache<String, String>>()
    private val parentNodeCache = mock<MapCache<Long, MegaNode?>>()
    private val transferPathCache = mock<MapCache<Pair<Long, TransferType>, String>>()

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
            deviceGateway = deviceGateway,
            inProgressTransferMapper = inProgressTransferMapper,
            monitorFetchNodesFinishUseCase = monitorFetchNodesFinishUseCase,
            megaUploadOptionsMapper = megaUploadOptionsMapper,
            displayPathFromUriCache = displayPathFromUriCache,
            parentNodeCache = parentNodeCache,
            transferPathCache = transferPathCache
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiGateway,
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
            deviceGateway,
            inProgressTransferMapper,
            transferAppDataStringMapper,
            activeTransferTotalsMapper,
            monitorFetchNodesFinishUseCase,
            megaUploadOptionsMapper,
            displayPathFromUriCache,
            parentNodeCache,
            transferPathCache,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UploadDownloadTests {

        private fun mockStartUpload() = megaApiGateway.startUpload(
            localPath = any(),
            parent = any(),
            fileName = anyOrNull(),
            mtime = any(),
            appData = anyOrNull(),
            isSourceTemporary = any(),
            startFirst = any(),
            cancelToken = anyOrNull(),
            listener = any(),
        )

        private fun mockStartUploadWithOptions() = megaApiGateway.startUpload(
            localPath = any(),
            parent = any(),
            cancelToken = anyOrNull(),
            options = any(),
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
                pitagTrigger = PitagTrigger.NotApplicable,
                pitagTarget = PitagTarget.NotApplicable,
            )

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
        fun `test that correct startUpload is invoked if MegaUploadOptions is get`() = runTest {
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
            whenever(mockStartUploadWithOptions()).thenAnswer {
                (it.arguments.last() as OptionalMegaTransferListenerInterface).onTransferData(
                    api = mock(),
                    transfer = mock(),
                    buffer = byteArrayOf(),
                )
            }
            whenever(transferEventMapper.invoke(any())).thenReturn(mock<TransferEvent.TransferDataEvent>())
            whenever(
                megaUploadOptionsMapper(
                    fileName = anyOrNull(),
                    mtime = anyOrNull(),
                    appData = anyOrNull(),
                    isSourceTemporary = any(),
                    startFirst = any(),
                    pitagTrigger = any(),
                    pitagTarget = any(),
                )
            ) doReturn mock()

            startUploadFlow().test {
                assertThat(awaitItem()).isNotNull()
            }

            verify(megaApiGateway).startUpload(
                localPath = any(),
                parent = any(),
                cancelToken = anyOrNull(),
                options = any(),
                listener = any()
            )
        }

        @Test
        fun `test that correct startUpload is invoked if MegaUploadOptions is null`() = runTest {
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
            whenever(mockStartUpload()).thenAnswer {
                (it.arguments.last() as OptionalMegaTransferListenerInterface).onTransferData(
                    api = mock(),
                    transfer = mock(),
                    buffer = byteArrayOf(),
                )
            }
            whenever(transferEventMapper.invoke(any())).thenReturn(mock<TransferEvent.TransferDataEvent>())
            whenever(
                megaUploadOptionsMapper(
                    fileName = anyOrNull(),
                    mtime = anyOrNull(),
                    appData = anyOrNull(),
                    isSourceTemporary = any(),
                    startFirst = any(),
                    pitagTrigger = any(),
                    pitagTarget = any(),
                )
            ) doReturn null

            startUploadFlow().test {
                assertThat(awaitItem()).isNotNull()
            }

            verify(megaApiGateway).startUpload(
                localPath = any(),
                parent = any(),
                fileName = anyOrNull(),
                mtime = anyOrNull(),
                appData = anyOrNull(),
                isSourceTemporary = any(),
                startFirst = any(),
                cancelToken = anyOrNull(),
                listener = any()
            )
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
        whenever(megaApiGateway.getTransferByTag(transferTag)).thenReturn(megaTransfer)
        whenever(transferMapper(megaTransfer)).thenReturn(transfer)
        assertThat(underTest.getTransferByTag(transferTag)).isEqualTo(transfer)
    }

    @Test
    fun `test that getTransferByTag return null when call api returns null`() = runTest {
        val transferTag = 1
        whenever(megaApiGateway.getTransferByTag(transferTag)).thenReturn(null)
        assertThat(underTest.getTransferByTag(transferTag)).isNull()
    }

    @Test
    fun `test that getInProgressTransfers empty when both getTransferData numDownloads and numUploads equal zero`() =
        runTest {
            whenever(megaApiGateway.getTransfers(any())).thenReturn(emptyList())
            assertThat(underTest.getInProgressTransfersFromSdk()).isEmpty()
        }

    @Test
    fun `test that getInProgressTransfers returns correctly when both getTransferData numDownloads and numUploads differ zero`() =
        runTest {
            whenever(megaApiGateway.getTransfers(MegaTransfer.TYPE_UPLOAD))
                .thenReturn(listOf(mock(), mock()))
            whenever(megaApiGateway.getTransfers(MegaTransfer.TYPE_DOWNLOAD))
                .thenReturn(listOf(mock()))
            whenever(transferMapper.invoke(any())).thenReturn(mock())
            assertThat(underTest.getInProgressTransfersFromSdk()).hasSize(3)
        }

    @Test
    fun `test that addCompletedTransfers call local storage gateway addCompletedTransfers and app event gateway broadcastCompletedTransfer with error`() =
        runTest {
            val transfer = mock<Transfer> {
                on { it.state } doReturn TransferState.STATE_FAILED
            }
            val expected = listOf(mock<CompletedTransfer>())
            val event = mock<TransferEvent.TransferFinishEvent> {
                on { it.transfer } doReturn transfer
            }
            whenever(completedTransferMapper(event)).thenReturn(expected.first())
            underTest.addCompletedTransfers(listOf(event))
            verify(megaLocalRoomGateway).addCompletedTransfers(expected)
        }

    @Test
    fun `test that addCompletedTransfers call local storage gateway addCompletedTransfers and app event gateway broadcastCompletedTransfer with completed`() =
        runTest {
            val transfer = mock<Transfer> {
                on { it.state } doReturn TransferState.STATE_COMPLETED
            }
            val expected = listOf(mock<CompletedTransfer>())
            val event = mock<TransferEvent.TransferFinishEvent> {
                on { it.transfer } doReturn transfer
            }
            whenever(completedTransferMapper(event)).thenReturn(expected.first())
            underTest.addCompletedTransfers(listOf(event))
            verify(megaLocalRoomGateway).addCompletedTransfers(expected)
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
    fun `test that deleteCompletedTransfersByState room gateway is called when deleteFailedOrCanceledTransfers is called`() =
        runTest {
            underTest.deleteFailedOrCancelledTransfers()
            verify(megaLocalRoomGateway).deleteCompletedTransfersByState(
                listOf(
                    MegaTransfer.STATE_FAILED,
                    MegaTransfer.STATE_CANCELLED
                )
            )
        }

    @Test
    fun `test that deleteCompletedTransfersByState room gateway is called when deleteCompletedTransfers is called`() =
        runTest {
            underTest.deleteCompletedTransfers()
            verify(megaLocalRoomGateway).deleteCompletedTransfersByState(
                listOf(MegaTransfer.STATE_COMPLETED)
            )
        }

    @Test
    fun `test that deleteCompletedTransfersById room gateway is called when deleteCompletedTransfersById is called`() =
        runTest {
            val ids = listOf(1, 2, 3)
            underTest.deleteCompletedTransfersById(ids)
            verify(megaLocalRoomGateway).deleteCompletedTransfersById(ids)
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

        private fun setActiveTransfers(transfers: List<Transfer>) = runTest {
            underTest.deleteAllActiveTransfers()
            advanceUntilIdle()
            underTest.putActiveTransfers(transfers)
        }


        @Test
        fun `test that transfer added by insertOrUpdateActiveTransfer are returned by getCurrentActiveTransfers`() =
            runTest {
                underTest.deleteAllActiveTransfers()
                val transfer = createActiveTransfer()
                underTest.putActiveTransfer(transfer)

                assertThat(underTest.getActiveTransfers()).containsExactly(transfer)
            }

        @Test
        fun `test that all transfers are added when insertOrUpdateActiveTransfers is called`() =
            runTest {
                val expected = (0..5).map { stubTransfer(uniqueId = it.toLong()) }

                setActiveTransfers(expected)

                assertThat(underTest.getActiveTransfers()).containsExactlyElementsIn(expected)
            }

        @ParameterizedTest
        @EnumSource(TransferType::class)
        fun `test that flow with correct transfers is returned when getActiveTransfersByType is called`(
            transferType: TransferType,
        ) = runTest {
            var expected: List<Transfer>? = null
            val list = TransferType.entries.flatMap { type ->
                (0..5L).map { stubTransfer(uniqueId = it + type.ordinal * 10, transferType = type) }
                    .also {
                        if (type == transferType) {
                            expected = it
                        }
                    }
            }
            setActiveTransfers(list)

            val actual = underTest.monitorActiveTransfersByType(transferType).first()
            assertThat(actual).isNotEmpty()
            assertThat(actual.size).isLessThan(list.size)
            assertThat(actual).containsExactlyElementsIn(expected)
        }

        @ParameterizedTest
        @EnumSource(TransferType::class)
        fun `test that correct transfers are returned when getCurrentActiveTransfersByType is called`(
            transferType: TransferType,
        ) = runTest {
            var expected: List<Transfer>? = null
            val list = TransferType.entries.flatMap { type ->
                (0..5L).map { stubTransfer(uniqueId = it + type.ordinal * 10, transferType = type) }
                    .also {
                        if (type == transferType) {
                            expected = it
                        }
                    }
            }
            setActiveTransfers(list)

            val actual = underTest.getActiveTransfersByType(transferType)
            assertThat(actual).isNotEmpty()
            assertThat(actual.size).isLessThan(list.size)
            assertThat(actual).containsExactlyElementsIn(expected)
        }

        @ParameterizedTest
        @EnumSource(TransferType::class, names = ["NONE"])
        fun `test that mapped current active transfers are returned when getActiveTransferTotalsByType is called`(
            transferType: TransferType,
        ) = runTest {
            val expected = mock<ActiveTransferTotals>()
            val list =
                (10..15L).map { createActiveTransfer(uniqueId = it, transferType = transferType) }
            setActiveTransfers(list)
            whenever(
                activeTransferTotalsMapper(
                    type = eq(transferType),
                    transfers = argThat { this.size == list.size && this.containsAll(list) },
                    previousActionGroups = anyOrNull()
                )
            )
                .thenReturn(expected)

            val actual = underTest.monitorActiveTransferTotalsByType(transferType).first()
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
            val firstList =
                (0..5L).map { createActiveTransfer(uniqueId = it, transferType = transferType) }
            val secondList =
                (0..6L).map { createActiveTransfer(uniqueId = it, transferType = transferType) }
            setActiveTransfers(firstList)
            whenever(
                activeTransferTotalsMapper(
                    type = eq(transferType),
                    transfers = argThat { this.size == firstList.size && this.containsAll(firstList) },
                    previousActionGroups = anyOrNull()
                )
            ) doReturn firstActiveTransferTotals
            whenever(
                activeTransferTotalsMapper(
                    type = eq(transferType),
                    transfers = argThat {
                        this.size == secondList.size && this.containsAll(
                            secondList
                        )
                    },
                    previousActionGroups = eq(actionGroups) //this comes from first emission
                )
            ) doReturn secondActiveTransferTotals
            underTest.monitorActiveTransferTotalsByType(transferType).test {
                assertThat(awaitItem()).isEqualTo(firstActiveTransferTotals)
                underTest.putActiveTransfers(secondList)
                assertThat(awaitItem()).isEqualTo(secondActiveTransferTotals)
            }
        }

        @ParameterizedTest
        @EnumSource(TransferType::class)
        fun `test that getCurrentActiveTransferTotalsByType gateway result is returned when getCurrentActiveTransferTotalsByType is called`(
            transferType: TransferType,
        ) = runTest {
            val list =
                (0..5L).map { createActiveTransfer(uniqueId = it, transferType = transferType) }
            val expected = mock<ActiveTransferTotals>()
            setActiveTransfers(list)
            whenever(activeTransferTotalsMapper(eq(transferType), eq(list), anyOrNull()))
                .thenReturn(expected)
            val actual = underTest.getCurrentActiveTransferTotalsByType(transferType)
            assertThat(actual).isEqualTo(expected)
        }

        @ParameterizedTest
        @EnumSource(TransferType::class)
        fun `test that updateTransferredBytes adds currentTransferred bytes and deleteAllActiveTransfersByType clears the values`(
            transferType: TransferType,
        ) = runTest {
            val transfer = createActiveTransfer(transferType = transferType)
            testCurrentActiveTransferTotals(
                transfer = transfer,
                callToTest = {
                    underTest.updateActiveTransfersBytes(listOf(transfer))
                }
            )
        }

        @ParameterizedTest
        @EnumSource(TransferType::class)
        fun `test that updateTransferredBytes doesn't update when the new value is 0 bytes`(
            transferType: TransferType,
        ) = runTest {
            val transfer = createActiveTransfer(transferType = transferType)
            testCurrentActiveTransferTotals(
                transfer = transfer,
                callToTest = {
                    val transferZero =
                        stubTransfer(transferType = transferType, transferredBytes = 0L)

                    underTest.updateActiveTransfersBytes(listOf(transfer))
                    underTest.updateActiveTransfersBytes(listOf(transferZero))
                }
            )
        }

        @ParameterizedTest
        @EnumSource(TransferType::class)
        fun `test that updateTransferredBytes is not updated when updateTransferredBytes is called with transfers with less progress`(
            transferType: TransferType,
        ) = runTest {
            val transfer = createActiveTransfer()
            testCurrentActiveTransferTotals(
                transfer = transfer,
                callToTest = {
                    val transferZero = mock<Transfer>()
                    stubTransfer(
                        transferZero,
                        transferType,
                        transferredBytes = transfer.transferredBytes - 1
                    )

                    underTest.updateActiveTransfersBytes(listOf(transfer))
                    underTest.updateActiveTransfersBytes(listOf(transferZero))
                }
            )
        }

        /**
         * As getCurrentActiveTransferTotalsByType is based on a state flow, we need to reset this state to make testing stateless
         * This is a convenient function to test changes on this state and then reset it to its initial empty value.
         */
        private suspend fun testCurrentActiveTransferTotals(
            transfer: Transfer,
            callToTest: suspend () -> Unit,
        ) {
            val transferType = transfer.transferType
            val list = listOf(transfer)
            setActiveTransfers(list)

            // test updateTransferredBytes
            callToTest()
            underTest.getCurrentActiveTransferTotalsByType(transferType)
            //here we check that the mapper is called with the proper expectedMap
            verify(activeTransferTotalsMapper).invoke(
                eq(transferType),
                eq(list),
                anyOrNull()
            )

            // test deleteAllActiveTransfersByType so we also clear the cached values
            underTest.deleteAllActiveTransfers()
            underTest.getCurrentActiveTransferTotalsByType(transferType)
            verify(activeTransferTotalsMapper).invoke(
                eq(transferType),
                eq(list),
                anyOrNull()
            )
        }
    }

    private fun stubTransfer(
        transfer: Transfer = mock(),
        transferType: TransferType = TransferType.DOWNLOAD,
        transferredBytes: Long = 900L,
        totalBytes: Long = 1000L,
        uniqueId: Long = 5L,
    ): Transfer {
        val tag = 1
        whenever(transfer.isFinished).thenReturn(false)
        whenever(transfer.transferType).thenReturn(transferType)
        whenever(transfer.transferredBytes).thenReturn(transferredBytes)
        whenever(transfer.totalBytes).thenReturn(totalBytes)
        whenever(transfer.tag).thenReturn(tag)
        whenever(transfer.uniqueId).thenReturn(uniqueId)
        whenever(transfer.progress).thenReturn(Progress(transferredBytes, totalBytes))
        return transfer
    }

    private fun createActiveTransfer(
        uniqueId: Long = 5L,
        transferType: TransferType = TransferType.DOWNLOAD,
        fileName: String = "test_file.txt",
        isFinished: Boolean = false,
    ) = Transfer(
        uniqueId = uniqueId,
        transferType = transferType,
        startTime = 0L,
        transferredBytes = 100L,
        totalBytes = 1000L,
        localPath = "/path/to/file",
        parentPath = "",
        nodeHandle = 0L,
        parentHandle = 0L,
        fileName = fileName,
        stage = TransferStage.STAGE_NONE,
        tag = 1,
        folderTransferTag = null,
        speed = 0L,
        isSyncTransfer = false,
        isBackupTransfer = false,
        isForeignOverQuota = false,
        isStreamingTransfer = false,
        isFinished = isFinished,
        isFolderTransfer = false,
        appData = emptyList(),
        state = TransferState.STATE_ACTIVE,
        priority = BigInteger.ZERO,
        notificationNumber = 0L,
    )

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
    fun `test that in progress transfers flow is updated correctly when updateInProgressTransfers and removeInProgressTransfers are called`() =
        runTest {
            val uniqueId = 5L
            val transfer = mock<Transfer> {
                on { it.uniqueId } doReturn uniqueId
                on { it.isFolderTransfer } doReturn false
            }
            val inProgressTransfer = mock<InProgressTransfer.Download> {
                on { it.uniqueId } doReturn uniqueId
            }
            whenever(inProgressTransferMapper(transfer)).thenReturn(inProgressTransfer)
            setUp()
            underTest.monitorInProgressTransfers().test {
                assertThat(awaitItem()).isEmpty()
                underTest.updateInProgressTransfers(listOf(transfer))

                assertThat(awaitItem()).containsExactly(uniqueId, inProgressTransfer)
                underTest.removeInProgressTransfers(setOf(uniqueId))

                assertThat(awaitItem()).isEmpty()
            }
        }

    @Test
    fun `test that in progress transfers flow is updated correctly when updateInProgressTransfers is called with updated and finished transfers`() =
        runTest {
            val uniqueId1 = 5L
            val uniqueId2 = 6L
            val transfer1 = mock<Transfer> {
                on { it.uniqueId } doReturn uniqueId1
            }
            val inProgressTransfer1 = mock<InProgressTransfer.Download> {
                on { it.uniqueId } doReturn uniqueId1
            }
            val transfer2 = mock<Transfer> {
                on { it.uniqueId } doReturn uniqueId2
            }
            val inProgressTransfer2 = mock<InProgressTransfer.Download> {
                on { it.uniqueId } doReturn uniqueId2
            }

            whenever(inProgressTransferMapper(transfer1)).thenReturn(inProgressTransfer1)
            whenever(inProgressTransferMapper(transfer2)).thenReturn(inProgressTransfer2)

            setUp()
            underTest.monitorInProgressTransfers().test {
                assertThat(awaitItem()).isEmpty()

                underTest.updateInProgressTransfers(listOf(transfer1))

                assertThat(awaitItem()).containsExactly(uniqueId1, inProgressTransfer1)

                underTest.updateInProgressTransfers(listOf(transfer2), listOf(uniqueId1))

                assertThat(awaitItem()).containsExactly(uniqueId2, inProgressTransfer2)
            }
        }

    @Test
    fun `test that in progress transfers progress is not updated when updateInProgressTransfer is called with transfers with less progress`() =
        runTest {
            val initialProgress = Progress(0.2f)
            val regressedProgress = Progress(0.1f)
            val totalBytes = 10_000L
            val uniqueId = 6L
            val transfer1 = stubTransfer(
                uniqueId = uniqueId,
                transferredBytes = initialProgress * totalBytes,
                totalBytes = totalBytes,
            )

            val transfer2 = stubTransfer(
                uniqueId = uniqueId,
                transferredBytes = regressedProgress * totalBytes,
                totalBytes = totalBytes,
            )
            whenever(transfer2.copy(transferredBytes = initialProgress * totalBytes)) doReturn transfer1

            val inProgressTransfer1 = mock<InProgressTransfer.Download> {
                on { it.uniqueId } doReturn uniqueId
                on { it.progress } doReturn initialProgress
                on { it.totalBytes } doReturn totalBytes
            }

            val inProgressTransfer2 = mock<InProgressTransfer.Download> {
                on { it.uniqueId } doReturn uniqueId
                on { it.progress } doReturn regressedProgress
                on { it.totalBytes } doReturn totalBytes
            }

            whenever(inProgressTransferMapper(transfer1)).thenReturn(inProgressTransfer1)
            whenever(inProgressTransferMapper(transfer2)).thenReturn(inProgressTransfer2)

            setUp()
            underTest.monitorInProgressTransfers().test {
                assertThat(awaitItem()).isEmpty()

                underTest.updateInProgressTransfers(listOf(transfer1))
                underTest.updateInProgressTransfers(listOf(transfer2))
                underTest.updateInProgressTransfers(listOf(transfer2), emptyList())

                assertThat(awaitItem().values.single().progress).isEqualTo(initialProgress)
                this.ensureAllEventsConsumed()
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
            val uniqueId = 15L
            val expected = mock<PendingTransfer>()
            whenever(megaLocalRoomGateway.getPendingTransfersByUniqueId(uniqueId)).thenReturn(
                expected
            )
            val actual = underTest.getPendingTransfersByUniqueId(uniqueId)
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

    @Test
    fun `test that delete Active Transfer Group invokes room gateway`() = runTest {
        val groupId = 454234
        underTest.deleteActiveTransferGroup(groupId)

        verify(megaLocalRoomGateway).deleteActiveTransferGroup(groupId)
    }

    @ParameterizedTest(name = " when tag is {0}")
    @ValueSource(ints = [123])
    @NullSource
    fun `test that broadcastTransferUniqueIdToCancel invokes correct appEventGateway fun`(
        transferTag: Int?,
    ) = runTest {
        whenever(appEventGateway.broadcastTransferTagToCancel(transferTag)) doReturn Unit

        underTest.broadcastTransferTagToCancel(transferTag)

        verify(appEventGateway).broadcastTransferTagToCancel(transferTag)
    }

    @Test
    fun `test that monitorTransferUniqueIdToCancel invokes correct appEventGateway fun and emits value`() =
        runTest {
            val transferTag = 123
            val nullValue: Int? = null

            whenever(appEventGateway.monitorTransferTagToCancel()) doReturn
                    flowOf(transferTag, nullValue)

            underTest.monitorTransferTagToCancel().test {
                assertThat(awaitItem()).isEqualTo(transferTag)
                assertThat(awaitItem()).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that monitorTransferOverQuotaErrorTimestamp emits value when setTransferOverQuotaErrorTimestamp is invoked`() =
        runTest {
            val currentTime = 12356L
            val expected = Instant.fromEpochMilliseconds(currentTime)

            whenever(deviceGateway.getCurrentTimeInMillis()).thenReturn(currentTime)

            underTest.setTransferOverQuotaErrorTimestamp()
            underTest.monitorTransferOverQuotaErrorTimestamp().test {
                assertThat(awaitItem()).isEqualTo(expected)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that resumeTransfersForNotLoggedInInstance invokes megaApi method`() = runTest {
        whenever(megaApiGateway.resumeTransfersForNotLoggedInInstance()) doReturn Unit

        underTest.resumeTransfersForNotLoggedInInstance()

        verify(megaApiGateway).resumeTransfersForNotLoggedInInstance()
        verifyNoMoreInteractions(megaApiGateway)
    }

    @ParameterizedTest
    @ValueSource(longs = [123456, 9876543])
    fun `test that transferOverQuotaTimestamp returns value`(
        timestamp: Long,
    ) = runTest {
        val value = AtomicLong(timestamp)

        underTest.transferOverQuotaTimestamp = value

        assertThat(underTest.transferOverQuotaTimestamp).isEqualTo(value)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnLogoutSuccessTests {

        @Test
        fun `test that onLogoutSuccess resets paused transfers asked resume over quota timestamp error status and atomic timestamp`() =
            runTest {
                underTest = createDefaultTransfersRepository()
                stubPauseTransfers(true)
                underTest.pauseTransfers(true)
                underTest.setAskedResumeTransfers()

                val currentTime = 99L
                whenever(deviceGateway.getCurrentTimeInMillis()).thenReturn(currentTime)
                underTest.setTransferOverQuotaErrorTimestamp()

                val failedTransfer = mock<Transfer> {
                    on { it.state } doReturn TransferState.STATE_FAILED
                }
                val finishEvent = mock<TransferEvent.TransferFinishEvent> {
                    on { it.transfer } doReturn failedTransfer
                }
                whenever(completedTransferMapper(finishEvent)).thenReturn(mock())
                underTest.addCompletedTransfers(listOf(finishEvent))

                underTest.transferOverQuotaTimestamp = AtomicLong(42L)

                assertThat(underTest.monitorPausedTransfers().value).isTrue()
                assertThat(underTest.monitorAskedResumeTransfers().value).isTrue()
                assertThat(underTest.monitorTransferOverQuotaErrorTimestamp().value)
                    .isEqualTo(Instant.fromEpochMilliseconds(currentTime))
                assertThat(underTest.monitorTransferInErrorStatus().value).isTrue()
                assertThat(underTest.transferOverQuotaTimestamp.get()).isEqualTo(42L)

                underTest.onLogoutSuccess()

                assertThat(underTest.monitorPausedTransfers().value).isFalse()
                assertThat(underTest.monitorAskedResumeTransfers().value).isFalse()
                assertThat(underTest.monitorTransferOverQuotaErrorTimestamp().value).isNull()
                assertThat(underTest.monitorTransferInErrorStatus().value).isFalse()
                assertThat(underTest.transferOverQuotaTimestamp.get()).isEqualTo(0L)

                verify(displayPathFromUriCache).clear()
                verify(parentNodeCache).clear()
                verify(transferPathCache).clear()
            }

        @Test
        fun `test that onLogoutSuccess clears in progress transfers and active transfers`() =
            runTest {
                underTest = createDefaultTransfersRepository()

                val uniqueId = 7L
                val transfer = mock<Transfer> {
                    on { it.uniqueId } doReturn uniqueId
                    on { it.isFolderTransfer } doReturn false
                }
                val inProgressTransfer = mock<InProgressTransfer.Download> {
                    on { it.uniqueId } doReturn uniqueId
                }
                whenever(inProgressTransferMapper(transfer)).thenReturn(inProgressTransfer)
                underTest.updateInProgressTransfers(listOf(transfer))

                val activeTransfer =
                    stubTransfer(uniqueId = 8L, transferType = TransferType.DOWNLOAD)
                underTest.putActiveTransfer(activeTransfer)

                assertThat(underTest.monitorInProgressTransfers().value).isNotEmpty()
                assertThat(underTest.getActiveTransfers()).isNotEmpty()

                underTest.onLogoutSuccess()

                assertThat(underTest.monitorInProgressTransfers().value).isEmpty()
                assertThat(underTest.getActiveTransfers()).isEmpty()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MonitorTransferInErrorStatusTests {
        private fun mockFailedTransferEvent(): TransferEvent.TransferFinishEvent {
            val transfer = mock<Transfer>()
            val transferEvent = mock<TransferEvent.TransferFinishEvent>()
            whenever(transfer.state) doReturn TransferState.STATE_FAILED
            whenever(transfer.uniqueId) doReturn 32453L
            whenever(transferEvent.error) doReturn mock()
            whenever(transferEvent.transfer) doReturn transfer
            return transferEvent
        }

        @Test
        fun `test that monitorTransferInErrorStatus change to true when a failed transfer is added with addCompletedTransfers`() =
            runTest {
                underTest.clearTransferErrorStatus()
                underTest.monitorTransferInErrorStatus().test {
                    assertThat(awaitItem()).isFalse()
                    underTest.addCompletedTransfers(listOf(mockFailedTransferEvent()))
                    assertThat(awaitItem()).isTrue()
                }
            }

        @Test
        fun `test that monitorTransferInErrorStatus change to true when a failed transfer is added with addCompletedTransferFromFailedPendingTransfer`() =
            runTest {
                underTest.clearTransferErrorStatus()
                underTest.monitorTransferInErrorStatus().test {
                    assertThat(awaitItem()).isFalse()
                    underTest.addCompletedTransferFromFailedPendingTransfer(mock(), 0L, mock())
                    assertThat(awaitItem()).isTrue()
                }
            }

        @Test
        fun `test that monitorTransferInErrorStatus change to true when a failed transfer is added with addCompletedTransferFromFailedPendingTransfers`() =
            runTest {
                underTest.clearTransferErrorStatus()
                underTest.monitorTransferInErrorStatus().test {
                    assertThat(awaitItem()).isFalse()
                    underTest.addCompletedTransferFromFailedPendingTransfers(listOf(mock()), mock())
                    assertThat(awaitItem()).isTrue()
                }
            }

        @Test
        fun `test that monitorTransferInErrorStatus change to false when clearTransferErrorStatus is invoked`() =
            runTest {
                underTest.addCompletedTransfers(listOf(mockFailedTransferEvent()))
                underTest.monitorTransferInErrorStatus().test {
                    assertThat(awaitItem()).isTrue()
                    underTest.clearTransferErrorStatus()
                    assertThat(awaitItem()).isFalse()
                }
            }
    }
}
