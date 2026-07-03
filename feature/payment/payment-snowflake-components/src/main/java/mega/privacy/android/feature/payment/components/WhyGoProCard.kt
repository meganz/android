package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack

/**
 * Card highlighting why the user should go Pro, shown on the redesigned subscription page.
 *
 * @param title section title (e.g. "Why go Pro?")
 * @param storageText storage benefit (e.g. "Store up to 20 TB of data")
 * @param transferText transfer benefit (e.g. "Enjoy up to 240 TB transfer quota")
 * @param vpnText VPN benefit
 * @param passText password manager benefit
 */
@Composable
fun WhyGoProCard(
    title: String,
    storageText: String,
    transferText: String,
    vpnText: String,
    passText: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(TEST_TAG_WHY_GO_PRO_CARD),
        horizontalAlignment = Alignment.Start,
    ) {
        MegaText(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textColor = TextColor.Primary,
            modifier = Modifier
                .padding(bottom = 4.dp)
                .testTag(TEST_TAG_WHY_GO_PRO_TITLE),
        )
        PlanFeatureRow(
            icon = IconPack.Medium.Thin.Outline.Cloud,
            text = storageText,
        )
        PlanFeatureRow(
            icon = IconPack.Medium.Thin.Outline.ArrowsUpDown,
            text = transferText,
        )
        PlanFeatureRow(
            icon = IconPack.Medium.Thin.Outline.VPN,
            text = vpnText,
        )
        PlanFeatureRow(
            icon = IconPack.Medium.Thin.Outline.LockKeyholeCircle,
            text = passText,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun WhyGoProCardPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        WhyGoProCard(
            title = "Why go Pro?",
            storageText = "Store up to 20 TB of data",
            transferText = "Enjoy up to 240 TB transfer quota",
            vpnText = "Stay safe online with MEGA VPN",
            passText = "Keep passwords safe with MEGA Pass",
        )
    }
}

/**
 * Tag for the WhyGoProCard root container
 */
const val TEST_TAG_WHY_GO_PRO_CARD = "why_go_pro_card"

/**
 * Tag for the WhyGoProCard title
 */
const val TEST_TAG_WHY_GO_PRO_TITLE = "why_go_pro_card:title"
