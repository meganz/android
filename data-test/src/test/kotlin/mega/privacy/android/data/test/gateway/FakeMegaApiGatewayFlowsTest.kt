package mega.privacy.android.data.test.gateway

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.model.RequestEvent
import mega.privacy.android.data.test.stub.StubMegaError
import mega.privacy.android.data.test.stub.StubMegaRequest
import mega.privacy.android.data.test.stub.StubMegaTransfer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Documents the [FakeMegaApiGateway] flow properties and their emit helpers.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeMegaApiGatewayFlowsTest {

    private lateinit var underTest: FakeMegaApiGateway

    @BeforeEach
    fun setUp() {
        underTest = FakeMegaApiGateway()
    }

    @Test
    fun `test that globalUpdates emits when emitGlobalUpdate is called`() = runTest {
        underTest.globalUpdates.test {
            underTest.emitGlobalUpdate(GlobalUpdate.OnReloadNeeded)

            assertThat(awaitItem()).isEqualTo(GlobalUpdate.OnReloadNeeded)
        }
    }

    @Test
    fun `test that globalUpdates emits events in order when emitGlobalUpdate is called repeatedly`() =
        runTest {
            underTest.globalUpdates.test {
                underTest.emitGlobalUpdate(GlobalUpdate.OnAccountUpdate)
                underTest.emitGlobalUpdate(GlobalUpdate.OnGlobalSyncStateChanged)

                assertThat(awaitItem()).isEqualTo(GlobalUpdate.OnAccountUpdate)
                assertThat(awaitItem()).isEqualTo(GlobalUpdate.OnGlobalSyncStateChanged)
            }
        }

    @Test
    fun `test that globalTransfer emits when emitGlobalTransfer is called`() = runTest {
        val transfer = StubMegaTransfer(tag = 42)

        underTest.globalTransfer.test {
            underTest.emitGlobalTransfer(GlobalTransfer.OnTransferStart(transfer))

            val event = awaitItem()
            assertThat(event).isInstanceOf(GlobalTransfer.OnTransferStart::class.java)
            assertThat(event.transfer).isSameInstanceAs(transfer)
        }
    }

    @Test
    fun `test that globalRequestEvents emits when emitRequestEvent is called`() = runTest {
        val request = StubMegaRequest(type = 1)
        val error = StubMegaError()

        underTest.globalRequestEvents.test {
            underTest.emitRequestEvent(RequestEvent.OnRequestFinish(request, error))

            val event = awaitItem()
            assertThat(event).isInstanceOf(RequestEvent.OnRequestFinish::class.java)
            assertThat(event.request).isSameInstanceAs(request)
            assertThat((event as RequestEvent.OnRequestFinish).error).isSameInstanceAs(error)
        }
    }

    @Test
    fun `test that globalUpdates stays silent when a transfer event is emitted`() = runTest {
        underTest.globalUpdates.test {
            underTest.emitGlobalTransfer(GlobalTransfer.OnTransferUpdate(StubMegaTransfer()))

            expectNoEvents()
        }
    }
}
