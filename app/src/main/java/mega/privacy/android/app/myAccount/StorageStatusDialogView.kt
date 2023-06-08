package mega.privacy.android.app.myAccount

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.h6
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StorageState

internal const val TITLE_TAG = "storage_status_dialog:text_title"
internal const val IMAGE_STATUS_TAG = "storage_status_dialog:image_status"
internal const val BODY_TAG = "storage_status_dialog:text_body"
internal const val HORIZONTAL_DISMISS_TAG = "storage_status_dialog:button_horizontal_dismiss"
internal const val VERTICAL_DISMISS_TAG = "storage_status_dialog:button_vertical_dismiss"
internal const val ACHIEVEMENT_TAG = "storage_status_dialog:button_achievement"
internal const val HORIZONTAL_ACTION_TAG = "storage_status_dialog:button_horizontal_action"
internal const val VERTICAL_ACTION_TAG = "storage_status_dialog:button_vertical_action"

@Composable
internal fun StorageStatusDialogView(
    onDismissRequest: () -> Unit,
    state: StorageStatusDialogState,
    dismissClickListener: () -> Unit,
    horizontalActionButtonClickListener: () -> Unit,
    verticalActionButtonClickListener: () -> Unit,
    achievementButtonClickListener: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = getDetail(state = state)
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        Surface(
            modifier = modifier
                .padding(16.dp),
            elevation = 24.dp,
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 24.dp, end = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Dialog title
                    Text(
                        modifier = Modifier.testTag(TITLE_TAG),
                        textAlign = TextAlign.Center,
                        text = detail.titleText,
                        style = h6,
                    )

                    Image(
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .testTag(IMAGE_STATUS_TAG),
                        painter = painterResource(
                            id = detail.imageResource ?: R.drawable.ic_storage_almost_full
                        ),
                        contentDescription = "StorageStatusImage"
                    )

                    // Dialog body
                    Text(
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .testTag(BODY_TAG),
                        text = detail.descriptionText
                    )
                }

                if (!state.isAchievementsEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, top = 32.dp, bottom = 16.dp, end = 24.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextMegaButton(
                            modifier = Modifier.testTag(HORIZONTAL_DISMISS_TAG),
                            textId = R.string.general_dismiss,
                            onClick = dismissClickListener,
                        )

                        TextMegaButton(
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .testTag(HORIZONTAL_ACTION_TAG),
                            text = detail.horizontalActionButtonText,
                            onClick = horizontalActionButtonClickListener,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp, bottom = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        TextMegaButton(
                            modifier = Modifier.testTag(VERTICAL_ACTION_TAG),
                            text = detail.verticalActionButtonText,
                            onClick = verticalActionButtonClickListener,
                        )

                        TextMegaButton(
                            modifier = Modifier.testTag(ACHIEVEMENT_TAG),
                            textId = R.string.button_bonus_almost_full_warning,
                            onClick = achievementButtonClickListener,
                        )

                        TextMegaButton(
                            modifier = Modifier.testTag(VERTICAL_DISMISS_TAG),
                            textId = R.string.general_dismiss,
                            onClick = dismissClickListener,
                        )
                    }
                }
            }
        }
    }
}

private data class DialogViewDetail(
    val titleText: String = "",
    @DrawableRes val imageResource: Int? = null,
    val descriptionText: String = "",
    val horizontalActionButtonText: String = "",
    val verticalActionButtonText: String = "",
)

@Composable
private fun getDetail(state: StorageStatusDialogState): DialogViewDetail {
    var contentText: String
    var storageString = ""
    var transferString = ""
    var titleText: String
    val imageResource: Int
    val verticalActionButtonText: String
    val horizontalActionButtonText: String

    if (state.product != null) {
        storageString = Util.getSizeStringGBBased(state.product.storage.toLong())
        transferString = Util.getSizeStringGBBased(state.product.transfer.toLong())
    }

    when (state.storageState) {
        StorageState.Orange -> {
            imageResource = R.drawable.ic_storage_almost_full
            contentText = String.format(
                stringResource(R.string.text_almost_full_warning),
                storageString,
                transferString
            )
        }

        else -> {
            imageResource = R.drawable.ic_storage_full
            contentText = String.format(
                stringResource(R.string.text_storage_full_warning),
                storageString,
                transferString
            )
        }
    }
    titleText = stringResource(id = R.string.action_upgrade_account)

    when (state.accountType) {
        AccountType.PRO_III -> {
            when (state.storageState) {
                StorageState.Orange -> {
                    contentText = stringResource(R.string.text_almost_full_warning_pro3_account)
                }

                StorageState.Red -> {
                    contentText = stringResource(R.string.text_storage_full_warning_pro3_account)
                }

                else -> {}
            }
            verticalActionButtonText =
                stringResource(id = R.string.button_custom_almost_full_warning)
            horizontalActionButtonText =
                stringResource(id = R.string.button_custom_almost_full_warning)
        }

        AccountType.PRO_LITE, AccountType.PRO_I, AccountType.PRO_II -> {
            when (state.storageState) {
                StorageState.Orange -> {
                    contentText = String.format(
                        stringResource(R.string.text_almost_full_warning_pro_account),
                        storageString,
                        transferString
                    )
                }

                StorageState.Red -> {
                    contentText = String.format(
                        stringResource(R.string.text_storage_full_warning_pro_account),
                        storageString,
                        transferString
                    )
                }

                else -> {}
            }
            verticalActionButtonText =
                stringResource(id = R.string.my_account_upgrade_pro)
            horizontalActionButtonText =
                stringResource(id = R.string.my_account_upgrade_pro)
        }

        else -> {
            verticalActionButtonText =
                stringResource(id = R.string.button_plans_almost_full_warning)
            horizontalActionButtonText =
                stringResource(id = R.string.button_plans_almost_full_warning)
        }
    }

    if (state.overQuotaAlert) {
        if (state.preWarning) {
            titleText = stringResource(id = R.string.action_upgrade_account)
            contentText = stringResource(id = R.string.pre_overquota_alert_text)
        } else {
            titleText = stringResource(id = R.string.overquota_alert_title)
            contentText = stringResource(id = R.string.overquota_alert_text)
        }
    }

    return DialogViewDetail(
        titleText = titleText,
        imageResource = imageResource,
        descriptionText = contentText,
        verticalActionButtonText = verticalActionButtonText,
        horizontalActionButtonText = horizontalActionButtonText
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "HorizontalButtonDialog")
@Composable
private fun PreviewHorizontalButtonDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        val storageState = StorageStatusDialogState().copy(
            storageState = StorageState.Red,
            accountType = AccountType.FREE,
            product = null,
            isAchievementsEnabled = false,
            overQuotaAlert = true,
            preWarning = false
        )

        StorageStatusDialogView(
            onDismissRequest = { },
            state = storageState,
            dismissClickListener = {},
            horizontalActionButtonClickListener = {},
            verticalActionButtonClickListener = {},
            achievementButtonClickListener = {}
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "VerticalButtonDialog")
@Composable
private fun PreviewVerticalButtonDialogView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        val storageState = StorageStatusDialogState(
            storageState = StorageState.Red,
            accountType = AccountType.FREE,
            product = null,
            isAchievementsEnabled = true,
            overQuotaAlert = true,
            preWarning = false
        )

        StorageStatusDialogView(
            onDismissRequest = { },
            state = storageState,
            dismissClickListener = {},
            horizontalActionButtonClickListener = {},
            verticalActionButtonClickListener = {},
            achievementButtonClickListener = {}
        )
    }
}