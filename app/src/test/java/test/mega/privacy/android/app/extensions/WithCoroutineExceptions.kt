package test.mega.privacy.android.app.extensions

import java.util.Vector

/**
 * Runs a function, making sure that the exceptions that happen throughout its execution get reported.
 */
fun <T> withCoroutineExceptions(
    testBody: () -> T,
): T {
    val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
    val errors = Vector<Throwable>()
    try {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable -> errors.add(throwable) }
        val result = try {
            testBody()
        } catch (testError: Throwable) {
            for (e in errors) {
                testError.addSuppressed(e)
            }
            throw testError
        }
        errors.firstOrNull()?.apply {
            errors.drop(1).forEach { addSuppressed(it) }
            throw this
        }
        return result
    } finally {
        Thread.setDefaultUncaughtExceptionHandler(oldHandler)
    }
}