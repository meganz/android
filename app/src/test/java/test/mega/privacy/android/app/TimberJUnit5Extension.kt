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
    override fun beforeAll(context: ExtensionContext?) {
        Timber.plant(timberDebugTree)
    }

    override fun afterAll(context: ExtensionContext?) {
        Timber.uproot(timberDebugTree)
    }
}