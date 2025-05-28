package mega.privacy.android.app.presentation.achievements.freetrial.view

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.legacy.core.ui.controls.appbar.SimpleTopAppBar
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.dark_blue_500_dark_blue_200
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_020_dark_grey
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.shared.resources.R as sharedR


@Composable
internal fun FreeTrialView(
    @DrawableRes icon: Int,
    freeTrialText: String,
    @StringRes installButtonText: Int,
    howItWorksText: String,
    modifier: Modifier = Modifier,
    isReceivedAward: Boolean = false,
    installButtonClicked: () -> Unit = {},
) {
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val scrollState = rememberScrollState()

    MegaScaffold(
        modifier = modifier,
        topBar = {
            SimpleTopAppBar(
                modifier = Modifier.testTag(FreeTrialViewTestTags.TOOLBAR),
                titleId = R.string.achievements_title,
                elevation = scrollState.value > 0,
                onBackPressed = {
                    onBackPressedDispatcher?.onBackPressed()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colors.grey_020_dark_grey)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 6.dp)
                    .background(MaterialTheme.colors.surface)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 20.dp)
                ) {
                    Image(
                        modifier = Modifier
                            .size(120.dp)
                            .testTag(FreeTrialViewTestTags.IMAGE_MAIN),
                        painter = painterResource(id = icon),
                        contentDescription = "Free Trial Image"
                    )

                    if (isReceivedAward) {
                        Image(
                            modifier = Modifier
                                .size(32.dp)
                                .testTag(FreeTrialViewTestTags.CHECK_ICON),
                            painter = painterResource(id = iconPackR.drawable.ic_achievements_check),
                            contentDescription = "Check icon"
                        )
                    }
                }

                Text(
                    modifier = Modifier
                        .testTag(FreeTrialViewTestTags.DESCRIPTION)
                        .padding(top = 30.dp, start = 24.dp, end = 24.dp)
                        .align(Alignment.CenterHorizontally),
                    text = freeTrialText,
                    color = MaterialTheme.colors.textColorSecondary,
                    textAlign = TextAlign.Center
                )
                RaisedDefaultMegaButton(
                    modifier = Modifier
                        .testTag(FreeTrialViewTestTags.INSTALL_APP_BUTTON)
                        .padding(top = 30.dp, bottom = 26.dp)
                        .align(Alignment.CenterHorizontally),
                    textId = installButtonText,
                    onClick = installButtonClicked,
                    enabled = !isReceivedAward
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 6.dp)
                    .weight(1f)
                    .background(MaterialTheme.colors.surface)
            ) {
                if (!isReceivedAward) {
                    Text(
                        modifier = Modifier
                            .testTag(FreeTrialViewTestTags.HOW_IT_WORKS_TITLE)
                            .padding(top = 24.dp)
                            .align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.subtitle1.copy(
                            letterSpacing = 0.sp
                        ),
                        color = MaterialTheme.colors.dark_blue_500_dark_blue_200,
                        text = stringResource(id = sharedR.string.title_how_it_works_free_trial)
                    )
                }

                Text(
                    modifier = Modifier
                        .testTag(FreeTrialViewTestTags.HOW_IT_WORKS_DESCRIPTION)
                        .padding(all = 24.dp)
                        .fillMaxSize()
                        .align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.body2.copy(
                        letterSpacing = 0.sp
                    ),
                    color = MaterialTheme.colors.textColorSecondary,
                    text = howItWorksText
                )
            }
        }
    }
}

internal object FreeTrialViewTestTags {
    private const val FREE_TRIAL_VIEW = "achievement_free_trial"
    const val TOOLBAR = "$FREE_TRIAL_VIEW:toolbar"
    const val IMAGE_MAIN = "$FREE_TRIAL_VIEW:image_main"
    const val CHECK_ICON = "$FREE_TRIAL_VIEW:image_check_icon"
    const val DESCRIPTION = "$FREE_TRIAL_VIEW:description"
    const val INSTALL_APP_BUTTON = "$FREE_TRIAL_VIEW:install_app_button"
    const val HOW_IT_WORKS_TITLE = "$FREE_TRIAL_VIEW:how_it_works_title"
    const val HOW_IT_WORKS_DESCRIPTION = "$FREE_TRIAL_VIEW:how_it_works_description"
}


@CombinedThemePreviews
@Composable
fun FreeTrialViewPreview() {
    OriginalTheme(isSystemInDarkTheme()) {
        FreeTrialView(
            icon = iconPackR.drawable.ic_mega_vpn_free_trial,
            freeTrialText = stringResource(sharedR.string.text_start_mega_vpn_free_trial, "5 GB"),
            installButtonText = sharedR.string.button_text_install_mega_vpn,
            howItWorksText = stringResource(sharedR.string.text_how_it_works_mega_vpn_free_trial)
        )
    }
}

@CombinedThemePreviews
@Composable
fun FreeTrialViewDisableButtonPreview() {
    OriginalTheme(isSystemInDarkTheme()) {
        FreeTrialView(
            icon = iconPackR.drawable.ic_mega_vpn_free_trial,
            freeTrialText = stringResource(sharedR.string.text_start_mega_vpn_free_trial, "5 GB"),
            installButtonText = sharedR.string.button_text_install_mega_vpn,
            howItWorksText = stringResource(
                sharedR.string.text_how_it_works_mega_vpn_free_trial,
                "5 GB"
            ),
            isReceivedAward = true
        )
    }
}