package mega.privacy.android.navigation.contract.state

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalTopLevelNavKeyClassTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private object SectionNavKey : NavKey
    private object OtherNavKey : NavKey

    @Test
    fun `test that isMainNavBarRoot returns true when the section is the top-level destination`() {
        var result: Boolean? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalTopLevelNavKeyClass provides SectionNavKey::class) {
                result = isMainNavBarRoot(SectionNavKey::class)
            }
        }

        assertThat(result).isTrue()
    }

    @Test
    fun `test that isMainNavBarRoot returns false when another section is the top-level destination`() {
        var result: Boolean? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalTopLevelNavKeyClass provides OtherNavKey::class) {
                result = isMainNavBarRoot(SectionNavKey::class)
            }
        }

        assertThat(result).isFalse()
    }

    @Test
    fun `test that isMainNavBarRoot returns false when no top-level destination is provided`() {
        var result: Boolean? = null
        composeTestRule.setContent {
            result = isMainNavBarRoot(SectionNavKey::class)
        }

        assertThat(result).isFalse()
    }

    @Test
    fun `test that isPushedMainNavScreen returns false when no top-level destination is provided`() {
        var result: Boolean? = null
        composeTestRule.setContent {
            result = isPushedMainNavScreen(SectionNavKey::class)
        }

        assertThat(result).isFalse()
    }

    @Test
    fun `test that isPushedMainNavScreen returns true when another section is the top-level destination`() {
        var result: Boolean? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalTopLevelNavKeyClass provides OtherNavKey::class) {
                result = isPushedMainNavScreen(SectionNavKey::class)
            }
        }

        assertThat(result).isTrue()
    }

    @Test
    fun `test that isPushedMainNavScreen returns false when the section is the top-level destination`() {
        var result: Boolean? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalTopLevelNavKeyClass provides SectionNavKey::class) {
                result = isPushedMainNavScreen(SectionNavKey::class)
            }
        }

        assertThat(result).isFalse()
    }
}
