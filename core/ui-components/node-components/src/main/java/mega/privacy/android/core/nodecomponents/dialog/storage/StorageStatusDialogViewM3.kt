package mega.privacy.android.core.nodecomponents.dialog.storage

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.dialogs.DialogButton
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.shared.resources.R as sharedR

// Constants for test tags
internal const val TITLE_TAG_M3 = "storage_status_dialog_m3:text_title"
internal const val IMAGE_STATUS_TAG_M3 = "storage_status_dialog_m3:image_status"
internal const val BODY_TAG_M3 = "storage_status_dialog_m3:text_body"
internal const val HORIZONTAL_DISMISS_TAG_M3 = "storage_status_dialog_m3:button_horizontal_dismiss"
internal const val VERTICAL_DISMISS_TAG_M3 = "storage_status_dialog_m3:button_vertical_dismiss"
internal const val ACHIEVEMENT_TAG_M3 = "storage_status_dialog_m3:button_achievement"
internal const val HORIZONTAL_ACTION_TAG_M3 = "storage_status_dialog_m3:button_horizontal_action"
internal const val VERTICAL_ACTION_TAG_M3 = "storage_status_dialog_m3:button_vertical_action"

/**
 * Helper compose view to show StorageStatusDialogView M3 including viewModel and navigation logic
 */
@Composable
internal fun StorageStatusDialogViewM3(
    storageState: StorageState,
    preWarning: Boolean,
    overQuotaAlert: Boolean,
    onUpgradeClick: () -> Unit,
    onCustomizedPlanClick: (email: String, accountType: AccountType) -> Unit,
    onAchievementsClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    viewModel: StorageStatusViewModel = viewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val dialogState = StorageStatusDialogState(
        storageState = storageState,
        overQuotaAlert = overQuotaAlert,
        preWarning = preWarning,
        isAchievementsEnabled = uiState.isAchievementsEnabled,
        product = uiState.product,
        accountType = uiState.accountType,
    )

    StorageStatusDialogViewM3(
        modifier = modifier,
        isVisible = isVisible,
        dismissClickListener = onClose,
        state = dialogState,
        actionButtonClickListener = {
            when (uiState.accountType) {
                AccountType.PRO_III -> {
                    coroutineScope.launch {
                        onCustomizedPlanClick(viewModel.getUserEmail(), uiState.accountType)
                    }
                }

                else -> onUpgradeClick()
            }
            onClose()
        },
        achievementButtonClickListener = {
            onAchievementsClick()
            onClose()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StorageStatusDialogViewM3(
    state: StorageStatusDialogState,
    dismissClickListener: () -> Unit,
    actionButtonClickListener: () -> Unit,
    achievementButtonClickListener: () -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
) {
    val context = LocalContext.current
    val detail = remember(state) {
        getDialogDetail(context = context, state = state)
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = expandIn(),
        exit = shrinkOut()
    ) {
        BasicAlertDialog(
            onDismissRequest = dismissClickListener, /* is not dismissible, but just in case */
            modifier = modifier,
            properties = DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false
            ),
        ) {
            StorageStatusDialogContentM3(
                title = {
                    MegaText(
                        modifier = Modifier.testTag(TITLE_TAG_M3),
                        text = detail.titleText,
                        style = AppTheme.typography.headlineSmall,
                        textColor = TextColor.Primary,
                    )
                },
                content = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.x24)
                    ) {
                        Image(
                            modifier = Modifier.testTag(IMAGE_STATUS_TAG_M3),
                            painter = painterResource(
                                id = detail.imageResource
                            ),
                            contentDescription = "StorageStatusImage"
                        )

                        MegaText(
                            modifier = Modifier.testTag(BODY_TAG_M3),
                            text = detail.descriptionText,
                            style = AppTheme.typography.bodyMedium,
                            textColor = TextColor.Secondary,
                        )
                    }
                },
                buttons = {
                    if (state.isAchievementsEnabled) {
                        // Vertical button layout (like in M2 version)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.x8)
                        ) {
                            Box(modifier = Modifier.testTag(VERTICAL_ACTION_TAG_M3)) {
                                DialogButton(
                                    buttonText = detail.verticalActionButtonText,
                                    onButtonClicked = actionButtonClickListener,
                                )
                            }

                            Box(modifier = Modifier.testTag(ACHIEVEMENT_TAG_M3)) {
                                DialogButton(
                                    buttonText = stringResource(R.string.button_bonus_almost_full_warning),
                                    onButtonClicked = achievementButtonClickListener,
                                )
                            }

                            Box(modifier = Modifier.testTag(VERTICAL_DISMISS_TAG_M3)) {
                                DialogButton(
                                    buttonText = stringResource(R.string.general_dismiss),
                                    onButtonClicked = dismissClickListener,
                                )
                            }
                        }
                    } else {
                        // Horizontal button layout (like in M2 version)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.testTag(HORIZONTAL_DISMISS_TAG_M3)) {
                                DialogButton(
                                    buttonText = stringResource(R.string.general_dismiss),
                                    onButtonClicked = dismissClickListener,
                                )
                            }

                            Box(modifier = Modifier.testTag(HORIZONTAL_ACTION_TAG_M3)) {
                                DialogButton(
                                    buttonText = detail.horizontalActionButtonText,
                                    onButtonClicked = actionButtonClickListener,
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun StorageStatusDialogContentM3(
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
    buttons: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = DSTokens.colors.background.surface1
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(LocalSpacing.current.x24),
            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.x16),
        ) {
            title()
            content()
            buttons()
        }
    }
}

/**
 * Simple clone of FlowRow that arranges its children in a horizontal flow
 * following the same pattern as MegaBasicDialogFlowRow from BasicDialog
 */
@Composable
private fun StorageStatusDialogFlowRow(content: @Composable () -> Unit) {
    // For now, using a simple Row layout for horizontal buttons
    // This matches the behavior of the M2 version
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

private data class DialogViewDetailM3(
    val titleText: String = "",
    @DrawableRes val imageResource: Int,
    val descriptionText: String = "",
    val horizontalActionButtonText: String = "",
    val verticalActionButtonText: String = "",
)

private fun getDialogDetail(context: Context, state: StorageStatusDialogState): DialogViewDetailM3 {
    var contentText: String
    var storageString = ""
    var transferString = ""
    var titleText: String
    val imageResource: Int
    val verticalActionButtonText: String
    val horizontalActionButtonText: String

    val formattedSizeMapper = FormattedSizeMapper()

    val product = state.product
    if (product != null) {
        val formattedStorage = formattedSizeMapper(product.storage, true)
        storageString = context.getString(formattedStorage.unit, formattedStorage.size)
        val formattedTransfer = formattedSizeMapper(product.transfer, true)
        transferString = context.getString(formattedTransfer.unit, formattedTransfer.size)
    }

    when (state.storageState) {
        StorageState.Orange -> {
            imageResource = R.drawable.ic_storage_almost_full
            contentText = String.format(
                context.getString(R.string.text_almost_full_warning),
                storageString,
                transferString
            )
        }

        else -> {
            imageResource = R.drawable.ic_storage_full
            contentText = String.format(
                context.getString(R.string.text_storage_full_warning),
                storageString,
                transferString
            )
        }
    }
    titleText = context.getString(R.string.action_upgrade_account)

    when (state.accountType) {
        AccountType.PRO_III -> {
            when (state.storageState) {
                StorageState.Orange -> {
                    contentText = context.getString(R.string.text_almost_full_warning_pro3_account)
                }

                StorageState.Red -> {
                    contentText = context.getString(R.string.text_storage_full_warning_pro3_account)
                }

                else -> {}
            }
            verticalActionButtonText =
                context.getString(R.string.button_custom_almost_full_warning)
            horizontalActionButtonText =
                context.getString(R.string.button_custom_almost_full_warning)
        }

        AccountType.PRO_LITE, AccountType.PRO_I,
        AccountType.PRO_II, AccountType.BASIC,
        AccountType.STARTER, AccountType.ESSENTIAL,
            -> {
            when (state.storageState) {
                StorageState.Orange -> {
                    contentText = String.format(
                        context.getString(R.string.text_almost_full_warning_pro_account),
                        storageString,
                        transferString
                    )
                }

                StorageState.Red -> {
                    contentText = String.format(
                        context.getString(R.string.text_storage_full_warning_pro_account),
                        storageString,
                        transferString
                    )
                }

                else -> {}
            }
            verticalActionButtonText =
                context.getString(sharedR.string.general_upgrade_button)
            horizontalActionButtonText =
                context.getString(sharedR.string.general_upgrade_button)
        }

        else -> {
            verticalActionButtonText =
                context.getString(R.string.button_plans_almost_full_warning)
            horizontalActionButtonText =
                context.getString(R.string.button_plans_almost_full_warning)
        }
    }

    if (state.overQuotaAlert) {
        if (state.preWarning) {
            titleText = context.getString(R.string.action_upgrade_account)
            contentText = context.getString(R.string.pre_overquota_alert_text)
        } else {
            titleText = context.getString(R.string.overquota_alert_title)
            contentText = context.getString(R.string.overquota_alert_text)
        }
    }

    return DialogViewDetailM3(
        titleText = titleText,
        imageResource = imageResource,
        descriptionText = contentText,
        verticalActionButtonText = verticalActionButtonText,
        horizontalActionButtonText = horizontalActionButtonText
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewHorizontalButtonDialogM3(
    @PreviewParameter(StorageStatusDialogM3PreviewProvider::class) storageState: StorageStatusDialogState,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        StorageStatusDialogViewM3(
            state = storageState,
            dismissClickListener = {},
            actionButtonClickListener = {},
            achievementButtonClickListener = {}
        )
    }
}

private class StorageStatusDialogM3PreviewProvider :
    PreviewParameterProvider<StorageStatusDialogState> {
    override val values: Sequence<StorageStatusDialogState>
        get() = sequenceOf(
            StorageStatusDialogState(
                storageState = StorageState.Red,
                accountType = AccountType.PRO_III,
                product = null,
                isAchievementsEnabled = false,
                overQuotaAlert = false,
                preWarning = false
            ),
            StorageStatusDialogState(
                storageState = StorageState.Red,
                accountType = AccountType.PRO_I,
                product = null,
                isAchievementsEnabled = true,
                overQuotaAlert = false,
                preWarning = false
            ),
            StorageStatusDialogState(
                storageState = StorageState.Red,
                accountType = AccountType.ESSENTIAL,
                product = null,
                isAchievementsEnabled = false,
                overQuotaAlert = true,
                preWarning = false
            ),
            StorageStatusDialogState(
                storageState = StorageState.Red,
                accountType = AccountType.BASIC,
                product = null,
                isAchievementsEnabled = false,
                overQuotaAlert = true,
                preWarning = false
            ),
            StorageStatusDialogState(
                storageState = StorageState.Red,
                accountType = AccountType.STARTER,
                product = null,
                isAchievementsEnabled = false,
                overQuotaAlert = true,
                preWarning = false
            ),
            StorageStatusDialogState(
                storageState = StorageState.Red,
                accountType = AccountType.FREE,
                product = null,
                isAchievementsEnabled = false,
                overQuotaAlert = true,
                preWarning = false
            ),
            StorageStatusDialogState(
                storageState = StorageState.Orange,
                accountType = AccountType.FREE,
                product = null,
                isAchievementsEnabled = false,
                overQuotaAlert = true,
                preWarning = true
            )
        )
}
