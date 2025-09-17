package mega.privacy.android.domain.logging

interface Logger {
    fun d(message: String)
    fun e(throwable: Throwable? = null, message: String)
    fun i(message: String)
    fun w(message: String)
}