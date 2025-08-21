package mega.privacy.android.core.nodecomponents.list

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.devicetype.DeviceType
import mega.android.core.ui.theme.devicetype.LocalDeviceType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RememberDynamicSpanCountTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test rememberDynamicSpanCount returns default when isListView is true`() {
        val configuration = Configuration().apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
            screenWidthDp = 800
        }
        var result = 0

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDeviceType provides DeviceType.Tablet
            ) {
                result = rememberDynamicSpanCount(
                    defaultSpanCount = 3,
                    isListView = true
                )
            }
        }

        assertThat(result).isEqualTo(3)
    }

    @Test
    fun `test rememberDynamicSpanCount returns 2 for phone portrait`() {
        val configuration = Configuration().apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
            screenWidthDp = 360
        }
        var result = 0

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDeviceType provides DeviceType.Phone
            ) {
                result = rememberDynamicSpanCount(isListView = false)
            }
        }

        assertThat(result).isEqualTo(2)
    }

    @Test
    fun `test rememberDynamicSpanCount returns 4 for phone landscape`() {
        val configuration = Configuration().apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
            screenWidthDp = 640
        }
        var result = 0

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDeviceType provides DeviceType.Phone
            ) {
                result = rememberDynamicSpanCount(isListView = false)
            }
        }

        assertThat(result).isEqualTo(4)
    }

    @Test
    fun `test rememberDynamicSpanCount returns 5 for large tablet portrait`() {
        val configuration = Configuration().apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
            screenWidthDp = 900
        }
        var result = 0

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDeviceType provides DeviceType.Tablet
            ) {
                result = rememberDynamicSpanCount(isListView = false)
            }
        }

        assertThat(result).isEqualTo(5)
    }

    @Test
    fun `test rememberDynamicSpanCount returns 8 for large tablet landscape`() {
        val configuration = Configuration().apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
            screenWidthDp = 1200
        }
        var result = 0

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDeviceType provides DeviceType.Tablet
            ) {
                result = rememberDynamicSpanCount(isListView = false)
            }
        }

        assertThat(result).isEqualTo(8)
    }

    @Test
    fun `test rememberDynamicSpanCount returns 4 for medium tablet portrait`() {
        val configuration = Configuration().apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
            screenWidthDp = 720
        }
        var result = 0

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDeviceType provides DeviceType.Tablet
            ) {
                result = rememberDynamicSpanCount(isListView = false)
            }
        }

        assertThat(result).isEqualTo(4)
    }

    @Test
    fun `test rememberDynamicSpanCount returns 6 for medium tablet landscape`() {
        val configuration = Configuration().apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
            screenWidthDp = 800
        }
        var result = 0

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDeviceType provides DeviceType.Tablet
            ) {
                result = rememberDynamicSpanCount(isListView = false)
            }
        }

        assertThat(result).isEqualTo(6)
    }

    @Test
    fun `test rememberDynamicSpanCount returns 3 for small tablet portrait`() {
        val configuration = Configuration().apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
            screenWidthDp = 500
        }
        var result = 0

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDeviceType provides DeviceType.Tablet
            ) {
                result = rememberDynamicSpanCount(isListView = false)
            }
        }

        assertThat(result).isEqualTo(3)
    }

    @Test
    fun `test rememberDynamicSpanCount returns 5 for small tablet landscape`() {
        val configuration = Configuration().apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
            screenWidthDp = 500
        }
        var result = 0

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDeviceType provides DeviceType.Tablet
            ) {
                result = rememberDynamicSpanCount(isListView = false)
            }
        }

        assertThat(result).isEqualTo(5)
    }

    @Test
    fun `test rememberDynamicSpanCount uses default parameters correctly`() {
        val configuration = Configuration().apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
            screenWidthDp = 360
        }
        var result = 0

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDeviceType provides DeviceType.Phone
            ) {
                result = rememberDynamicSpanCount()
            }
        }

        assertThat(result).isEqualTo(2)
    }

    @Test
    fun `test tablet boundary at 840dp - medium tablet`() {
        val configuration = Configuration().apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
            screenWidthDp = 839
        }
        var result = 0

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDeviceType provides DeviceType.Tablet
            ) {
                result = rememberDynamicSpanCount(isListView = false)
            }
        }

        assertThat(result).isEqualTo(4) // Medium tablet
    }

    @Test
    fun `test tablet boundary at 840dp - large tablet`() {
        val configuration = Configuration().apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
            screenWidthDp = 840
        }
        var result = 0

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDeviceType provides DeviceType.Tablet
            ) {
                result = rememberDynamicSpanCount(isListView = false)
            }
        }

        assertThat(result).isEqualTo(5) // Large tablet
    }

    @Test
    fun `test tablet boundary at 600dp - small tablet`() {
        val configuration = Configuration().apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
            screenWidthDp = 599
        }
        var result = 0

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDeviceType provides DeviceType.Tablet
            ) {
                result = rememberDynamicSpanCount(isListView = false)
            }
        }

        assertThat(result).isEqualTo(3) // Small tablet
    }

    @Test
    fun `test tablet boundary at 600dp - medium tablet`() {
        val configuration = Configuration().apply {
            orientation = Configuration.ORIENTATION_PORTRAIT
            screenWidthDp = 600
        }
        var result = 0

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDeviceType provides DeviceType.Tablet
            ) {
                result = rememberDynamicSpanCount(isListView = false)
            }
        }

        assertThat(result).isEqualTo(4) // Medium tablet
    }
}
