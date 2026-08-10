package mega.privacy.android.app.myAccount.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.SpannedText
import mega.android.core.ui.components.card.RoundCard
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.myAccount.MyAccountUsageUiState
import mega.privacy.android.app.presentation.myaccount.view.MyAccountQuotaProgressBar
import mega.privacy.android.core.formatter.stripLinkAnnotations
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.transfer.UsedTransferStatus
import mega.privacy.android.feature.myaccount.presentation.model.QuotaLevel

@Composable
internal fun UsageMeterSection(
    uiState: MyAccountUsageUiState,
    showRealUsageContent: Boolean,
    modifier: Modifier = Modifier,
) {
    val isBusinessOrProFlexi = uiState.isBusinessAccount || uiState.isProFlexiAccount
    val cardHeight = if (isBusinessOrProFlexi) 80.dp else 88.dp

    RoundCard(
        modifier = modifier.height(cardHeight),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (!showRealUsageContent) {
                UsageMeterShimmerLayout(
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                when {
                    isBusinessOrProFlexi -> {
                        // Business/Pro Flexi Account Layout
                        BusinessUsageLayout(
                            usedStorage = uiState.usedStorage,
                            usedTransfer = uiState.usedTransfer,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        // Free/Pro Account Layout with Progress Bars
                        RegularUsageLayout(
                            uiState = uiState,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessUsageLayout(
    usedStorage: String,
    usedTransfer: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            MegaText(
                text = stringResource(id = R.string.account_storage_label),
                style = AppTheme.typography.labelLarge,
                textColor = TextColor.Info,
                textAlign = TextAlign.Center
            )
            MegaText(
                text = usedStorage,
                style = AppTheme.typography.bodyMedium,
                textColor = TextColor.Primary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            MegaText(
                text = stringResource(id = R.string.transfer_label),
                style = AppTheme.typography.labelLarge,
                textColor = TextColor.Success,
                textAlign = TextAlign.Center
            )
            MegaText(
                text = usedTransfer,
                style = AppTheme.typography.bodyMedium,
                textColor = TextColor.Primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RegularUsageLayout(
    uiState: MyAccountUsageUiState,
    modifier: Modifier = Modifier,
) {
    val isStorageOverQuota = uiState.storageState == StorageState.Red

    val storageQuotaLevel = when (uiState.storageState) {
        StorageState.Red -> QuotaLevel.Error
        StorageState.Orange -> QuotaLevel.Warning
        else -> QuotaLevel.Success
    }

    val transferQuotaLevel = when (uiState.usedTransferStatus) {
        UsedTransferStatus.Full -> QuotaLevel.Error
        UsedTransferStatus.AlmostFull -> QuotaLevel.Warning
        else -> QuotaLevel.Success
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Storage Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            MyAccountQuotaProgressBar(
                level = storageQuotaLevel,
                progress = uiState.usedStoragePercentage,
                progressIndicatorTestTag = STORAGE_PROGRESS_BAR_TAG,
                progressTextTestTag = STORAGE_PROGRESS_PERCENTAGE_TAG,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                val storageText = stringResource(
                    R.string.used_storage_transfer,
                    uiState.usedStorage,
                    uiState.totalStorage
                ).let { if (isStorageOverQuota) it else it.stripLinkAnnotations() }

                SpannedText(
                    value = storageText,
                    baseStyle = AppTheme.typography.bodyMedium,
                    spanStyles = if (isStorageOverQuota) {
                        mapOf(
                            SpanIndicator('A') to
                                    MegaSpanStyle.TextColorStyle(
                                        SpanStyle(),
                                        TextColor.Error
                                    ),
                        )
                    } else {
                        emptyMap()
                    },
                    baseTextColor = TextColor.Primary,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                MegaText(
                    text = stringResource(id = R.string.account_storage_label),
                    style = AppTheme.typography.labelLarge,
                    textColor = TextColor.Primary,
                )
            }
        }

        // Transfer Section (only for non-free accounts)
        if (!uiState.isFreeAccount) {
            Spacer(modifier = Modifier.width(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                MyAccountQuotaProgressBar(
                    level = transferQuotaLevel,
                    progress = uiState.usedTransferPercentage,
                    progressIndicatorTestTag = TRANSFER_PROGRESS_BAR_TAG,
                    progressTextTestTag = TRANSFER_PROGRESS_PERCENTAGE_TAG,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    val transferText = stringResource(
                        R.string.used_storage_transfer,
                        uiState.usedTransfer,
                        uiState.totalTransfer
                    ).stripLinkAnnotations()

                    MegaText(
                        text = transferText,
                        style = AppTheme.typography.bodyMedium,
                        textColor = TextColor.Primary,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    MegaText(
                        text = stringResource(id = R.string.transfer_label),
                        style = AppTheme.typography.labelLarge,
                        textColor = TextColor.Primary
                    )
                }
            }
        }
    }
}

internal const val STORAGE_PROGRESS_BAR_TAG = "my_account_usage_screen:progress_bar_storage"
internal const val STORAGE_PROGRESS_PERCENTAGE_TAG = "my_account_usage_screen:text_storage_progress"
internal const val TRANSFER_PROGRESS_BAR_TAG = "my_account_usage_screen:progress_bar_transfer"
internal const val TRANSFER_PROGRESS_PERCENTAGE_TAG = "my_account_usage_screen:text_transfer_progress"

