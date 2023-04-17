package mega.privacy.android.app.mediaplayer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.model.SubtitleFileInfoItem
import mega.privacy.android.app.mediaplayer.model.SubtitleLoadState
import mega.privacy.android.core.ui.controls.SearchAppBar
import mega.privacy.android.core.ui.controls.SearchWidgetState
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import timber.log.Timber

/**
 * The screen for select subtitle file
 *
 * @param onAddSubtitleCallback the callback after added subtitle
 * @param onBackPressedCallback the callback for back button pressed
 */
@Composable
internal fun SelectSubtitleFileView(
    onAddSubtitleCallback: (SubtitleFileInfo?) -> Unit,
    onBackPressedCallback: () -> Unit,
) {
    val viewModel: SelectSubtitleFileViewModel = viewModel()
    val isEmpty = viewModel.state is SubtitleLoadState.Empty
    val isLoading = viewModel.state is SubtitleLoadState.Loading
    val items = if (viewModel.state is SubtitleLoadState.Success) {
        (viewModel.state as SubtitleLoadState.Success).items
    } else {
        emptyList()
    }
    val selectedSubtitleFileInfo by viewModel.getSelectedSubtitleFileInfoFlow()
        .collectAsStateWithLifecycle()
    val query by viewModel.getQueryStateFlow().collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.getSubtitleFileInfoList()
    }

    Scaffold(
        topBar = {
            when {
                isEmpty && isLoading && viewModel.searchState != SearchWidgetState.EXPANDED ->
                    EmptyTopBar {
                        onBackPressedCallback()
                    }
                selectedSubtitleFileInfo != null ->
                    SelectedTopBar {
                        onBackPressedCallback()
                    }
                else -> SearchAppBar(
                    searchWidgetState = viewModel.searchState,
                    typedSearch = query ?: "",
                    onSearchTextChange = { search ->
                        viewModel.searchQuery(search)
                    },
                    onCloseClicked = {
                        viewModel.closeSearch()
                    },
                    onBackPressed = onBackPressedCallback,
                    onSearchClicked = {
                        viewModel.searchWidgetStateUpdate()
                    },
                    elevation = false,
                    titleId = R.string.media_player_video_select_subtitle_file_title,
                    hintId = R.string.hint_action_search,
                    isHideAfterSearch = true
                )
            }
        }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(color = colorResource(id = R.color.white_dark_grey)),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    when {
                        isLoading -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                            content = {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(44.dp),
                                    color = teal_300,
                                )
                            },
                        )
                        isEmpty || items.isEmpty() -> SubtitleEmptyView(
                            modifier = Modifier.fillMaxSize(),
                            isSearchMode = viewModel.searchState == SearchWidgetState.EXPANDED
                        )
                        else -> SubtitleFileInfoListView(
                            subtitleInfoList = items,
                        ) { index ->
                            viewModel.itemClickedUpdate(index)
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 24.dp),
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                    onClick = onBackPressedCallback
                ) {
                    Text(
                        text = stringResource(id = R.string.general_cancel),
                        color = colorResource(id = R.color.teal_300)
                    )
                }

                Button(
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 24.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorResource(id = R.color.teal_300),
                        disabledBackgroundColor = colorResource(id = R.color.teal_100)
                    ),
                    onClick = {
                        onAddSubtitleCallback(viewModel.getSelectedSubtitleFileInfoFlow().value)
                    },
                    enabled = items.firstOrNull { it.selected } != null
                ) {
                    Text(
                        text = stringResource(id = R.string.media_player_video_select_subtitle_file_button_add_subtitles),
                        color = colorResource(id = R.color.white_dark_grey)
                    )
                }
            }
        }
    }
}

/**
 * The item view of subtitle file info list
 *
 * @param subtitleFileInfoItem [SubtitleFileInfoItem]
 * @param onSubtitleFileInfoClicked the callback for subtitle file info item is clicked
 */
@Composable
internal fun SubtitleFileInfoListItem(
    subtitleFileInfoItem: SubtitleFileInfoItem,
    onSubtitleFileInfoClicked: (SubtitleFileInfo) -> Unit,
) {
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.clickable {
            if (!subtitleFileInfoItem.selected) {
                scope.launch {
                    rotation.animateTo(
                        targetValue = 180f,
                        animationSpec = tween(100, easing = LinearEasing)
                    )
                    rotation.snapTo(0f)
                }
            }
            onSubtitleFileInfoClicked(subtitleFileInfoItem.subtitleFileInfo)
        }
    ) {
        Row {
            Image(
                painter = painterResource(
                    id =
                    if (subtitleFileInfoItem.selected) {
                        R.drawable.ic_select_thumbnail
                    } else {
                        R.drawable.ic_text_thumbnail
                    }
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 36.dp, height = 40.dp)
                    .align(Alignment.CenterVertically)
                    .graphicsLayer {
                        rotationY = rotation.value
                    }
            )
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = subtitleFileInfoItem.subtitleFileInfo.name,
                    fontSize = 20.sp,
                    color = colorResource(id = R.color.grey_087_white_087)
                )
                Text(
                    text = subtitleFileInfoItem.subtitleFileInfo.parentName ?: "",
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.grey_054_white_054)
                )
            }
        }
        Divider(
            color = colorResource(id = R.color.grey_300_alpha_026),
            thickness = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 52.dp),
        )
    }
}

/**
 * The empty view of subtitle
 */
@Composable
internal fun SubtitleEmptyView(
    modifier: Modifier,
    isSearchMode: Boolean,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = if (isSearchMode) {
                painterResource(id = R.drawable.ic_no_search_results)
            } else {
                painterResource(id = R.drawable.ic_subtitles_empty)
            },
            contentDescription = null,
        )
        Text(
            text = stringResource(
                id = if (isSearchMode) {
                    R.string.no_results_found
                } else {
                    R.string.media_player_video_select_subtitle_file_empty_message
                }
            ).uppercase(),
            color = colorResource(id = R.color.grey_300)
        )
    }
}

/**
 * The list view for subtitle file info list
 *
 * @param subtitleInfoList subtitle file info list
 * @param onClicked the callback for item clicked
 */
@Composable
internal fun SubtitleFileInfoListView(
    subtitleInfoList: List<SubtitleFileInfoItem>,
    onClicked: (SubtitleFileInfo) -> Unit,
) {
    Timber.d("render SubtitleFileInfoListView")
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        itemsIndexed(
            items = subtitleInfoList,
            key = { _, item -> item.subtitleFileInfo.id },
            itemContent = { _, item ->
                SubtitleFileInfoListItem(
                    subtitleFileInfoItem = item,
                    onSubtitleFileInfoClicked = onClicked
                )
            })
    }
}

@Composable
internal fun SelectedTopBar(
    onBackPressedCallback: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = "1",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium,
                color = colorResource(id = R.color.teal_300)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackPressedCallback) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back button",
                    tint = colorResource(id = R.color.teal_300)
                )
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 0.dp
    )
}

@Composable
internal fun EmptyTopBar(
    onBackPressedCallback: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.media_player_video_select_subtitle_file_title),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackPressedCallback) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back button"
                )
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 0.dp
    )
}

@Preview
@Composable
private fun PreviewSelectSubtitleFileViewWithEmptyList() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SelectSubtitleFileView({}, {})
    }
}

@Preview
@Composable
private fun PreviewSelectSubtitleFileView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SelectSubtitleFileView({}, {})
    }
}

@Preview
@Composable
private fun PreviewSubtitleEmptyView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SubtitleEmptyView(
            Modifier.fillMaxSize(),
            false
        )
    }
}

@Preview
@Composable
private fun PreviewSubtitleEmptyViewWhenSearchMode() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SubtitleEmptyView(
            Modifier.fillMaxSize(),
            true
        )
    }
}

@Preview
@Composable
private fun PreviewSubtitleFileInfoListItem() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SubtitleFileInfoListItem(
            subtitleFileInfoItem = SubtitleFileInfoItem(
                false,
                SubtitleFileInfo(
                    id = 123456,
                    name = "Testing.srt",
                    url = "test@test.com",
                    parentName = "testFolder"
                )
            ),
            onSubtitleFileInfoClicked = { }
        )
    }
}

@Preview
@Composable
private fun PreviewSubtitleFileInfoListView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SubtitleFileInfoListView(
            getTestSubtitleFileInfoList(),
        ) { }
    }
}

private fun getTestSubtitleFileInfoList() =
    listOf(
        SubtitleFileInfoItem(
            false,
            SubtitleFileInfo(
                id = 123456,
                name = "Testing.srt",
                url = "test@test.com",
                parentName = "testFolder"
            )
        ),
        SubtitleFileInfoItem(
            true,
            SubtitleFileInfo(
                id = 1234567,
                name = "Testing1.srt",
                url = "test1@test.com",
                parentName = "testFolder1"
            )
        ),
        SubtitleFileInfoItem(
            false,
            SubtitleFileInfo(
                id = 12345678,
                name = "Testing2.srt",
                url = "test2@test.com",
                parentName = "testFolder2"
            )
        ),
        SubtitleFileInfoItem(
            false,
            SubtitleFileInfo(
                id = 12345789,
                name = "Testing3.srt",
                url = "test3@test.com",
                parentName = "testFolder3"
            )
        ),
    )

