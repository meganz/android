package mega.privacy.android.app.logging

import mega.privacy.android.app.utils.LogUtil
import timber.log.Timber

class TimberLegacyLog : LegacyLog, Timber.DebugTree() {

    private val ignoredClasses = listOf(
        Timber::class.java.name,
        Timber.Forest::class.java.name,
        Timber.Tree::class.java.name,
        Timber.DebugTree::class.java.name,
        TimberLegacyLog::class.java.name,
        LogUtil::class.java.name,
    )

    fun createTag(): String {
        return Throwable().stackTrace
            .firstOrNull{ it.className !in ignoredClasses }
            ?.let { "[clientApp]: (${it.fileName}:${it.lineNumber})#${it.methodName}" } ?: "[clientApp]:"
    }
    
    override fun logFatal(message: String) {
        Timber.tag(createTag())
        wtf(message)
    }

    override fun logFatal(message: String, exception: Throwable) {
        Timber.tag(createTag())
        wtf(exception, message)
    }

    override fun logError(message: String?) {
        Timber.tag(createTag())
        e(message)
    }

    override fun logError(message: String?, exception: Throwable?) {
        Timber.tag(createTag())
        e(exception, message)
    }

    override fun logWarning(message: String) {
        Timber.tag(createTag())
        w(message)
    }

    override fun logWarning(message: String, exception: Throwable) {
        Timber.tag(createTag())
        w(exception, message)
    }

    override fun logInfo(message: String) {
        Timber.tag(createTag())
        i(message)
    }

    override fun logDebug(message: String) {
        Timber.tag(createTag())
        d(message)
    }

    override fun logMax(message: String) {
        Timber.tag(createTag())
        v(message)
    }
    
}