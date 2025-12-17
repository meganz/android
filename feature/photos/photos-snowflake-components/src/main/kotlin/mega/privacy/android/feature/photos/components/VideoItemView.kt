package mega.privacy.android.feature.photos.components

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.text.HighlightedText
import mega.android.core.ui.components.util.normalize
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR

@SuppressLint("ComposeUnstableCollections")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoItemView(
    @DrawableRes icon: Int,
    name: String,
    fileSize: String?,
    duration: String,
    isFavourite: Boolean,
    isSelected: Boolean,
    isSharedWithPublicLink: Boolean,
    onClick: () -> Unit,
    thumbnailData: Any?,
    modifier: Modifier = Modifier,
    labelView: @Composable (() -> Unit)? = null,
    tagsRow: (@Composable () -> Unit)? = null,
    description: String? = null,
    highlightText: String = "",
    collectionTitle: String? = null,
    showMenuButton: Boolean = true,
    nodeAvailableOffline: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onMenuClick: () -> Unit = {},
    isSensitive: Boolean = false,
) {
    Row(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 80.dp)
            .testTag(VIDEO_ITEM_VIEW_TEST_TAG)
    ) {
        VideoThumbnailView(
            icon = icon,
            modifier = Modifier
                .align(Alignment.Top)
                .alpha(0.5f.takeIf { isSensitive } ?: 1f)
                .blur(16.dp.takeIf { isSensitive } ?: 0.dp),
            thumbnailData = thumbnailData,
            duration = duration,
            isFavourite = isFavourite
        )

        VideoInfoView(
            modifier = Modifier.weight(1f),
            name = name,
            fileSize = fileSize,
            collectionTitle = collectionTitle,
            nodeAvailableOffline = nodeAvailableOffline,
            labelView = labelView,
            isSharedWithPublicLink = isSharedWithPublicLink,
            description = description,
            tagsRow = tagsRow,
            highlightText = highlightText,
        )

        if (showMenuButton) {
            MegaIcon(
                imageVector = if (isSelected)
                    IconPack.Medium.Thin.Solid.CheckCircle
                else
                    IconPack.Medium.Thin.Outline.MoreVertical,
                tint = IconColor.Primary,
                contentDescription = null,
                modifier = Modifier
                    .clickable(enabled = !isSelected, onClick = onMenuClick)
                    .padding(top = 25.dp)
                    .testTag(VIDEO_ITEM_MENU_ICON_TEST_TAG),
            )
        }
    }
}

@Composable
internal fun VideoThumbnailView(
    @DrawableRes icon: Int,
    thumbnailData: Any?,
    duration: String,
    isFavourite: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(top = 4.dp)
            .width(130.dp)
            .aspectRatio(1.77f)
    ) {
        val thumbnailModifier = Modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(5.dp))

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(thumbnailData)
                .crossfade(true)
                .build(),
            contentDescription = null,
            placeholder = painterResource(id = icon),
            error = painterResource(id = icon),
            contentScale = ContentScale.Crop,
            modifier = thumbnailModifier.testTag(VIDEO_ITEM_THUMBNAIL_TEST_TAG)
        )

        if (duration.isNotEmpty()) {
            MegaText(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 5.dp, end = 5.dp)
                    .background(Color.Black)
                    .padding(horizontal = 3.dp)
                    .testTag(VIDEO_ITEM_DURATION_VIEW_TEST_TAG),
                text = duration,
                style = MaterialTheme.typography.bodySmall,
                textColor = TextColor.OnColor
            )
        }

        MegaIcon(
            imageVector = IconPack.Medium.Thin.Solid.PlayCircle,
            tint = IconColor.OnColor,
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.Center)
                .testTag(VIDEO_ITEM_PLAY_ICON_TEST_TAG)
        )
        if (isFavourite) {
            MegaIcon(
                imageVector = IconPack.Small.Thin.Solid.Heart,
                tint = IconColor.OnColor,
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 5.dp, end = 5.dp)
                    .size(12.dp)
                    .align(Alignment.TopEnd)
                    .testTag(VIDEO_ITEM_FAVOURITE_ICON_TEST_TAG)
            )
        }
    }
}

@SuppressLint("ComposeUnstableCollections")
@Composable
internal fun VideoInfoView(
    name: String,
    fileSize: String?,
    nodeAvailableOffline: Boolean,
    isSharedWithPublicLink: Boolean,
    modifier: Modifier = Modifier,
    collectionTitle: String? = null,
    description: String? = null,
    labelView: @Composable (() -> Unit)? = null,
    tagsRow: @Composable (() -> Unit)? = null,
    highlightText: String = "",
) {
    Column(modifier = modifier) {
        VideoNameWithLabel(
            name = name,
            labelView = labelView,
            highlightText = highlightText
        )

        collectionTitle?.let {
            CollectionTitleView(it)
        }

        VideoSizeAndIconsView(
            fileSize = fileSize,
            nodeAvailableOffline = nodeAvailableOffline,
            isSharedWithPublicLink = isSharedWithPublicLink,
        )

        if (highlightText.isNotBlank()) {
            if (description != null) {
                val normalizedHighlight = remember(highlightText) { highlightText.normalize() }
                val normalizedDescription = remember(description) { description.normalize() }
                if (normalizedDescription.contains(normalizedHighlight, ignoreCase = true)) {
                    HighlightedText(
                        modifier = Modifier
                            .testTag(VIDEO_ITEM_NODE_DESCRIPTION_TEST_TAG)
                            .padding(start = 10.dp, top = 5.dp, bottom = 5.dp),
                        text = description,
                        highlightText = highlightText,
                        highlightFontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        textColor = TextColor.Secondary,
                    )
                }
            }

            tagsRow?.invoke()
        }
    }
}

@Composable
fun VideoNameWithLabel(
    name: String,
    modifier: Modifier = Modifier,
    labelView: @Composable (() -> Unit)? = null,
    maxLines: Int = 2,
    highlightText: String = "",
) {
    val inlineContentId = "box"
    val ellipsis = "..."

    val text = AnnotatedString.Builder().also { builder ->
        builder.append(name)
        builder.appendInlineContent(inlineContentId)
    }.toAnnotatedString()

    var finalText by remember { mutableStateOf(text) }
    val textLayoutResultState = remember { mutableStateOf<TextLayoutResult?>(null) }
    val textLayoutResult = textLayoutResultState.value

    // Adjust the text to show ellipsis when it overflows, and show label icon afterwards
    LaunchedEffect(textLayoutResult) {
        if (textLayoutResult == null) return@LaunchedEffect
        if (textLayoutResult.hasVisualOverflow) {
            val lastCharIndex = textLayoutResult.getLineEnd(maxLines - 1)
            val textWithoutOverflow =
                AnnotatedString(text.substring(startIndex = 0, endIndex = lastCharIndex))

            val lastCharIndexBeforeEllipse = maxOf(
                0,
                textWithoutOverflow.length - ellipsis.length - 1,
                textWithoutOverflow.lastIndexOf("\n")
            )

            val adjustedText = text
                .subSequence(startIndex = 0, endIndex = lastCharIndex)
                .subSequence(startIndex = 0, endIndex = lastCharIndexBeforeEllipse)
                .plus(AnnotatedString(ellipsis))

            finalText = AnnotatedString.Builder().also { builder ->
                builder.append(adjustedText)
                builder.appendInlineContent(inlineContentId)
            }.toAnnotatedString()
        }
    }

    Row(modifier = modifier.padding(bottom = 5.dp)) {
        val inlineContent = mapOf(
            inlineContentId to InlineTextContent(
                Placeholder(
                    width = 20.sp,
                    height = 10.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                labelView?.invoke()
            }
        )

        if (highlightText.isNotBlank()) {
            HighlightedText(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .align(Alignment.CenterVertically)
                    .testTag(VIDEO_ITEM_NAME_VIEW_TEST_TAG),
                text = finalText,
                highlightText = highlightText,
                highlightFontWeight = FontWeight.Medium,
                maxLines = maxLines,
                textColor = TextColor.Primary,
                style = AppTheme.typography.bodyLarge,
                inlineContent = inlineContent,
            )
        } else {
            Text(
                text = finalText,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .align(Alignment.CenterVertically)
                    .testTag(VIDEO_ITEM_NAME_VIEW_TEST_TAG),
                color = DSTokens.colors.text.primary,
                maxLines = maxLines,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                inlineContent = inlineContent,
                onTextLayout = { textLayoutResultState.value = it }
            )
        }
    }
}

@Composable
internal fun CollectionTitleView(
    collectionTitle: String,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MegaIcon(
            painter = painterResource(id = iconPackR.drawable.ic_recently_watched_collection_title),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .size(11.dp)
        )
        MegaText(
            modifier = modifier.testTag(VIDEO_ITEM_COLLECTION_TITLE_TEST_TAG),
            text = collectionTitle,
            style = MaterialTheme.typography.bodySmall,
            textColor = TextColor.Secondary
        )
    }
}

@Composable
internal fun VideoSizeAndIconsView(
    fileSize: String?,
    nodeAvailableOffline: Boolean,
    isSharedWithPublicLink: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(start = 10.dp, top = 5.dp)) {
        fileSize?.let {
            MegaText(
                modifier = Modifier.testTag(VIDEO_ITEM_SIZE_VIEW_TEST_TAG),
                text = it,
                style = MaterialTheme.typography.bodySmall,
                textColor = TextColor.Secondary,
            )
        }

        if (nodeAvailableOffline) {
            MegaIcon(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(16.dp)
                    .testTag(VIDEO_ITEM_OFFLINE_ICON_TEST_TAG),
                imageVector = IconPack.Medium.Thin.Outline.ArrowDownCircle,
                tint = IconColor.Secondary,
                contentDescription = null
            )
        }

        if (isSharedWithPublicLink) {
            MegaIcon(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(16.dp)
                    .testTag(VIDEO_ITEM_LINK_ICON_TEST_TAG),
                imageVector = IconPack.Medium.Thin.Outline.Link01,
                tint = IconColor.Secondary,
                contentDescription = null
            )
        }
    }
}

/**
 * Test tag for the video item view
 */
const val VIDEO_ITEM_VIEW_TEST_TAG = "video_item:row_item"

/**
 * Test tag for the video item duration view
 */
const val VIDEO_ITEM_DURATION_VIEW_TEST_TAG = "video_item:text_duration"

/**
 * Test tag for the video item name view
 */
const val VIDEO_ITEM_NAME_VIEW_TEST_TAG = "video_item:text_name"

/**
 * Test tag for the video item size view
 */
const val VIDEO_ITEM_SIZE_VIEW_TEST_TAG = "video_item:text_size"

/**
 * Test tag for the video item thumbnail view
 */
const val VIDEO_ITEM_THUMBNAIL_TEST_TAG = "video_item:image_thumbnail"

/**
 * Test tag for the video item menu icon view
 */
const val VIDEO_ITEM_MENU_ICON_TEST_TAG = "video_item:image_menu"

/**
 * Test tag for the video item play icon view
 */
const val VIDEO_ITEM_PLAY_ICON_TEST_TAG = "video_item:image_play"

/**
 * Test tag for the video item favourite icon view
 */
const val VIDEO_ITEM_FAVOURITE_ICON_TEST_TAG = "video_item:image_favourite"

/**
 * Test tag for the video item offline icon view
 */
const val VIDEO_ITEM_OFFLINE_ICON_TEST_TAG = "video_item:image_offline"

/**
 * Test tag for the video item link icon view
 */
const val VIDEO_ITEM_LINK_ICON_TEST_TAG = "video_item:image_link"

/**
 * Test tag for the video item collection title view
 */
const val VIDEO_ITEM_COLLECTION_TITLE_TEST_TAG = "video_item:collection_title_view"

/**
 * Test tag for the video item node description
 */
const val VIDEO_ITEM_NODE_DESCRIPTION_TEST_TAG = "video_item:node_description"