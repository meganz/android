package mega.privacy.android.app.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class generates a basic startup baseline profile for the target package.
 *
 * We recommend you start with this but add important user flows to the profile to improve their performance.
 * Refer to the [baseline profile documentation](https://d.android.com/topic/performance/baselineprofiles)
 * for more information.
 *
 * You can run the generator with the Generate Baseline Profile run configuration,
 * or directly with `generateBaselineProfile` Gradle task:
 * ```
 * ./gradlew --no-daemon :app:generateReleaseBaselineProfile -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile
 * ```
 * The run configuration runs the Gradle task and applies filtering to run only the generators.
 *
 * Check [documentation](https://d.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args)
 * for more information about available instrumentation arguments.
 *
 * After you run the generator, you can verify the improvements running the [StartupBenchmarks] benchmark.
 **/
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    /**
     * Generate baseline profile for the target package.
     * For mega app make sure you choose gmsNonMinifiedRelease variant
     * For TEST_ACCOUNT_USER_NAME and TEST_ACCOUNT_PASSWORD, you can use your own test account and update in the local.properties file
     */
    @Test
    fun generate() {
        rule.collect("mega.privacy.android.app") {
            // This block defines the app's critical user journey. Here we are interested in
            // optimizing for app startup. But you can also navigate and scroll
            // through your most important UI.
            // Start default activity for your app
            pressHome()
            startActivityAndWait()

            // Click Login on tour screen (usp_carousel_screen:log_in_button)
            device.findObject(By.res("usp_carousel_screen:log_in_button"))
                ?.click()
            device.waitForWindowUpdate(null, 1000)

            require(BuildConfig.TEST_ACCOUNT_USER_NAME.isNotEmpty()) { "Please put test_account_username in local.properties" }
            device.findObject(By.res("login_screen:email_input"))
                ?.let {
                    it.click()
                    it.setText(BuildConfig.TEST_ACCOUNT_USER_NAME)
                    device.waitForWindowUpdate(null, 1000)
                }

            require(BuildConfig.TEST_ACCOUNT_PASSWORD.isNotEmpty()) { "Please put test_account_password in local.properties" }
            device.findObject(By.res("login_screen:password_input"))
                ?.let {
                    it.click()
                    it.setText(BuildConfig.TEST_ACCOUNT_PASSWORD)
                    device.waitForWindowUpdate(null, 1000)
                }

            device.findObject(By.res("login_screen:login_button"))
                ?.let {
                    it.click()
                    // wait for fetch nodes
                    device.waitForWindowUpdate(null, 3000)
                }
        }
    }
}