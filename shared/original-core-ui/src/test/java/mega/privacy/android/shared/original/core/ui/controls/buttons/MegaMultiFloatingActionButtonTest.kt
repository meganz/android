package mega.privacy.android.shared.original.core.ui.controls.buttons

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.icon.pack.R as iconPackR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MegaMultiFloatingActionButtonTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that fab content is shown correctly in collapsed status`() {
        composeRule.setContent {
            MegaMultiFloatingActionButton(
                items = listOf(
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_sync_01_medium_thin_outline),
                        label = "Sync",
                        onClicked = {},
                    ),
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_database_medium_thin_outline),
                        label = "Backup",
                        onClicked = {},
                    ),
                ),
            )
        }
        composeRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG, useUnmergedTree = true)
            .assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("Sync").assertDoesNotExist()
        composeRule.onNodeWithText("Backup").assertDoesNotExist()
    }

    @Test
    fun `test that fab content is shown correctly in expanded status`() {
        composeRule.setContent {
            val multiFabState = rememberMultiFloatingActionButtonState()
            MegaMultiFloatingActionButton(
                items = listOf(
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_sync_01_medium_thin_outline),
                        label = "Sync",
                        onClicked = {},
                    ),
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_database_medium_thin_outline),
                        label = "Backup",
                        onClicked = {},
                    ),
                ),
                multiFabState = multiFabState,
                onStateChanged = { state -> multiFabState.value = state },
            )
        }
        composeRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG, useUnmergedTree = true)
            .assertExists().assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Sync").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("Backup").assertExists().assertIsDisplayed()
    }

    @Test
    fun `test that main fab is clickable if it is enabled`() {
        var clicked = false
        composeRule.setContent {
            MegaMultiFloatingActionButton(
                items = listOf(
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_sync_01_medium_thin_outline),
                        label = "Sync",
                        onClicked = {},
                    ),
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_database_medium_thin_outline),
                        label = "Backup",
                        onClicked = {},
                    ),
                ),
                onStateChanged = { clicked = true },
            )
        }
        composeRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG).performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `test that main fab is not clickable if it is not enabled`() {
        var clicked = false
        composeRule.setContent {
            MegaMultiFloatingActionButton(
                items = listOf(
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_sync_01_medium_thin_outline),
                        label = "Sync",
                        onClicked = {},
                    ),
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_database_medium_thin_outline),
                        label = "Backup",
                        onClicked = {},
                    ),
                ),
                enabled = false,
                onStateChanged = { clicked = true },
            )
        }
        composeRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG).performClick()
        assertThat(clicked).isFalse()
    }

    @Test
    fun `test that main fab has correct size depending on status`() {
        val mainButtonCollapsedStyle = FloatingActionButtonStyle.Big
        val mainButtonExpandedStyle = FloatingActionButtonStyle.Medium
        composeRule.setContent {
            val multiFabState = rememberMultiFloatingActionButtonState()
            MegaMultiFloatingActionButton(
                items = listOf(
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_sync_01_medium_thin_outline),
                        label = "Sync",
                        onClicked = {},
                    ),
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_database_medium_thin_outline),
                        label = "Backup",
                        onClicked = {},
                    ),
                ),
                mainButtonCollapsedStyle = mainButtonCollapsedStyle,
                mainButtonExpandedStyle = mainButtonExpandedStyle,
                multiFabState = multiFabState,
                onStateChanged = { state -> multiFabState.value = state },
            )
        }
        composeRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG)
            .assertHeightIsEqualTo(mainButtonCollapsedStyle.size)
        composeRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG)
            .assertWidthIsEqualTo(mainButtonCollapsedStyle.size)
        composeRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG).performClick()
        composeRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG)
            .assertHeightIsEqualTo(mainButtonExpandedStyle.size)
        composeRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG)
            .assertWidthIsEqualTo(mainButtonExpandedStyle.size)
    }

    @Test
    fun `test that option fab is clickable if it is enabled`() {
        var clicked = false
        composeRule.setContent {
            val multiFabState = rememberMultiFloatingActionButtonState()
            MegaMultiFloatingActionButton(
                items = listOf(
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_sync_01_medium_thin_outline),
                        label = "Sync",
                        onClicked = { clicked = true },
                    ),
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_database_medium_thin_outline),
                        label = "Backup",
                        onClicked = {},
                    ),
                ),
                multiFabState = multiFabState,
                onStateChanged = { state -> multiFabState.value = state },
            )
        }
        composeRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG).performClick()
        composeRule.onNodeWithTag("${MULTI_FAB_OPTION_FAB_TEST_TAG}_Sync").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `test that option fab is not clickable if it is not enabled`() {
        var clicked = false
        composeRule.setContent {
            val multiFabState = rememberMultiFloatingActionButtonState()
            MegaMultiFloatingActionButton(
                items = listOf(
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_sync_01_medium_thin_outline),
                        label = "Sync",
                        enabled = false,
                        onClicked = { clicked = true },
                    ),
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_database_medium_thin_outline),
                        label = "Backup",
                        onClicked = {},
                    ),
                ),
                multiFabState = multiFabState,
                onStateChanged = { state -> multiFabState.value = state },
            )
        }
        composeRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG).performClick()
        composeRule.onNodeWithTag("${MULTI_FAB_OPTION_FAB_TEST_TAG}_Sync").performClick()
        assertThat(clicked).isFalse()
    }

    @Test
    fun `test that option fab label is clickable if it is enabled`() {
        var clicked = false
        composeRule.setContent {
            val multiFabState = rememberMultiFloatingActionButtonState()
            MegaMultiFloatingActionButton(
                items = listOf(
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_sync_01_medium_thin_outline),
                        label = "Sync",
                        onClicked = { clicked = true },
                    ),
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_database_medium_thin_outline),
                        label = "Backup",
                        onClicked = {},
                    ),
                ),
                multiFabState = multiFabState,
                onStateChanged = { state -> multiFabState.value = state },
            )
        }
        composeRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG).performClick()
        composeRule.onNodeWithTag("${MULTI_FAB_OPTION_LABEL_TEST_TAG}_Sync").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `test that option fab label is not clickable if it is not enabled`() {
        var clicked = false
        composeRule.setContent {
            val multiFabState = rememberMultiFloatingActionButtonState()
            MegaMultiFloatingActionButton(
                items = listOf(
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_sync_01_medium_thin_outline),
                        label = "Sync",
                        enabled = false,
                        onClicked = { clicked = true },
                    ),
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_database_medium_thin_outline),
                        label = "Backup",
                        onClicked = {},
                    ),
                ),
                multiFabState = multiFabState,
                onStateChanged = { state -> multiFabState.value = state },
            )
        }
        composeRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG).performClick()
        composeRule.onNodeWithTag("${MULTI_FAB_OPTION_LABEL_TEST_TAG}_Sync").performClick()
        assertThat(clicked).isFalse()
    }

    @Test
    fun `test that entire option fab row is clickable if it is enabled`() {
        var clicked = false
        composeRule.setContent {
            val multiFabState = rememberMultiFloatingActionButtonState()
            MegaMultiFloatingActionButton(
                items = listOf(
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_sync_01_medium_thin_outline),
                        label = "Sync",
                        onClicked = { clicked = true },
                    ),
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_database_medium_thin_outline),
                        label = "Backup",
                        onClicked = {},
                    ),
                ),
                multiFabState = multiFabState,
                onStateChanged = { state -> multiFabState.value = state },
            )
        }
        composeRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG).performClick()
        composeRule.onNodeWithTag("${MULTI_FAB_OPTION_ROW_TEST_TAG}_Sync").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `test that entire option fab row is not clickable if it is not enabled`() {
        var clicked = false
        composeRule.setContent {
            val multiFabState = rememberMultiFloatingActionButtonState()
            MegaMultiFloatingActionButton(
                items = listOf(
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_sync_01_medium_thin_outline),
                        label = "Sync",
                        enabled = false,
                        onClicked = { clicked = true },
                    ),
                    MultiFloatingActionButtonItem(
                        icon = painterResource(id = iconPackR.drawable.ic_database_medium_thin_outline),
                        label = "Backup",
                        onClicked = {},
                    ),
                ),
                multiFabState = multiFabState,
                onStateChanged = { state -> multiFabState.value = state },
            )
        }
        composeRule.onNodeWithTag(MULTI_FAB_MAIN_FAB_TEST_TAG).performClick()
        composeRule.onNodeWithTag("${MULTI_FAB_OPTION_ROW_TEST_TAG}_Sync").performClick()
        assertThat(clicked).isFalse()
    }
}