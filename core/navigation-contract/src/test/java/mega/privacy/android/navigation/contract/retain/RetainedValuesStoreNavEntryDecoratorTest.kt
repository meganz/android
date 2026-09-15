package mega.privacy.android.navigation.contract.retain

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RetainedValuesStoreNavEntryDecoratorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val backStack = mutableStateListOf<NavKey>(Root)
    private val rootRetained = mutableListOf<Any>()
    private val childRetained = mutableListOf<Any>()

    @Test
    fun `test that a retained value survives its entry being covered and uncovered`() {
        setContent()
        val first = rootRetained.last()

        backStack.add(Child)
        composeTestRule.waitForIdle()
        backStack.removeAt(backStack.lastIndex)
        composeTestRule.waitForIdle()

        assertThat(rootRetained.last()).isSameInstanceAs(first)
    }

    @Test
    fun `test that a retained value is discarded when its entry is popped`() {
        setContent()
        backStack.add(Child)
        composeTestRule.waitForIdle()
        val first = childRetained.last()

        backStack.removeAt(backStack.lastIndex)
        composeTestRule.waitForIdle()
        backStack.add(Child)
        composeTestRule.waitForIdle()

        assertThat(childRetained.last()).isNotSameInstanceAs(first)
    }

    @Test
    fun `test that entries do not share retained values`() {
        setContent()
        backStack.add(Child)
        composeTestRule.waitForIdle()

        assertThat(childRetained.last()).isNotSameInstanceAs(rootRetained.last())
    }

    private fun setContent() {
        composeTestRule.setContent {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberRetainedValuesStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<Root> {
                        rootRetained += retain { Any() }
                        Text("root")
                    }
                    entry<Child> {
                        childRetained += retain { Any() }
                        Text("child")
                    }
                },
            )
        }
        composeTestRule.waitForIdle()
    }

    @Serializable
    private data object Root : NavKey

    @Serializable
    private data object Child : NavKey
}
