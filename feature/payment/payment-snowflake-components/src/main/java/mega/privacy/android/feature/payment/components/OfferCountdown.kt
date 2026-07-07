package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Countdown shown on the offer/discount subscription page. Displays a "valid until" caption above a
 * brand-tinted container with the remaining Days, Hours and Minutes separated by vertical dividers.
 *
 * This is a stateless display component: the caller is responsible for computing and formatting the
 * remaining time (e.g. from a ViewModel) and for driving updates over time.
 *
 * @param validUntilText the caption text (e.g. "valid until July 11, 2026")
 * @param days the days value, pre-formatted (e.g. "00")
 * @param hours the hours value, pre-formatted (e.g. "00")
 * @param minutes the minutes value, pre-formatted (e.g. "01")
 * @param daysLabel the localized label for the days unit (e.g. "Days")
 * @param hoursLabel the localized label for the hours unit (e.g. "Hours")
 * @param minutesLabel the localized label for the minutes unit (e.g. "Minutes")
 * @param modifier
 */
@Composable
fun OfferCountdown(
    validUntilText: String,
    days: String,
    hours: String,
    minutes: String,
    daysLabel: String,
    hoursLabel: String,
    minutesLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TEST_TAG_OFFER_COUNTDOWN),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MegaText(
            text = validUntilText,
            style = MaterialTheme.typography.bodyLarge,
            textColor = TextColor.Secondary,
            modifier = Modifier.testTag(TEST_TAG_OFFER_COUNTDOWN_VALID_UNTIL),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = DSTokens.colors.brand.containerDefault,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CountdownUnit(
                value = days,
                label = daysLabel,
                modifier = Modifier.testTag(TEST_TAG_OFFER_COUNTDOWN_DAYS),
            )
            CountdownDivider()
            CountdownUnit(
                value = hours,
                label = hoursLabel,
                modifier = Modifier.testTag(TEST_TAG_OFFER_COUNTDOWN_HOURS),
            )
            CountdownDivider()
            CountdownUnit(
                value = minutes,
                label = minutesLabel,
                modifier = Modifier.testTag(TEST_TAG_OFFER_COUNTDOWN_MINUTES),
            )
        }
    }
}

@Composable
private fun CountdownUnit(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(52.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MegaText(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            textColor = TextColor.Primary,
        )
        MegaText(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            textColor = TextColor.Secondary,
        )
    }
}

@Composable
private fun CountdownDivider() {
    VerticalDivider(
        modifier = Modifier.height(52.dp),
        thickness = 1.dp,
        color = DSTokens.colors.border.strong,
    )
}

@CombinedThemePreviews
@Composable
private fun OfferCountdownPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        OfferCountdown(
            validUntilText = "valid until July 11, 2026",
            days = "00",
            hours = "00",
            minutes = "01",
            daysLabel = "Days",
            hoursLabel = "Hours",
            minutesLabel = "Minutes",
            modifier = Modifier.padding(16.dp),
        )
    }
}

/**
 * Tag for the OfferCountdown root container
 */
const val TEST_TAG_OFFER_COUNTDOWN = "offer_countdown"

/**
 * Tag for the OfferCountdown "valid until" caption
 */
const val TEST_TAG_OFFER_COUNTDOWN_VALID_UNTIL = "offer_countdown:valid_until"

/**
 * Tag for the OfferCountdown days unit
 */
const val TEST_TAG_OFFER_COUNTDOWN_DAYS = "offer_countdown:days"

/**
 * Tag for the OfferCountdown hours unit
 */
const val TEST_TAG_OFFER_COUNTDOWN_HOURS = "offer_countdown:hours"

/**
 * Tag for the OfferCountdown minutes unit
 */
const val TEST_TAG_OFFER_COUNTDOWN_MINUTES = "offer_countdown:minutes"
