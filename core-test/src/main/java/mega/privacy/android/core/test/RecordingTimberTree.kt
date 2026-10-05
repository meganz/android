package mega.privacy.android.core.test

import timber.log.Timber

/**
 * Timber tree that keeps every logged message in memory so tests can assert on what a
 * component writes to the log, e.g. that a sensitive value never reaches it.
 */
class RecordingTimberTree : Timber.Tree() {
    private val _messages = mutableListOf<String>()

    /**
     * Messages logged since the tree was planted, in order.
     */
    val messages: List<String> get() = _messages

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        _messages += message
    }
}
