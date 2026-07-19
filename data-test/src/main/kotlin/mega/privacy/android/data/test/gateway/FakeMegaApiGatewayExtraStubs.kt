package mega.privacy.android.data.test.gateway

import nz.mega.sdk.MegaApiJava

/**
 * Inert [MegaApiJava] instance passed as the `api` parameter of listener callbacks.
 *
 * The SDK listener interfaces declare the parameter as non-null Kotlin types, so Kotlin-implemented
 * listeners null-check it on entry and passing null would throw. [MegaApiJava] cannot be
 * constructed normally without loading the native SDK, so this instance is allocated without
 * running any constructor. Every field is null/zero: listeners may hold or ignore the reference but
 * must never call methods on it.
 */
internal val inertMegaApiJava: MegaApiJava by lazy {
    allocateWithoutConstructor(MegaApiJava::class.java)
}

/**
 * Allocates an instance of [type] without running any constructor, for classes that cannot be
 * constructed without loading the native SDK. Every field of the result is null/zero.
 */
internal fun <T> allocateWithoutConstructor(type: Class<T>): T {
    val unsafeClass = Class.forName("sun.misc.Unsafe")
    val theUnsafe = unsafeClass.getDeclaredField("theUnsafe")
        .apply { isAccessible = true }
        .get(null)
    @Suppress("UNCHECKED_CAST")
    return unsafeClass.getMethod("allocateInstance", Class::class.java)
        .invoke(theUnsafe, type) as T
}
