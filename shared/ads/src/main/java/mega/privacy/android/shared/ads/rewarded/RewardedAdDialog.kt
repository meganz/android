package mega.privacy.android.shared.ads.rewarded

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.MegaOutlinedButton
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.card.RoundCard
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.ColumnSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR


@Composable
fun RewardedAdDialog(
    isAdLoading: Boolean,
    onDismiss: () -> Unit,
    onWatchAd: () -> Unit,
    onUpgradePro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false)
    ) {
        ColumnSurface(
            surfaceColor = SurfaceColor.PageBackground,
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
        ) {

            BoxSurface(
                surfaceColor = SurfaceColor.PageBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 12.dp)
                        .size(32.dp)
                ) {
                    MegaIcon(
                        painter = rememberVectorPainter(Icons.Default.Close),
                        tint = IconColor.Secondary,
                        contentDescription = stringResource(sharedR.string.general_dismiss_dialog),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 24.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BoxSurface(
                    surfaceColor = SurfaceColor.Surface2,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    MegaIcon(
                        painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ArrowRight),
                        contentDescription = null,
                        tint = IconColor.Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                MegaText(
                    modifier = Modifier,
                    text = stringResource(sharedR.string.rewarded_ad_dialog_title),
                    textColor = TextColor.Primary,
                    style = AppTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryFilledButton(
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Play),
                    text = stringResource(sharedR.string.rewarded_ad_dialog_watch_ad_button),
                    onClick = onWatchAd,
                    isLoading = isAdLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                MegaText(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(sharedR.string.rewarded_ad_dialog_upgrade_subtitle),
                    style = AppTheme.typography.labelMedium,
                    textColor = TextColor.Primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                ProCard(onUpgradePro = onUpgradePro)
            }

        }
    }
}


@Composable
private fun ProCard(onUpgradePro: () -> Unit) {
    RoundCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MegaText(
                    text = stringResource(sharedR.string.rewarded_ad_dialog_pro_card_title),
                    style = AppTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val perkResIds = remember {
                listOf(
                    sharedR.string.rewarded_ad_dialog_perk_ad_free_downloads,
                    sharedR.string.rewarded_ad_dialog_perk_storage,
                    sharedR.string.rewarded_ad_dialog_perk_pass_vpn,
                )
            }
            perkResIds.forEachIndexed { index, resId ->
                PerkItem(text = stringResource(resId))
                if (index < perkResIds.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            MegaOutlinedButton(
                onClick = onUpgradePro,
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(sharedR.string.rewarded_ad_dialog_upgrade_button),
            )
        }
    }
}

@Composable
private fun PerkItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MegaIcon(
            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.CheckCircle),
            contentDescription = null,
            tint = IconColor.Primary,
            modifier = Modifier.size(14.dp)
        )

        MegaText(
            text = text,
            style = AppTheme.typography.bodySmall
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun RewardedAdDialogPreview() {
    AndroidThemeForPreviews {
        RewardedAdDialog(
            isAdLoading = false,
            onDismiss = {},
            onWatchAd = {},
            onUpgradePro = {}
        )
    }
}