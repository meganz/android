package mega.privacy.android.app.presentation.login.view

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.indicators.MegaAnimatedLinearProgressIndicator
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import mega.privacy.android.domain.entity.login.TemporaryWaitingError
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Login In Progress Screen.
 *
 * @param modifier Modifier to be applied to the layout
 * @param currentProgress Current progress of the login operation
 * @param currentStatusText Resource ID of the current status text to be displayed
 * @param requestStatusProgress Progress of the request status, if applicable
 */
@Composable
fun LoginInProgressContent(
    isRequestStatusInProgress: Boolean,
    currentProgress: Float,
    @StringRes currentStatusText: Int,
    requestStatusProgress: Progress?,
    modifier: Modifier = Modifier,
) {
    val isInLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_splash_logo),
            contentDescription = stringResource(id = R.string.login_to_mega),
            modifier = Modifier
                .align(Alignment.Center)
                .size(288.dp)
                .testTag(MEGA_LOGO_TEST_TAG),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 56.dp, end = 56.dp)
                    .widthIn(max = 300.dp)
            ) {
                MegaAnimatedLinearProgressIndicator(
                    indicatorProgress = currentProgress,
                    progressAnimDuration = if (currentProgress > 0.5f) 1000 else 3000,
                    modifier = Modifier
                        .testTag(FETCH_NODES_PROGRESS_TEST_TAG)
                )

                if (isRequestStatusInProgress) {
                    val infiniteTransition =
                        rememberInfiniteTransition(label = "Request Status Progress")
                    val shimmerTranslateX by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ), label = "Progress"
                    )
                    // Shimmer Effect
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .fillMaxWidth(fraction = currentProgress + 0.015f)
                            .clip(RoundedCornerShape(20.dp))
                            .graphicsLayer(translationX = shimmerTranslateX * 700f)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.6f),
                                        Color.Transparent
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(
                                        100f,
                                        0f
                                    )
                                ),
                                shape = RoundedCornerShape(40.dp)
                            )
                            .testTag(REQUEST_STATUS_PROGRESS_TEST_TAG)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box {
                LoginInProgressText(
                    stringId = currentStatusText,
                    progress = requestStatusProgress,
                    modifier = Modifier
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                        )
                        .testTag(CONNECTING_TO_SERVER_TAG)
                )
                // White-space to prevent jumping when visibility animates
                MegaText(
                    text = " ",
                    style = MaterialTheme.typography.bodyMedium,
                    textColor = TextColor.Primary,
                    minLines = 2
                )
            }
            Spacer(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(if (isInLandscape) 0.dp else 20.dp)
            )
        }
    }
}

/**
 * Composable to show current status text with a fade in/out animation.
 */
@Composable
private fun LoginInProgressText(
    modifier: Modifier,
    @StringRes stringId: Int,
    progress: Progress? = null,
    textChangeDuration: Long = 200,
) {
    val isInPreview = LocalInspectionMode.current // To avoid text being hidden in previews
    var visible by rememberSaveable { mutableStateOf(isInPreview) }
    var currentTextId by rememberSaveable { mutableIntStateOf(stringId) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        MegaText(
            text = if (progress != null) {
                stringResource(sharedR.string.login_completing_operation, progress.intValue)
            } else {
                stringResource(id = currentTextId)
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier,
            textAlign = TextAlign.Center,
            textColor = TextColor.Primary,
            minLines = 2
        )
    }

    LaunchedEffect(stringId) {
        visible = false
        delay(textChangeDuration)
        currentTextId = stringId
        visible = true
    }
}

@CombinedThemePreviews
@Composable
private fun LoginInProgressScreenPreview(
    @PreviewParameter(LoginInProgressStateProvider::class) state: LoginState,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        LoginInProgressContent(
            isRequestStatusInProgress = state.isRequestStatusInProgress,
            currentProgress = state.currentProgress,
            currentStatusText = state.currentStatusText,
            requestStatusProgress = state.requestStatusProgress,
            modifier = Modifier
                .fillMaxSize()
        )
    }
}

/**
 * LoginInProgressState parameter provider for compose previews.
 */
private class LoginInProgressStateProvider : PreviewParameterProvider<LoginState> {
    override val values = listOf(
        LoginState(
            isLoginInProgress = true,
        ),
        LoginState(
            isLoginInProgress = true,
            requestStatusProgress = Progress(0.2f)
        ),
        LoginState(
            isLoginInProgress = true,
            requestStatusProgress = Progress(0.7f)
        ),
        LoginState(
            fetchNodesUpdate = FetchNodesUpdate(
                progress = Progress(0.5F),
                temporaryError = TemporaryWaitingError.ConnectivityIssues
            ),
        ),
    ).asSequence()
}

internal const val MEGA_LOGO_TEST_TAG = "MEGA_LOGO"
internal const val FETCH_NODES_PROGRESS_TEST_TAG = "FETCH_NODES_PROGRESS"
internal const val REQUEST_STATUS_PROGRESS_TEST_TAG = "login_in_progress:request_status_progress"
internal const val CONNECTING_TO_SERVER_TAG =
    "login_in_progress:login_in_progress_text_connecting_to_the_server" 