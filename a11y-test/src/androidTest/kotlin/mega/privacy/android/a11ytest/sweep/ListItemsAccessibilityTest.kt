package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.R as CoreUiR
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.list.GenericListItem
import mega.android.core.ui.components.list.OneLineListItem
import mega.android.core.ui.components.toggle.Toggle
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.a11ytest.ThemeReporters
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility baseline sweep of the design-system list item family, covering one-line,
 * flexible and generic list items with leading and trailing elements.
 */
@RunWith(AndroidJUnit4::class)
class ListItemsAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporters = ThemeReporters("ListItemsAccessibilityTest")

    @Test
    fun test_that_one_line_list_items_pass_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            OneLineListItem(text = "Plain item")
            OneLineListItem(
                text = "Item with elements",
                leadingElement = {
                    MegaIcon(
                        painter = painterResource(CoreUiR.drawable.ic_close_medium_thin_outline),
                        tint = IconColor.Primary,
                    )
                },
                trailingElement = {
                    MegaText(text = "Detail", textColor = TextColor.Secondary)
                },
            )
            OneLineListItem(
                text = "Item without click",
                enableClick = false,
            )
        }

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_flexible_line_list_items_pass_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            FlexibleLineListItem(
                title = "Two line item",
                subtitle = "Supporting text",
            )
            FlexibleLineListItem(
                title = "Item with toggle",
                subtitle = "Toggle detail",
                trailingElement = {
                    Toggle(isChecked = true, onCheckedChange = {})
                },
            )
            FlexibleLineListItem(
                title = "Disabled item",
                subtitle = "Disabled detail",
                enable = false,
                enableClick = false,
            )
        }

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_generic_list_items_pass_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            GenericListItem(
                title = { MegaText(text = "Generic item", textColor = TextColor.Primary) },
                subtitle = { MegaText(text = "Generic detail", textColor = TextColor.Secondary) },
                leadingElement = {
                    MegaIcon(
                        painter = painterResource(CoreUiR.drawable.ic_close_medium_thin_outline),
                        tint = IconColor.Primary,
                    )
                },
            )
            GenericListItem(
                title = { MegaText(text = "Selected item", textColor = TextColor.Primary) },
                subtitle = { MegaText(text = "Selected detail", textColor = TextColor.Secondary) },
                selected = true,
            )
        }

        sweep.checkInEachTheme()
    }
}
