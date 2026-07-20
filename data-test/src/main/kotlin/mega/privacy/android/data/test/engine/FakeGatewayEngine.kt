package mega.privacy.android.data.test.engine

import kotlinx.coroutines.runBlocking
import kotlin.reflect.KFunction

/**
 * Thread-safe engine backing a fake gateway. One instance per fake gateway instance.
 *
 * [dispatch]/[dispatchBlocking] always record the invocation first, then answer via the
 * latest matching stub, falling back to the given default when nothing matches.
 */
class FakeGatewayEngine : FakeGatewayStubbing {

    private data class AnswerStub(
        val methodName: String,
        val matcher: (List<Any?>) -> Boolean,
        val answer: suspend (List<Any?>) -> Any?,
    )

    private data class OutcomeStub(
        val methodName: String,
        val matcher: (List<Any?>) -> Boolean,
        val outcome: Any,
    )

    private val lock = Any()
    private val answerStubs = mutableListOf<AnswerStub>()
    private val outcomeStubs = mutableListOf<OutcomeStub>()
    private val recordedInvocations = mutableListOf<Invocation>()

    override fun <R> stub(method: KFunction<R>, answer: suspend (arguments: List<Any?>) -> R) =
        stub(method, { true }, answer)

    override fun <R> stub(
        method: KFunction<R>,
        matcher: (arguments: List<Any?>) -> Boolean,
        answer: suspend (arguments: List<Any?>) -> R,
    ) {
        synchronized(lock) {
            answerStubs += AnswerStub(method.name, matcher) { arguments -> answer(arguments) }
        }
    }

    override fun <R> stubResult(method: KFunction<R>, result: R) =
        stub(method) { result }

    override fun stubError(method: KFunction<*>, error: Throwable) =
        stub(method) { throw error }

    override fun stubRequestOutcome(method: KFunction<*>, outcome: Any) =
        stubRequestOutcome(method, { true }, outcome)

    override fun stubRequestOutcome(
        method: KFunction<*>,
        matcher: (arguments: List<Any?>) -> Boolean,
        outcome: Any,
    ) {
        synchronized(lock) {
            outcomeStubs += OutcomeStub(method.name, matcher, outcome)
        }
    }

    override val invocations: List<Invocation>
        get() = synchronized(lock) { recordedInvocations.toList() }

    override fun invocationsOf(method: KFunction<*>): List<Invocation> =
        invocations.filter { it.methodName == method.name }

    override fun clearStubs() {
        synchronized(lock) {
            answerStubs.clear()
            outcomeStubs.clear()
        }
    }

    override fun clearInvocations() {
        synchronized(lock) {
            recordedInvocations.clear()
        }
    }

    override fun reset() {
        clearStubs()
        clearInvocations()
    }

    /** Record + return stubbed answer or [default]. For suspend gateway methods. */
    suspend fun <R> dispatch(
        method: KFunction<R>,
        arguments: List<Any?>,
        default: suspend (arguments: List<Any?>) -> R,
    ): R {
        record(method, arguments)
        val stub = latestMatchingStub(method.name, arguments)
        @Suppress("UNCHECKED_CAST")
        return if (stub != null) stub.answer(arguments) as R else default(arguments)
    }

    /** Same for non-suspend gateway methods (runs suspend answers via [runBlocking]). */
    fun <R> dispatchBlocking(
        method: KFunction<R>,
        arguments: List<Any?>,
        default: (arguments: List<Any?>) -> R,
    ): R {
        record(method, arguments)
        val stub = latestMatchingStub(method.name, arguments)
        @Suppress("UNCHECKED_CAST")
        return if (stub != null) runBlocking { stub.answer(arguments) } as R else default(arguments)
    }

    /** Record only (listener-based methods record via this, then complete the listener). */
    fun record(method: KFunction<*>, arguments: List<Any?>) {
        synchronized(lock) {
            recordedInvocations += Invocation(method.name, arguments)
        }
    }

    /** Matching stubbed outcome for a listener-based call, or null → fake uses its default. */
    fun requestOutcomeFor(method: KFunction<*>, arguments: List<Any?>): Any? =
        synchronized(lock) { outcomeStubs.filter { it.methodName == method.name } }
            .lastOrNull { it.matcher(arguments) }
            ?.outcome

    private fun latestMatchingStub(methodName: String, arguments: List<Any?>): AnswerStub? =
        synchronized(lock) { answerStubs.filter { it.methodName == methodName } }
            .lastOrNull { it.matcher(arguments) }
}
