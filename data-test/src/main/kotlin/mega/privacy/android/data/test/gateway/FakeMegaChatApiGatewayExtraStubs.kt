package mega.privacy.android.data.test.gateway

import nz.mega.sdk.MegaChatApiJava

/**
 * Inert [MegaChatApiJava] instance passed as the `api` parameter of chat listener callbacks.
 *
 * Kotlin-implemented listeners (e.g. OptionalMegaChatRequestListenerInterface) declare the
 * parameter as non-null and null-check it on entry, so passing null would throw at runtime.
 * Listeners may hold or ignore the reference but must never call methods on it.
 */
internal val inertMegaChatApiJava: MegaChatApiJava by lazy {
    allocateWithoutConstructor(MegaChatApiJava::class.java)
}
