package mega.privacy.android.app.presentation.notification.view

import android.content.res.Configuration
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.presentation.notification.model.NotificationState
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.core.ui.controls.MegaEmptyView
import java.util.Locale

/**
 * Notification View in Compose
 */
@Composable
fun NotificationView(
    state: NotificationState,
    modifier: Modifier = Modifier,
    onClick: (Notification) -> Unit = {},
    onNotificationsLoaded: () -> Unit = {},
) {
    if (state.notifications.isNotEmpty()) {
        NotificationListView(modifier,
            state,
            onClick = { notification: Notification -> onClick(notification) },
            onNotificationsLoaded = onNotificationsLoaded)
    } else {
        NotificationEmptyView(modifier)
    }
}

@Composable
private fun NotificationListView(
    modifier: Modifier,
    state: NotificationState,
    onClick: (Notification) -> Unit,
    onNotificationsLoaded: () -> Unit,
) {
    val listState = rememberLazyListState()

    if (state.scrollToTop) {
        LaunchedEffect(listState) {
            listState.scrollToItem(0, 0)
        }
    }

    val allItemsLoaded by remember {
        derivedStateOf { listState.layoutInfo.totalItemsCount == state.notifications.size }
    }

    if (allItemsLoaded) {
        onNotificationsLoaded()
    }

    LazyColumn(state = listState, modifier = modifier.testTag("NotificationListView")) {
        itemsIndexed(state.notifications) { index, notification ->
            NotificationItemView(modifier, notification, index, state.notifications) {
                onClick(notification)
            }
        }
    }
}


@Composable
private fun NotificationEmptyView(modifier: Modifier) {
    val context = LocalContext.current
    val isPortrait =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    val emptyImgResId =
        if (isPortrait) R.drawable.empty_notification_portrait else R.drawable.empty_notification_landscape

    Surface(modifier.testTag("NotificationEmptyView")) {
        MegaEmptyView(
            modifier = modifier,
            imageBitmap = ImageBitmap.imageResource(id = emptyImgResId),
            text = context.getString(R.string.context_empty_notifications)
                .formatColorTag(context, 'A', R.color.grey_900_grey_100)
                .formatColorTag(context, 'B', R.color.grey_300_grey_600)
                .toSpannedHtmlText()
        )
    }

}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "PreviewNotificationViewDark")
@Composable
private fun PreviewNotificationView() {
    NotificationView(state = NotificationState(emptyList()))
}