package mega.privacy.android.feature.payment.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.SecondaryFilledButton
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Composable function to display the bottom bar for the Buy Plan screen.
 */
@Composable
fun BuyPlanBottomBar(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    text: String,
) {
    Box(
        modifier = modifier
            .background(color = DSTokens.colors.background.pageBackground)
            .navigationBarsPadding()
            .fillMaxWidth(),
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = DSTokens.colors.border.strong
        )

        PrimaryFilledButton(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth()
                .testTag(TEST_TAG_BUY_BUTTON),
            text = text,
            onClick = onClick,
        )
    }
}

@Composable
fun BuyPlanBottomBar(
    modifier: Modifier = Modifier,
    isExternalCheckoutDefault: Boolean = false,
    onClick: (Boolean) -> Unit = {},
    inAppCheckoutText: String,
    externalCheckoutText: String,
) {
    Box(
        modifier = modifier
            .background(color = DSTokens.colors.background.pageBackground)
            .navigationBarsPadding()
            .fillMaxWidth(),
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = DSTokens.colors.border.strong
        )

        Column {
            if (isExternalCheckoutDefault) {
                PrimaryFilledButton(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 20.dp, bottom = 10.dp)
                        .fillMaxWidth()
                        .testTag(TEST_TAG_BUY_ON_WEBSITE_BUTTON),
                    text = externalCheckoutText,
                    onClick = { onClick(true) },
                )
                SecondaryFilledButton(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp, bottom = 20.dp)
                        .fillMaxWidth()
                        .testTag(TEST_TAG_BUY_BUTTON),
                    text = inAppCheckoutText,
                    onClick = { onClick(false) },
                )
            } else {
                PrimaryFilledButton(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 20.dp, bottom = 10.dp)
                        .fillMaxWidth()
                        .testTag(TEST_TAG_BUY_BUTTON),
                    text = inAppCheckoutText,
                    onClick = { onClick(false) },
                )
                SecondaryFilledButton(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp, bottom = 20.dp)
                        .fillMaxWidth()
                        .testTag(TEST_TAG_BUY_ON_WEBSITE_BUTTON),
                    text = externalCheckoutText,
                    onClick = { onClick(true) },
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@CombinedThemePreviews
@Composable
private fun BuyPlanBottomBarPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        Column {
            BuyPlanBottomBar(
                text = "Buy now",
            )

            BuyPlanBottomBar(
                inAppCheckoutText = "Buy now",
                externalCheckoutText = String.format(
                    "Buy on our website (Save up to %.0f%%)",
                    15f
                )
            )

            BuyPlanBottomBar(
                isExternalCheckoutDefault = true,
                inAppCheckoutText = "Buy now",
                externalCheckoutText = String.format(
                    "Buy on our website (Save up to %.0f%%)",
                    15f
                )
            )
        }
    }
}


/**
 * Tag for the ProPlanCard root container
 */
const val TEST_TAG_BUY_BUTTON = "buy_button"
const val TEST_TAG_BUY_ON_WEBSITE_BUTTON = "buy_on_website_button"
