package mega.privacy.android.app.presentation.notification.view

import android.text.format.DateFormat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.view.getAppropiateSubTextString
import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.presentation.notification.model.NotificationState
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PromoNotificationItemView
import mega.privacy.android.domain.entity.notifications.PromoNotification
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView
import mega.privacy.android.shared.original.core.ui.controls.notifications.NotificationItemType
import mega.privacy.android.shared.original.core.ui.controls.notifications.NotificationItemView
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.utils.ComposableLifecycle
import mega.privacy.android.shared.original.core.ui.utils.shimmerEffect


/**
 * Notification View in Compose
 */
@Composable
fun NotificationView(
    state: NotificationState,
    modifier: Modifier = Modifier,
    onNotificationClick: (Notification) -> Unit,
    onPromoNotificationClick: (PromoNotification) -> Unit,
    onNotificationsLoaded: () -> Unit = {},
) {
    val allNotifications = state.promoNotifications + state.notifications
    if (allNotifications.isNotEmpty()) {
        NotificationListView(
            modifier,
            state,
            allNotifications = allNotifications,
            onNotificationClick = { notification: Notification -> onNotificationClick(notification) },
            onPromoNotificationClick = onPromoNotificationClick,
            onNotificationsLoaded = onNotificationsLoaded
        )
    } else if (state.isLoading) {
        NotificationLoadingView()
    } else {
        NotificationEmptyView(modifier)
    }
}

@Composable
fun NotificationLoadingView(
    modifier: Modifier = Modifier,
    loadingItemsCount: Int = 10,
) {
    LazyColumn(
        modifier = modifier.testTag(NOTIFICATION_LOADING_VIEW_TEST_TAG),
    ) {
        items(count = loadingItemsCount) { index ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .wrapContentHeight()
            ) {
                Spacer(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(shape = RoundedCornerShape(12.dp))
                        .height(12.dp)
                        .width(80.dp)
                        .shimmerEffect()
                )
                Spacer(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clip(shape = RoundedCornerShape(12.dp))
                        .height(12.dp)
                        .width(180.dp)
                        .shimmerEffect()
                )

                Spacer(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(shape = RoundedCornerShape(12.dp))
                        .height(12.dp)
                        .width(80.dp)
                        .shimmerEffect()
                )

                Spacer(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .height(1.dp)
                        .fillMaxWidth()
                        .shimmerEffect()
                )
            }
        }
    }
}

@Composable
private fun NotificationListView(
    modifier: Modifier,
    state: NotificationState,
    allNotifications: List<Any>,
    onNotificationClick: (Notification) -> Unit,
    onPromoNotificationClick: (PromoNotification) -> Unit,
    onNotificationsLoaded: () -> Unit,
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current

    if (state.scrollToTop) {
        LaunchedEffect(listState) {
            listState.scrollToItem(0, 0)
        }
    }

    val allItemsLoaded by remember {
        derivedStateOf { listState.layoutInfo.totalItemsCount == allNotifications.size }
    }

    ComposableLifecycle { event ->
        if (event == Lifecycle.Event.ON_PAUSE) {
            if (allItemsLoaded) {
                onNotificationsLoaded()
            }
        }
    }

    LazyColumn(state = listState, modifier = modifier.testTag(NOTIFICATION_LIST_VIEW_TEST_TAG)) {
        items(items = allNotifications) { notification ->
            if (notification is PromoNotification) {
                PromoNotificationItemView(
                    modifier = modifier,
                    notification = notification
                ) {
                    onPromoNotificationClick(notification)
                }
            } else if (notification is Notification) {
                NotificationItemView(
                    type = notification.sectionType,
                    typeTitle = notification.sectionTitle(context),
                    title = notification.title(context),
                    description = notification.description(context),
                    subText = notification.schedMeetingNotification
                        ?.let { notification ->

                            getAppropiateSubTextString(
                                scheduledMeeting = notification.scheduledMeeting,
                                occurrence = notification.occurrenceChanged,
                                is24HourFormat = DateFormat.is24HourFormat(LocalContext.current),
                                highLightTime = notification.hasTimeChanged,
                                highLightDate = notification.hasDateChanged,
                            )
                        },
                    date = notification.dateText(context),
                    isNew = notification.isNew,
                    modifier = modifier,
                ) {
                    onNotificationClick(notification)
                }
            }
        }
    }
}


@Composable
private fun NotificationEmptyView(modifier: Modifier) {
    val context = LocalContext.current

    val emptyImgResId = iconPackR.drawable.ic_bell_glass

    Surface(modifier.testTag(NOTIFICATION_EMPTY_VIEW_TEST_TAG)) {
        LegacyMegaEmptyView(
            modifier = modifier,
            imageBitmap = ImageBitmap.imageResource(id = emptyImgResId),
            text = context.getString(R.string.context_empty_notifications)
        )
    }
}

internal const val NOTIFICATION_LOADING_VIEW_TEST_TAG = "notification_loading_view_test_tag"
internal const val NOTIFICATION_LIST_VIEW_TEST_TAG = "notification_list_view_test_tag"
internal const val NOTIFICATION_EMPTY_VIEW_TEST_TAG = "notification_empty_view_test_tag"

@CombinedThemePreviews
@Composable
private fun EmptyNotificationViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        NotificationView(
            state = NotificationState(emptyList()),
            onNotificationClick = {},
            onPromoNotificationClick = {})
    }
}

@CombinedThemePreviews
@Composable
private fun NotificationViewPreview() {
    val promoNotification = PromoNotification(
        promoID = 1,
        title = "Title",
        description = "Description",
        iconURL = "https://www.mega.co.nz",
        imageURL = "https://www.mega.co.nz",
        startTimeStamp = 1,
        endTimeStamp = 1,
        actionName = "Action name",
        actionURL = "https://www.mega.co.nz"
    )

    val normalNotification = Notification(
        sectionTitle = { "CONTACTS" },
        sectionType = NotificationItemType.Others,
        title = { "New Contact" },
        titleTextSize = 16.sp,
        description = { "xyz@gmail.com is now a contact" },
        schedMeetingNotification = null,
        dateText = { "11 October 2022 6:46 pm" },
        isNew = true,
    ) {}
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        NotificationView(
            state = NotificationState(
                promoNotifications = (listOf(promoNotification)),
                notifications = (listOf(normalNotification))
            ),
            onNotificationClick = {},
            onPromoNotificationClick = {})
    }
}

@CombinedThemePreviews
@Composable
private fun NotificationLoadingViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        NotificationView(
            state = NotificationState(emptyList(), isLoading = true),
            onNotificationClick = {},
            onPromoNotificationClick = {})
    }
}