package mega.privacy.android.app.presentation.achievements.info.view

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.app.data.extensions.toUnitString
import mega.privacy.android.app.presentation.achievements.info.AchievementsInfoViewModel
import mega.privacy.android.app.presentation.achievements.info.model.AchievementsInfoUIState
import mega.privacy.android.app.presentation.achievements.info.util.toAchievementsInfoAttribute
import mega.privacy.android.legacy.core.ui.controls.appbar.SimpleTopAppBar
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.conditional
import mega.privacy.android.core.ui.theme.extensions.dark_blue_500_dark_blue_200
import mega.privacy.android.core.ui.theme.extensions.grey_020_dark_grey
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.red_600_red_300
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.core.ui.theme.extensions.white_transparent
import mega.privacy.android.domain.entity.achievement.AchievementType

internal object AchievementsInfoViewTestTags {
    private const val ACHIEVEMENTS_INFO_VIEW = "achievements_info_view"
    const val TOOLBAR = "$ACHIEVEMENTS_INFO_VIEW:toolbar"
    const val MAIN_ICON = "$ACHIEVEMENTS_INFO_VIEW:main_icon"
    const val CHECK_ICON = "$ACHIEVEMENTS_INFO_VIEW:check_icon"
    const val TITLE = "$ACHIEVEMENTS_INFO_VIEW:title"
    const val SUBTITLE = "$ACHIEVEMENTS_INFO_VIEW:subtitle"
    const val HOW_IT_WORKS = "$ACHIEVEMENTS_INFO_VIEW:how_it_works"
}

/**
 * Achievements Information Screen in Jetpack Compose
 */
@Composable
fun AchievementsInfoRoute(viewModel: AchievementsInfoViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AchievementsInfoView(
        modifier = Modifier.fillMaxSize(),
        uiState = uiState
    )
}

@Composable
internal fun AchievementsInfoView(
    modifier: Modifier = Modifier,
    uiState: AchievementsInfoUIState,
) {
    val attributes =
        uiState.achievementType.toAchievementsInfoAttribute(uiState.isAchievementAwarded)

    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = rememberScaffoldState(),
        topBar = {
            SimpleTopAppBar(
                modifier = Modifier.testTag(AchievementsInfoViewTestTags.TOOLBAR),
                titleId = R.string.achievements_title,
                elevation = scrollState.value > 0,
                onBackPressed = {
                    onBackPressedDispatcher?.onBackPressed()
                }
            )
        },
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colors.grey_020_dark_grey)
        ) {

            // Center Icon
            Box(
                modifier = Modifier
                    .padding(top = 48.dp)
                    .align(Alignment.CenterHorizontally)
                    .wrapContentSize()
            ) {
                Image(
                    modifier = Modifier
                        .semantics {
                            contentDescription = attributes.name
                        }
                        .testTag(AchievementsInfoViewTestTags.MAIN_ICON)
                        .wrapContentSize()
                        .align(Alignment.Center),
                    painter = painterResource(id = attributes.iconResourceId),
                    contentDescription = "Main"
                )

                if (uiState.isAchievementAwarded) {
                    Image(
                        modifier = Modifier
                            .testTag(AchievementsInfoViewTestTags.CHECK_ICON)
                            .wrapContentSize()
                            .align(Alignment.TopStart),
                        painter = painterResource(id = R.drawable.ic_check_achievements),
                        contentDescription = "Main"
                    )
                }
            }

            // Title
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .wrapContentSize()
                    .align(Alignment.CenterHorizontally)
                    .conditional(uiState.isAchievementAwarded.not()) {
                        this.background(Color.Transparent)
                    }
                    .conditional(uiState.isAchievementAwarded && uiState.isAchievementAlmostExpired) {
                        this
                            .background(Color.Transparent, RoundedCornerShape(5.dp))
                            .border(1.dp, MaterialTheme.colors.red_600_red_300)
                    }
                    .conditional(uiState.isAchievementAwarded && uiState.isAchievementAlmostExpired.not()) {
                        this
                            .background(
                                MaterialTheme.colors.white_transparent,
                                RoundedCornerShape(5.dp)
                            )
                            .border(1.dp, MaterialTheme.colors.grey_alpha_012_white_alpha_012)
                    }
            ) {
                Text(
                    modifier = Modifier
                        .testTag(AchievementsInfoViewTestTags.TITLE)
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                        .align(Alignment.Center),
                    text = getAchievementTitle(uiState = uiState),
                    style = MaterialTheme.typography.body2.copy(letterSpacing = 0.sp, color = getAchievementTitleColor(uiState = uiState))
                )
            }

            // How it Works Section
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.surface)
            ) {
                Text(
                    modifier = Modifier
                        .testTag(AchievementsInfoViewTestTags.HOW_IT_WORKS)
                        .padding(top = 24.dp)
                        .align(Alignment.CenterHorizontally),
                    text = stringResource(id = R.string.title_achievement_invite_friends),
                    style = MaterialTheme.typography.subtitle1.copy(letterSpacing = 0.sp),
                    color = MaterialTheme.colors.dark_blue_500_dark_blue_200
                )
                // Subtitle
                Text(
                    modifier = Modifier
                        .testTag(AchievementsInfoViewTestTags.SUBTITLE)
                        .padding(24.dp)
                        .align(Alignment.CenterHorizontally),
                    text = stringResource(
                        id = attributes.subtitleTextResourceId,
                        uiState.awardStorageInBytes.toUnitString(LocalContext.current)
                    ),
                    style = MaterialTheme.typography.body2.copy(letterSpacing = 0.sp),
                    color = MaterialTheme.colors.textColorSecondary,
                )
            }
        }
    }
}

@Composable
private fun getAchievementTitle(uiState: AchievementsInfoUIState): String {
    return when {
        uiState.isAchievementAwarded.not() -> stringResource(
            id = R.string.figures_achievements_text,
            uiState.awardStorageInBytes.toUnitString(LocalContext.current)
        )

        uiState.isAchievementExpired.not() -> pluralStringResource(
            id = R.plurals.account_achievements_bonus_expiration_date,
            count = uiState.achievementRemainingDays.toInt(),
            uiState.achievementRemainingDays
        )

        else -> stringResource(id = R.string.expired_label)
    }
}

@Composable
private fun getAchievementTitleColor(uiState: AchievementsInfoUIState): Color {
    return if (uiState.isAchievementAwarded && uiState.isAchievementAlmostExpired) {
        MaterialTheme.colors.red_600_red_300
    } else {
        MaterialTheme.colors.textColorSecondary
    }
}

@Composable
@CombinedThemePreviews
internal fun AchievementsInfoViewPreview() {
    AchievementsInfoView(
        modifier = Modifier,
        uiState = AchievementsInfoUIState(
            achievementType = AchievementType.MEGA_ACHIEVEMENT_MOBILE_INSTALL,
            achievementRemainingDays = 16,
            awardStorageInBytes = 178347128371,
            isAchievementAwarded = false,
            isAchievementExpired = false,
            isAchievementAlmostExpired = false
        )
    )
}
