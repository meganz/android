package mega.privacy.android.app.presentation.settings.compose.home.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import mega.privacy.android.app.presentation.settings.compose.home.model.SettingHeaderItem
import mega.privacy.android.app.presentation.settings.compose.home.model.SettingModelItem
import mega.privacy.android.app.presentation.settings.compose.home.model.SettingsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsListViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that header items are displayed`() {
        val key = "key"
        composeTestRule.setContent {
            SettingsListView(
                data = SettingsUiState.Data(
                    settings = persistentListOf(
                        SettingHeaderItem(headerText = { "text" }, key = key)
                    )
                ),
                modifier = Modifier.fillMaxSize(),
                initialKey = null,
                navHostController = rememberNavController(),
            )
        }

        composeTestRule.onNodeWithTag(settingsHeaderTag(key)).assertIsDisplayed()
    }

    @Test
    fun `test that setting items are displayed`() {
        val key = "key"
        composeTestRule.setContent {
            SettingsListView(
                data = SettingsUiState.Data(
                    settings = persistentListOf(
                        SettingModelItem(key = key,
                            name = "name",
                            description = null,
                            isEnabled = null,
                            isDestructive = false,
                            onClick = {})
                    )
                ),
                modifier = Modifier.fillMaxSize(),
                initialKey = null,
                navHostController = rememberNavController(),
            )
        }

        composeTestRule.onNodeWithTag(settingsItemTag(key)).assertIsDisplayed()
    }

    @Test
    fun `test that first item is displayed if no initial item specified`() {
        val settings = (0..500).map {
            SettingHeaderItem(headerText = { "text $it" }, key = "$it")
        }.toPersistentList()

        composeTestRule.setContent {
            SettingsListView(
                data = SettingsUiState.Data(
                    settings = settings
                ),
                modifier = Modifier.fillMaxSize(),
                initialKey = null,
                navHostController = rememberNavController(),
            )
        }

        composeTestRule.onNodeWithTag(settingsHeaderTag(0.toString())).assertIsDisplayed()
        composeTestRule.onNodeWithTag(settingsHeaderTag(250.toString()))
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that list scrolls to initial item`() {
        val settings = (0..500).map {
            SettingHeaderItem(headerText = { "text $it" }, key = "$it")
        }.toPersistentList()
        val initialKey = 250

        composeTestRule.setContent {
            SettingsListView(
                data = SettingsUiState.Data(
                    settings = settings
                ),
                modifier = Modifier.fillMaxSize(),
                initialKey = initialKey.toString(),
                navHostController = rememberNavController(),
            )
        }

        composeTestRule.onNodeWithTag(settingsHeaderTag(initialKey.toString())).assertIsDisplayed()
        composeTestRule.onNodeWithTag(settingsHeaderTag((initialKey - 1).toString()))
            .assertIsNotDisplayed()
    }
}