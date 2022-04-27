package mega.privacy.android.app.logging

import timber.log.Timber

/**
 * Timber legacy log
 *
 * Forwards log calls from the legacy log util to Timber
 */
class TimberLegacyLog : LegacyLog {

    override fun logFatal(message: String) {
        Timber.wtf(message)
    }

    override fun logFatal(message: String, exception: Throwable) {
        Timber.wtf(exception, message)
    }

    override fun logError(message: String?) {
        Timber.e(message)
    }

    override fun logError(message: String?, exception: Throwable?) {
        Timber.e(exception, message)
    }

    override fun logWarning(message: String) {
        Timber.w(message)
    }

    override fun logWarning(message: String, exception: Throwable) {
        Timber.w(exception, message)
    }

    override fun logInfo(message: String) {
        Timber.i(message)
    }

    override fun logDebug(message: String) {
        Timber.d(message)
    }

    override fun logMax(message: String) {
        Timber.v(message)
    }

}