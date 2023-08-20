package mega.privacy.android.app.presentation.meeting.view

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.WaitingRoomState
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.black_white
import java.time.ZonedDateTime


/**
 * Waiting room View
 */
@Composable
internal fun WaitingRoomView(
    state: WaitingRoomState,
    onScrollChange: (Boolean) -> Unit,
    onBackPressed: () -> Unit,
    onInfoClicked: () -> Unit,
    onCloseClicked: () -> Unit,
) {

    val scaffoldState = rememberScaffoldState()
    val listState = rememberLazyListState()
    val firstItemVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = {
            SnackbarHost(hostState = it) { data ->
                Snackbar(
                    snackbarData = data,
                    backgroundColor = MaterialTheme.colors.black_white
                )
            }
        },
        topBar = {
            WaitingRoomAppBar(
                title = state.schedMeetTitle,
                date = state.schedMeetDate,
                onInfoClicked = onInfoClicked,
                onCloseClicked = onCloseClicked,
                elevation = !firstItemVisible
            )

        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(paddingValues)
        ) {

        }
    }

    onScrollChange(!firstItemVisible)
}

/**
 * Schedule meeting App bar view
 *
 * @param title                     Scheduled meeting title
 * @param date                      Scheduled meeting [ZonedDateTime]
 * @param onInfoClicked             When on waiting room info is clicked
 * @param onCloseClicked            When on discard is clicked
 * @param elevation                 True if it has elevation. False, if it does not.
 */
@Composable
private fun WaitingRoomAppBar(
    title: String,
    date: ZonedDateTime,
    onInfoClicked: () -> Unit,
    onCloseClicked: () -> Unit,
    elevation: Boolean,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = if (elevation) AppBarDefaults.TopAppBarElevation else 0.dp,
        color = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    IconButton(onClick = onCloseClicked) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_close),
                            contentDescription = "Close waiting room button",
                            tint = MaterialTheme.colors.onSurface
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .wrapContentSize(Alignment.CenterEnd)
                ) {
                    IconButton(onClick = onInfoClicked) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = mega.privacy.android.core.R.drawable.ic_info),
                            contentDescription = "Waiting room info button",
                            tint = MaterialTheme.colors.onSurface
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 15.dp, end = 15.dp, bottom = 20.dp)
            ) {
                Text(
                    modifier = Modifier.padding(bottom = 2.dp), text = title,
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Monday, 30 May · 10:25-11:25",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface
                )
            }
        }
    }
}

/**
 * Waiting Room View Preview
 */
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewWaitingRoomView")
@Composable
fun PreviewWaitingRoomView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        WaitingRoomView(
            state = WaitingRoomState(
                chatId = -1,
                schedId = -1,
                schedMeetTitle = "Test title"
            ),
            onScrollChange = {},
            onBackPressed = {},
            onCloseClicked = {},
            onInfoClicked = {}
        )
    }
}