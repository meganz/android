package mega.privacy.android.app.presentation.achievements.referral.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.AsyncImage
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.achievements.referral.model.ReferralBonusesUIState
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.dark_blue_500_dark_blue_200
import mega.privacy.android.core.ui.theme.extensions.green_500_green_400
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.grey_500
import mega.privacy.android.core.ui.theme.red_800
import mega.privacy.android.domain.entity.achievement.ReferralBonusAchievements

internal object TestTags {
    /**
     * Because this screens components are a list of items, the suffix index_ is necessary to help
     * determine the row of the tested views.
     */
    const val ROUNDED_IMAGE = "referral_bonuses_view:image_avatar_index_"
    const val TITLE = "referral_bonuses_view:text_title_index_"
    const val STORAGE = "referral_bonuses_view:text_storage_index_"
    const val TRANSFER = "referral_bonuses_view:text_transfer_index_"
    const val DAYS_LEFT = "referral_bonuses_view:text_days_left_index"
}

/**
 * Referral Bonus Fragment in Jetpack Compose
 * @see [ReferralBonusesFragment]
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReferralBonusView(uiState: ReferralBonusesUIState) {
    Scaffold(scaffoldState = rememberScaffoldState()) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(
                key = { it },
                count = uiState.awardedInviteAchievements.size,
            ) { idx ->
                ReferralListItem(
                    modifier = Modifier
                        .animateItemPlacement(),
                    data = uiState.awardedInviteAchievements[idx],
                    index = idx
                )
            }
        }
    }
}

@Composable
private fun ReferralListItem(
    modifier: Modifier,
    data: ReferralBonusAchievements,
    index: Int,
) {
    ConstraintLayout(
        modifier = modifier
            .height(72.dp)
            .fillMaxWidth()
    ) {
        val (avatar, title, storage, transfer, days, divider) = createRefs()

        AsyncImage(
            modifier = Modifier
                .testTag("${TestTags.ROUNDED_IMAGE}$index")
                .size(40.dp)
                .clip(CircleShape)
                .constrainAs(avatar) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start, 18.dp)
                },
            model = data.referredAvatarUri,
            contentScale = ContentScale.Crop,
            contentDescription = "Avatar"
        )
        Text(
            modifier = Modifier
                .testTag("${TestTags.TITLE}$index")
                .constrainAs(title) {
                    top.linkTo(avatar.top)
                    linkTo(
                        start = avatar.end,
                        end = days.start,
                        startMargin = 13.dp,
                        endMargin = 16.dp,
                        bias = 0f
                    )
                },
            text = data.referredName ?: data.referredEmails[0],
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.black_white,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            modifier = Modifier
                .testTag("${TestTags.STORAGE}$index")
                .constrainAs(storage) {
                    top.linkTo(title.bottom)
                    start.linkTo(avatar.end, 13.dp)
                },
            text = Util.getSizeString(data.rewardedStorageInBytes, LocalContext.current),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.dark_blue_500_dark_blue_200
        )
        Text(
            modifier = Modifier
                .testTag("${TestTags.TRANSFER}$index")
                .constrainAs(transfer) {
                    top.linkTo(title.bottom)
                    linkTo(
                        start = storage.end,
                        end = days.start,
                        startMargin = 12.dp,
                        endMargin = 16.dp,
                        bias = 0f
                    )
                },
            text = Util.getSizeString(data.rewardedTransferInBytes, LocalContext.current),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.green_500_green_400
        )
        Text(
            modifier = Modifier
                .testTag("${TestTags.DAYS_LEFT}$index")
                .constrainAs(days) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end, 10.dp)
                },
            text = if (data.expirationInDays > 0) {
                stringResource(R.string.general_num_days_left, data.expirationInDays)
            } else {
                stringResource(id = R.string.expired_label)
            },
            style = MaterialTheme.typography.body2,
            color = if (data.expirationInDays <= 15) red_800 else grey_500
        )
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 72.dp)
                .constrainAs(divider) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            thickness = 1.dp,
            color = MaterialTheme.colors.grey_alpha_012_white_alpha_012
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ReferralBonusViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ReferralBonusView(
            uiState = ReferralBonusesUIState(
                awardedInviteAchievements = listOf(
                    // The image here is free to use anywhere for preview purposes
                    // @see https://pixabay.com/service/faq/
                    ReferralBonusAchievements(
                        referredAvatarUri = "https://cdn.pixabay.com/photo/2023/05/05/11/07/sweet-7972193_1280.jpg",
                        referredName = "Qwerty Uiop",
                        awardId = 1,
                        expirationInDays = 100,
                        rewardedStorageInBytes = 51283102,
                        rewardedTransferInBytes = 12381273,
                        referredEmails = listOf("qwerty@uiop.com")
                    )
                )
            )
        )
    }
}