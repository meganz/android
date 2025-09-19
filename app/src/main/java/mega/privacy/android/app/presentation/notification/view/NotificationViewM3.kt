package mega.privacy.android.app.presentation.notification.view

import android.text.format.DateFormat
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import mega.android.core.ui.components.LinkSpannedText
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.view.getAppropiateSubTextString
import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.presentation.notification.model.NotificationItemType
import mega.privacy.android.app.presentation.notification.model.NotificationState
import mega.privacy.android.app.presentation.notification.view.notificationviewtype.PromoNotificationItemViewM3
import mega.privacy.android.domain.entity.notifications.PromoNotification
import mega.privacy.android.feature.notifications.snowflakes.NotificationItemViewM3
import mega.privacy.android.icon.pack.R as iconPackR


/**
 * Notification View in Compose Material 3
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationViewM3(
    state: NotificationState,
    modifier: Modifier = Modifier,
    onNotificationClick: (Notification) -> Unit,
    onPromoNotificationClick: (PromoNotification) -> Unit,
    onNotificationsLoaded: () -> Unit = {},
) {
    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .navigationBarsPadding(),
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(NOTIFICATION_TOP_BAR_M3_TEST_TAG),
                navigationType = AppBarNavigationType.Back(
                    onNavigationIconClicked = {
                        onBackPressedDispatcher?.onBackPressed()
                    }
                ),
                title = stringResource(R.string.title_properties_chat_contact_notifications))
        },
    ) { innerPadding ->
        val allNotifications = state.promoNotifications + state.notifications
        if (allNotifications.isNotEmpty()) {
            NotificationListViewM3(
                modifier,
                state,
                innerPadding,
                allNotifications = allNotifications,
                onNotificationClick = { notification: Notification ->
                    onNotificationClick(
                        notification
                    )
                },
                onPromoNotificationClick = onPromoNotificationClick,
                onNotificationsLoaded = onNotificationsLoaded
            )
        } else if (state.isLoading) {
            NotificationLoadingViewM3(contentPadding = innerPadding)
        } else {
            NotificationEmptyViewM3(modifier)
        }
    }
}

@Composable
fun NotificationLoadingViewM3(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    loadingItemsCount: Int = 10,
) {
    LazyColumn(
        modifier = modifier.testTag(NOTIFICATION_LOADING_VIEW_M3_TEST_TAG),
        contentPadding = contentPadding
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
private fun NotificationListViewM3(
    modifier: Modifier,
    state: NotificationState,
    contentPadding: PaddingValues,
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

    val lifeCycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifeCycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                if (allItemsLoaded) {
                    onNotificationsLoaded()
                }
            }
        }
        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.testTag(NOTIFICATION_LIST_VIEW_M3_TEST_TAG),
        contentPadding = contentPadding
    ) {
        items(items = allNotifications) { notification ->
            if (notification is PromoNotification) {
                PromoNotificationItemViewM3(
                    modifier = modifier,
                    notification = notification
                ) {
                    onPromoNotificationClick(notification)
                }
            } else if (notification is Notification) {
                NotificationItemViewM3(
                    titleColor = notification.sectionType.titleColor(),
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
fun NotificationEmptyViewM3(modifier: Modifier = Modifier) {
    val imageDrawable = iconPackR.drawable.ic_bell_glass
    val textId = R.string.context_empty_notifications
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .testTag(NOTIFICATION_EMPTY_VIEW_M3_TEST_TAG),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier
                .size(120.dp)
                .testTag(NOTIFICATION_EMPTY_VIEW_IMAGE_M3_TEST_TAG),
            painter = painterResource(imageDrawable),
            contentDescription = "Empty",
        )
        Spacer(modifier = Modifier.height(6.dp))
        LinkSpannedText(
            modifier = Modifier.testTag(NOTIFICATION_EMPTY_VIEW_TEXT_M3_TEST_TAG),
            value = stringResource(textId),
            spanStyles = mapOf(
                SpanIndicator('A') to SpanStyleWithAnnotation(
                    megaSpanStyle = MegaSpanStyle.TextColorStyle(
                        spanStyle = SpanStyle(),
                        textColor = TextColor.Primary
                    ),
                    annotation = "A"
                ),
                SpanIndicator('B') to SpanStyleWithAnnotation(
                    megaSpanStyle = MegaSpanStyle.TextColorStyle(
                        spanStyle = SpanStyle(),
                        textColor = TextColor.Secondary
                    ),
                    annotation = "B"
                )
            ),
            baseStyle = AppTheme.typography.bodyLarge,
            baseTextColor = TextColor.Secondary,
            onAnnotationClick = {}
        )
    }
}

internal const val NOTIFICATION_LOADING_VIEW_M3_TEST_TAG = "notification_loading_view_test_tag"
internal const val NOTIFICATION_LIST_VIEW_M3_TEST_TAG = "notification_list_view_test_tag"
internal const val NOTIFICATION_EMPTY_VIEW_M3_TEST_TAG = "notification_empty_view_test_tag"
internal const val NOTIFICATION_EMPTY_VIEW_IMAGE_M3_TEST_TAG =
    "notification_empty_view_image_test_tag"
internal const val NOTIFICATION_EMPTY_VIEW_TEXT_M3_TEST_TAG =
    "notification_empty_view_text_test_tag"
internal const val NOTIFICATION_TOP_BAR_M3_TEST_TAG = "notification_top_bar_test_tag"


@CombinedThemePreviews
@Composable
private fun NotificationEmptyViewM3Preview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NotificationViewM3(
            state = NotificationState(emptyList()),
            onNotificationClick = {},
            onPromoNotificationClick = {})
    }
}

@CombinedThemePreviews
@Composable
private fun NotificationViewM3Preview() {
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
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NotificationViewM3(
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
private fun NotificationLoadingViewM3Preview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        NotificationViewM3(
            state = NotificationState(emptyList(), isLoading = true),
            onNotificationClick = {},
            onPromoNotificationClick = {})
    }
}