package mega.privacy.android.app.presentation.imagepreview.view

import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retainRetainedValuesStoreRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.retain.rememberRetainedValuesStoreNavEntryDecorator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImagePreviewOptionsSheetStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val backStack = mutableStateListOf<NavKey>(Host, Preview)
    private val sheetStates = mutableListOf<ImagePreviewOptionsSheetState>()
    private var isPasscodeLocked by mutableStateOf(false)

    @Test
    fun `test that the options sheet stays shown when its navigation entry is covered`() {
        setContent()
        showOptionsSheet()

        backStack.add(Cover)
        composeTestRule.waitForIdle()
        backStack.removeAt(backStack.lastIndex)
        composeTestRule.waitForIdle()

        assertThat(sheetStates.last().isShown).isTrue()
    }

    @Test
    fun `test that the options sheet stays shown when the passcode lock removes the screen from composition`() {
        setContent()
        showOptionsSheet()

        isPasscodeLocked = true
        composeTestRule.waitForIdle()
        isPasscodeLocked = false
        composeTestRule.waitForIdle()

        assertThat(sheetStates.last().isShown).isTrue()
    }

    @Test
    fun `test that the options sheet is not shown when its navigation entry is popped and reopened`() {
        setContent()
        showOptionsSheet()

        backStack.removeAt(backStack.lastIndex)
        composeTestRule.waitForIdle()
        backStack.add(Preview)
        composeTestRule.waitForIdle()

        assertThat(sheetStates.last().isShown).isFalse()
    }

    private fun showOptionsSheet() {
        sheetStates.last().show()
        composeTestRule.waitForIdle()
    }

    /**
     * Mirrors how `LegacyActivityScaffold` hosts the image preview: the retained values registry
     * is created above the container that the passcode lock swaps out.
     */
    private fun setContent() {
        composeTestRule.setContent {
            val retainedValuesStoreRegistry = retainRetainedValuesStoreRegistry()
            if (isPasscodeLocked) {
                Text("passcode")
            } else {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberRetainedValuesStoreNavEntryDecorator(retainedValuesStoreRegistry),
                    ),
                    entryProvider = entryProvider {
                        entry<Host> { Text("host") }
                        entry<Preview> {
                            sheetStates += rememberImagePreviewOptionsSheetState()
                            Text("preview")
                        }
                        entry<Cover> { Text("cover") }
                    },
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Serializable
    private data object Host : NavKey

    @Serializable
    private data object Preview : NavKey

    @Serializable
    private data object Cover : NavKey
}
