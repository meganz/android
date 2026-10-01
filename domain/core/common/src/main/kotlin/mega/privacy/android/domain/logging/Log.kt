package mega.privacy.android.domain.logging

object Log {
    private var delegate: Logger = object : Logger {
        override fun d(message: String) {}
        override fun i(message: String) {}
        override fun w(message: String) {}
        override fun e(throwable: Throwable?, message: String) {}
    }

    fun setLogger(logger: Logger) {
        delegate = logger
    }

    fun d(message: String) = delegate.d(message)
    fun i(message: String) = delegate.i(message)
    fun w(message: String) = delegate.w(message)
    fun e(message: String, throwable: Throwable? = null) = delegate.e(throwable, message)

}