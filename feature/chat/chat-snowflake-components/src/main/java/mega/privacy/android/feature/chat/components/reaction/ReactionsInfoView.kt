package mega.privacy.android.feature.chat.components.reaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.feature.chat.components.reaction.model.UIReaction
import mega.privacy.android.feature.chat.components.reaction.model.UIReactionUser

/**
 * The details of the reactions of a message. It shows a tab for each reaction and, below it,
 * the users who gave the selected reaction. The user can swipe between the reactions.
 *
 * @param currentReaction The reaction to select first
 * @param reactionList The reactions of the message. Nothing shows when it is empty.
 * @param onUserClick Called with the user handle when the user taps a user
 * @param modifier [Modifier]
 */
@Composable
fun ReactionsInfoView(
    currentReaction: String,
    reactionList: List<UIReaction>,
    onUserClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (reactionList.isEmpty()) return

    val initialPage =
        reactionList.indexOfFirst { it.reaction == currentReaction }.takeIf { it != -1 } ?: 0
    val pagerState = rememberPagerState(initialPage = initialPage) { reactionList.size }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag(TEST_TAG_REACTIONS_INFO_VIEW),
    ) {
        SecondaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            containerColor = Color.Transparent,
            edgePadding = 0.dp,
            minTabWidth = 72.dp,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(pagerState.currentPage)
                        .padding(horizontal = 13.dp),
                    height = 2.dp,
                    color = DSTokens.colors.components.selectionControl,
                )
            },
            divider = {},
        ) {
            reactionList.forEachIndexed { index, reaction ->
                // One content colour for both states, so the tab text does not change tint.
                Tab(
                    modifier = Modifier.fillMaxHeight(),
                    selected = pagerState.currentPage == index,
                    selectedContentColor = DSTokens.colors.text.primary,
                    unselectedContentColor = DSTokens.colors.text.primary,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = { ReactionTabContent(reaction) },
                )
            }
        }

        SubtleDivider()

        HorizontalPager(
            modifier = Modifier.fillMaxWidth(),
            state = pagerState,
            beyondViewportPageCount = 3,
            verticalAlignment = Alignment.Top,
        ) { page ->
            ReactionUsersPage(
                reaction = reactionList[page],
                onUserClick = onUserClick,
            )
        }
    }
}

@Composable
private fun ReactionTabContent(reaction: UIReaction) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = reaction.reaction,
            fontSize = 20.sp,
            modifier = Modifier.padding(end = 4.dp),
        )
        MegaText(
            text = reaction.countString(),
            textColor = if (reaction.hasMe) TextColor.Accent else TextColor.Secondary,
            style = AppTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ReactionUsersPage(
    reaction: UIReaction,
    onUserClick: (Long) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Row(
            modifier = Modifier.height(32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MegaText(
                text = reaction.shortCode,
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodySmall,
            )
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(count = reaction.userList.size) { index ->
                val reactionGiver = reaction.userList[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { onUserClick(reactionGiver.userHandle) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    reactionGiver.avatarContent(reactionGiver.userHandle, Modifier.size(40.dp))
                    MegaText(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = reactionGiver.name,
                        textColor = TextColor.Primary,
                        style = AppTheme.typography.bodyLarge,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }

                if (index < reaction.userList.size - 1) {
                    SubtleDivider(modifier = Modifier.padding(start = 56.dp))
                }
            }
        }
    }
}

/**
 * Test tag for the reactions info view.
 */
const val TEST_TAG_REACTIONS_INFO_VIEW = "chat_view:reactions_info_view"

private val sampleAvatar: @Composable (Long, Modifier) -> Unit = { _, avatarModifier ->
    Box(modifier = avatarModifier.background(DSTokens.colors.background.surface3, CircleShape))
}

internal fun sampleReactionsInfo(hasMe: Boolean) = listOf(
    SLIGHT_SMILE_REACTION,
    ROLLING_EYES_REACTION,
    ROLLING_ON_THE_FLOOR_LAUGHING_REACTION,
    THUMBS_UP_REACTION,
    CLAP_REACTION,
).map {
    UIReaction(
        reaction = it,
        hasMe = hasMe,
        shortCode = ":shortcode:",
        count = 3,
        userList = listOf(
            UIReactionUser(userHandle = 1L, name = "Jack London", avatarContent = sampleAvatar),
            UIReactionUser(userHandle = 2L, name = "Alexandre Dumas", avatarContent = sampleAvatar),
            UIReactionUser(
                userHandle = 3L,
                name = "William Shakespeare with a very very very very long name",
                avatarContent = sampleAvatar,
            ),
        ),
    )
}

@CombinedThemePreviews
@Composable
private fun ReactionsInfoViewPreview(
    @PreviewParameter(BooleanProvider::class) hasMe: Boolean,
) {
    AndroidThemeForPreviews {
        ReactionsInfoView(
            currentReaction = ROLLING_EYES_REACTION,
            reactionList = sampleReactionsInfo(hasMe),
            onUserClick = {},
        )
    }
}
