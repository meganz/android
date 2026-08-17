package mega.privacy.android.app.myAccount.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.extensions.delayedTrue
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.transfer.UsedTransferStatus
import mega.privacy.android.feature.myaccount.presentation.usage.MyAccountUsageUiState
import mega.privacy.android.feature.myaccount.presentation.usage.PaymentAlertType
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.resources.R as sharedR
import kotlin.time.Duration.Companion.milliseconds

/**
 * Compose screen for my account usage
 *
 * @param uiState The UI state for the account usage screen
 * @param modifier Modifier for the screen
 * @param onUpgradeClick Callback when upgrade button is clicked
 * @param onUsageLoadErrorDismiss After user acknowledges load failure; typically pop back stack (new entry gets a new ViewModel).
 */
@Composable
fun MyAccountUsageScreen(
    uiState: MyAccountUsageUiState,
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {},
    onUsageLoadErrorDismiss: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val shouldShowSkeleton by delayedTrue(MY_ACCOUNT_USAGE_SKELETON_DELAY)
    var errorDialogDismissed by remember(uiState.usageLoadFailed) { mutableStateOf(false) }

    // Within the short grace window before [MY_ACCOUNT_USAGE_SKELETON_DELAY] elapses, render nothing —
    // avoids a skeleton flash for quick loads.
    if (uiState.isUsageContentReady || shouldShowSkeleton) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            // Usage Meter Section
            UsageMeterSection(
                uiState = uiState,
                showRealUsageContent = uiState.isUsageContentReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )

            if (!uiState.isUsageContentReady) {
                StorageBreakdownLoadingSkeleton(modifier = Modifier.fillMaxWidth())
            } else {
                // Payment Alert Section
                if (uiState.showPaymentAlert) {
                    PaymentAlertSection(
                        uiState = uiState,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Storage Details Section
                MegaText(
                    text = stringResource(id = R.string.usage_storage_details_label),
                    style = AppTheme.typography.titleMedium,
                    textColor = TextColor.Primary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(5.dp))
                // Storage Details Items
                StorageDetailItem(
                    title = stringResource(id = R.string.section_cloud_drive),
                    value = uiState.cloudStorage?.let { formatFileSize(it, context) }.orEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.backupStorageSize > 0) {
                    StorageDetailItem(
                        title = stringResource(id = R.string.home_side_menu_backups_title),
                        value = formatFileSize(uiState.backupStorageSize, context),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                StorageDetailItem(
                    title = stringResource(id = R.string.title_incoming_shares_explorer),
                    value = uiState.incomingStorage?.let { formatFileSize(it, context) }.orEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )

                StorageDetailItem(
                    title = stringResource(id = sharedR.string.general_section_rubbish_bin),
                    value = uiState.rubbishStorage?.let { formatFileSize(it, context) }.orEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.isFileVersioningEnabled) {
                    StorageDetailItem(
                        title = stringResource(id = R.string.file_properties_folder_previous_versions),
                        value = uiState.versionsSize?.let { formatFileSize(it, context) }.orEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Upgrade Button
                if (uiState.showUpgradeButton) {
                    Spacer(modifier = Modifier.height(20.dp))
                    PrimaryFilledButton(
                        text = stringResource(id = R.string.account_my_account_usage_get_more_storage_button),
                        onClick = onUpgradeClick,
                        modifier = Modifier
                            .widthIn(max = 310.dp)
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (uiState.usageLoadFailed && !errorDialogDismissed) {
        BasicDialog(
            title = stringResource(R.string.general_error_word),
            description = stringResource(sharedR.string.general_request_failed_message),
            positiveButtonText = stringResource(sharedR.string.general_ok),
            onPositiveButtonClicked = {
                errorDialogDismissed = true
                onUsageLoadErrorDismiss()
            },
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
            onDismiss = {},
        )
    }
}

/** Grace window before the loading skeleton is shown — keeps quick loads from flashing a skeleton. */
internal val MY_ACCOUNT_USAGE_SKELETON_DELAY = 300.milliseconds

private class MyAccountUsageStateProvider : PreviewParameterProvider<MyAccountUsageUiState> {

    private val gb = 1024L * 1024 * 1024
    private val mb = 1024L * 1024

    private val baseProState = MyAccountUsageUiState(
        isUsageContentReady = true,
        accountType = AccountType.PRO_I,
        usedStorage = 18 * gb,
        totalStorage = 400 * gb,
        usedStoragePercentage = 75,
        usedTransfer = 120 * gb,
        totalTransfer = 400 * gb,
        usedTransferPercentage = 30,
        usedTransferStatus = UsedTransferStatus.NoTransferProblems,
        cloudStorage = 10 * gb,
        incomingStorage = 5 * gb,
        rubbishStorage = 2 * gb,
        backupStorageSize = 1 * gb,
        isFileVersioningEnabled = true,
        versionsSize = 200 * mb,
        hasRenewableSubscription = true,
        paymentAlertType = PaymentAlertType.AccountRenewsOn,
        paymentAlertDate = 1_780_000_000L,
    )

    override val values = sequenceOf(
        // Free
        MyAccountUsageUiState(
            isUsageContentReady = true,
            accountType = AccountType.FREE,
            usedStorage = 5 * gb,
            totalStorage = 20 * gb,
            usedStoragePercentage = 26,
            cloudStorage = 3 * gb,
            incomingStorage = 1 * gb,
            rubbishStorage = 600 * mb,
            backupStorageSize = 0L,
            isFileVersioningEnabled = false,
        ),
        // Pro
        baseProState,
        // Pro - storage warning
        baseProState.copy(
            storageState = StorageState.Orange,
            usedStorage = 36 * gb,
            totalStorage = 40 * gb,
            usedStoragePercentage = 90,
            cloudStorage = 300 * gb,
            incomingStorage = 40 * gb,
            rubbishStorage = 20 * gb,
            backupStorageSize = 0L,
        ),
        // Pro - over quota
        baseProState.copy(
            storageState = StorageState.Red,
            usedStorage = 410 * gb,
            usedStoragePercentage = 100,
            usedTransfer = 400 * gb,
            usedTransferPercentage = 100,
            usedTransferStatus = UsedTransferStatus.Full,
            cloudStorage = 350 * gb,
            incomingStorage = 40 * gb,
            rubbishStorage = 20 * gb,
            backupStorageSize = 0L,
        ),
        // Business
        MyAccountUsageUiState(
            isUsageContentReady = true,
            isBusinessAccount = true,
            isMasterBusinessAccount = true,
            usedStorage = 340 * gb,
            usedTransfer = 80 * gb,
            paymentAlertType = PaymentAlertType.BusinessGracePeriod,
        ),
        // Loading
        MyAccountUsageUiState(isUsageContentReady = false),
    )
}

@CombinedThemePreviews
@Composable
private fun MyAccountUsageScreenPreview(
    @PreviewParameter(MyAccountUsageStateProvider::class) uiState: MyAccountUsageUiState,
) {
    AndroidThemeForPreviews {
        MyAccountUsageScreen(uiState = uiState)
    }
}

