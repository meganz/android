package test.mega.privacy.android.app

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import timber.log.Timber

/** USAGE
 * Add this annotation at the above the test class
 * @ExtendWith(TimberJUnit5Extension::class)
 */
class TimberJUnit5Extension : BeforeAllCallback, AfterAllCallback {
    private val printlnTree = object : Timber.DebugTree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            println("$tag: $message")
        }
    }

    override fun beforeAll(context: ExtensionContext?) {
        Timber.plant(printlnTree)
    }

    override fun afterAll(context: ExtensionContext?) {
        Timber.uproot(printlnTree)
    }
}