package mega.privacy.android.app.presentation.cancelaccountplan.view

import mega.privacy.android.shared.resources.R as SharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.cancelaccountplan.model.UIAccountDetails
import mega.privacy.android.icon.pack.R.drawable
import mega.privacy.android.shared.original.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeTabletLandscapePreviews
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeTabletPortraitPreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.h6Medium
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle1medium
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

@Composable
internal fun CancelAccountPlanView(
    accountDetailsUI: UIAccountDetails,
    onKeepPlanButtonClicked: () -> Unit,
    onContinueCancellationButtonClicked: () -> Unit,
) {
    val context = LocalContext.current

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(state = scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .systemBarsPadding()
                .width(390.dp)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MegaText(
                text = stringResource(id = SharedR.string.account_cancel_account_screen_plan_miss_out_on_features),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.h6Medium,
                modifier = Modifier
                    .padding(top = 30.dp)
                    .testTag(CANCEL_ACCOUNT_PLAN_TITLE_TEST_TAG),
            )
            MegaText(
                text = stringResource(id = SharedR.string.account_cancel_account_screen_plan_access_until_expiration),
                textColor = TextColor.Secondary,
                style = MaterialTheme.typography.subtitle1medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .testTag(CANCEL_ACCOUNT_PLAN_SUBTITLE_TEST_TAG),
            )
            MegaText(
                text = stringResource(
                    id = SharedR.string.account_cancel_account_screen_plan_current_storage_warning,
                    accountDetailsUI.usedStorageSize
                ),
                textColor = TextColor.Secondary,
                style = MaterialTheme.typography.subtitle1medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .testTag(CANCEL_ACCOUNT_PLAN_STORAGE_HINT_TEST_TAG),
            )


            val cells = remember {
                listOf(
                    TableCell.TextCell(
                        text = context.getString(SharedR.string.account_cancel_account_screen_plan_feature),
                        style = TableCell.TextCellStyle.Header,
                        cellAlignment = TableCell.CellAlignment.Start,
                    ),
                    TableCell.TextCell(
                        text = context.getString(SharedR.string.account_cancel_account_screen_plan_free_plan),
                        style = TableCell.TextCellStyle.Header,
                        cellAlignment = TableCell.CellAlignment.Center,
                    ),
                    TableCell.TextCell(
                        text = accountDetailsUI.accountType, style = TableCell.TextCellStyle.Header,
                        cellAlignment = TableCell.CellAlignment.Center,
                    ),

                    TableCell.TextCell(
                        text = context.getString(SharedR.string.account_cancel_account_screen_plan_storage),
                        style = TableCell.TextCellStyle.SubHeader,
                        cellAlignment = TableCell.CellAlignment.Start,
                    ),
                    TableCell.TextCell(
                        text = context.getString(
                            SharedR.string.account_cancel_account_screen_plan_free_storage,
                            accountDetailsUI.freeStorageQuota
                        ),
                        style = TableCell.TextCellStyle.Normal,
                        cellAlignment = TableCell.CellAlignment.Center,
                    ),
                    TableCell.TextCell(
                        text = accountDetailsUI.storageQuotaSize,
                        style = TableCell.TextCellStyle.Normal,
                        cellAlignment = TableCell.CellAlignment.Center,
                    ),

                    TableCell.TextCell(
                        text = context.getString(SharedR.string.account_cancel_account_screen_plan_transfer),
                        style = TableCell.TextCellStyle.SubHeader,
                        cellAlignment = TableCell.CellAlignment.Start,
                    ),
                    TableCell.TextCell(
                        text = context.getString(SharedR.string.account_cancel_account_screen_plan_free_limited_transfer),
                        style = TableCell.TextCellStyle.Normal,
                        cellAlignment = TableCell.CellAlignment.Center,
                    ),
                    TableCell.TextCell(
                        text = accountDetailsUI.transferQuotaSize,
                        style = TableCell.TextCellStyle.Normal,
                        cellAlignment = TableCell.CellAlignment.Center,
                    ),
                    TableCell.TextCell(
                        text = context.getString(SharedR.string.account_cancel_account_screen_plan_password_protected_links),
                        style = TableCell.TextCellStyle.SubHeader,
                        cellAlignment = TableCell.CellAlignment.Start,
                    ),
                    TableCell.IconCell(iconResId = drawable.ic_not_available),
                    TableCell.IconCell(iconResId = drawable.ic_available),

                    TableCell.TextCell(
                        text = context.getString(SharedR.string.account_cancel_account_screen_plan_links_with_expiry_dates),
                        style = TableCell.TextCellStyle.SubHeader,
                        cellAlignment = TableCell.CellAlignment.Start,
                    ),
                    TableCell.IconCell(iconResId = drawable.ic_not_available),
                    TableCell.IconCell(iconResId = drawable.ic_available),

                    TableCell.TextCell(
                        text = context.getString(SharedR.string.account_cancel_account_screen_plan_transfer_sharing),
                        style = TableCell.TextCellStyle.SubHeader,
                        cellAlignment = TableCell.CellAlignment.Start,
                    ),
                    TableCell.IconCell(iconResId = drawable.ic_not_available),
                    TableCell.IconCell(iconResId = drawable.ic_available),

                    TableCell.TextCell(
                        text = context.getString(SharedR.string.account_cancel_account_screen_plan_rewind),
                        style = TableCell.TextCellStyle.SubHeader,
                        cellAlignment = TableCell.CellAlignment.Start,
                    ),
                    TableCell.TextCell(
                        text = context.getString(SharedR.string.account_cancel_account_screen_plan_rewind_free),
                        style = TableCell.TextCellStyle.Normal,
                        cellAlignment = TableCell.CellAlignment.Center,
                    ),
                    TableCell.TextCell(
                        text = context.getString(
                            SharedR.string.account_cancel_account_screen_plan_rewind_current,
                            accountDetailsUI.rewindDaysQuota
                        ),
                        style = TableCell.TextCellStyle.Normal,
                        cellAlignment = TableCell.CellAlignment.Center,
                    ),

                    TableCell.TextCell(
                        text = context.getString(SharedR.string.account_cancel_account_screen_plan_mega_vpn),
                        style = TableCell.TextCellStyle.SubHeader,
                        cellAlignment = TableCell.CellAlignment.Start,
                    ),
                    TableCell.IconCell(iconResId = drawable.ic_not_available),
                    TableCell.IconCell(iconResId = drawable.ic_available),

                    TableCell.TextCell(
                        text = context.getString(SharedR.string.account_cancel_account_screen_plan_call_meeting_duration),
                        style = TableCell.TextCellStyle.SubHeader,
                        cellAlignment = TableCell.CellAlignment.Start,
                    ),
                    TableCell.TextCell(
                        text = context.getString(SharedR.string.account_cancel_account_screen_plan_call_meeting_duration_free),
                        style = TableCell.TextCellStyle.Normal,
                        cellAlignment = TableCell.CellAlignment.Center,
                    ),
                    TableCell.TextCell(
                        text = context.getString(SharedR.string.cancel_account_plan_call_meeting_duration_pro),
                        style = TableCell.TextCellStyle.Normal,
                        cellAlignment = TableCell.CellAlignment.Center,
                    ),

                    TableCell.TextCell(
                        text = context.getString(SharedR.string.account_cancel_account_plan_call_meeting_participants),
                        style = TableCell.TextCellStyle.SubHeader,
                        cellAlignment = TableCell.CellAlignment.Start,
                    ),
                    TableCell.TextCell(
                        text = context.getString(SharedR.string.account_cancel_account_free_plan_call_meeting_participants),
                        style = TableCell.TextCellStyle.Normal,
                        cellAlignment = TableCell.CellAlignment.Center,
                    ),
                    TableCell.TextCell(
                        text = context.getString(SharedR.string.account_cancel_account_pro_plan_call_meeting_participants),
                        style = TableCell.TextCellStyle.Normal,
                        cellAlignment = TableCell.CellAlignment.Center,
                    ),
                )
            }


            MegaTable(
                modifier = Modifier
                    .height(550.dp)
                    .padding(top = 30.dp)
                    .testTag(CANCEL_ACCOUNT_PLAN_FEATURE_TABLE_TEST_TAG),
                numOfColumn = 3,
                tableCells = cells,
            )
            Column {
                RaisedDefaultMegaButton(
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .fillMaxWidth()
                        .testTag(KEEP_PRO_PLAN_BUTTON_TEST_TAG),
                    text = stringResource(
                        id = SharedR.string.account_cancel_account_plan_keep_pro_plan,
                        accountDetailsUI.accountType
                    ),
                    onClick = onKeepPlanButtonClicked,
                )
                OutlinedMegaButton(
                    textId = SharedR.string.account_cancel_account_plan_continue_cancellation,
                    onClick = onContinueCancellationButtonClicked,
                    rounded = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag(CONTINUE_CANCELLATION_BUTTON_TEST_TAG),
                )
            }
        }
    }
}

@Composable
@CombinedThemePreviews
@CombinedThemeTabletLandscapePreviews
@CombinedThemeTabletPortraitPreviews
private fun CancelAccountPlanViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        CancelAccountPlanView(
            accountDetailsUI = UIAccountDetails(
                accountType = "Pro Lite",
                freeStorageQuota = "1 GB",
                rewindDaysQuota = "90",
                usedStorageSize = "1 TB",
                storageQuotaSize = "3 TB",
                transferQuotaSize = "1 TB",
            ),
            onKeepPlanButtonClicked = {},
            onContinueCancellationButtonClicked = {},
        )
    }
}

internal const val CANCEL_ACCOUNT_PLAN_FEATURE_TABLE_TEST_TAG =
    "cancel_account_plan_view:feature_table"
internal const val CANCEL_ACCOUNT_PLAN_STORAGE_HINT_TEST_TAG =
    "cancel_account_plan_view:text_storage_hint"
internal const val CANCEL_ACCOUNT_PLAN_SUBTITLE_TEST_TAG = "cancel_account_plan_view:text_subtitle"
internal const val CANCEL_ACCOUNT_PLAN_TITLE_TEST_TAG = "cancel_account_plan_view:text_title"
internal const val KEEP_PRO_PLAN_BUTTON_TEST_TAG =
    "cancel_Account_plan_view:raised_default_mega_button_keep_pro_plan"
internal const val CONTINUE_CANCELLATION_BUTTON_TEST_TAG =
    "cancel_Account_plan_view:outlined_mega_button_continue_cancellation"