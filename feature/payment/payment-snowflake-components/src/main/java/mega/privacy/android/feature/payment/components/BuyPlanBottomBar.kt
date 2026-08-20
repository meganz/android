package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.button.BrandFilledButton
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.TextOnlyButton
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Composable function to display the bottom bar for the Buy Plan screen.
 *
 * @param text label of the primary buy button
 * @param textOnlyButtonText label of an optional underlined text button shown below the buy button,
 * null to show the buy button alone
 * @param onTextOnlyButtonClick called when the text button is tapped
 * @param maxContentWidth caps the width of the buttons and centres them, leaving the divider and
 * background full width; [Dp.Unspecified] lets the buttons span the whole bar
 * @param useBrandButton when true, renders the brand (red) buy button instead of the primary one
 */
@Composable
fun BuyPlanBottomBar(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    text: String,
    textOnlyButtonText: String? = null,
    onTextOnlyButtonClick: () -> Unit = {},
    maxContentWidth: Dp = Dp.Unspecified,
    useBrandButton: Boolean = false,
) {
    Box(
        modifier = modifier
            .background(color = DSTokens.colors.background.pageBackground)
            .fillMaxWidth(),
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = DSTokens.colors.border.subtle
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = maxContentWidth)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val buttonModifier = Modifier
                .fillMaxWidth()
                .testTag(TEST_TAG_BUY_BUTTON)
            if (useBrandButton) {
                BrandFilledButton(
                    modifier = buttonModifier,
                    text = text,
                    onClick = onClick,
                )
            } else {
                PrimaryFilledButton(
                    modifier = buttonModifier,
                    text = text,
                    onClick = onClick,
                )
            }

            textOnlyButtonText?.let {
                TextOnlyButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TEST_TAG_BUY_PLAN_TEXT_ONLY_BUTTON),
                    text = it,
                    onClick = onTextOnlyButtonClick,
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun BuyPlanBottomBarPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        BuyPlanBottomBar(
            text = "Buy now",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun BuyPlanBottomBarWithTextOnlyButtonPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        BuyPlanBottomBar(
            text = "Get Pro I",
            textOnlyButtonText = "View all plans",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun BuyPlanBottomBarBrandButtonPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        BuyPlanBottomBar(
            text = "Get Pro I",
            textOnlyButtonText = "View all plans",
            useBrandButton = true,
        )
    }
}

/**
 * Tag for the ProPlanCard root container
 */
const val TEST_TAG_BUY_BUTTON = "buy_button"

/**
 * Tag for the optional text button below the buy button
 */
const val TEST_TAG_BUY_PLAN_TEXT_ONLY_BUTTON = "buy_plan_bottom_bar:text_only_button"
