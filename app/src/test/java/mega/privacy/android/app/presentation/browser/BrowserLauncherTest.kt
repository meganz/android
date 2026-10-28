package mega.privacy.android.app.presentation.browser

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class BrowserLauncherTest {

    private val appPackage = "mega.privacy.android.app"
    private val chrome = "com.android.chrome"
    private val firefox = "org.mozilla.firefox"

    @Test
    fun `test that selectBrowser returns the Custom Tabs provider when one exists`() {
        val actual = selectBrowser(
            customTabsPackage = chrome,
            defaultBrowserPackage = firefox,
            browserPackages = listOf(firefox, chrome),
            appPackage = appPackage,
        )

        assertThat(actual).isEqualTo(SelectedBrowser.CustomTab(chrome))
    }

    @Test
    fun `test that selectBrowser returns the default browser when no Custom Tabs provider exists`() {
        val actual = selectBrowser(
            customTabsPackage = null,
            defaultBrowserPackage = firefox,
            browserPackages = listOf(chrome, firefox),
            appPackage = appPackage,
        )

        assertThat(actual).isEqualTo(SelectedBrowser.Browser(firefox))
    }

    @Test
    fun `test that selectBrowser returns an installed browser when no default browser is selected`() {
        val actual = selectBrowser(
            customTabsPackage = null,
            defaultBrowserPackage = "android",
            browserPackages = listOf(appPackage, firefox),
            appPackage = appPackage,
        )

        assertThat(actual).isEqualTo(SelectedBrowser.Browser(firefox))
    }

    @Test
    fun `test that selectBrowser does not return this app when it is the Custom Tabs provider`() {
        val actual = selectBrowser(
            customTabsPackage = appPackage,
            defaultBrowserPackage = appPackage,
            browserPackages = listOf(appPackage, chrome),
            appPackage = appPackage,
        )

        assertThat(actual).isEqualTo(SelectedBrowser.Browser(chrome))
    }

    @Test
    fun `test that selectBrowser returns null when only this app can open the URL`() {
        val actual = selectBrowser(
            customTabsPackage = null,
            defaultBrowserPackage = appPackage,
            browserPackages = listOf(appPackage),
            appPackage = appPackage,
        )

        assertThat(actual).isNull()
    }
}
