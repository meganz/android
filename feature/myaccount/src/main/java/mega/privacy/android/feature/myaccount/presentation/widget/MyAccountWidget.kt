package mega.privacy.android.feature.myaccount.presentation.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.profile.MediumProfilePicture
import mega.android.core.ui.components.surface.CardSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.myaccount.R
import mega.privacy.android.feature.myaccount.presentation.model.MyAccountWidgetUiState
import mega.privacy.android.feature.myaccount.presentation.model.QuotaLevel
import mega.privacy.android.feature.myaccount.presentation.util.getSizeString
import mega.privacy.android.feature.myaccount.presentation.widget.view.MyAccountHorizontalProgressBar
import mega.privacy.android.icon.pack.IconPack

/**
 * Shimmer view that mimics the MyAccount widget layout
 */
@Composable
private fun MyAccountWidgetShimmerView() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Avatar shimmer
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .shimmerEffect(visible = true)
        )

        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // User name shimmer
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(visible = true)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Account type shimmer
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(visible = true)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Storage usage shimmer
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(visible = true)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .shimmerEffect(visible = true)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Chevron shimmer (smaller)
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .shimmerEffect(visible = true)
        )
    }
}

/**
 * MyAccount widget composable
 *
 * @param state Widget UI state
 * @param modifier Modifier
 * @param onClick Click handler
 */
@Composable
internal fun MyAccountWidget(
    state: MyAccountWidgetUiState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    CardSurface(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        surfaceColor = SurfaceColor.Surface1
    ) {
        if (state.isLoading) {
            MyAccountWidgetShimmerView()
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Avatar
                MediumProfilePicture(
                    imageFile = state.avatarFile,
                    contentDescription = state.name,
                    name = state.name ?: ""
                )

                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // User name
                    MegaText(
                        text = "${stringResource(R.string.general_hi)} ${state.name ?: ""}!",
                        style = AppTheme.typography.titleMedium,
                        modifier = Modifier.height(24.dp),
                        maxLines = 1
                    )

                    // Account type
                    if (state.accountTypeNameResource != 0) {
                        MegaText(
                            text = stringResource(state.accountTypeNameResource),
                            style = AppTheme.typography.bodyMedium,
                            modifier = Modifier.height(20.dp)
                        )
                    }

                    // Storage usage (formatted)
                    MegaText(
                        text = stringResource(
                            R.string.storage_usage_format,
                            getSizeString(state.usedStorage),
                            getSizeString(state.totalStorage)
                        ),
                        textColor = TextColor.Secondary,
                        style = AppTheme.typography.bodySmall,
                        modifier = Modifier.height(20.dp)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Horizontal progress bar
                    MyAccountHorizontalProgressBar(
                        level = state.storageQuotaLevel,
                        progress = state.usedStoragePercentage.toFloat()
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Chevron arrow
                MegaIcon(
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ChevronRight),
                    tint = IconColor.Secondary,
                    contentDescription = null
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun MyAccountWidgetLoadingPreview() {
    AndroidThemeForPreviews {
        MyAccountWidget(
            state = MyAccountWidgetUiState(isLoading = true),
            onClick = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MyAccountWidgetLoadedPreview() {
    AndroidThemeForPreviews {
        MyAccountWidget(
            state = MyAccountWidgetUiState(
                name = "John Doe",
                accountTypeNameResource = android.R.string.ok, // Placeholder
                usedStorage = 124470000000L,  // ~116 GB
                totalStorage = 750000000000L,  // ~698 GB
                usedStoragePercentage = 50,
                storageQuotaLevel = QuotaLevel.Success,
                isLoading = false
            ),
            onClick = {}
        )
    }
}
