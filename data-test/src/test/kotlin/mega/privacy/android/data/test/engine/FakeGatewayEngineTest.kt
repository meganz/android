package mega.privacy.android.data.test.engine

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeGatewayEngineTest {

    private lateinit var underTest: FakeGatewayEngine

    @BeforeEach
    fun setUp() {
        underTest = FakeGatewayEngine()
    }

    @Test
    fun `test that dispatch returns default when no stub is registered`() = runTest {
        val result = underTest.dispatch(SampleGateway::fetchNumber, listOf(7L)) { 42 }

        assertThat(result).isEqualTo(42)
    }

    @Test
    fun `test that dispatch passes arguments to the default when no stub is registered`() =
        runTest {
            val result = underTest.dispatch(SampleGateway::fetchNumber, listOf(7L)) { arguments ->
                (arguments.first() as Long).toInt() + 1
            }

            assertThat(result).isEqualTo(8)
        }

    @Test
    fun `test that dispatch records the invocation when no stub is registered`() = runTest {
        underTest.dispatch(SampleGateway::fetchNumber, listOf(7L)) { 0 }

        assertThat(underTest.invocations)
            .containsExactly(Invocation("fetchNumber", listOf(7L)))
    }

    @Test
    fun `test that dispatch records the invocation when the stub throws`() = runTest {
        underTest.stubError(SampleGateway::fetchNumber, IllegalStateException("boom"))

        runCatching { underTest.dispatch(SampleGateway::fetchNumber, listOf(7L)) { 0 } }

        assertThat(underTest.invocations)
            .containsExactly(Invocation("fetchNumber", listOf(7L)))
    }

    @Test
    fun `test that invocations are recorded in call order when multiple methods are dispatched`() =
        runTest {
            underTest.dispatch(SampleGateway::fetchNumber, listOf(1L)) { 0 }
            underTest.dispatchBlocking(SampleGateway::currentName, listOf(2L)) { "" }
            underTest.record(SampleGateway::startRequest, listOf(3L, null))

            assertThat(underTest.invocations).containsExactly(
                Invocation("fetchNumber", listOf(1L)),
                Invocation("currentName", listOf(2L)),
                Invocation("startRequest", listOf(3L, null)),
            ).inOrder()
        }

    @Test
    fun `test that dispatch returns the stubbed answer when a stub is registered`() = runTest {
        underTest.stub(SampleGateway::fetchNumber) { 99 }

        val result = underTest.dispatch(SampleGateway::fetchNumber, listOf(7L)) { 0 }

        assertThat(result).isEqualTo(99)
    }

    @Test
    fun `test that dispatch passes arguments to the stubbed answer`() = runTest {
        underTest.stub(SampleGateway::fetchNumber) { arguments ->
            (arguments.first() as Long).toInt() * 2
        }

        val result = underTest.dispatch(SampleGateway::fetchNumber, listOf(21L)) { 0 }

        assertThat(result).isEqualTo(42)
    }

    @Test
    fun `test that dispatch uses the latest stub when multiple stubs match`() = runTest {
        underTest.stub(SampleGateway::fetchNumber) { 1 }
        underTest.stub(SampleGateway::fetchNumber) { 2 }

        val result = underTest.dispatch(SampleGateway::fetchNumber, listOf(7L)) { 0 }

        assertThat(result).isEqualTo(2)
    }

    @Test
    fun `test that dispatch uses a matcher stub only when the arguments match`() = runTest {
        underTest.stub(SampleGateway::fetchNumber, { it.first() == 7L }) { 99 }

        val matched = underTest.dispatch(SampleGateway::fetchNumber, listOf(7L)) { 0 }
        val unmatched = underTest.dispatch(SampleGateway::fetchNumber, listOf(8L)) { 0 }

        assertThat(matched).isEqualTo(99)
        assertThat(unmatched).isEqualTo(0)
    }

    @Test
    fun `test that dispatch falls back to an earlier stub when the later matcher does not match`() =
        runTest {
            underTest.stub(SampleGateway::fetchNumber) { 1 }
            underTest.stub(SampleGateway::fetchNumber, { it.first() == 7L }) { 2 }

            val matched = underTest.dispatch(SampleGateway::fetchNumber, listOf(7L)) { 0 }
            val unmatched = underTest.dispatch(SampleGateway::fetchNumber, listOf(8L)) { 0 }

            assertThat(matched).isEqualTo(2)
            assertThat(unmatched).isEqualTo(1)
        }

    @Test
    fun `test that stubs are shared between methods with the same name`() = runTest {
        underTest.stub(OtherGateway::fetchNumber) { 99 }

        val result = underTest.dispatch(SampleGateway::fetchNumber, listOf(7L)) { 0 }

        assertThat(result).isEqualTo(99)
    }

    @Test
    fun `test that stubResult always returns the given result`() = runTest {
        underTest.stubResult(SampleGateway::fetchNumber, 99)

        val first = underTest.dispatch(SampleGateway::fetchNumber, listOf(1L)) { 0 }
        val second = underTest.dispatch(SampleGateway::fetchNumber, listOf(2L)) { 0 }

        assertThat(first).isEqualTo(99)
        assertThat(second).isEqualTo(99)
    }

    @Test
    fun `test that stubError throws the given error when the method is dispatched`() = runTest {
        val error = IllegalStateException("boom")
        underTest.stubError(SampleGateway::fetchNumber, error)

        val thrown = assertThrows<IllegalStateException> {
            underTest.dispatch(SampleGateway::fetchNumber, listOf(7L)) { 0 }
        }

        assertThat(thrown).isSameInstanceAs(error)
    }

    @Test
    fun `test that stubs are persistent when the method is dispatched multiple times`() = runTest {
        underTest.stub(SampleGateway::fetchNumber) { 99 }

        repeat(3) {
            assertThat(underTest.dispatch(SampleGateway::fetchNumber, listOf(7L)) { 0 })
                .isEqualTo(99)
        }
    }

    @Test
    fun `test that dispatchBlocking returns default when no stub is registered`() {
        val result = underTest.dispatchBlocking(SampleGateway::currentName, listOf(7L)) { "default" }

        assertThat(result).isEqualTo("default")
    }

    @Test
    fun `test that dispatchBlocking runs a suspend answer when a stub is registered`() {
        underTest.stub(SampleGateway::currentName) { "stubbed" }

        val result = underTest.dispatchBlocking(SampleGateway::currentName, listOf(7L)) { "default" }

        assertThat(result).isEqualTo("stubbed")
    }

    @Test
    fun `test that dispatchBlocking records the invocation`() {
        underTest.dispatchBlocking(SampleGateway::currentName, listOf(7L)) { "" }

        assertThat(underTest.invocations)
            .containsExactly(Invocation("currentName", listOf(7L)))
    }

    @Test
    fun `test that record stores the invocation without answering`() {
        underTest.record(SampleGateway::startRequest, listOf(7L, "listener"))

        assertThat(underTest.invocations)
            .containsExactly(Invocation("startRequest", listOf(7L, "listener")))
    }

    @Test
    fun `test that invocationsOf returns only invocations of the given method`() = runTest {
        underTest.dispatch(SampleGateway::fetchNumber, listOf(1L)) { 0 }
        underTest.record(SampleGateway::startRequest, listOf(2L, null))
        underTest.dispatch(SampleGateway::fetchNumber, listOf(3L)) { 0 }

        assertThat(underTest.invocationsOf(SampleGateway::fetchNumber)).containsExactly(
            Invocation("fetchNumber", listOf(1L)),
            Invocation("fetchNumber", listOf(3L)),
        ).inOrder()
    }

    @Test
    fun `test that requestOutcomeFor returns null when no outcome stub is registered`() {
        val outcome = underTest.requestOutcomeFor(SampleGateway::startRequest, listOf(7L, null))

        assertThat(outcome).isNull()
    }

    @Test
    fun `test that requestOutcomeFor returns the stubbed outcome when one is registered`() {
        underTest.stubRequestOutcome(SampleGateway::startRequest, "outcome")

        val outcome = underTest.requestOutcomeFor(SampleGateway::startRequest, listOf(7L, null))

        assertThat(outcome).isEqualTo("outcome")
    }

    @Test
    fun `test that requestOutcomeFor returns the latest outcome when multiple are stubbed`() {
        underTest.stubRequestOutcome(SampleGateway::startRequest, "first")
        underTest.stubRequestOutcome(SampleGateway::startRequest, "second")

        val outcome = underTest.requestOutcomeFor(SampleGateway::startRequest, listOf(7L, null))

        assertThat(outcome).isEqualTo("second")
    }

    @Test
    fun `test that requestOutcomeFor honours the matcher when arguments differ`() {
        underTest.stubRequestOutcome(SampleGateway::startRequest, { it.first() == 7L }, "matched")

        val matched = underTest.requestOutcomeFor(SampleGateway::startRequest, listOf(7L, null))
        val unmatched = underTest.requestOutcomeFor(SampleGateway::startRequest, listOf(8L, null))

        assertThat(matched).isEqualTo("matched")
        assertThat(unmatched).isNull()
    }

    @Test
    fun `test that requestOutcomeFor does not record an invocation`() {
        underTest.stubRequestOutcome(SampleGateway::startRequest, "outcome")

        underTest.requestOutcomeFor(SampleGateway::startRequest, listOf(7L, null))

        assertThat(underTest.invocations).isEmpty()
    }

    @Test
    fun `test that clearStubs removes answer stubs but keeps invocations`() = runTest {
        underTest.stub(SampleGateway::fetchNumber) { 99 }
        underTest.dispatch(SampleGateway::fetchNumber, listOf(1L)) { 0 }

        underTest.clearStubs()

        val result = underTest.dispatch(SampleGateway::fetchNumber, listOf(2L)) { 0 }
        assertThat(result).isEqualTo(0)
        assertThat(underTest.invocations).hasSize(2)
    }

    @Test
    fun `test that clearStubs removes outcome stubs`() {
        underTest.stubRequestOutcome(SampleGateway::startRequest, "outcome")

        underTest.clearStubs()

        assertThat(underTest.requestOutcomeFor(SampleGateway::startRequest, listOf(7L, null)))
            .isNull()
    }

    @Test
    fun `test that clearInvocations removes invocations but keeps stubs`() = runTest {
        underTest.stub(SampleGateway::fetchNumber) { 99 }
        underTest.dispatch(SampleGateway::fetchNumber, listOf(1L)) { 0 }

        underTest.clearInvocations()

        assertThat(underTest.invocations).isEmpty()
        assertThat(underTest.dispatch(SampleGateway::fetchNumber, listOf(2L)) { 0 })
            .isEqualTo(99)
    }

    @Test
    fun `test that reset clears stubs and invocations`() = runTest {
        underTest.stub(SampleGateway::fetchNumber) { 99 }
        underTest.stubRequestOutcome(SampleGateway::startRequest, "outcome")
        underTest.dispatch(SampleGateway::fetchNumber, listOf(1L)) { 0 }

        underTest.reset()

        assertThat(underTest.invocations).isEmpty()
        assertThat(underTest.dispatch(SampleGateway::fetchNumber, listOf(2L)) { 0 }).isEqualTo(0)
        assertThat(underTest.requestOutcomeFor(SampleGateway::startRequest, listOf(7L, null)))
            .isNull()
    }
}

private interface SampleGateway {
    suspend fun fetchNumber(id: Long): Int
    fun currentName(id: Long): String
    fun startRequest(id: Long, listener: Any?)
}

private interface OtherGateway {
    suspend fun fetchNumber(id: Long): Int
}
