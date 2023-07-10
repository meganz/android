package test.mega.privacy.android.app

import timber.log.Timber

private const val RESET = "\u001b[0m"
private const val GREEN = "\u001b[1;32m"
private const val RED = "\u001b[1;31m"

/**
 * Timber debug tree to show Timber logs in the `Run` tab
 * Logs has been color coded to Red and Green
 * Green will indicate regular logs with its message,
 * while Red indicate logs with a throwable message, if any
 **/
val timberDebugTree = object : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        println(GREEN + ("Tag: $tag Message: $message") + RESET)

        if (t != null) {
            println(RED + "Tag: $tag: Message: ${t.message}" + RESET)
        }
    }
}


