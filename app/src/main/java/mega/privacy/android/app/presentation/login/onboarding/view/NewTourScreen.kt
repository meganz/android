package mega.privacy.android.app.presentation.login.onboarding.view

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.TextOnlyButton
import mega.android.core.ui.components.indicators.PageControlsIndicator
import mega.android.core.ui.preview.CombinedThemePreviewsTablet
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.devicetype.DeviceType
import mega.android.core.ui.theme.devicetype.LocalDeviceType
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.presentation.login.onboarding.view.TourTestTags.CREATE_ACCOUNT_BUTTON
import mega.privacy.android.app.presentation.login.onboarding.view.TourTestTags.LOG_IN_BUTTON
import mega.privacy.android.app.presentation.login.onboarding.view.TourTestTags.ONBOARDING_IMAGE
import mega.privacy.android.app.presentation.login.onboarding.view.TourTestTags.ONBOARDING_SUBTITLE
import mega.privacy.android.app.presentation.login.onboarding.view.TourTestTags.ONBOARDING_TITLE
import mega.privacy.android.app.presentation.login.onboarding.view.TourTestTags.PAGER_INDICATOR
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePhoneLandscapePreviews
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeTabletLandscapePreviews
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber

@Composable
internal fun NewTourRoute(
    activityViewModel: LoginViewModel,
    onBackPressed: () -> Unit,
    viewModel: TourViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(onBack = onBackPressed)

    AndroidTheme(isDark = uiState.themeMode.isDarkMode()) {
        NewTourScreen(
            modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
            onLoginClick = {
                Timber.d("onLoginClick")
                activityViewModel.setPendingFragmentToShow(LoginFragmentType.Login)
            },
            onCreateAccountClick = {
                Timber.d("onRegisterClick")
                activityViewModel.setPendingFragmentToShow(LoginFragmentType.CreateAccount)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NewTourScreen(
    onLoginClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTablet = LocalDeviceType.current == DeviceType.Tablet
    val orientation = LocalConfiguration.current.orientation
    val isPhoneLandscape =
        orientation == Configuration.ORIENTATION_LANDSCAPE && !isTablet
    val isTabletPortrait =
        orientation == Configuration.ORIENTATION_PORTRAIT && isTablet

    val pagerItems = listOf(
        TourCarouselPagerItem.First,
        TourCarouselPagerItem.Second,
        TourCarouselPagerItem.Third,
        TourCarouselPagerItem.Fourth
    )

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { Int.MAX_VALUE })
    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            HorizontalPager(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = pagerState,
            ) { pageIndex ->
                val pageItem = pagerItems[pageIndex % pagerItems.size]
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = if (isTabletPortrait) Alignment.Center else Alignment.TopCenter
                ) {
                    OnboardingPageContent(
                        pageIndex = pageIndex,
                        pageItem = pageItem,
                        isTablet = isTablet,
                        isPhoneLandscape = isPhoneLandscape
                    )
                }
            }
        }

        PageControlsIndicator(
            modifier = Modifier.testTag(PAGER_INDICATOR),
            pagerState = pagerState,
            itemCount = pagerItems.size,
            onClick = {}
        )

        if (isPhoneLandscape) {
            ButtonsRow(onCreateAccountClick, onLoginClick)
        } else {
            ButtonsColumn(onCreateAccountClick, onLoginClick, isTablet)
        }
    }
}

@Composable
private fun OnboardingPageContent(
    pageIndex: Int,
    pageItem: TourCarouselPagerItem,
    isTablet: Boolean,
    isPhoneLandscape: Boolean,
) {
    val spacing = LocalSpacing.current
    if (isPhoneLandscape) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.x24, vertical = spacing.x16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .testTag("${ONBOARDING_IMAGE}_$pageIndex")
                    .size(TOUR_SCREEN_IMAGE_SIZE_PHONE_LANDSCAPE_OR_SMALL_PHONE_PORTRAIT.dp)
                    .weight(1f),
                painter = painterResource(id = pageItem.image),
                contentDescription = stringResource(id = pageItem.title)
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                OnboardingTitle(
                    pageIndex = pageIndex,
                    titleRes = pageItem.title,
                    isTablet = isTablet,
                    isPhoneLandscape = true
                )
                OnboardingSubtitle(
                    pageIndex = pageIndex,
                    subtitleRes = pageItem.subtitle,
                    isTablet = isTablet
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .padding(horizontal = spacing.x16),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val isSmallDevice = isSmallDevice()

            val imageSize = when {
                isTablet -> TOUR_SCREEN_TABLET_IMAGE_SIZE
                isSmallDevice -> TOUR_SCREEN_IMAGE_SIZE_PHONE_LANDSCAPE_OR_SMALL_PHONE_PORTRAIT
                else -> TOUR_SCREEN_IMAGE_SIZE_PHONE_PORTRAIT
            }

            Image(
                modifier = Modifier
                    .testTag("${ONBOARDING_IMAGE}_$pageIndex")
                    .padding(top = spacing.x16)
                    .size(imageSize.dp),
                painter = painterResource(id = pageItem.image),
                contentDescription = stringResource(id = pageItem.title)
            )
            OnboardingTitle(
                pageIndex = pageIndex,
                titleRes = pageItem.title,
                isTablet = isTablet,
                isPhoneLandscape = false
            )
            OnboardingSubtitle(
                pageIndex = pageIndex,
                subtitleRes = pageItem.subtitle,
                isTablet = isTablet
            )
        }
    }
}

@Composable
private fun isSmallDevice(): Boolean {
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current

    val screenHeightDp = with(density) { containerSize.height.toDp() }
    val screenWidthDp = with(density) { containerSize.width.toDp() }

    return screenHeightDp <= 640.dp || screenWidthDp <= 360.dp
}

@Composable
private fun OnboardingTitle(
    pageIndex: Int,
    titleRes: Int,
    isTablet: Boolean,
    isPhoneLandscape: Boolean,
) {
    val spacing = LocalSpacing.current
    MegaText(
        modifier = Modifier
            .testTag("${ONBOARDING_TITLE}_$pageIndex")
            .then(
                Modifier.adaptiveWidth(isTablet)
            )
            .padding(top = if (isPhoneLandscape) 0.dp else spacing.x24),
        text = stringResource(id = titleRes),
        textAlign = TextAlign.Center,
        style = AppTheme.typography.headlineMedium,
        textColor = TextColor.Primary,
    )
}

@Composable
private fun OnboardingSubtitle(pageIndex: Int, subtitleRes: Int, isTablet: Boolean) {
    val spacing = LocalSpacing.current
    MegaText(
        modifier = Modifier
            .testTag("${ONBOARDING_SUBTITLE}_$pageIndex")
            .then(
                Modifier.adaptiveWidth(isTablet)
            )
            .padding(top = spacing.x16),
        text = stringResource(id = subtitleRes),
        textAlign = TextAlign.Center,
        style = AppTheme.typography.bodyMedium,
        textColor = TextColor.Secondary,
    )
}

@Composable
private fun ButtonsRow(
    onCreateAccountClick: () -> Unit,
    onLoginClick: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = spacing.x16, end = spacing.x16, bottom = spacing.x20),
        horizontalArrangement = Arrangement.spacedBy(spacing.x16)
    ) {
        PrimaryFilledButton(
            modifier = Modifier
                .testTag(CREATE_ACCOUNT_BUTTON)
                .height(spacing.x48)
                .weight(1f),
            text = stringResource(id = sharedR.string.general_label_create_account),
            onClick = onCreateAccountClick,
        )

        TextOnlyButton(
            modifier = Modifier
                .testTag(LOG_IN_BUTTON)
                .height(spacing.x48)
                .weight(1f),
            text = stringResource(id = sharedR.string.login_text),
            onClick = onLoginClick
        )
    }
}

@Composable
private fun ButtonsColumn(
    onCreateAccountClick: () -> Unit,
    onLoginClick: () -> Unit,
    isTablet: Boolean,
) {
    val spacing = LocalSpacing.current
    Column {
        PrimaryFilledButton(
            modifier = Modifier
                .testTag(CREATE_ACCOUNT_BUTTON)
                .then(
                    Modifier.adaptiveWidth(isTablet, 16.dp)
                )
                .padding(bottom = spacing.x16)
                .height(spacing.x48),
            text = stringResource(id = sharedR.string.general_label_create_account),
            onClick = onCreateAccountClick,
        )

        TextOnlyButton(
            modifier = Modifier
                .testTag(LOG_IN_BUTTON)
                .then(
                    Modifier.adaptiveWidth(isTablet, 16.dp)
                )
                // Temporary add bottom padding to avoid the buttons over the bottom navigation bar
                // it will be removed when all the fragments are migrated to full Compose
                .padding(bottom = spacing.x56)
                .height(spacing.x48),
            text = stringResource(id = sharedR.string.login_text),
            onClick = onLoginClick
        )
    }
}

private fun Modifier.adaptiveWidth(isTablet: Boolean, horizontalPadding: Dp = 0.dp): Modifier =
    if (isTablet) this.width(TOUR_SCREEN_TABLET_WIDTH.dp) else this
        .fillMaxWidth()
        .padding(horizontal = horizontalPadding)

@CombinedThemePreviews
@Composable
fun NewTourScreenPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NewTourScreen(
            onLoginClick = {},
            onCreateAccountClick = {}
        )
    }
}

@CombinedThemePhoneLandscapePreviews
@Composable
fun NewTourScreenPhoneLandscapePreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NewTourScreen(
            onLoginClick = {},
            onCreateAccountClick = {}
        )
    }
}

@CombinedThemePreviewsTablet
@Composable
fun NewTourScreenTabletPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NewTourScreen(
            onLoginClick = {},
            onCreateAccountClick = {}
        )
    }
}

@CombinedThemeTabletLandscapePreviews
@Composable
fun NewTourScreenTabletLandscapePreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NewTourScreen(
            onLoginClick = {},
            onCreateAccountClick = {}
        )
    }
}


internal object TourTestTags {
    private const val USP_CAROUSEL_SCREEN = "usp_carousel_screen"
    internal const val ONBOARDING_IMAGE = "$USP_CAROUSEL_SCREEN:onboarding_image"
    internal const val ONBOARDING_TITLE = "$USP_CAROUSEL_SCREEN:onboarding_title"
    internal const val ONBOARDING_SUBTITLE = "$USP_CAROUSEL_SCREEN:onboarding_subtitle"
    internal const val PAGER_INDICATOR = "$USP_CAROUSEL_SCREEN:pager_indicator"
    internal const val CREATE_ACCOUNT_BUTTON = "$USP_CAROUSEL_SCREEN:create_account_button"
    internal const val LOG_IN_BUTTON = "$USP_CAROUSEL_SCREEN:log_in_button"
}

private const val TOUR_SCREEN_TABLET_WIDTH = 500
private const val TOUR_SCREEN_IMAGE_SIZE_PHONE_PORTRAIT = 380
private const val TOUR_SCREEN_IMAGE_SIZE_PHONE_LANDSCAPE_OR_SMALL_PHONE_PORTRAIT = 204
private const val TOUR_SCREEN_TABLET_IMAGE_SIZE = 358