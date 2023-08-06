package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.WorkManagerGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import mega.privacy.android.data.mapper.transfer.TransferAppDataStringMapper
import mega.privacy.android.data.mapper.transfer.TransferDataMapper
import mega.privacy.android.data.mapper.transfer.TransferEventMapper
import mega.privacy.android.data.mapper.transfer.TransferMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.MegaException
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
    private val localStorageGateway: MegaLocalStorageGateway = mock()
    private val workerManagerGateway = mock<WorkManagerGateway>()
    private val megaLocalRoomGateway = mock<MegaLocalRoomGateway>()
    private val transferDataMapper = mock<TransferDataMapper>()
    private val cancelTokenProvider = mock<CancelTokenProvider>()

    @BeforeAll
    fun setUp() {
        underTest = DefaultTransfersRepository(
            megaApiGateway = megaApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            transferEventMapper = transferEventMapper,
            appEventGateway = appEventGateway,
            transferMapper = transferMapper,
            transferAppDataStringMapper = transferAppDataStringMapper,
            localStorageGateway = localStorageGateway,
            workerManagerGateway = workerManagerGateway,
            megaLocalRoomGateway = megaLocalRoomGateway,
            transferDataMapper = transferDataMapper,
            cancelTokenProvider = cancelTokenProvider,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiGateway,
            transferEventMapper,
            appEventGateway,
            transferMapper,
            localStorageGateway,
            workerManagerGateway,
            megaLocalRoomGateway,
            transferDataMapper,
            cancelTokenProvider,
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

        private fun startUploadFlow() =
            underTest.startUpload(
                localPath = "test local path",
                parentNodeId = NodeId(1L),
                fileName = "test filename",
                modificationTime = 123456789L,
                appData = null,
                isSourceTemporary = false,
                shouldStartFirst = false,
            )

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
                nodeId = NodeId(1L),
                localPath = "test local path",
                appData = null,
                shouldStartFirst = false,
            )

        private fun provideParameters() = listOf(
            Arguments.of(
                { mockStartUpload() }, { startUploadFlow() }),
            Arguments.of(
                { mockStartDownload() }, { startDownloadFlow() }
            )
        )

        @ParameterizedTest
        @MethodSource("provideParameters")
        fun `test that OnTransferStart is returned when the upload and download begins`(
            mockStart: () -> Unit, startFlow: () -> Flow<TransferEvent>,
        ) = runTest {
            whenever(mockStart()).thenAnswer {
                (it.arguments[8] as OptionalMegaTransferListenerInterface).onTransferStart(
                    api = mock(),
                    transfer = mock(),
                )
            }
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
            val expected = mock<TransferEvent.TransferStartEvent>()
            whenever(transferEventMapper.invoke(any())).thenReturn(expected)
            startFlow().test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }

        @ParameterizedTest
        @MethodSource("provideParameters")
        fun `test that OnTransferFinished is returned when the upload and download is finished`(
            mockStart: () -> Unit, startFlow: () -> Flow<TransferEvent>,
        ) = runTest {
            whenever(mockStart()).thenAnswer {
                (it.arguments[8] as OptionalMegaTransferListenerInterface).onTransferFinish(
                    api = mock(),
                    transfer = mock(),
                    error = mock { on { errorCode }.thenReturn(MegaError.API_OK) },
                )
            }
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
            val expected = mock<TransferEvent.TransferFinishEvent>()
            whenever(transferEventMapper.invoke(any())).thenReturn(expected)
            startFlow().test {
                assertThat(awaitItem()).isEqualTo(expected)
                awaitComplete()
            }
            verify(megaApiGateway).removeTransferListener(any())
        }

        @ParameterizedTest
        @MethodSource("provideParameters")
        fun `test that OnTransferFinished is returned when the download and upload is finished`(
            mockStart: () -> Unit, startFlow: () -> Flow<TransferEvent>,
        ) = runTest {
            whenever(mockStart()).thenAnswer {
                (it.arguments[8] as OptionalMegaTransferListenerInterface).onTransferFinish(
                    api = mock(),
                    transfer = mock(),
                    error = mock { on { errorCode }.thenReturn(MegaError.API_OK) },
                )
            }
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
            val expected = mock<TransferEvent.TransferFinishEvent>()
            whenever(transferEventMapper.invoke(any())).thenReturn(expected)
            startFlow().test {
                assertThat(awaitItem()).isEqualTo(expected)
                awaitComplete()
            }
            verify(megaApiGateway).removeTransferListener(any())
        }

        @ParameterizedTest
        @MethodSource("provideParameters")
        fun `test that OnTransferUpdate is returned when the ongoing upload has been updated`(
            mockStart: () -> Unit, startFlow: () -> Flow<TransferEvent>,
        ) = runTest {
            whenever(mockStart()).thenAnswer {
                (it.arguments[8] as OptionalMegaTransferListenerInterface).onTransferUpdate(
                    api = mock(),
                    transfer = mock(),
                )
            }
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
            val expected = mock<TransferEvent.TransferUpdateEvent>()
            whenever(transferEventMapper.invoke(any())).thenReturn(expected)
            startFlow().test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }

        @ParameterizedTest
        @MethodSource("provideParameters")
        fun `test that OnTransferTemporaryError is returned when the upload experiences a temporary error`(
            mockStart: () -> Unit, startFlow: () -> Flow<TransferEvent>,
        ) = runTest {
            whenever(mockStart()).thenAnswer {
                (it.arguments[8] as OptionalMegaTransferListenerInterface).onTransferTemporaryError(
                    api = mock(),
                    transfer = mock(),
                    error = mock { on { errorCode }.thenReturn(MegaError.API_OK + 1) },
                )
            }
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
            val expected = mock<TransferEvent.TransferTemporaryErrorEvent>()
            whenever(transferEventMapper.invoke(any())).thenReturn(expected)
            startFlow().test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }

        @ParameterizedTest
        @MethodSource("provideParameters")
        fun `test that OnTransferData is returned when the upload data is being read`(
            mockStart: () -> Unit, startFlow: () -> Flow<TransferEvent>,
        ) = runTest {
            whenever(mockStart()).thenAnswer {
                (it.arguments[8] as OptionalMegaTransferListenerInterface).onTransferData(
                    api = mock(),
                    transfer = mock(),
                    buffer = byteArrayOf(),
                )
            }
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(mock())
            val expected = mock<TransferEvent.TransferDataEvent>()
            whenever(transferEventMapper.invoke(any())).thenReturn(expected)
            startFlow().test {
                assertThat(awaitItem()).isEqualTo(expected)
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
    fun `test that addCompletedTransfer call local storage gateway addCompletedTransfer and app event gateway broadcastCompletedTransfer`() =
        runTest {
            val expected = mock<CompletedTransfer>()
            underTest.addCompletedTransfer(expected)
            verify(megaLocalRoomGateway).addCompletedTransfer(expected)
            verify(appEventGateway).broadcastCompletedTransfer(expected)
        }

    @Test
    fun `test that addCompletedTransfer call correctly when call addCompletedTransfersIfNotExist`() =
        runTest {
            val transfer1 = CompletedTransfer(
                id = 1,
                fileName = "filename1",
                type = 1,
                state = 1,
                size = "1Kb",
                handle = 1L,
                path = "filePath",
                isOffline = false,
                timestamp = 123L,
                error = null,
                originalPath = "originalFilePath",
                parentHandle = 2L
            )
            val transfer2 = CompletedTransfer(
                id = 1,
                fileName = "filename2",
                type = 1,
                state = 1,
                size = "1Kb",
                handle = 1L,
                path = "filePath",
                isOffline = false,
                timestamp = 123L,
                error = null,
                originalPath = "originalFilePath",
                parentHandle = 2L
            )
            val existingTransfer1 = CompletedTransfer(
                id = 3,
                fileName = "filename1",
                type = 1,
                state = 1,
                size = "1Kb",
                handle = 1L,
                path = "filePath",
                isOffline = false,
                timestamp = 123L,
                error = null,
                originalPath = "originalFilePath",
                parentHandle = 2L
            )
            whenever(megaLocalRoomGateway.getAllCompletedTransfers())
                .thenReturn(flowOf(listOf(existingTransfer1)))
            underTest.addCompletedTransfersIfNotExist(listOf(transfer1, transfer2))
            verify(megaLocalRoomGateway).addCompletedTransfer(transfer2.copy(id = null))
            verify(megaLocalRoomGateway, times(0)).addCompletedTransfer(transfer1.copy(id = null))
        }

    @Test
    fun `test that monitorCompletedTransfer returns the result of app event gateway monitorCompletedTransfer`() =
        runTest {
            val expected = mock<CompletedTransfer>()
            whenever(appEventGateway.monitorCompletedTransfer).thenReturn(flowOf(expected))
            underTest.monitorCompletedTransfer().test {
                assertThat(awaitItem()).isEqualTo(expected)
                awaitComplete()
            }
        }

    @Test
    @Suppress("DEPRECATION")
    fun `test that reset total uploads is invoked`() = runTest {
        underTest.resetTotalUploads()
        verify(megaApiGateway).resetTotalUploads()
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
                    TransferState.STATE_FAILED,
                    TransferState.STATE_CANCELLED
                )
            )
        }

    @Test
    fun `test that deleteCompletedTransfersByState room gateway is called when deleteFailedOrCanceledTransfers is called`() =
        runTest {
            underTest.deleteFailedOrCanceledTransfers()
            verify(megaLocalRoomGateway).deleteCompletedTransfersByState(
                listOf(
                    TransferState.STATE_FAILED,
                    TransferState.STATE_CANCELLED
                )
            )
        }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ActiveTransfersTest {
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
        fun `test that getActiveTransfersByType gateway first result is returned when getCurrentActiveTransfersByType is called`(
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
        fun `test that insertOrUpdateActiveTransfer gateway is called when insertOrUpdateActiveTransfer is called`() =
            runTest {
                val activeTransfer = mock<ActiveTransfer>()
                underTest.insertOrUpdateActiveTransfer(activeTransfer)
                verify(megaLocalRoomGateway).insertOrUpdateActiveTransfer(activeTransfer)
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
        fun `test that deleteActiveTransferByTag gateway is called when deleteActiveTransferByTag is called`(
        ) = runTest {
            val tags = mock<List<Int>>()
            underTest.deleteActiveTransferByTag(tags)
            verify(megaLocalRoomGateway).deleteActiveTransferByTag(tags)
        }

        @ParameterizedTest
        @EnumSource(TransferType::class)
        fun `test that getActiveTransferTotalsByType gateway result is returned when getActiveTransferTotalsByType is called`(
            transferType: TransferType,
        ) = runTest {
            val expected = mock<ActiveTransferTotals>()
            val flow = flowOf(expected)
            whenever(megaLocalRoomGateway.getActiveTransferTotalsByType(transferType))
                .thenReturn(flow)
            val actual = underTest.getActiveTransferTotalsByType(transferType).first()
            assertThat(actual).isEqualTo(expected)
        }

        @ParameterizedTest
        @EnumSource(TransferType::class)
        fun `test that getCurrentActiveTransferTotalsByType gateway result is returned when getCurrentActiveTransferTotalsByType is called`(
            transferType: TransferType,
        ) = runTest {
            val expected = mock<ActiveTransferTotals>()
            whenever(megaLocalRoomGateway.getCurrentActiveTransferTotalsByType(transferType))
                .thenReturn(expected)
            val actual = underTest.getCurrentActiveTransferTotalsByType(transferType)
            assertThat(actual).isEqualTo(expected)
        }
    }
}
