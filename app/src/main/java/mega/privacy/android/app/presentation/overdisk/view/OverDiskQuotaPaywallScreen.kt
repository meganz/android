package mega.privacy.android.app.presentation.overdisk.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.SpannedText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.TextOnlyButton
import mega.android.core.ui.components.indicators.LargeHUD
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.overdisk.model.OverDiskQuotaPaywallUiState
import mega.privacy.android.app.utils.TimeUtils.DATE_LONG_FORMAT
import mega.privacy.android.app.utils.TimeUtils.formatDate
import mega.privacy.android.app.utils.TimeUtils.getHumanizedTimeMs
import mega.privacy.android.app.utils.Util.getSizeString
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.shared.resources.R as sharedR
import java.util.concurrent.TimeUnit

private const val GIGABYTE = 1073741824L // 1024(KB) * 1024(MB) * 1024(GB)

/**
 * Over Disk Quota Paywall screen.
 *
 * @param uiState the current [OverDiskQuotaPaywallUiState].
 * @param onDismiss invoked when the user dismisses the paywall.
 * @param onUpgrade invoked when the user chooses to upgrade.
 * @param modifier [Modifier].
 */
@Composable
fun OverDiskQuotaPaywallScreen(
    uiState: OverDiskQuotaPaywallUiState,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.storage_full_xl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth(),
            )
            MegaText(
                text = stringResource(id = R.string.over_disk_quota_paywall_header),
                textColor = TextColor.Accent,
                style = AppTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 47.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 18.dp, bottom = 30.dp),
        ) {
            MegaText(
                text = stringResource(id = R.string.over_disk_quota_paywall_title),
                textColor = TextColor.Primary,
                style = AppTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 27.dp),
            )

            if (uiState.isLoading) {
                LargeHUD(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag(TEST_TAG_ODQ_LOADING)
                )
            } else {
                MegaText(
                    text = bodyText(uiState),
                    textColor = TextColor.Primary,
                    style = AppTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 22.dp)
                        .testTag(TEST_TAG_ODQ_BODY),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 23.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_suspention_warning),
                        contentDescription = null,
                        modifier = Modifier.size(70.dp),
                    )
                    SpannedText(
                        value = flattenNestedMarkup(deletionWarningText(uiState.deadlineTimestamp)),
                        baseStyle = AppTheme.typography.bodyMedium,
                        baseTextColor = TextColor.Primary,
                        spanStyles = mapOf(
                            SpanIndicator('B') to MegaSpanStyle.DefaultColorStyle(
                                SpanStyle(fontWeight = FontWeight.Bold),
                            ),
                            SpanIndicator('M') to MegaSpanStyle.TextColorStyle(
                                SpanStyle(fontWeight = FontWeight.Bold),
                                TextColor.Error,
                            ),
                        ),
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .fillMaxWidth()
                            .testTag(TEST_TAG_ODQ_DELETION_WARNING),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 45.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextOnlyButton(
                    text = stringResource(id = sharedR.string.general_dismiss_dialog),
                    onClick = onDismiss,
                    modifier = Modifier
                        .wrapContentSize()
                        .testTag(TEST_TAG_ODQ_DISMISS_BUTTON),
                )
                Spacer(modifier = Modifier.width(26.dp))
                PrimaryFilledButton(
                    text = stringResource(id = sharedR.string.general_upgrade_button),
                    onClick = onUpgrade,
                    modifier = Modifier
                        .wrapContentSize()
                        .testTag(TEST_TAG_ODQ_UPGRADE_BUTTON),
                )
            }
        }
    }
}

/**
 * Builds the main paywall body text depending on the number of over quota warning timestamps.
 */
@Composable
private fun bodyText(uiState: OverDiskQuotaPaywallUiState): String {
    val context = LocalContext.current
    val size = getSizeString(uiState.usedStorage, context)
    val proPlan = proPlanNeeded(uiState.products, uiState.usedStorage)
    val files = uiState.fileCount
    val warnings = uiState.warningTimestamps

    fun date(timestamp: Long) = formatDate(timestamp, DATE_LONG_FORMAT, false, context)

    return when {
        warnings.isEmpty() -> stringResource(
            id = R.string.over_disk_quota_paywall_text_no_warning_dates_info,
            uiState.email, files.toString(), size, proPlan,
        )

        warnings.size == 1 -> pluralStringResource(
            id = R.plurals.over_disk_quota_paywall_text,
            count = 1,
            uiState.email, date(warnings[0]), files.toString(), size, proPlan,
        )

        else -> {
            val lastIndex = warnings.size - 1
            val dates = warnings.subList(0, lastIndex).joinToString(", ") { date(it) }
            pluralStringResource(
                id = R.plurals.over_disk_quota_paywall_text,
                count = warnings.size,
                uiState.email, dates, date(warnings[lastIndex]), files.toString(), size, proPlan,
            )
        }
    }
}

/**
 * Resolves the required PRO plan label from the available products and the used storage.
 */
@Composable
private fun proPlanNeeded(products: List<Product>, usedStorage: Long): String {
    val level = products.firstOrNull { it.storage > usedStorage / GIGABYTE }?.level
    return when (level) {
        1 -> stringResource(id = R.string.pro1_account)
        2 -> stringResource(id = R.string.pro2_account)
        3 -> stringResource(id = R.string.pro3_account)
        4 -> stringResource(id = R.string.prolite_account)
        else -> stringResource(id = R.string.pro_account)
    }
}

/**
 * Builds the deletion warning text, ticking every second to reflect the remaining time.
 * The returned string keeps the bold/color markup tags, which are styled by [SpannedText].
 */
@Composable
private fun deletionWarningText(deadlineTimestamp: Long): String {
    var nowMs by remember(deadlineTimestamp) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(deadlineTimestamp) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000)
        }
    }

    val remainingMs = TimeUnit.SECONDS.toMillis(deadlineTimestamp) - nowMs
    return when {
        deadlineTimestamp < 0 ->
            stringResource(id = R.string.over_disk_quota_paywall_deletion_warning_no_data)

        TimeUnit.MILLISECONDS.toSeconds(remainingMs) <= 0 ->
            stringResource(id = R.string.over_disk_quota_paywall_deletion_warning_no_time_left)

        else -> stringResource(
            id = R.string.over_disk_quota_paywall_deletion_warning,
            getHumanizedTimeMs(remainingMs),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun OverDiskQuotaPaywallScreenNoWarningsPreview() {
    AndroidThemeForPreviews {
        OverDiskQuotaPaywallScreen(
            uiState = OverDiskQuotaPaywallUiState(
                isLoading = false,
                email = "user@mega.co.nz",
                fileCount = 1280,
                usedStorage = 53_687_091_200L,
                warningTimestamps = emptyList(),
                deadlineTimestamp = -1,
            ),
            onDismiss = {},
            onUpgrade = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun OverDiskQuotaPaywallScreenWithDeadlinePreview() {
    AndroidThemeForPreviews {
        OverDiskQuotaPaywallScreen(
            uiState = OverDiskQuotaPaywallUiState(
                isLoading = false,
                email = "user@mega.co.nz",
                fileCount = 1280,
                usedStorage = 53_687_091_200L,
                warningTimestamps = listOf(1_700_000_000L, 1_700_086_400L),
                deadlineTimestamp = 4_102_444_800L,
            ),
            onDismiss = {},
            onUpgrade = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun OverDiskQuotaPaywallScreenLoadingPreview() {
    AndroidThemeForPreviews {
        OverDiskQuotaPaywallScreen(
            uiState = OverDiskQuotaPaywallUiState(isLoading = true),
            onDismiss = {},
            onUpgrade = {},
        )
    }
}


/**
 * Test tag for the dismiss button.
 */
const val TEST_TAG_ODQ_DISMISS_BUTTON = "over_disk_quota_paywall:dismiss_button"

/**
 * Test tag for the upgrade button.
 */
const val TEST_TAG_ODQ_UPGRADE_BUTTON = "over_disk_quota_paywall:upgrade_button"

/**
 * Test tag for the main body text.
 */
const val TEST_TAG_ODQ_BODY = "over_disk_quota_paywall:body"

/**
 * Test tag for the deletion warning text.
 */
const val TEST_TAG_ODQ_DELETION_WARNING = "over_disk_quota_paywall:deletion_warning"

/**
 * Test tag for the loading indicator.
 */
const val TEST_TAG_ODQ_LOADING = "over_disk_quota_paywall:loading"

/**
 * The deletion-warning source string nests [M] inside [B]. [SpannedText] does not support
 * nested tags, so close and reopen the bold span around the colored one to keep the tags
 * non-overlapping (bold + color is then applied to the colored span directly).
 */
internal fun flattenNestedMarkup(value: String): String =
    value.replace("[M]", "[/B][M]").replace("[/M]", "[/M][B]")
