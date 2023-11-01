package mega.privacy.android.legacy.core.ui.controls.lists

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.legacy.core.ui.controls.divider.CustomDivider

/**
 * Compose UI, which is used as List item, containing Image or Icon title and description. Currently is used on My Account screen
 *
 * @param icon Drawable id for image or icon
 * @param title String id for title
 * @param description String for description
 * @param isIconMode Boolean to show either icon or image
 * @param modifier
 * @param withDivider Boolean to show or hide divider
 * @param testTag String for test tag, which will used to identify the item itself
 * @param onClickListener to pass any function on click
 */
@Composable
fun ImageIconItem(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    description: String,
    isIconMode: Boolean,
    modifier: Modifier = Modifier,
    withDivider: Boolean = false,
    testTag: String = "",
    onClickListener: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }

    ConstraintLayout(
        modifier = modifier
            .testTag(testTag)
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable(
                onClick = onClickListener,
                interactionSource = interactionSource,
                indication = null
            )
    ) {
        val (iconImage, titleTv, subtitleTv, divider) = createRefs()
        val topMargin = if (isIconMode) 14.dp else 10.dp

        if (isIconMode) {
            Icon(
                modifier = Modifier.constrainAs(iconImage) {
                    top.linkTo(parent.top, 16.dp)
                    start.linkTo(parent.start, 16.dp)
                }
                    .testTag(ICON_ITEM_TAG),
                painter = painterResource(id = icon),
                contentDescription = stringResource(id = title),
            )
        } else {
            Image(
                modifier = Modifier
                    .size(40.dp)
                    .constrainAs(iconImage) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start, 16.dp)
                    }
                    .testTag(IMAGE_ITEM_TAG),
                painter = painterResource(id = icon),
                contentDescription = stringResource(id = title)
            )
        }

        Text(
            modifier = Modifier.constrainAs(titleTv) {
                top.linkTo(parent.top, topMargin)
                linkTo(
                    start = parent.start,
                    end = parent.end,
                    startMargin = 72.dp,
                    endMargin = 18.dp,
                    bias = 0f
                )
                width = Dimension.fillToConstraints
            }
                .testTag(IMAGE_ICON_ITEM_TITLE_TAG),
            text = stringResource(id = title),
            style = MaterialTheme.typography.subtitle2.copy(
                fontWeight = if (isIconMode) FontWeight.Normal else FontWeight.Medium,
                fontSize = if (isIconMode) 16.sp else 14.sp
            ),
            color = MaterialTheme.colors.textColorPrimary
        )

        Text(
            modifier = Modifier
                .testTag("${testTag}_description")
                .constrainAs(subtitleTv) {
                    top.linkTo(titleTv.bottom, 3.dp)
                    linkTo(
                        start = parent.start,
                        end = parent.end,
                        startMargin = 72.dp,
                        endMargin = 18.dp,
                        bias = 0f
                    )
                    width = Dimension.fillToConstraints
                }
                .testTag(IMAGE_ICON_ITEM_TEXT_TAG),
            text = description,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.textColorSecondary
        )

        if (withDivider) {
            CustomDivider(
                withStartPadding = false,
                modifier = Modifier.constrainAs(divider) {
                    top.linkTo(subtitleTv.bottom, topMargin)
                    end.linkTo(parent.end)
                    start.linkTo(parent.start, 144.dp)
                }
                    .testTag(IMAGE_ICON_ITEM_DIVIDER_TAG),
            )
        }
    }
}

internal const val IMAGE_ITEM_TAG = "image_icon_item:image"
internal const val ICON_ITEM_TAG = "image_icon_item:icon"
internal const val IMAGE_ICON_ITEM_TITLE_TAG = "image_icon_item:title"
internal const val IMAGE_ICON_ITEM_TEXT_TAG = "image_icon_item:description"
internal const val IMAGE_ICON_ITEM_DIVIDER_TAG = "image_icon_item:divider"

@CombinedThemePreviews
@Composable
private fun ImageItemPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ImageIconItem(
            icon = R.drawable.ic_recovery_key_circle,
            title = R.string.back_up_recovery_key,
            description = stringResource(R.string.backup_recovery_key_subtitle),
            isIconMode = false,
            testTag = "ADD_PHONE_NUMBER",
            onClickListener = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun IconItemPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ImageIconItem(
            icon = R.drawable.ic_contacts_connection,
            title = R.string.back_up_recovery_key,
            description = stringResource(R.string.back_up_recovery_key),
            isIconMode = true,
            testTag = "ADD_PHONE_NUMBER",
            onClickListener = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun IconItemWithDividerPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ImageIconItem(
            icon = R.drawable.ic_contacts_connection,
            title = R.string.back_up_recovery_key,
            description = stringResource(R.string.back_up_recovery_key),
            isIconMode = true,
            testTag = "ADD_PHONE_NUMBER",
            withDivider = true,
        )
    }
}
