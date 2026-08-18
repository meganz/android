package mega.privacy.mobile.home.presentation.home.widget.banner.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.banner.HOME_BANNER_MIN_HEIGHT_DP
import mega.android.core.ui.components.button.PrimaryFilledButtonXXSmall
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR

/**
 * Compact discount offer banner for the Home carousel (Figma 9588:2157).
 *
 * The layout mirrors the core-ui `HomeBanner` (same min height, `Row` padding, title min-height and
 * button top-padding) so the offer card is the same height as the other cards in the carousel. Only
 * the palette differs: the campaign artwork is always light, so the content is themed light via
 * [AndroidTheme] with `isDark = false`, giving dark [TextColor.Primary] copy, a dark
 * [PrimaryFilledButtonXXSmall] "Grab deal" button and a dark dismiss icon regardless of the device
 * theme. `useLegacyStatusBarColor = false` keeps the forced theme from touching the window.
 *
 * @param title Banner copy; the campaign headline and price on two lines (`"headline\nprice"`).
 * @param buttonText Call-to-action label, e.g. "Grab deal".
 * @param onClick Invoked when the banner or its call-to-action is tapped.
 * @param onDismissClick Invoked when the dismiss (✕) button is tapped.
 * @param modifier Modifier for the banner container.
 */
@Composable
internal fun HomeOfferBanner(
    title: String,
    buttonText: String,
    onClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidTheme(isDark = false, useLegacyStatusBarColor = false) {
        val spacing = LocalSpacing.current
        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .heightIn(min = HOME_BANNER_MIN_HEIGHT_DP.dp)
                .clip(RoundedCornerShape(10.dp))
                .testTag(HOME_OFFER_BANNER_TAG),
        ) {
            Image(
                painter = painterResource(id = iconPackR.drawable.offer_banner_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(0f, BACKGROUND_VERTICAL_BIAS),
                modifier = Modifier.matchParentSize(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(min = HOME_BANNER_MIN_HEIGHT_DP.dp)
                    .padding(horizontal = spacing.x8, vertical = spacing.x12),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.7f)
                        .wrapContentHeight()
                        .padding(start = spacing.x4),
                    verticalArrangement = Arrangement.Top,
                ) {
                    MegaText(
                        text = title,
                        textColor = TextColor.Primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = AppTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier
                            .heightIn(min = 32.dp)
                            .testTag(HOME_OFFER_BANNER_TITLE_TAG),
                    )
                    PrimaryFilledButtonXXSmall(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .testTag(HOME_OFFER_BANNER_BUTTON_TAG),
                        text = buttonText,
                        onClick = onClick,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight(),
                )
            }

            MegaIcon(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = spacing.x8, top = spacing.x8)
                    .size(spacing.x16)
                    .clickable { onDismissClick() }
                    .testTag(HOME_OFFER_BANNER_DISMISS_TAG),
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.X),
                contentDescription = "Dismiss offer",
                tint = IconColor.Primary,
            )
        }
    }
}

/**
 * Vertical crop bias for the campaign artwork, anchoring the visible band slightly below centre so
 * the stopwatch and basket stay in view (matches the profile `OfferBanner`).
 */
private const val BACKGROUND_VERTICAL_BIAS = 0.25f

internal const val HOME_OFFER_BANNER_TAG = "home_offer_banner"
internal const val HOME_OFFER_BANNER_TITLE_TAG = "home_offer_banner:title"
internal const val HOME_OFFER_BANNER_BUTTON_TAG = "home_offer_banner:button"
internal const val HOME_OFFER_BANNER_DISMISS_TAG = "home_offer_banner:dismiss"

@CombinedThemePreviews
@Composable
private fun HomeOfferBannerPreview() {
    AndroidThemeForPreviews {
        HomeOfferBanner(
            title = "Black Friday · Get 50% off\n€4.99/month for Pro I",
            buttonText = "Grab deal",
            onClick = {},
            onDismissClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
