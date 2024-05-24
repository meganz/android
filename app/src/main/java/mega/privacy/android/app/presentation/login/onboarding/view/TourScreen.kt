package mega.privacy.android.app.presentation.login.onboarding.view

import mega.privacy.android.shared.resources.R as SharedR
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.extensions.navigateToAppSettings
import mega.privacy.android.app.presentation.login.onboarding.model.OnboardingUiItem
import mega.privacy.android.shared.original.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.pager.InfiniteHorizontalPagerWithIndicator
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.tokens.TextColor
import mega.privacy.android.shared.theme.MegaAppTheme
import timber.log.Timber

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TourRoute(
    onLoginClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TourScreen(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        onLoginClick = onLoginClick,
        onCreateAccountClick = onCreateAccountClick,
    )
}

@Composable
internal fun TourScreen(
    onLoginClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val scrollState = rememberScrollState()

    // For small screen like nexus one or bigger screen, this is to force the scroll view to bottom to show buttons
    // Meanwhile, tour image glide could also be shown
    LaunchedEffect(Unit) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    val bluetoothLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Timber.d("onActivityResult: PERMISSION GRANTED")
            } else {
                Timber.d("onActivityResult: PERMISSION DENIED")
                coroutineScope.launch {
                    val result = snackBarHostState.showSnackbar(
                        message = context.getString(R.string.meeting_bluetooth_connect_required_permissions_warning),
                        actionLabel = context.getString(R.string.action_settings)
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        context.navigateToAppSettings()
                    }
                }
            }
        }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalTourPager(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp),
                    item = getOnboardingUiItem(context)
                )

                Row(
                    modifier = Modifier.padding(top = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TextMegaButton(
                        textId = R.string.login_text,
                        onClick = onLoginClick
                    )

                    RaisedDefaultMegaButton(
                        textId = R.string.create_account,
                        onClick = onCreateAccountClick
                    )
                }
            }

            OutlinedMegaButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 40.dp, start = 24.dp, end = 24.dp),
                textId = R.string.join_meeting_as_guest,
                onClick = {
                    Timber.d("onJoinMeetingAsGuestClick")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        when {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                // VAP: Show meeting link dialog later.
                            }

                            else -> {
                                bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            }
                        }
                    } else {
                        // VAP: Show meeting link dialog later.
                    }
                },
                rounded = false
            )
        }

        SnackbarHost(
            modifier = Modifier
                .safeDrawingPadding()
                .align(Alignment.BottomCenter),
            hostState = snackBarHostState
        )
    }
}

private fun getOnboardingUiItem(context: Context) = OnboardingUiItem(
    drawableImageResources = listOf(
        R.drawable.tour1,
        R.drawable.tour2,
        R.drawable.tour3,
        R.drawable.tour4,
        R.drawable.tour5
    ),
    titles = context.resources.getStringArray(SharedR.array.cloud_drive_tour_titles).toList(),
    subtitles = context.resources.getStringArray(SharedR.array.cloud_drive_tour_subtitles).toList()
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HorizontalTourPager(
    item: OnboardingUiItem,
    modifier: Modifier = Modifier,
) {
    InfiniteHorizontalPagerWithIndicator(
        modifier = modifier,
        pageCount = item.drawableImageResources.size,
        isOverScrollModeEnable = false,
        // We need to know the tallest height among the onboarding items.
        // Since the initial page is the middle item,
        // we only need to render the half left and right sides.
        beyondBoundsPageCount = item.drawableImageResources.size / 2,
        verticalAlignment = Alignment.Top
    ) { page ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val painter = painterResource(id = item.drawableImageResources[page])
            val imageRatio = painter.intrinsicSize.width / painter.intrinsicSize.height
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(imageRatio),
                painter = painter,
                contentScale = ContentScale.Fit,
                contentDescription = item.titles[page]
            )

            MegaText(
                modifier = Modifier.padding(top = 20.dp),
                text = item.titles[page],
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.h6
            )

            MegaText(
                modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp),
                text = item.subtitles[page],
                textAlign = TextAlign.Center,
                textColor = TextColor.Secondary,
                style = MaterialTheme.typography.subtitle2
            )
        }
    }
}

@CombinedTextAndThemePreviews
@Composable
private fun TourScreenPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        TourScreen(
            modifier = Modifier.fillMaxSize(),
            onLoginClick = {},
            onCreateAccountClick = {}
        )
    }
}
