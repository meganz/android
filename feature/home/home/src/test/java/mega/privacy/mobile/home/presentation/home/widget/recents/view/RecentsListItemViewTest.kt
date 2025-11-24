package mega.privacy.mobile.home.presentation.home.widget.recents.view

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.icon.pack.R as IconPackR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class RecentsListItemViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val title = "First line text"
    private val parentFolderName = "Parent folder"
    private val time = "12:00 PM"
    private val icon = IconPackR.drawable.ic_folder_incoming_medium_solid
    private val shareIcon = IconPackR.drawable.ic_share_network_medium_thin_outline
    private val updatedByText = "[A]added by[/A] [B]John Doe[/B]"

    @Test
    fun `test that recent action list item is displayed correctly when all information provided`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsListItemView(
                    title = title,
                    icon = icon,
                    parentFolderName = parentFolderName,
                    time = time,
                    shareIcon = shareIcon,
                    onItemClicked = {},
                    onMenuClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(FIRST_LINE_TEST_TAG, true).assertExists()
        composeRule.onNodeWithTag(FOLDER_NAME_TEST_TAG, true).assertExists()
        composeRule.onNodeWithTag(TIME_TEST_TAG, true).assertTextEquals(time)
        composeRule.onNodeWithTag(ICON_TEST_TAG, true).assertExists()
        composeRule.onNodeWithTag(ACTION_ICON_TEST_TAG, true).assertExists()
        composeRule.onNodeWithTag(MENU_TEST_TAG, true).assertExists()
    }

    @Test
    fun `test that favorite icon is displayed when showFavourite is true`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsListItemView(
                    title = title,
                    icon = icon,
                    parentFolderName = parentFolderName,
                    time = time,
                    showFavourite = true,
                    onItemClicked = {},
                    onMenuClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(FAVORITE_TEST_TAG, true).assertExists()
    }

    @Test
    fun `test that share icon is displayed when shareIcon is provided`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsListItemView(
                    title = title,
                    icon = icon,
                    parentFolderName = parentFolderName,
                    time = time,
                    shareIcon = shareIcon,
                    onItemClicked = {},
                    onMenuClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(SHARES_ICON_TEST_TAG, true).assertExists()
    }

    @Test
    fun `test that label is displayed when label is provided`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsListItemView(
                    title = title,
                    icon = icon,
                    parentFolderName = parentFolderName,
                    time = time,
                    label = NodeLabel.RED,
                    onItemClicked = {},
                    onMenuClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(LABEL_TEST_TAG, true).assertExists()
    }

    @Test
    fun `test that updatedByText is displayed when provided`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsListItemView(
                    title = title,
                    icon = icon,
                    parentFolderName = parentFolderName,
                    time = time,
                    updatedByText = updatedByText,
                    onItemClicked = {},
                    onMenuClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(SECOND_LINE_TEST_TAG, true).assertExists()
    }

    @Test
    fun `test that version icon is displayed when showVersion is true`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsListItemView(
                    title = title,
                    icon = icon,
                    parentFolderName = parentFolderName,
                    time = time,
                    showVersion = true,
                    onItemClicked = {},
                    onMenuClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(ACTION_ICON_TEST_TAG, true).assertExists()
    }

    @Test
    fun `test that menu button is displayed when isMediaBucket is false`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsListItemView(
                    title = title,
                    icon = icon,
                    parentFolderName = parentFolderName,
                    time = time,
                    isMediaBucket = false,
                    onItemClicked = {},
                    onMenuClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(MENU_TEST_TAG, true).assertExists()
        composeRule.onNodeWithTag(MEDIA_BUCKET_MENU_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that media bucket menu button is displayed when isMediaBucket is true`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsListItemView(
                    title = title,
                    icon = icon,
                    parentFolderName = parentFolderName,
                    time = time,
                    isMediaBucket = true,
                    onItemClicked = {},
                    onMenuClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(MEDIA_BUCKET_MENU_TEST_TAG, true).assertExists()
        composeRule.onNodeWithTag(MENU_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that view is displayed with default values when some parameters are not provided`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsListItemView(
                    title = title,
                    parentFolderName = parentFolderName,
                    time = time,
                    onItemClicked = {},
                    onMenuClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(ICON_TEST_TAG, true).assertExists()
        composeRule.onNodeWithTag(ACTION_ICON_TEST_TAG, true).assertExists()
        composeRule.onNodeWithTag(MENU_TEST_TAG, true).assertExists()
    }

    @Test
    fun `test that favorite icon is not displayed when showFavourite is false`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsListItemView(
                    title = title,
                    icon = icon,
                    parentFolderName = parentFolderName,
                    time = time,
                    showFavourite = false,
                    onItemClicked = {},
                    onMenuClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(FAVORITE_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that share icon is not displayed when shareIcon is null`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsListItemView(
                    title = title,
                    icon = icon,
                    parentFolderName = parentFolderName,
                    time = time,
                    shareIcon = null,
                    onItemClicked = {},
                    onMenuClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(SHARES_ICON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that label is not displayed when label is null`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsListItemView(
                    title = title,
                    icon = icon,
                    parentFolderName = parentFolderName,
                    time = time,
                    label = null,
                    onItemClicked = {},
                    onMenuClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(LABEL_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that updatedByText is not displayed when not provided`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsListItemView(
                    title = title,
                    icon = icon,
                    parentFolderName = parentFolderName,
                    time = time,
                    updatedByText = null,
                    onItemClicked = {},
                    onMenuClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(SECOND_LINE_TEST_TAG).assertDoesNotExist()
    }
}