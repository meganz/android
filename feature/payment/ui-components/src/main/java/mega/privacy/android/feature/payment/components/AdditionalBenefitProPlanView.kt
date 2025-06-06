package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Composable to display a list of additional benefits.
 */
@Composable
fun AdditionalBenefitProPlanView(
    title: String,
    modifier: Modifier = Modifier,
    benefits: List<String>,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LocalSpacing.current.x16, vertical = LocalSpacing.current.x8),
        verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.x8),
    ) {
        MegaText(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textColor = TextColor.Primary,
            modifier = Modifier.padding(bottom = LocalSpacing.current.x16)
        )

        benefits.forEach { benefit ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.x8),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp, start = 8.dp)
                        .background(
                            color = DSTokens.colors.text.primary,
                            shape = CircleShape
                        )
                        .size(2.dp)
                )
                MegaText(
                    text = benefit,
                    style = MaterialTheme.typography.bodyMedium,
                    textColor = TextColor.Primary,
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun AdditionalBenefitViewPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        AdditionalBenefitProPlanView(
            title = stringResource(id = sharedR.string.pro_plan_additional_benefits_section_title),
            benefits = listOf(
                stringResource(id = sharedR.string.pro_plan_benefit_password_protected_links),
                stringResource(id = sharedR.string.pro_plan_benefit_links_with_expiry_dates),
                stringResource(id = sharedR.string.pro_plan_benefit_auto_sync_mobile),
                stringResource(id = sharedR.string.pro_plan_benefit_rewind_180_days),
                stringResource(id = sharedR.string.pro_plan_benefit_host_calls_unlimited),
                stringResource(id = sharedR.string.pro_plan_benefit_schedule_rubbish_bin_clearing),
                stringResource(id = sharedR.string.pro_plan_benefit_priority_support),
            )
        )
    }
} 