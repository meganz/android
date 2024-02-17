package mega.privacy.android.core.test.extension

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Coroutine main dispatcher test extension. [Reference](https://developer.android.com/kotlin/coroutines/test#setting-main-dispatcher)
 * ```
 * ```
 * ### Add this annotation to the test class if you use the UnconfinedTestDispatcher.
 * ```
 * @ExtendWith(CoroutineMainDispatcherExtension::class)
 * ```
 * ### Else, use @RegisterExtension to provide a different dispatcher. [Reference](https://junit.org/junit5/docs/current/user-guide/#extensions-registration-programmatic-static-fields-kotlin)
 * ```
 * companion object {
 *     @JvmField
 *     @RegisterExtension
 *     val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
 * }
 * ```
 * @param testDispatcher Dispatcher for the test. By default is UnconfinedTestDispatcher.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineMainDispatcherExtension(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : BeforeAllCallback, AfterAllCallback {

    /**
     * A function that defines the API for Extensions that wish to provide additional behavior
     * to test containers before all tests are invoked.
     */
    override fun beforeAll(p0: ExtensionContext?) {
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * A function that defines the API for Extensions that wish to provide additional behavior
     * to test containers after all tests have been invoked.
     */
    override fun afterAll(p0: ExtensionContext?) {
        Dispatchers.resetMain()
    }
}
