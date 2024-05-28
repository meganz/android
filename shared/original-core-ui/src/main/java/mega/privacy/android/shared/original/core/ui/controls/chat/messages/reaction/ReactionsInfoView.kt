package mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReactionUser
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.tab.MegaScrollableTabRow
import mega.privacy.android.shared.original.core.ui.controls.tab.TabPosition
import mega.privacy.android.shared.original.core.ui.controls.tab.TabRowDefaults.tabIndicatorOffset
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * View to show the information of reactions.
 * User can swipe to see the users who gave the reaction.
 *
 * @param currentReaction the reaction user selected
 * @param reactionList the list of reactions for the message
 * @param modifier
 * @param onUserClick the callback when a certain user clicked
 */
@OptIn(ExperimentalFoundationApi::class)
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
    ) {
        MegaScrollableTabRow(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp),
            backgroundColor = Color.Transparent,
            selectedTabIndex = pagerState.currentPage,
            indicator = { tabPositions: List<TabPosition> ->
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                        .height(2.dp)
                        .padding(horizontal = 13.dp)
                        .background(color = MegaOriginalTheme.colors.components.selectionControl)
                )
            },
            divider = {},
            edgePadding = 0.dp,
        ) {
            reactionList.forEachIndexed { index, reaction ->
                Tab(
                    modifier = Modifier
                        .width(72.dp)
                        .fillMaxHeight(),
                    selected = pagerState.currentPage == index,
                    unselectedContentColor = Color.White, // set solid color to avoid the color change on unselected tab
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val counterColor =
                                if (reaction.hasMe) TextColor.Accent else TextColor.Secondary
                            Text(
                                text = reaction.reaction,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            MegaText(
                                text = reaction.userList.size.toString(),
                                textColor = counterColor,
                                style = MaterialTheme.typography.subtitle2,
                            )
                        }
                    }
                )
            }
        }

        MegaDivider(dividerType = DividerType.FullSize)

        HorizontalPager(
            modifier = Modifier.fillMaxWidth(),
            state = pagerState,
            beyondBoundsPageCount = 3,
            verticalAlignment = Alignment.Top,
        ) { page: Int ->
            val reaction = reactionList[page]
            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Top,
            ) {

                Row(
                    modifier = Modifier.height(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MegaText(
                        text = reaction.shortCode,
                        textColor = TextColor.Secondary,
                        style = MaterialTheme.typography.caption,
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(count = reaction.userList.size) { index ->
                        val reactionGiver = reaction.userList[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable { onUserClick(reactionGiver.userHandle) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            reactionGiver.avatarContent(
                                reactionGiver.userHandle,
                                Modifier.size(40.dp)
                            )

                            MegaText(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                                text = reactionGiver.name,
                                textColor = TextColor.Primary,
                                style = MaterialTheme.typography.subtitle1,
                                overflow = LongTextBehaviour.Ellipsis(maxLines = 1),
                            )
                        }

                        if (index < reaction.userList.size - 1) {
                            MegaDivider(
                                DividerType.FullSize,
                                modifier = Modifier.padding(start = 56.dp)
                            )
                        }
                    }
                }
            }
        }

    }
}

@CombinedThemePreviews
@Composable
private fun ReactionsInfoViewPreview(
    @PreviewParameter(BooleanProvider::class) hasMe: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {

        val reactionList = listOf(
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
                count = 2,
                userList = listOf(
                    UIReactionUser(name = "Jack London", userHandle = 1L),
                    UIReactionUser(name = "Alexandre Dumas", userHandle = 2L),
                    UIReactionUser(
                        name = "William Shakespeare with very very very long name",
                        userHandle = 3L
                    ),
                )
            )
        }
        ReactionsInfoView(
            currentReaction = ROLLING_EYES_REACTION,
            reactionList = reactionList,
            onUserClick = {},
        )
    }
}