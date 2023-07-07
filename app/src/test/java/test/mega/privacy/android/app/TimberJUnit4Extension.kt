package test.mega.privacy.android.app

import org.junit.rules.TestWatcher
import org.junit.runner.Description
import timber.log.Timber

/** USAGE
 * Declare it as a rule as below
 * @get:ClassRule
 * @JvmStatic
 * var timberRule = TimberJUnit4Extension()
 */
class TimberJUnit4Extension : TestWatcher() {
    private val printlnTree = object : Timber.DebugTree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            println("$tag: $message")
        }
    }

    override fun starting(description: Description) {
        super.starting(description)
        Timber.plant(printlnTree)
    }

    override fun finished(description: Description) {
        super.finished(description)
        Timber.uproot(printlnTree)
    }
}