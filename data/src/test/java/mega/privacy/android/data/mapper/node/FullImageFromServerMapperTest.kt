package mega.privacy.android.data.mapper.node

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import mega.privacy.android.data.mapper.transfer.TransferQuotaStateMapper
import mega.privacy.android.domain.entity.transfer.TransferQuotaState
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FullImageFromServerMapperTest {

    private lateinit var underTest: FullImageFromServerMapper

    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaChatApiGateway = mock<MegaChatApiGateway>()
    private val megaNodeFromChatMessageMapper = mock<MegaNodeFromChatMessageMapper>()
    private val appEventGateway = mock<AppEventGateway>()
    private val cancelToken = mock<MegaCancelToken>()
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeAll
    fun setup() {
        underTest = FullImageFromServerMapper(
            megaApiGateway = megaApiGateway,
            megaChatApiGateway = megaChatApiGateway,
            megaNodeFromChatMessageMapper = megaNodeFromChatMessageMapper,
            transferQuotaStateMapper = TransferQuotaStateMapper(),
            appEventGateway = appEventGateway,
            appScope = CoroutineScope(testDispatcher),
            ioDispatcher = testDispatcher,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiGateway,
            megaChatApiGateway,
            megaNodeFromChatMessageMapper,
            appEventGateway,
            cancelToken,
        )
        whenever(megaApiGateway.createCancelToken()).thenReturn(cancelToken)
    }

    @Test
    fun `test that the predicted quota state is broadcast when the download reports one`() =
        runTest {
            stubQuotaState(MegaApiJava.TRANSFER_QUOTA_STATE_ORANGE)

            underTest(mock<MegaNode>())(PATH, false) {}.test {
                cancelAndIgnoreRemainingEvents()
            }

            verify(appEventGateway).broadcastTransferQuotaState(TransferQuotaState.Orange)
        }

    @Test
    fun `test that the predicted quota state is broadcast when a chat image download reports one`() =
        runTest {
            whenever(megaNodeFromChatMessageMapper(any(), any(), any())).thenReturn(mock())
            stubQuotaState(MegaApiJava.TRANSFER_QUOTA_STATE_RED)

            underTest(CHAT_ID, MESSAGE_ID)(PATH, false) {}.test {
                cancelAndIgnoreRemainingEvents()
            }

            verify(appEventGateway).broadcastTransferQuotaState(TransferQuotaState.Red)
        }

    @Test
    fun `test that nothing is broadcast when the download reports no quota state`() = runTest {
        underTest(mock<MegaNode>())(PATH, false) {}.test {
            cancelAndIgnoreRemainingEvents()
        }

        verifyNoInteractions(appEventGateway)
    }

    @Test
    fun `test that nothing is broadcast when the chat node is gone`() = runTest {
        whenever(megaNodeFromChatMessageMapper(any(), any(), any())).thenReturn(null)

        underTest(CHAT_ID, MESSAGE_ID)(PATH, false) {}.test {
            awaitError()
        }

        verify(megaApiGateway, never()).getFullImage(any(), any(), any(), any(), any())
        verifyNoInteractions(appEventGateway)
    }

    @Test
    fun `test that the download is started with its own cancel token`() = runTest {
        underTest(mock<MegaNode>())(PATH, false) {}.test {
            cancelAndIgnoreRemainingEvents()
        }

        verify(megaApiGateway).getFullImage(any(), any(), any(), eq(cancelToken), any())
    }

    @Test
    fun `test that the download is cancelled only once the flow closes`() = runTest {
        underTest(mock<MegaNode>())(PATH, false) {}.test {
            verify(cancelToken, never()).cancel()
            cancelAndIgnoreRemainingEvents()
        }

        verify(cancelToken).cancel()
    }

    @Test
    fun `test that the chat image download is cancelled once the flow closes`() = runTest {
        whenever(megaNodeFromChatMessageMapper(any(), any(), any())).thenReturn(mock())

        underTest(CHAT_ID, MESSAGE_ID)(PATH, false) {}.test {
            cancelAndIgnoreRemainingEvents()
        }

        verify(megaApiGateway).getFullImage(any(), any(), any(), eq(cancelToken), any())
        verify(cancelToken).cancel()
    }

    private fun stubQuotaState(state: Int) {
        whenever(megaApiGateway.getFullImage(any(), any(), any(), any(), any())).thenAnswer {
            (it.arguments.last() as OptionalMegaTransferListenerInterface)
                .onTransferQuotaState(mock(), mock(), state)
        }
    }

    private companion object {
        const val PATH = "/cache/full/image.jpg"
        const val CHAT_ID = 1L
        const val MESSAGE_ID = 2L
    }
}
