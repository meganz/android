package mega.privacy.android.app.presentation.testpassword.view

import android.content.Context
import android.content.res.Configuration
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.net.toUri
import androidx.print.PrintHelper
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.changepassword.model.TestPasswordAttribute
import mega.privacy.android.app.presentation.testpassword.model.PasswordState
import mega.privacy.android.app.presentation.testpassword.model.TestPasswordUIState
import mega.privacy.android.app.presentation.testpassword.view.Constants.BACKUP_BUTTON_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.CLOSE_BUTTON_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.CONFIRM_BUTTON_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.DISMISS_BUTTON_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.PASSWORD_REMINDER_APP_BAR_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.PASSWORD_REMINDER_DESC_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.PASSWORD_REMINDER_DISMISS_BUTTON_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.PASSWORD_REMINDER_LAYOUT_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.PASSWORD_TEXT_FIELD_FOOTER_ICON_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.PASSWORD_TEXT_FIELD_FOOTER_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.PASSWORD_TEXT_FIELD_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.PROCEED_TO_LOGOUT_BUTTON_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.PROGRESS_LOADING_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.SEE_PASSWORD_TEST_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.TEST_PASSWORD_APP_BAR_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.TEST_PASSWORD_BUTTON_TAG
import mega.privacy.android.app.presentation.testpassword.view.Constants.TEST_PASSWORD_LAYOUT_TAG
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.extensions.autofill
import mega.privacy.android.core.ui.theme.extensions.green_500_green_400
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_038
import mega.privacy.android.core.ui.theme.extensions.red_600_red_300
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.legacy.core.ui.controls.appbar.SimpleTopAppBar
import mega.privacy.android.legacy.core.ui.controls.textfields.MegaTextField
import java.io.File

internal object Constants {
    /**
     * Test tag for message SnackBar
     */
    const val SNACKBAR_TAG = "snackbar_test_tag"

    /**
     * Test tag for password reminder layout
     */
    const val PASSWORD_REMINDER_LAYOUT_TAG = "password_layout_test_tag"

    /**
     * Test tag for test password layout
     */
    const val TEST_PASSWORD_LAYOUT_TAG = "test_password_layout_test_tag"

    /**
     * Test tag for test password app bar
     */
    const val TEST_PASSWORD_APP_BAR_TAG = "test_password_app_bar_tag"

    /**
     * Test tag for password reminder app bar
     */
    const val PASSWORD_REMINDER_APP_BAR_TAG = "test_password_app_bar_tag"

    /**
     * Test tag for close button
     */
    const val CLOSE_BUTTON_TAG = "close_button_tag"

    /**
     * Test tag for progress loading
     */
    const val PROGRESS_LOADING_TAG = "progress_loading_tag"

    /**
     * Test tag for test password button
     */
    const val TEST_PASSWORD_BUTTON_TAG = "test_password_button_tag"

    /**
     * Test tag for confirm password button
     */
    const val CONFIRM_BUTTON_TAG = "confirm_button_tag"

    /**
     * Test tag for test backup button
     */
    const val BACKUP_BUTTON_TAG = "backup_button_tag"

    /**
     * Test tag for test backup button
     */
    const val DISMISS_BUTTON_TAG = "dismiss_button_tag"

    /**
     * Test tag for password reminder description
     */
    const val PASSWORD_REMINDER_DESC_TAG = "password_reminder_desc_tag"

    /**
     * Test tag for password reminder  dismiss button
     */
    const val PASSWORD_REMINDER_DISMISS_BUTTON_TAG = "password_reminder_dismiss_button_tag"

    /**
     * Test tag for password text field
     */
    const val PASSWORD_TEXT_FIELD_TAG = "password_text_field_tag"

    /**
     * Test tag for password text field footer
     */
    const val PASSWORD_TEXT_FIELD_FOOTER_TAG = "password_text_field_footer_tag"

    /**
     * Test tag for password text field footer icon
     */
    const val PASSWORD_TEXT_FIELD_FOOTER_ICON_TAG = "password_text_field_footer_icon_tag"

    /**
     * Test tag for proceed to logout button
     */
    const val PROCEED_TO_LOGOUT_BUTTON_TAG = "proceed_to_logout_button_tag"

    /**
     * Test tag for see password icon
     */
    const val SEE_PASSWORD_TEST_TAG = "see_password_test_tag"

    /**
     * Test tag for see bottom sheet title
     * @see RecoveryKeyBottomSheet
     */
    const val BOTTOM_SHEET_TITLE = "bottom_sheet_title"

    /**
     * Test tag for see bottom sheet print menu
     * @see RecoveryKeyBottomSheet
     */
    const val BOTTOM_SHEET_PRINT = "bottom_sheet_print"

    /**
     * Test tag for see bottom sheet copy menu
     * @see RecoveryKeyBottomSheet
     */
    const val BOTTOM_SHEET_COPY = "bottom_sheet_copy"

    /**
     * Test tag for see bottom sheet save menu
     * @see RecoveryKeyBottomSheet
     */
    const val BOTTOM_SHEET_SAVE = "bottom_sheet_save"
}

/**
 * Test Password Feature in Jetpack Compose
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun TestPasswordComposeView(
    uiState: TestPasswordUIState,
    onResetUserMessage: () -> Unit,
    onCheckCurrentPassword: (String) -> Unit,
    onTestPasswordClick: () -> Unit,
    onCheckboxValueChanged: (Boolean) -> Unit,
    onDismiss: (Boolean) -> Unit,
    onResetPasswordVerificationState: () -> Unit,
    onUserLogout: (Boolean) -> Unit,
    onResetUserLogout: () -> Unit,
    onFinishedCopyingRecoveryKey: (Boolean) -> Unit,
    onResetFinishedCopyingRecoveryKey: () -> Unit,
    onExhaustedPasswordAttempts: () -> Unit,
    onResetExhaustedPasswordAttempts: () -> Unit,
    onPrintRecoveryKey: () -> Unit,
    onCopyRecoveryKey: () -> Unit,
    onSaveRecoveryKey: () -> Unit,
    onPrintRecoveryKeyConsumed: () -> Unit,
    onPrintRecoveryKeyCompleted: (File) -> Unit,
) {
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    var errorAlertMessage by remember { mutableStateOf<String?>(null) }

    EventEffect(event = uiState.userMessage, onConsumed = onResetUserMessage) { res ->
        snackBarHostState.showSnackbar(context.resources.getString(res))
    }

    EventEffect(
        event = uiState.isUserLogout,
        onConsumed = onResetUserLogout,
        action = onUserLogout
    )

    EventEffect(
        event = uiState.isFinishedCopyingRecoveryKey,
        onConsumed = onResetFinishedCopyingRecoveryKey,
        action = onFinishedCopyingRecoveryKey
    )

    EventEffect(
        event = uiState.isUserExhaustedPasswordAttempts,
        onConsumed = onExhaustedPasswordAttempts,
        action = onResetExhaustedPasswordAttempts
    )

    EventEffect(
        event = uiState.printRecoveryKey,
        onConsumed = onPrintRecoveryKeyConsumed
    ) { file ->
        file?.let {
            printRecoveryKey(context, it, onPrintRecoveryKeyCompleted)
        } ?: run {
            errorAlertMessage = context.getString(R.string.general_text_error)
        }
    }

    Scaffold(
        scaffoldState = rememberScaffoldState(),
        topBar = {

        },
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                Snackbar(
                    modifier = Modifier.testTag(Constants.SNACKBAR_TAG),
                    snackbarData = data,
                    backgroundColor = black.takeIf { MaterialTheme.colors.isLight } ?: white
                )
            }
        }
    ) { padding ->
        val coroutineScope = rememberCoroutineScope()
        val modalSheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded },
            skipHalfExpanded = true,
        )

        BackHandler(enabled = modalSheetState.isVisible) {
            coroutineScope.launch { modalSheetState.hide() }
        }

        ConstraintLayout(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            val (appbar, tpLayout, prLayout, loading) = createRefs()

            TestPasswordScreenAppBar(
                modifier = Modifier.constrainAs(appbar) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
                uiState = uiState,
                onBackPressedDispatcher = onBackPressedDispatcher
            )

            if (uiState.isUITestPasswordMode) {
                TestPasswordModeLayout(
                    modifier = Modifier
                        .constrainAs(tpLayout) {
                            top.linkTo(appbar.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                        .testTag(TEST_PASSWORD_LAYOUT_TAG),
                    uiState = uiState,
                    modalSheetState = modalSheetState,
                    coroutineScope = coroutineScope,
                    onBackPressedDispatcher = onBackPressedDispatcher,
                    onCheckCurrentPassword = onCheckCurrentPassword,
                    onResetPasswordVerificationState = onResetPasswordVerificationState,
                    onDismiss = onDismiss,
                )
            } else {
                PasswordReminderModeLayout(
                    modifier = Modifier
                        .constrainAs(prLayout) {
                            top.linkTo(appbar.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                        .testTag(PASSWORD_REMINDER_LAYOUT_TAG),
                    uiState = uiState,
                    modalSheetState = modalSheetState,
                    coroutineScope = coroutineScope,
                    onBackPressedDispatcher = onBackPressedDispatcher,
                    onTestPasswordClick = onTestPasswordClick,
                    onCheckboxValueChanged = onCheckboxValueChanged,
                    onDismiss = onDismiss
                )
            }

            if (uiState.isLoading) {
                MegaCircularProgressIndicator(
                    modifier = Modifier
                        .constrainAs(loading) {
                            top.linkTo(appbar.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        }
                        .testTag(PROGRESS_LOADING_TAG)
                        .size(44.dp))
            }
        }

        RecoveryKeyBottomSheet(
            modalSheetState = modalSheetState,
            onPrint = onPrintRecoveryKey,
            onCopy = onCopyRecoveryKey,
            onSave = onSaveRecoveryKey
        )

        errorAlertMessage?.let { message ->
            MegaAlertDialog(
                text = message,
                confirmButtonText = stringResource(id = R.string.general_ok),
                cancelButtonText = null,
                onConfirm = { errorAlertMessage = null },
                onDismiss = { errorAlertMessage = null }
            )
        }
    }
}

@Composable
private fun TestPasswordScreenAppBar(
    modifier: Modifier,
    uiState: TestPasswordUIState,
    onBackPressedDispatcher: OnBackPressedDispatcher?,
) {
    if (uiState.isUITestPasswordMode) {
        TestPasswordModeAppBar(
            modifier = modifier.testTag(TEST_PASSWORD_APP_BAR_TAG),
            isEnabled = uiState.isLoading.not(),
            onBackPressedDispatcher = onBackPressedDispatcher
        )
    } else {
        PasswordReminderModeAppBar(
            modifier = modifier.testTag(PASSWORD_REMINDER_APP_BAR_TAG),
            isEnabled = uiState.isLoading.not(),
            isIconVisible = uiState.isLogoutMode,
            onBackPressedDispatcher = onBackPressedDispatcher
        )
    }
}

@Composable
private fun PasswordReminderModeAppBar(
    modifier: Modifier,
    isEnabled: Boolean,
    isIconVisible: Boolean,
    onBackPressedDispatcher: OnBackPressedDispatcher?,
) {
    TopAppBar(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isEnabled) 1f else 0.3f),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 0.dp
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val (title, icon) = createRefs()

            Text(
                modifier = Modifier.constrainAs(title) {
                    top.linkTo(parent.top, margin = 17.dp)
                    bottom.linkTo(parent.bottom)
                    linkTo(
                        start = parent.start,
                        end = icon.start,
                        startMargin = 24.dp,
                        endMargin = 40.dp,
                        bias = 0f
                    )
                },
                text = stringResource(id = R.string.remember_pwd_dialog_title),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium
            )

            if (isIconVisible) {
                IconButton(
                    modifier = Modifier
                        .testTag(CLOSE_BUTTON_TAG)
                        .constrainAs(icon) {
                            top.linkTo(title.top)
                            bottom.linkTo(title.bottom)
                            end.linkTo(parent.end)
                        },
                    onClick = { onBackPressedDispatcher?.onBackPressed() },
                    enabled = isEnabled
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close button",
                        tint = if (MaterialTheme.colors.isLight) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TestPasswordModeAppBar(
    modifier: Modifier,
    isEnabled: Boolean,
    onBackPressedDispatcher: OnBackPressedDispatcher?,
) {
    SimpleTopAppBar(
        modifier = modifier.alpha(if (isEnabled) 1f else 0.3f),
        titleId = R.string.remember_pwd_dialog_button_test,
        elevation = false,
        onBackPressed = {
            onBackPressedDispatcher?.onBackPressed()
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun PasswordReminderModeLayout(
    modifier: Modifier,
    uiState: TestPasswordUIState,
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    onBackPressedDispatcher: OnBackPressedDispatcher?,
    onTestPasswordClick: () -> Unit,
    onCheckboxValueChanged: (Boolean) -> Unit,
    onDismiss: (Boolean) -> Unit,
) {
    var isChecked by remember { mutableStateOf(uiState.isPasswordReminderBlocked) }
    @StringRes val descriptionText: Int =
        if (uiState.isLogoutMode) R.string.remember_pwd_dialog_text_logout else R.string.remember_pwd_dialog_text
    @StringRes val dismissButtonText: Int =
        if (uiState.isLogoutMode) R.string.proceed_to_logout else R.string.general_dismiss
    val dismissButtonColor =
        if (uiState.isLogoutMode) MaterialTheme.colors.red_600_red_300 else MaterialTheme.colors.secondary

    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 32.dp, start = 24.dp, end = 16.dp)
            .verticalScroll(rememberScrollState())
            .alpha(if (uiState.isLoading) 0.3f else 1f)
    ) {
        val (
            icon,
            description,
            checkboxDescription,
            checkbox,
            testPasswordButton,
            backupRKButton,
            dismissButton,
        ) = createRefs()

        Image(
            painter = painterResource(id = R.drawable.ic_key),
            modifier = Modifier
                .constrainAs(icon) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                }
                .width(80.dp)
                .height(80.dp),
            contentDescription = "Key Icon",
            contentScale = ContentScale.Crop
        )

        Text(
            modifier = Modifier
                .testTag(PASSWORD_REMINDER_DESC_TAG)
                .constrainAs(description) {
                    top.linkTo(icon.bottom, margin = 32.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .fillMaxWidth()
                .wrapContentHeight(),
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.textColorSecondary,
            text = stringResource(id = descriptionText)
        )

        Text(
            modifier = Modifier
                .constrainAs(checkboxDescription) {
                    top.linkTo(description.bottom, margin = 30.dp)
                    linkTo(parent.start, checkbox.end, 0.dp, 4.dp, bias = 0f)
                }
                .fillMaxWidth()
                .wrapContentHeight(),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.textColorSecondary,
            text = stringResource(id = R.string.general_do_not_show)
        )

        Checkbox(
            modifier = Modifier
                .constrainAs(checkbox) {
                    top.linkTo(checkboxDescription.top)
                    bottom.linkTo(checkboxDescription.bottom)
                    end.linkTo(parent.end)
                },
            checked = isChecked,
            enabled = uiState.isLoading.not(),
            onCheckedChange = {
                isChecked = it
                onCheckboxValueChanged(it)
            }
        )

        OutlinedButton(
            modifier = Modifier
                .testTag(TEST_PASSWORD_BUTTON_TAG)
                .constrainAs(testPasswordButton) {
                    top.linkTo(checkboxDescription.bottom, margin = 24.dp)
                    start.linkTo(parent.start)
                },
            onClick = onTestPasswordClick,
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = MaterialTheme.colors.secondary
            ),
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colors.secondary),
            enabled = uiState.isLoading.not()
        ) {
            Text(
                text = stringResource(id = R.string.remember_pwd_dialog_button_test),
                style = MaterialTheme.typography.button.copy(letterSpacing = 0.25.sp),
                color = MaterialTheme.colors.secondary,
                fontWeight = FontWeight.Medium
            )
        }

        Button(
            modifier = Modifier
                .testTag(BACKUP_BUTTON_TAG)
                .constrainAs(backupRKButton) {
                    top.linkTo(testPasswordButton.bottom, margin = 20.dp)
                    start.linkTo(parent.start)
                },
            onClick = {
                coroutineScope.launch { modalSheetState.show() }
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = MaterialTheme.colors.surface
            ),
            enabled = uiState.isLoading.not()
        ) {
            Text(
                text = stringResource(id = R.string.action_export_master_key),
                color = MaterialTheme.colors.surface,
                style = MaterialTheme.typography.button.copy(letterSpacing = 0.25.sp),
                fontWeight = FontWeight.Medium
            )
        }

        TextButton(
            modifier = Modifier
                .testTag(PASSWORD_REMINDER_DISMISS_BUTTON_TAG)
                .constrainAs(dismissButton) {
                    linkTo(
                        top = backupRKButton.bottom,
                        bottom = parent.bottom,
                        topMargin = 24.dp,
                        bottomMargin = 40.dp,
                        bias = 0f
                    )
                    start.linkTo(parent.start)
                },
            onClick = {
                if (uiState.isLogoutMode) {
                    onDismiss(true)
                } else {
                    onBackPressedDispatcher?.onBackPressed()
                }
            },
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = Color.Transparent,
                contentColor = MaterialTheme.colors.red_600_red_300
            ),
            enabled = uiState.isLoading.not()
        ) {
            Text(
                text = stringResource(id = dismissButtonText),
                style = MaterialTheme.typography.button.copy(letterSpacing = 0.25.sp),
                color = dismissButtonColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@Composable
private fun TestPasswordModeLayout(
    modifier: Modifier,
    uiState: TestPasswordUIState,
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    onBackPressedDispatcher: OnBackPressedDispatcher?,
    onCheckCurrentPassword: (String) -> Unit,
    onResetPasswordVerificationState: () -> Unit,
    onDismiss: (Boolean) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
            .alpha(if (uiState.isLoading) 0.3f else 1f)
    ) {
        var passwordText by remember { mutableStateOf("") }
        var isShowPasswordChar by remember { mutableStateOf(false) }
        val errorFooterText = uiState.isCurrentPassword.toAttribute().footerMessage
        val keyboardController = LocalSoftwareKeyboardController.current

        PasswordTextField(
            modifier = Modifier.run {
                fillMaxWidth()
                    .padding(top = 24.dp)
                    .testTag(PASSWORD_TEXT_FIELD_TAG)
            },
            label = stringResource(id = R.string.hint_set_password_protection_dialog),
            passwordState = uiState.isCurrentPassword,
            isShowPassword = isShowPasswordChar,
            onValueChange = { value, _ ->
                passwordText = value
                onResetPasswordVerificationState()
            },
            isError = uiState.isCurrentPassword == PasswordState.False,
            onClickShowPassword = { isShowPasswordChar = !isShowPasswordChar },
            onKeyboardDone = {
                onCheckCurrentPassword(passwordText)
                keyboardController?.hide()
            },
            isEnabled = uiState.isLoading.not()
        )

        if (errorFooterText.isBlank()) {
            Spacer(
                modifier = Modifier
                    .height(32.dp)
                    .fillMaxWidth()
            )
        }

        BorderlessActionButton(
            modifier = Modifier
                .testTag(CONFIRM_BUTTON_TAG)
                .padding(top = 15.dp),
            text = stringResource(id = R.string.hint_confirm_password_protection_dialog),
            onClick = {
                onCheckCurrentPassword(passwordText)
                keyboardController?.hide()
            },
            isEnabled = uiState.isLoading.not()
        )

        BorderlessActionButton(
            modifier = Modifier
                .testTag(BACKUP_BUTTON_TAG)
                .padding(top = 12.dp),
            text = stringResource(id = R.string.action_export_master_key),
            onClick = {
                coroutineScope.launch { modalSheetState.show() }
            },
            isEnabled = uiState.isLoading.not()
        )

        if (uiState.isLogoutMode.not()) {
            BorderlessActionButton(
                modifier = Modifier
                    .testTag(DISMISS_BUTTON_TAG)
                    .padding(top = 12.dp),
                text = stringResource(id = R.string.general_dismiss),
                onClick = {
                    onBackPressedDispatcher?.onBackPressed()
                },
                isEnabled = uiState.isLoading.not(),
            )
        }

        if (uiState.isLogoutMode) {
            Button(
                modifier = Modifier
                    .testTag(PROCEED_TO_LOGOUT_BUTTON_TAG)
                    .padding(top = 60.dp)
                    .defaultMinSize(minHeight = 48.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                onClick = {
                    onDismiss(true)
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.secondary,
                    contentColor = MaterialTheme.colors.onPrimary
                ),
                enabled = uiState.isLoading.not()
            ) {
                Text(
                    text = stringResource(id = R.string.proceed_to_logout),
                    style = MaterialTheme.typography.button.copy(
                        letterSpacing = 0.25.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colors.surface
                )
            }
        }
    }
}

/**
 * Password Text Field for Password and Confirm Password
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PasswordTextField(
    modifier: Modifier,
    label: String,
    passwordState: PasswordState,
    isShowPassword: Boolean = false,
    isError: Boolean = false,
    onValueChange: (value: String, isAutoFill: Boolean) -> Unit,
    onClickShowPassword: () -> Unit,
    onKeyboardDone: (KeyboardActionScope) -> Unit,
    isEnabled: Boolean,
) {
    var passwordText by remember { mutableStateOf("") }
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val indicatorColor = when {
        isError -> MaterialTheme.colors.error
        isFocused -> passwordState.toAttribute().focusedColor
        passwordText.isBlank() -> MaterialTheme.colors.grey_alpha_012_white_alpha_038
        else -> passwordState.toAttribute().focusedColor
    }

    MegaTextField(
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                if (isFocused != it.isFocused) {
                    isFocused = it.isFocused
                }
            }
            .autofill(
                autofillTypes = listOf(AutofillType.Password, AutofillType.NewPassword),
                onAutoFilled = {
                    passwordText = it
                    onValueChange(it, true)
                }
            ),
        value = passwordText,
        onValueChange = { text ->
            passwordText = text
            onValueChange(text, false)
        },
        label = { Text(text = label) },
        textStyle = TextStyle.Default.copy(fontSize = 16.sp),
        visualTransformation = if (isShowPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            if (isFocused) {
                Icon(
                    modifier = Modifier
                        .testTag(SEE_PASSWORD_TEST_TAG)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClickShowPassword,
                        )
                        .testTag(mega.privacy.android.app.presentation.changepassword.view.Constants.SEE_PASSWORD_TEST_TAG),
                    painter = painterResource(id = R.drawable.ic_see),
                    tint = if (isShowPassword) MaterialTheme.colors.secondary else MaterialTheme.colors.grey_alpha_012_white_alpha_038,
                    contentDescription = "see"
                )
            }
        },
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = MaterialTheme.colors.surface,
            textColor = MaterialTheme.colors.onSurface,
            cursorColor = MaterialTheme.colors.secondary,
            focusedLabelColor = indicatorColor,
            unfocusedLabelColor = indicatorColor,
            focusedIndicatorColor = indicatorColor,
            unfocusedIndicatorColor = indicatorColor,
        ),
        isError = isError,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = onKeyboardDone),
        enabled = isEnabled
    )

    if (passwordState.toAttribute().footerMessage.isNotBlank()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.testTag(PASSWORD_TEXT_FIELD_FOOTER_TAG),
                text = passwordState.toAttribute().footerMessage,
                color = passwordState.toAttribute().focusedColor,
                style = MaterialTheme.typography.caption
            )
            Spacer(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            Icon(
                modifier = Modifier
                    .testTag(PASSWORD_TEXT_FIELD_FOOTER_ICON_TAG)
                    .padding(end = 2.dp),
                painter = painterResource(id = passwordState.toAttribute().footerIcon),
                tint = passwordState.toAttribute().focusedColor,
                contentDescription = "footer"
            )
        }
    }
}

@Composable
private fun BorderlessActionButton(
    modifier: Modifier,
    text: String,
    onClick: () -> Unit,
    isEnabled: Boolean,
) {
    TextButton(
        modifier = modifier
            .height(36.dp)
            .wrapContentWidth(),
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = Color.Transparent, contentColor = MaterialTheme.colors.secondary
        ),
        enabled = isEnabled
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.button.copy(
                letterSpacing = 0.25.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colors.secondary
        )
    }
}

@Composable
private fun PasswordState.toAttribute(): TestPasswordAttribute {
    return when (this) {
        PasswordState.Initial -> TestPasswordAttribute(
            focusedColor = MaterialTheme.colors.secondary,
            footerMessage = "",
            footerIcon = -1
        )

        PasswordState.True -> TestPasswordAttribute(
            focusedColor = MaterialTheme.colors.green_500_green_400,
            footerMessage = stringResource(id = R.string.test_pwd_accepted),
            footerIcon = R.drawable.ic_accept_test
        )

        PasswordState.False -> TestPasswordAttribute(
            focusedColor = MaterialTheme.colors.red_600_red_300,
            footerMessage = stringResource(id = R.string.test_pwd_wrong),
            footerIcon = R.drawable.ic_input_warning
        )
    }
}

private fun printRecoveryKey(
    context: Context,
    file: File,
    onPrintRecoveryKeyCompleted: (File) -> Unit,
) {
    PrintHelper(context).apply {
        scaleMode = PrintHelper.SCALE_MODE_FIT
        printBitmap("rKPrint", file.toUri()) {
            onPrintRecoveryKeyCompleted(file)
        }
    }
}


/**
 * Test Password Feature in Jetpack Compose Preview
 */
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkMode")
@Composable
private fun TestPasswordComposeViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        TestPasswordComposeView(
            uiState = TestPasswordUIState(
                isUITestPasswordMode = false,
                isCurrentPassword = PasswordState.Initial
            ),
            onResetUserMessage = {},
            onCheckCurrentPassword = {},
            onTestPasswordClick = {},
            onCheckboxValueChanged = {},
            onDismiss = {},
            onResetPasswordVerificationState = {},
            onUserLogout = {},
            onResetUserLogout = {},
            onFinishedCopyingRecoveryKey = {},
            onResetFinishedCopyingRecoveryKey = {},
            onExhaustedPasswordAttempts = {},
            onResetExhaustedPasswordAttempts = {},
            onPrintRecoveryKey = {},
            onCopyRecoveryKey = {},
            onSaveRecoveryKey = {},
            onPrintRecoveryKeyConsumed = {},
            onPrintRecoveryKeyCompleted = {},
        )
    }
}
