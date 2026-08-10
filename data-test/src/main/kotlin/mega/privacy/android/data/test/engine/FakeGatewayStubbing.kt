package mega.privacy.android.data.test.engine

import kotlin.reflect.KFunction

/**
 * Test-facing stubbing/verification surface (the WireMock-style secondary interface).
 *
 * Methods are keyed by [KFunction.name] — overloads share stubs; use a matcher to disambiguate.
 * Later stubs win over earlier ones. Stubs are persistent (not one-shot).
 */
interface FakeGatewayStubbing {

    /** Stub every call to [method] with [answer]. */
    fun <R> stub(method: KFunction<R>, answer: suspend (arguments: List<Any?>) -> R)

    /** Stub calls to [method] whose arguments match [matcher]. */
    fun <R> stub(
        method: KFunction<R>,
        matcher: (arguments: List<Any?>) -> Boolean,
        answer: suspend (arguments: List<Any?>) -> R,
    )

    /** Convenience: always return [result]. */
    fun <R> stubResult(method: KFunction<R>, result: R)

    /** Convenience: always throw [error]. */
    fun stubError(method: KFunction<*>, error: Throwable)

    /**
     * Stub the outcome delivered to the listener of a listener-based method.
     *
     * [outcome] is opaque to the engine; each fake defines its own outcome type
     * (e.g. MegaRequestOutcome / MegaChatRequestOutcome) and typed convenience wrappers.
     */
    fun stubRequestOutcome(method: KFunction<*>, outcome: Any)

    /** Stub the listener outcome of calls to [method] whose arguments match [matcher]. */
    fun stubRequestOutcome(
        method: KFunction<*>,
        matcher: (arguments: List<Any?>) -> Boolean,
        outcome: Any,
    )

    /** All recorded invocations, in call order. */
    val invocations: List<Invocation>

    /** Recorded invocations of [method], in call order. */
    fun invocationsOf(method: KFunction<*>): List<Invocation>

    /** Remove all stubs (both answer stubs and request-outcome stubs). */
    fun clearStubs()

    /** Forget all recorded invocations. */
    fun clearInvocations()

    /** [clearStubs] + [clearInvocations]. (Fakes layer their own state reset on top.) */
    fun reset()
}
