package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButtonXSmall
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Promotional offer banner shown at the top of a screen while a discount campaign is running.
 * Renders the campaign artwork as a full-bleed background with a headline, a compact
 * days/hours/minutes countdown and an action button, plus a dismiss (X) affordance.
 *
 * The countdown is driven by [validUntil] and recomputed periodically; it is hidden once the
 * offer has elapsed. The caller remains responsible for hiding the banner entirely when the
 * campaign is no longer active.
 *
 * The design uses the same light campaign artwork in both themes, so the whole banner is pinned
 * to the light palette to keep text and button contrast against the artwork.
 *
 * @param title the headline text (e.g. "Black Friday · Get 50% off")
 * @param subtitle the supporting text (e.g. "€4.99/month for Pro I")
 * @param validUntil the offer expiry as epoch seconds
 * @param actionButtonText the action button label (e.g. "Grab deal")
 * @param onActionClick called when the action button is tapped
 * @param onDismissClick called when the dismiss (X) icon is tapped
 * @param modifier
 */
@Composable
fun OfferBanner(
    title: String,
    subtitle: String,
    validUntil: Long,
    actionButtonText: String,
    onActionClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidTheme(isDark = false, useLegacyStatusBarColor = false) {
        OfferBannerContent(
            title = title,
            subtitle = subtitle,
            validUntil = validUntil,
            actionButtonText = actionButtonText,
            onActionClick = onActionClick,
            onDismissClick = onDismissClick,
            modifier = modifier,
        )
    }
}

/**
 * Emits the time remaining until [validUntil] (epoch seconds), refreshed every [COUNTDOWN_TICK].
 * [OfferBanner] collects this internally, but it is exposed so a screen or ViewModel can own the
 * countdown state instead and drive a stateless [OfferBannerCountdown].
 *
 * The current remaining time is emitted immediately, then re-emitted on each tick while time is
 * left; the flow completes once the offer has elapsed, with a final emission of [Duration.ZERO].
 *
 * @param validUntil the offer expiry as epoch seconds
 * @param currentTimeMillis source of the current wall-clock time in millis; overridable for tests
 */
fun offerCountdownFlow(
    validUntil: Long,
    currentTimeMillis: () -> Long = System::currentTimeMillis,
): Flow<Duration> = flow {
    while (true) {
        val remainingMillis = validUntil * 1000L - currentTimeMillis()
        emit(remainingMillis.coerceAtLeast(0L).milliseconds)
        if (remainingMillis <= 0L) break
        delay(COUNTDOWN_TICK)
    }
}

private val COUNTDOWN_TICK = 30.seconds

/**
 * Banner layout, themed by the forced-light [AndroidTheme] wrapper in [OfferBanner].
 *
 * @param title the headline text (e.g. "Black Friday · Get 50% off")
 * @param subtitle the supporting text (e.g. "€4.99/month for Pro I")
 * @param validUntil the offer expiry as epoch seconds
 * @param actionButtonText the action button label (e.g. "Grab deal")
 * @param onActionClick called when the action button is tapped
 * @param onDismissClick called when the dismiss (X) icon is tapped
 * @param modifier
 */
@Composable
private fun OfferBannerContent(
    title: String,
    subtitle: String,
    validUntil: Long,
    actionButtonText: String,
    onActionClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val remaining by remember(validUntil) { offerCountdownFlow(validUntil) }
        .collectAsState(
            initial = (validUntil * 1000L - System.currentTimeMillis())
                .coerceAtLeast(0L).milliseconds,
        )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(10.dp))
            .testTag(TEST_TAG_OFFER_BANNER),
    ) {
        Image(
            painter = painterResource(iconPackR.drawable.offer_banner_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(0f, BACKGROUND_VERTICAL_BIAS),
            modifier = Modifier.matchParentSize(),
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp, end = 48.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MegaText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textColor = TextColor.Primary,
                modifier = Modifier.testTag(TEST_TAG_OFFER_BANNER_TITLE),
            )
            MegaText(
                text = subtitle,
                style = MaterialTheme.typography.titleSmall,
                textColor = TextColor.Primary,
                modifier = Modifier.testTag(TEST_TAG_OFFER_BANNER_SUBTITLE),
            )
        }
        MegaIcon(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 12.dp)
                .size(16.dp)
                .clickable { onDismissClick() }
                .testTag(TEST_TAG_OFFER_BANNER_DISMISS),
            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.X),
            tint = IconColor.Primary,
            contentDescription = stringResource(sharedR.string.general_dismiss_dialog),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OfferBannerCountdown(remaining = remaining)
            }
            PrimaryFilledButtonXSmall(
                text = actionButtonText,
                onClick = onActionClick,
                modifier = Modifier.testTag(TEST_TAG_OFFER_BANNER_BUTTON),
            )
        }
    }
}

/**
 * Renders the compact countdown for the given [remaining] time. Renders nothing once the offer
 * has elapsed ([remaining] is zero or negative).
 *
 * @param remaining the time left before the offer expires
 * @param modifier
 */
@Composable
private fun OfferBannerCountdown(
    remaining: Duration,
    modifier: Modifier = Modifier,
) {
    if (remaining <= Duration.ZERO) return
    val totalMinutes = remaining.inWholeMinutes
    val days = totalMinutes / (60L * 24L)
    val hours = totalMinutes / 60L % 24L
    val minutes = totalMinutes % 60L
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CountdownUnit(
            value = days.toString().padStart(2, '0'),
            label = pluralStringResource(
                sharedR.plurals.subscription_offer_countdown_days,
                days.toInt(),
            ),
            modifier = Modifier.testTag(TEST_TAG_OFFER_BANNER_DAYS),
        )
        CountdownUnit(
            value = hours.toString().padStart(2, '0'),
            label = pluralStringResource(
                sharedR.plurals.subscription_offer_countdown_hours,
                hours.toInt(),
            ),
            modifier = Modifier.testTag(TEST_TAG_OFFER_BANNER_HOURS),
        )
        CountdownUnit(
            value = minutes.toString().padStart(2, '0'),
            label = pluralStringResource(
                sharedR.plurals.subscription_offer_countdown_minutes,
                minutes.toInt(),
            ),
            modifier = Modifier.testTag(TEST_TAG_OFFER_BANNER_MINUTES),
        )
    }
}

/**
 * Vertical alignment bias replicating the Figma crop of the campaign artwork, which anchors the
 * visible band slightly below the image centre so the stopwatch and basket stay in view.
 */
private const val BACKGROUND_VERTICAL_BIAS = 0.25f

/**
 * Single countdown unit: the value alongside its unit label.
 *
 * @param value the unit value, pre-formatted (e.g. "28")
 * @param label the localized unit label (e.g. "Days")
 * @param modifier
 */
@Composable
private fun CountdownUnit(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaText(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            textColor = TextColor.Primary,
        )
        MegaText(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
            textColor = TextColor.Secondary,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun OfferBannerPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        OfferBanner(
            title = "Black Friday · Get 50% off",
            subtitle = "€4.99/month for Pro I",
            validUntil = System.currentTimeMillis() / 1000L +
                    28L * 24L * 3600L + 12L * 3600L + 90L,
            actionButtonText = "Grab deal",
            onActionClick = {},
            onDismissClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

/**
 * Tag for the OfferBanner root container
 */
const val TEST_TAG_OFFER_BANNER = "offer_banner"

/**
 * Tag for the OfferBanner title
 */
const val TEST_TAG_OFFER_BANNER_TITLE = "offer_banner:title"

/**
 * Tag for the OfferBanner subtitle
 */
const val TEST_TAG_OFFER_BANNER_SUBTITLE = "offer_banner:subtitle"

/**
 * Tag for the OfferBanner dismiss icon
 */
const val TEST_TAG_OFFER_BANNER_DISMISS = "offer_banner:dismiss"

/**
 * Tag for the OfferBanner days unit
 */
const val TEST_TAG_OFFER_BANNER_DAYS = "offer_banner:days"

/**
 * Tag for the OfferBanner hours unit
 */
const val TEST_TAG_OFFER_BANNER_HOURS = "offer_banner:hours"

/**
 * Tag for the OfferBanner minutes unit
 */
const val TEST_TAG_OFFER_BANNER_MINUTES = "offer_banner:minutes"

/**
 * Tag for the OfferBanner action button
 */
const val TEST_TAG_OFFER_BANNER_BUTTON = "offer_banner:button"
