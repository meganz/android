package mega.privacy.android.app.presentation.login.confirmemail.changeemail

import android.content.res.Configuration
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaSnackbar
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.banner.BannerPaddingProvider
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.indicators.LargeHUD
import mega.android.core.ui.components.inputfields.TextInputField
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.preview.CombinedThemePreviewsTablet
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.devicetype.DeviceType
import mega.android.core.ui.theme.devicetype.LocalDeviceType
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.presentation.login.view.tabletScreenWidth
import mega.privacy.android.shared.resources.R as sharedR

internal const val changeEmailAddressRoute = "changeEmailAddress"
internal const val EMAIL = "email"
internal const val FULL_NAME = "full_name"

/**
 * function to build the ChangeEmailAddress screen.
 */
fun NavGraphBuilder.changeEmailAddress(
    onChangeEmailSuccess: (String) -> Unit,
) {
    composable(
        route = "$changeEmailAddressRoute?$EMAIL={$EMAIL}&$FULL_NAME={$FULL_NAME}",
        arguments = listOf(
            navArgument(EMAIL) { type = NavType.StringType },
            navArgument(FULL_NAME) { type = NavType.StringType }),
    ) {
        ChangeEmailAddressRoute(
            onChangeEmailSuccess = onChangeEmailSuccess
        )
    }
}

/**
 * Navigation for [ChangeEmailAddressRoute]
 */
fun NavController.navigateToChangeEmailAddress(
    email: String?,
    fullName: String?,
    navOptions: NavOptions? = null,
) {
    this.navigate(
        route = "$changeEmailAddressRoute?$EMAIL=$email&$FULL_NAME=$fullName",
        navOptions = navOptions
    )
}

@Composable
private fun ChangeEmailAddressRoute(
    onChangeEmailSuccess: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChangeEmailAddressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChangeEmailAddressScreen(
        modifier = modifier,
        uiState = uiState,
        onChangeEmailPressed = viewModel::changeEmailAddress,
        onEmailInputChanged = viewModel::onEmailInputChanged,
        onResetGeneralErrorEvent = viewModel::resetGeneralErrorEvent,
        onResetChangeEmailAddressSuccessEvent = viewModel::resetChangeEmailAddressSuccessEvent,
        onResetAccountExistEvent = viewModel::resetAccountExistEvent,
        onChangeEmailSuccess = onChangeEmailSuccess,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ChangeEmailAddressScreen(
    uiState: ChangeEmailAddressUIState,
    onEmailInputChanged: (String?) -> Unit,
    onChangeEmailPressed: () -> Unit,
    onResetGeneralErrorEvent: () -> Unit,
    onResetChangeEmailAddressSuccessEvent: () -> Unit,
    onResetAccountExistEvent: () -> Unit,
    onChangeEmailSuccess: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onBackPressedDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    val snackBarHostState = remember { SnackbarHostState() }
    var email by rememberSaveable { mutableStateOf(uiState.email) }
    val softKeyboard = LocalSoftwareKeyboardController.current
    var accountExists by rememberSaveable { mutableStateOf(false) }
    val orientation = LocalConfiguration.current.orientation
    val context = LocalContext.current
    val isTablet = LocalDeviceType.current == DeviceType.Tablet
    val isPhoneLandscape =
        orientation == Configuration.ORIENTATION_LANDSCAPE && !isTablet

    EventEffect(event = uiState.generalErrorEvent, onConsumed = onResetGeneralErrorEvent) {
        snackBarHostState.showSnackbar(context.getString(sharedR.string.general_request_failed_message))
    }

    EventEffect(
        event = uiState.changeEmailAddressSuccessEvent,
        onConsumed = onResetChangeEmailAddressSuccessEvent
    ) {
        onChangeEmailSuccess(email)
    }

    EventEffect(event = uiState.accountExistEvent, onConsumed = onResetAccountExistEvent) {
        accountExists = true
    }
    MegaScaffold(
        modifier = modifier
            .systemBarsPadding()
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        snackbarHost = {
            MegaSnackbar(
                snackBarHostState = snackBarHostState,
            )
        },
        topBar = {
            MegaTopAppBar(
                modifier = Modifier
                    .padding(top = BannerPaddingProvider.current)
                    .testTag(CHANGE_EMAIL_ADDRESS_SCREEN_TOP_BAR_TAG),
                title = stringResource(sharedR.string.change_email_address_title),
                navigationType = AppBarNavigationType.Back(
                    {
                        onBackPressedDispatcherOwner?.onBackPressedDispatcher?.onBackPressed()
                    },
                ),
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val contentModifier = if (isTablet || isPhoneLandscape) {
                Modifier
                    .fillMaxHeight()
                    .width(tabletScreenWidth(orientation))
            } else {
                Modifier
                    .fillMaxSize()
            }

            Column(modifier = contentModifier) {
                MegaText(
                    modifier = Modifier
                        .padding(
                            horizontal = LocalSpacing.current.x16,
                            vertical = LocalSpacing.current.x24
                        )
                        .testTag(CHANGE_EMAIL_ADDRESS_SCREEN_DESCRIPTION_TAG),
                    textColor = TextColor.Secondary,
                    style = AppTheme.typography.bodyLarge,
                    text = stringResource(sharedR.string.change_email_address_content)
                )

                TextInputField(
                    modifier = Modifier
                        .padding(
                            top = LocalSpacing.current.x16,
                            start = LocalSpacing.current.x16,
                            end = LocalSpacing.current.x16
                        )
                        .testTag(CHANGE_EMAIL_ADDRESS_SCREEN_EMAIL_INPUT_TAG),
                    text = email,
                    label = stringResource(id = sharedR.string.email_text),
                    onValueChanged = {
                        email = it
                        accountExists = false
                        onEmailInputChanged(it)
                    },
                    keyboardType = KeyboardType.Email,
                    errorText = when {
                        uiState.isEmailValid == false -> stringResource(id = sharedR.string.login_invalid_email_error_message)
                        accountExists -> stringResource(sharedR.string.sign_up_account_existed_error_message)
                        else -> null
                    }
                )

                PrimaryFilledButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = LocalSpacing.current.x16,
                            vertical = LocalSpacing.current.x48
                        )
                        .height(height = 48.dp)
                        .align(Alignment.CenterHorizontally)
                        .testTag(CHANGE_EMAIL_ADDRESS_SCREEN_UPDATE_BUTTON_TAG),
                    text = stringResource(sharedR.string.general_update),
                    onClick = {
                        softKeyboard?.hide()
                        onChangeEmailPressed()
                    },
                )

                if (uiState.isLoading) {
                    LargeHUD(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .testTag(CHANGE_EMAIL_ADDRESS_SCREEN_LOADING_INDICATOR_TAG)
                    )
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ChangeEmailAddressPreview() {
    AndroidThemeForPreviews {
        ChangeEmailAddressScreen(
            uiState = ChangeEmailAddressUIState(),
            onEmailInputChanged = {},
            onChangeEmailPressed = { String.toString() },
            onResetGeneralErrorEvent = {},
            onResetChangeEmailAddressSuccessEvent = {},
            onResetAccountExistEvent = {},
            onChangeEmailSuccess = {}
        )
    }
}

@CombinedThemePreviewsTablet
@Composable
private fun ChangeEmailAddressTabletPreview() {
    AndroidThemeForPreviews {
        ChangeEmailAddressScreen(
            uiState = ChangeEmailAddressUIState(),
            onEmailInputChanged = {},
            onChangeEmailPressed = { String.toString() },
            onResetGeneralErrorEvent = {},
            onResetChangeEmailAddressSuccessEvent = {},
            onResetAccountExistEvent = {},
            onChangeEmailSuccess = {}
        )
    }
}

internal const val CHANGE_EMAIL_ADDRESS_SCREEN_TOP_BAR_TAG = "change_email_address_screen:top_bar"
internal const val CHANGE_EMAIL_ADDRESS_SCREEN_DESCRIPTION_TAG =
    "change_email_address_screen:text_description"
internal const val CHANGE_EMAIL_ADDRESS_SCREEN_EMAIL_INPUT_TAG =
    "change_email_address_screen:input_field_email"
internal const val CHANGE_EMAIL_ADDRESS_SCREEN_UPDATE_BUTTON_TAG =
    "change_email_address_screen:button_update"
internal const val CHANGE_EMAIL_ADDRESS_SCREEN_LOADING_INDICATOR_TAG =
    "change_email_address_screen:loading_indicator"
