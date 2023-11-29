package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.WorkManagerGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import mega.privacy.android.data.mapper.transfer.AppDataTypeConstants
import mega.privacy.android.data.mapper.transfer.CompletedTransferMapper
import mega.privacy.android.data.mapper.transfer.PausedTransferEventMapper
import mega.privacy.android.data.mapper.transfer.TransferAppDataStringMapper
import mega.privacy.android.data.mapper.transfer.TransferDataMapper
import mega.privacy.android.data.mapper.transfer.TransferEventMapper
import mega.privacy.android.data.mapper.transfer.TransferMapper
import mega.privacy.android.data.mapper.transfer.active.ActiveTransferTotalsMapper
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.data.model.RequestEvent
import mega.privacy.android.domain.entity.SdTransfer
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransferData
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
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
import org.mockito.kotlin.eq
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
    private val megaChatApiGateway = mock<MegaChatApiGateway>()
    private val megaApiFolderGateway = mock<MegaApiFolderGateway>()
    private val transferEventMapper = mock<TransferEventMapper>()
    private val appEventGateway: AppEventGateway = mock()
    private val transferMapper: TransferMapper = mock()
    private val transferAppDataStringMapper: TransferAppDataStringMapper = mock()
    private val pausedTransferEventMapper = mock<PausedTransferEventMapper>()
    private val completedTransferMapper = mock<CompletedTransferMapper>()
    private val localStorageGateway: MegaLocalStorageGateway = mock()
    private val workerManagerGateway = mock<WorkManagerGateway>()
    private val megaLocalRoomGateway = mock<MegaLocalRoomGateway>()
    private val transferDataMapper = mock<TransferDataMapper>()
    private val cancelTokenProvider = mock<CancelTokenProvider>()
    private val activeTransferTotalsMapper = mock<ActiveTransferTotalsMapper>()

    private val testScope = CoroutineScope(UnconfinedTestDispatcher())

    @BeforeAll
    fun setUp() = runTest {
        underTest = createDefaultTransfersRepository()
    }

    private suspend fun createDefaultTransfersRepository(paused: Boolean = false): DefaultTransfersRepository {
        //need to stub this method as it's called on init
        whenever(localStorageGateway.getTransferQueueStatus()).thenReturn(paused)
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
            localStorageGateway = localStorageGateway,
            workerManagerGateway = workerManagerGateway,
            megaLocalRoomGateway = megaLocalRoomGateway,
            transferDataMapper = transferDataMapper,
            cancelTokenProvider = cancelTokenProvider,
            scope = testScope,
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
            transferDataMapper,
            cancelTokenProvider,
            completedTransferMapper,
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
                node = mock<TypedFileNode>(),
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Test start download with chat files")
    inner class StartDownloadWithChatFile {

        private val chatId = 11L
        private val messageId = 22L
        private val path = "path"

        private val megaNode = mock<MegaNode>()
        private val megaChatMessage = mock<MegaChatMessage>()
        private val megaNodeList = mock<MegaNodeList>()
        private val chatFile = mock<ChatDefaultFile>()
        fun resetMocks() = reset(megaNode, megaChatMessage, megaNodeList, chatFile)

        private fun commonSetup() {
            whenever(chatFile.chatId).thenReturn(chatId)
            whenever(chatFile.messageId).thenReturn(messageId)
            whenever(chatFile.messageIndex).thenReturn(0)
            whenever(megaNodeList.get(0)).thenReturn(megaNode)
            whenever(megaChatMessage.megaNodeList).thenReturn(megaNodeList)
        }

        @Test
        fun `test that start download is started with the correct chat node from gateway when start download with ChatFile`() =
            runTest {
                commonSetup()
                whenever(megaChatApiGateway.getMessage(chatId, messageId))
                    .thenReturn(megaChatMessage)
                verifyStartChatFile()
            }

        @Test
        fun `test that chat node history is used as a fallback when node is not found`() = runTest {
            commonSetup()
            whenever(megaChatApiGateway.getMessage(chatId, messageId)).thenReturn(null)
            whenever(megaChatApiGateway.getMessageFromNodeHistory(chatId, messageId))
                .thenReturn(megaChatMessage)
            verifyStartChatFile()
        }

        @Test
        fun `test that chat node is authorized if is in chat preview`() = runTest {
            commonSetup()
            whenever(megaChatApiGateway.getMessage(chatId, messageId)).thenReturn(megaChatMessage)
            val megaNodeAuthorized = mock<MegaNode>()
            val authorizationToken = "token"
            val chat = mock<MegaChatRoom> {
                on { isPreview }.thenReturn(true)
                on { this.authorizationToken }.thenReturn(authorizationToken)
            }
            whenever(megaChatApiGateway.getChatRoom(chatId)).thenReturn(chat)
            whenever(megaApiGateway.authorizeChatNode(megaNode, authorizationToken))
                .thenReturn(megaNodeAuthorized)
            verifyStartChatFile(megaNodeAuthorized)
            verify(megaApiGateway).authorizeChatNode(megaNode, authorizationToken)
        }

        private suspend fun verifyStartChatFile(node: MegaNode = megaNode) {
            underTest.startDownload(chatFile, path, null, false).test {
                cancelAndIgnoreRemainingEvents()
            }
            verify(megaApiGateway).startDownload(
                node = eq(node),
                localPath = eq(path),
                fileName = anyOrNull(),
                appData = anyOrNull(),
                startFirst = any(),
                cancelToken = anyOrNull(),
                collisionCheck = any(),
                collisionResolution = any(),
                listener = anyOrNull()
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Test start download with public links")
    inner class StartDownloadWithPublicLinks {

        @Test
        fun `test that start download is started with the correct node from gateway when start download with PublicLinkFolder`() =
            runTest {
                val handle = 23L
                val nodeId = NodeId(handle)
                val publicLinkFolder = mock<PublicLinkFolder> {
                    on { this.id }.thenReturn(nodeId)
                }
                val megaNode = mock<MegaNode>()
                val authorizedNode = mock<MegaNode>()

                whenever(megaApiFolderGateway.getMegaNodeByHandle(handle)).thenReturn(megaNode)
                whenever(megaApiFolderGateway.authorizeNode(megaNode)).thenReturn(authorizedNode)
                underTest.startDownload(publicLinkFolder, "path", null, false).test {
                    cancelAndIgnoreRemainingEvents()
                }
                verify(megaApiGateway).startDownload(
                    node = eq(authorizedNode),
                    localPath = anyOrNull(),
                    fileName = anyOrNull(),
                    appData = anyOrNull(),
                    startFirst = any(),
                    cancelToken = anyOrNull(),
                    collisionCheck = any(),
                    collisionResolution = any(),
                    listener = anyOrNull()
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
    fun `test that addCompletedTransfer call local storage gateway addCompletedTransfer and app event gateway broadcastCompletedTransfer with the mapped transfer`() =
        runTest {
            val transfer = mock<Transfer>()
            val error = mock<MegaException>()
            val expected = mock<CompletedTransfer>()
            whenever(completedTransferMapper(transfer, error)).thenReturn(expected)
            underTest.addCompletedTransfer(transfer, error)
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
        val finish = GlobalTransfer.OnTransferFinish(mock(), mock())
        val startEvent = TransferEvent.TransferStartEvent(mock())
        val finishEvent = TransferEvent.TransferFinishEvent(mock(), mock())
        val globalTransferEventsFlow = flowOf(start, finish)
        whenever(transferEventMapper(start)).thenReturn(startEvent)
        whenever(transferEventMapper(finish)).thenReturn(finishEvent)
        whenever(megaApiGateway.globalRequestEvents).thenReturn(emptyFlow())
        whenever(megaApiGateway.globalTransfer).thenReturn(globalTransferEventsFlow)
        underTest.monitorTransferEvents().test {
            assertThat(awaitItem()).isEqualTo(startEvent)
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
    fun `test that insertSdTransfer invokes when insertSdTransfer is called`() = runTest {
        val sdTransfer = mock<SdTransfer>()
        underTest.insertSdTransfer(sdTransfer)
        verify(megaLocalRoomGateway).insertSdTransfer(sdTransfer)
    }

    @Test
    fun `test that deleteSdTransferByTag invokes when deleteSdTransferByTag is called`() = runTest {
        val tag = 1
        underTest.deleteSdTransferByTag(tag)
        verify(megaLocalRoomGateway).deleteSdTransferByTag(tag)
    }

    @Test
    fun `test that getAllSdTransfers invokes when getAllSdTransfers is called`() = runTest {
        underTest.getAllSdTransfers()
        verify(megaLocalRoomGateway).getAllSdTransfers()
    }

    @Test
    fun `test that getCompletedTransferById invokes when getCompletedTransferById is called`() =
        runTest {
            val id = 1
            underTest.getCompletedTransferById(id)
            verify(megaLocalRoomGateway).getCompletedTransferById(id)
        }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PendingCounters {
        @Test
        fun `test that getNumPendingGeneralUploads returns correctly`() = runTest {
            stubUploadTransfers()
            assertThat(underTest.getNumPendingGeneralUploads()).isEqualTo(2)
        }

        @Test
        fun `test that getNumPendingCameraUploads returns correctly`() = runTest {
            stubUploadTransfers()
            assertThat(underTest.getNumPendingCameraUploads()).isEqualTo(2)
        }

        @Test
        fun `test that getNumPendingChatUploads returns correctly`() = runTest {
            stubUploadTransfers()
            assertThat(underTest.getNumPendingChatUploads()).isEqualTo(2)
        }

        @Test
        fun `test that getNumPendingPausedGeneralUploads returns correctly`() = runTest {
            stubUploadTransfers()
            assertThat(underTest.getNumPendingPausedGeneralUploads()).isEqualTo(1)
        }

        @Test
        fun `test that getNumPendingPausedCameraUploads returns correctly`() = runTest {
            stubUploadTransfers()
            assertThat(underTest.getNumPendingPausedCameraUploads()).isEqualTo(1)
        }

        @Test
        fun `test that getNumPendingPausedChatUploads returns correctly`() = runTest {
            stubUploadTransfers()
            assertThat(underTest.getNumPendingPausedChatUploads()).isEqualTo(1)
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
        fun `test that setActiveTransferAsFinishedByTag gateway is called when setActiveTransferAsFinishedByTag is called`(
        ) = runTest {
            val tags = mock<List<Int>>()
            underTest.setActiveTransferAsFinishedByTag(tags)
            verify(megaLocalRoomGateway).setActiveTransferAsFinishedByTag(tags)
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
            whenever(activeTransferTotalsMapper(eq(transferType), eq(list), any()))
                .thenReturn(expected)
            val actual = underTest.getActiveTransferTotalsByType(transferType).first()
            assertThat(actual).isEqualTo(expected)
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
            whenever(activeTransferTotalsMapper(eq(transferType), eq(list), any()))
                .thenReturn(expected)
            val actual = underTest.getCurrentActiveTransferTotalsByType(transferType)
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `test that workerManagerGateway enqueueDownloadsWorkerRequest is called when startDownloadWorker is called`() {
            underTest.startDownloadWorker()
            verify(workerManagerGateway).enqueueDownloadsWorkerRequest()
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
                    underTest.updateTransferredBytes(transfer)
                }
            )
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
            stubActiveTransfer(transferType)
            val list = mock<List<ActiveTransfer>>()
            whenever(megaLocalRoomGateway.getCurrentActiveTransfersByType(transferType))
                .thenReturn(list)

            // test updateTransferredBytes
            callToTest()
            underTest.getCurrentActiveTransferTotalsByType(transferType)
            //here we check that the mapper is called with the proper expectedMap
            val map = expectedMap(transfer)
            verify(activeTransferTotalsMapper).invoke(eq(transferType), eq(list), eq(map))

            // test deleteAllActiveTransfersByType so we also clear the cached values
            underTest.deleteAllActiveTransfersByType(transferType)
            underTest.getCurrentActiveTransferTotalsByType(transferType)
            //here we check that the mapper is called with the proper expectedMap
            verify(activeTransferTotalsMapper).invoke(eq(transferType), eq(list), eq(emptyMap()))
        }

        private fun stubActiveTransfer(transferType: TransferType) {
            val transferred = 900L
            val total = 1024L
            val tag = 1

            whenever(transfer.transferType).thenReturn(transferType)
            whenever(transfer.transferredBytes).thenReturn(transferred)
            whenever(transfer.totalBytes).thenReturn(total)
            whenever(transfer.tag).thenReturn(tag)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DownloadCountersTest {

        @ParameterizedTest(name = " total downloads is {0}")
        @ValueSource(ints = [0, 7, 200])
        fun `test that getTotalDownloads returns correctly if`(
            downloads: Int,
        ) = runTest {
            whenever(megaApiGateway.totalDownloads).thenReturn(downloads)
            assertThat(underTest.getTotalDownloads()).isEqualTo(downloads)
        }

        @ParameterizedTest(name = " megaApi call returns {0}")
        @ValueSource(ints = [0, 7, 200])
        fun `test that getCurrentDownloadSpeed returns correctly if`(
            speed: Int,
        ) = runTest {
            whenever(megaApiGateway.currentDownloadSpeed).thenReturn(speed)
            assertThat(underTest.getCurrentDownloadSpeed()).isEqualTo(speed)
        }

        @ParameterizedTest(name = " megaApi call returns {0}")
        @ValueSource(longs = [0, 7, 200])
        fun `test that getTotalDownloadedBytes returns correctly if`(
            bytes: Long,
        ) = runTest {
            whenever(megaApiGateway.totalDownloadedBytes).thenReturn(bytes)
            assertThat(underTest.getTotalDownloadedBytes()).isEqualTo(bytes)
        }

        @ParameterizedTest(name = " megaApi call returns {0}")
        @ValueSource(longs = [0, 7, 200])
        fun `test that getTotalDownloadBytes returns correctly if`(
            bytes: Long,
        ) = runTest {
            whenever(megaApiGateway.totalDownloadBytes).thenReturn(bytes)
            assertThat(underTest.getTotalDownloadBytes()).isEqualTo(bytes)
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
}
