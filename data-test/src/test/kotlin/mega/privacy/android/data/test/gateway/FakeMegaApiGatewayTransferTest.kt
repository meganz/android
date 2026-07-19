package mega.privacy.android.data.test.gateway

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.model.GlobalTransfer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaError
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.data.test.stub.StubMegaTransfer
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransferListenerInterface
import nz.mega.sdk.MegaUploadOptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KFunction10
import kotlin.reflect.KFunction6

/**
 * Documents the transfer-listener completion behaviour of the [FakeMegaApiGateway] transfer
 * methods (uploads, downloads, full-image fetches): recorded invocation, default success
 * completion with a stub transfer, and outcome stubbing via [FakeMegaApiGateway.stubTransfer].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeMegaApiGatewayTransferTest {

    private class FakeMegaUploadOptions : MegaUploadOptions(0, false)

    private class RecordingTransferListener : MegaTransferListenerInterface {
        val startedTransfers = mutableListOf<MegaTransfer>()
        val finishedTransfers = mutableListOf<Pair<MegaTransfer, MegaError>>()

        override fun onTransferStart(api: MegaApiJava, transfer: MegaTransfer) {
            startedTransfers.add(transfer)
        }

        override fun onTransferFinish(api: MegaApiJava, transfer: MegaTransfer, e: MegaError) {
            finishedTransfers.add(transfer to e)
        }

        override fun onTransferUpdate(api: MegaApiJava, transfer: MegaTransfer) = Unit

        override fun onTransferTemporaryError(
            api: MegaApiJava,
            transfer: MegaTransfer,
            e: MegaError,
        ) = Unit

        override fun onTransferData(
            api: MegaApiJava,
            transfer: MegaTransfer,
            buffer: ByteArray,
        ): Boolean = false

        override fun onFolderTransferUpdate(
            api: MegaApiJava,
            transfer: MegaTransfer,
            stage: Int,
            folderCount: Long,
            createdFolderCount: Long,
            fileCount: Long,
            currentFolder: String?,
            currentFileLeafName: String?,
        ) = Unit
    }

    @Suppress("DEPRECATION")
    private val startUploadDeprecatedRef: KFunction10<MegaApiGateway, String, MegaNode, String?, Long?, String?, Boolean, Boolean, MegaCancelToken?, MegaTransferListenerInterface, Unit> =
        MegaApiGateway::startUpload

    private val startUploadRef: KFunction6<MegaApiGateway, String, MegaNode, MegaCancelToken?, MegaUploadOptions, MegaTransferListenerInterface, Unit> =
        MegaApiGateway::startUpload

    private fun transferCase(
        name: String,
        block: (FakeMegaApiGateway, MegaTransferListenerInterface) -> Unit,
    ): Arguments = Arguments.of(name, block)

    @Suppress("DEPRECATION")
    fun transferMethods(): Stream<Arguments> = Stream.of(
        transferCase("startUpload") { g, l ->
            g.startUpload("/local/file.txt", StubMegaNode(handle = 1L), null, null, null, false, false, null, l)
        },
        transferCase("startUpload") { g, l ->
            g.startUpload("/local/file.txt", StubMegaNode(handle = 1L), null, FakeMegaUploadOptions(), l)
        },
        transferCase("startUploadForSupport") { g, l ->
            g.startUploadForSupport("/local/log.txt", l)
        },
        transferCase("startDownload") { g, l ->
            g.startDownload(StubMegaNode(handle = 1L), "/local/", "file.txt", null, false, null, 0, 0, l)
        },
        transferCase("getFullImage") { g, l ->
            g.getFullImage(StubMegaNode(handle = 1L), File("/local/full.jpg"), false, l)
        },
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("transferMethods")
    fun `test that transfer method records and completes with success when unstubbed`(
        name: String,
        block: (FakeMegaApiGateway, MegaTransferListenerInterface) -> Unit,
    ) {
        val underTest = FakeMegaApiGateway()
        val listener = RecordingTransferListener()

        block(underTest, listener)

        assertThat(underTest.invocations.single().methodName).isEqualTo(name)
        val started = listener.startedTransfers.single()
        val (finished, error) = listener.finishedTransfers.single()
        assertThat(finished).isSameInstanceAs(started)
        assertThat(error.errorCode).isEqualTo(MegaError.API_OK)
    }

    @Test
    fun `test that listener receives stubbed error when stubTransfer stubs a failure`() {
        val underTest = FakeMegaApiGateway()
        val listener = RecordingTransferListener()
        underTest.stubTransfer(
            MegaApiGateway::startUploadForSupport,
            error = StubMegaError(MegaError.API_EOVERQUOTA),
        )

        underTest.startUploadForSupport("/local/log.txt", listener)

        val (_, error) = listener.finishedTransfers.single()
        assertThat(error.errorCode).isEqualTo(MegaError.API_EOVERQUOTA)
    }

    @Test
    fun `test that listener receives stubbed transfer when stubTransfer provides one`() {
        val underTest = FakeMegaApiGateway()
        val listener = RecordingTransferListener()
        val stubbedTransfer = StubMegaTransfer(tag = 42)
        underTest.stubTransfer(MegaApiGateway::startUploadForSupport, transfer = stubbedTransfer)

        underTest.startUploadForSupport("/local/log.txt", listener)

        assertThat(listener.startedTransfers.single()).isSameInstanceAs(stubbedTransfer)
        assertThat(listener.finishedTransfers.single().first).isSameInstanceAs(stubbedTransfer)
    }

    @Test
    fun `test that only matching call fails when stubTransfer uses an argument matcher`() {
        val underTest = FakeMegaApiGateway()
        val failingListener = RecordingTransferListener()
        val succeedingListener = RecordingTransferListener()
        underTest.stubTransfer(
            MegaApiGateway::startUploadForSupport,
            error = StubMegaError(MegaError.API_EFAILED),
            matcher = { it[0] == "/local/broken.txt" },
        )

        underTest.startUploadForSupport("/local/broken.txt", failingListener)
        underTest.startUploadForSupport("/local/fine.txt", succeedingListener)

        assertThat(failingListener.finishedTransfers.single().second.errorCode)
            .isEqualTo(MegaError.API_EFAILED)
        assertThat(succeedingListener.finishedTransfers.single().second.errorCode)
            .isEqualTo(MegaError.API_OK)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `test that both startUpload overloads share stubs when stubTransfer targets one of them`() {
        val underTest = FakeMegaApiGateway()
        val deprecatedListener = RecordingTransferListener()
        val currentListener = RecordingTransferListener()
        underTest.stubTransfer(
            startUploadRef,
            error = StubMegaError(MegaError.API_EEXIST),
        )

        underTest.startUpload(
            "/local/file.txt", StubMegaNode(handle = 1L), null, null, null, false, false, null,
            deprecatedListener,
        )
        underTest.startUpload(
            "/local/file.txt", StubMegaNode(handle = 1L), null, FakeMegaUploadOptions(),
            currentListener,
        )

        assertThat(deprecatedListener.finishedTransfers.single().second.errorCode)
            .isEqualTo(MegaError.API_EEXIST)
        assertThat(currentListener.finishedTransfers.single().second.errorCode)
            .isEqualTo(MegaError.API_EEXIST)
        assertThat(underTest.invocationsOf(startUploadDeprecatedRef)).hasSize(2)
    }

    @Test
    fun `test that transfer callbacks are mirrored to the global transfer flow when a transfer completes`() =
        runTest {
            val underTest = FakeMegaApiGateway()

            underTest.globalTransfer.test {
                underTest.startUploadForSupport("/local/log.txt", RecordingTransferListener())

                assertThat(awaitItem()).isInstanceOf(GlobalTransfer.OnTransferStart::class.java)
                assertThat(awaitItem()).isInstanceOf(GlobalTransfer.OnTransferFinish::class.java)
            }
        }

    @Test
    fun `test that listener receives progressive updates when stubTransferScript is configured`() {
        val underTest = FakeMegaApiGateway()
        val events = mutableListOf<String>()
        val finished = CountDownLatch(1)
        val listener = object : MegaTransferListenerInterface {
            override fun onTransferStart(api: MegaApiJava, transfer: MegaTransfer) {
                events += "start:${transfer.transferredBytes}"
            }

            override fun onTransferFinish(
                api: MegaApiJava,
                transfer: MegaTransfer,
                e: MegaError,
            ) {
                events += "finish:${transfer.transferredBytes}:${e.errorCode}"
                finished.countDown()
            }

            override fun onTransferUpdate(api: MegaApiJava, transfer: MegaTransfer) {
                events += "update:${transfer.transferredBytes}"
            }

            override fun onTransferTemporaryError(
                api: MegaApiJava,
                transfer: MegaTransfer,
                e: MegaError,
            ) = Unit

            override fun onTransferData(
                api: MegaApiJava,
                transfer: MegaTransfer,
                buffer: ByteArray,
            ): Boolean = false

            override fun onFolderTransferUpdate(
                api: MegaApiJava,
                transfer: MegaTransfer,
                stage: Int,
                folderCount: Long,
                createdFolderCount: Long,
                fileCount: Long,
                currentFolder: String?,
                currentFileLeafName: String?,
            ) = Unit
        }
        underTest.stubTransferScript(
            MegaApiGateway::startUploadForSupport,
            steps = listOf(
                StubMegaTransfer(transferredBytes = 0L, totalBytes = 100L),
                StubMegaTransfer(transferredBytes = 50L, totalBytes = 100L),
            ),
            finalTransfer = StubMegaTransfer(
                transferredBytes = 100L,
                totalBytes = 100L,
                isFinished = true,
            ),
            stepDelayMs = 10L,
        )

        underTest.startUploadForSupport("/local/big.bin", listener)

        assertThat(finished.await(2, TimeUnit.SECONDS)).isTrue()
        assertThat(events).containsExactly("start:0", "update:50", "finish:100:0").inOrder()
    }

    @Test
    fun `test that invocation is recorded without crash when startDownload listener is null`() {
        val underTest = FakeMegaApiGateway()

        underTest.startDownload(
            StubMegaNode(handle = 1L), "/local/", "file.txt", null, false, null, 0, 0, null,
        )

        assertThat(underTest.invocations.single().methodName).isEqualTo("startDownload")
    }

    @Test
    fun `test that recorded arguments include the listener when a transfer method is called`() {
        val underTest = FakeMegaApiGateway()
        val listener = RecordingTransferListener()

        underTest.startUploadForSupport("/local/log.txt", listener)

        val invocation = underTest.invocationsOf(MegaApiGateway::startUploadForSupport).single()
        assertThat(invocation.arguments).containsExactly("/local/log.txt", listener).inOrder()
    }
}
