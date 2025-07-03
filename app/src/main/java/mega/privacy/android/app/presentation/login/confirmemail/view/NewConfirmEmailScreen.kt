package mega.privacy.android.app.presentation.login.confirmemail.view

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.LinkSpannedText
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaSnackbar
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.banner.BannerPaddingProvider
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.TextOnlyButton
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.indicators.LargeHUD
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.components.util.shimmerEffect
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.devicetype.DeviceType
import mega.android.core.ui.theme.devicetype.LocalDeviceType
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.LinkColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.confirmemail.ConfirmEmailViewModel
import mega.privacy.android.app.presentation.login.confirmemail.model.ConfirmEmailUiState
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.presentation.login.view.tabletScreenWidth
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ChangeEmailAddressButtonPressedEvent
import mega.privacy.mobile.analytics.event.ResendEmailConfirmationButtonPressedEvent

/**
 * Route for the Confirm Email composable.
 */
const val confirmEmailRoute = "confirmEmailRoute"

/**
 * Add the Confirm Email composable to the navigation graph.
 */
fun NavGraphBuilder.confirmEmail(
    fullName: String?,
    onShowPendingFragment: (loginFragmentType: LoginFragmentType) -> Unit,
    onSetTemporalEmail: (email: String) -> Unit,
    onNavigateToChangeEmailAddress: () -> Unit,
    onNavigateToHelpCentre: () -> Unit,
    viewModel: ConfirmEmailViewModel,
) {
    composable(confirmEmailRoute) {
        NewConfirmEmailRoute(
            fullName = fullName,
            onShowPendingFragment = onShowPendingFragment,
            onSetTemporalEmail = onSetTemporalEmail,
            onNavigateToChangeEmailAddress = onNavigateToChangeEmailAddress,
            onNavigateToHelpCentre = onNavigateToHelpCentre,
            viewModel = viewModel
        )
    }
}

@Composable
private fun NewConfirmEmailRoute(
    fullName: String?,
    onShowPendingFragment: (loginFragmentType: LoginFragmentType) -> Unit,
    onSetTemporalEmail: (email: String) -> Unit,
    onNavigateToChangeEmailAddress: () -> Unit,
    onNavigateToHelpCentre: () -> Unit,
    viewModel: ConfirmEmailViewModel = hiltViewModel(),
) {
    val snackBarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isPendingToShowFragment) {
        uiState.isPendingToShowFragment?.let {
            onShowPendingFragment(it)
            viewModel.isPendingToShowFragmentConsumed()
        }
    }

    LaunchedEffect(uiState.registeredEmail) {
        uiState.registeredEmail?.let {
            onSetTemporalEmail(it)
        }
    }

    val successMessage = stringResource(id = sharedR.string.general_email_resend_success_message)
    LaunchedEffect(uiState.shouldShowSuccessMessage) {
        if (uiState.shouldShowSuccessMessage) {
            snackBarHostState.showSnackbar(
                message = successMessage
            )
            viewModel.onSuccessMessageDisplayed()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackBarHostState.showSnackbar(
                message = it
            )
            viewModel.onErrorMessageDisplayed()
        }
    }

    val accountExistMessage =
        stringResource(id = sharedR.string.sign_up_account_existed_error_message)
    EventEffect(
        event = uiState.accountExistEvent,
        onConsumed = viewModel::resetAccountExistEvent
    ) {
        snackBarHostState.showSnackbar(
            message = accountExistMessage
        )
    }

    val generalErrorMessage = stringResource(id = sharedR.string.general_request_failed_message)
    EventEffect(
        event = uiState.generalErrorEvent,
        onConsumed = viewModel::resetGeneralErrorEvent
    ) {

        snackBarHostState.showSnackbar(
            message = generalErrorMessage
        )
    }

    NewConfirmEmailScreen(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxSize(),
        email = uiState.registeredEmail.orEmpty(),
        uiState = uiState,
        onCancelClick = {
            viewModel.cancelCreateAccount()
        },
        onResendSignUpLink = {
            viewModel.resendSignUpLink(email = it, fullName = fullName)
        },
        onNavigateToChangeEmailAddress = onNavigateToChangeEmailAddress,
        snackBarHostState = snackBarHostState,
        onNavigateToHelpCentre = onNavigateToHelpCentre
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NewConfirmEmailScreen(
    email: String,
    uiState: ConfirmEmailUiState,
    onCancelClick: () -> Unit,
    onResendSignUpLink: (email: String) -> Unit,
    onNavigateToChangeEmailAddress: () -> Unit,
    onNavigateToHelpCentre: () -> Unit,
    snackBarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    var cancelRegistration by remember { mutableStateOf(false) }
    val spacing = LocalSpacing.current
    val orientation = LocalConfiguration.current.orientation
    val isTablet = LocalDeviceType.current == DeviceType.Tablet
    val isPhoneLandscape =
        orientation == Configuration.ORIENTATION_LANDSCAPE && !isTablet
    val helpCentre = stringResource(id = sharedR.string.general_help_centre)

    MegaScaffold(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        snackbarHost = {
            MegaSnackbar(
                snackBarHostState = snackBarHostState,
            )
        },
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.padding(top = BannerPaddingProvider.current),
                title = "",
                navigationType = AppBarNavigationType.Close(
                    onNavigationIconClicked = {
                        cancelRegistration = true
                    },
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(paddingValues)
        ) {
            val contentModifier = if (isTablet || isPhoneLandscape) {
                Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxHeight()
                    .width(tabletScreenWidth(orientation))
                    .padding(
                        top = if (isPhoneLandscape) spacing.x24 else spacing.x48,
                        bottom = spacing.x24
                    )
                    .align(Alignment.TopCenter)
            } else {
                Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
                    .padding(horizontal = spacing.x16, vertical = spacing.x24)
            }

            Column(
                modifier = contentModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    modifier = Modifier.size(120.dp),
                    painter = painterResource(id = R.drawable.ic_sign_up_email_confirmation),
                    contentDescription = "Email confirmation icon"
                )

                MegaText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.x24),
                    text = stringResource(sharedR.string.email_confirmation_title),
                    style = AppTheme.typography.titleLarge,
                    textColor = TextColor.Primary
                )

                if (email.isNotBlank()) {
                    LinkSpannedText(
                        modifier = Modifier.padding(top = spacing.x16),
                        value = String.format(
                            stringResource(sharedR.string.email_confirmation_content),
                            email,
                            helpCentre
                        ),
                        spanStyles = hashMapOf(
                            SpanIndicator('A') to SpanStyleWithAnnotation(
                                MegaSpanStyle.LinkColorStyle(
                                    SpanStyle(),
                                    LinkColor.Primary
                                ), helpCentre
                            ),
                            SpanIndicator('P') to SpanStyleWithAnnotation(
                                MegaSpanStyle.TextColorStyle(
                                    SpanStyle().copy(fontWeight = AppTheme.typography.titleMedium.fontWeight),
                                    TextColor.Primary
                                ), email
                            )
                        ),
                        baseStyle = AppTheme.typography.bodyLarge,
                        baseTextColor = TextColor.Secondary,
                        onAnnotationClick = {
                            if (it == helpCentre) {
                                onNavigateToHelpCentre()
                            }
                        }
                    )
                } else {
                    ContentLoading()
                }

                Spacer(Modifier.weight(1f))

                Column(
                    modifier = Modifier
                        .padding(top = spacing.x16)
                        .fillMaxWidth()
                ) {
                    PrimaryFilledButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                        text = stringResource(sharedR.string.general_resend_button),
                        onClick = {
                            Analytics.tracker.trackEvent(ResendEmailConfirmationButtonPressedEvent)
                            onResendSignUpLink(email)
                        },
                    )

                    TextOnlyButton(
                        modifier = Modifier
                            .padding(top = spacing.x16)
                            .height(height = spacing.x48)
                            .align(Alignment.CenterHorizontally),
                        text = stringResource(sharedR.string.general_change_email_address),
                        onClick = {
                            Analytics.tracker.trackEvent(ChangeEmailAddressButtonPressedEvent)
                            onNavigateToChangeEmailAddress()
                        }
                    )
                }
            }

            if (cancelRegistration) {
                CancelRegistrationDialog(
                    onConfirmRequest = {
                        cancelRegistration = false
                        onCancelClick()
                    },
                    onDismissRequest = { cancelRegistration = false },
                )
            }

            if (uiState.isLoading) {
                BoxSurface(
                    modifier = Modifier.fillMaxSize(),
                    surfaceColor = SurfaceColor.Blur
                ) {
                    LargeHUD(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
private fun CancelRegistrationDialog(
    onConfirmRequest: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    BasicDialog(
        onDismiss = onDismissRequest,
        title = stringResource(sharedR.string.email_confirmation_dialog_cancel_registration_title),
        description = stringResource(sharedR.string.email_confirmation_dialog_cancel_registration_content),
        positiveButtonText = stringResource(sharedR.string.email_confirmation_dialog_cancel_registration_cancel_button),
        negativeButtonText = stringResource(sharedR.string.email_confirmation_dialog_cancel_registration_dismiss_button),
        onPositiveButtonClicked = onConfirmRequest,
        onNegativeButtonClicked = onDismissRequest
    )
}

@Composable
private fun ContentLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = LocalSpacing.current.x16),
        verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.x16)
    ) {
        (0 until 6).forEach { _ ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .shimmerEffect(),
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ConfirmEmailScreenPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NewConfirmEmailScreen(
            modifier = Modifier.fillMaxSize(),
            email = "email@email.com",
            uiState = ConfirmEmailUiState(),
            onCancelClick = {},
            onResendSignUpLink = {},
            onNavigateToChangeEmailAddress = {},
            onNavigateToHelpCentre = {},
            snackBarHostState = remember { SnackbarHostState() }
        )
    }
}
