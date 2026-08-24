package mega.privacy.android.feature.payment.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Background tint of the promotional offer plan cards on the subscription offer flows.
 *
 * Both values mirror the design's own `pricing-card/background` variable (DSN-3130 / DSN-3131) and
 * are spelled out because the design system exposes them under no semantic token: the light shade
 * lives in core-ui's internal palette as `Primary/025`, the dark one is not in the palette at all.
 * `brand.containerDefault` — the closest token — is the brand red at 10% (light) and 30% (dark),
 * far more saturated than either.
 */
internal val offerPlanCardBackground: Color
    @Composable get() = if (DSTokens.colors.isLight) {
        OFFER_PLAN_CARD_BACKGROUND_LIGHT
    } else {
        OFFER_PLAN_CARD_BACKGROUND_DARK
    }

private val OFFER_PLAN_CARD_BACKGROUND_LIGHT = Color(0xFFFDF9F8)

private val OFFER_PLAN_CARD_BACKGROUND_DARK = Color(0xFF231410)
