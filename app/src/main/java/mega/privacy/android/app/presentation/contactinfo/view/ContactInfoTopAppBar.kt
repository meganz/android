package mega.privacy.android.app.presentation.contactinfo.view

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalElevationOverlay
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.core.graphics.toColorInt
import coil.compose.rememberAsyncImagePainter
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.grey_alpha_070
import mega.privacy.android.core.ui.theme.white_alpha_087
import mega.privacy.android.domain.entity.contacts.UserChatStatus

/**
 * Collapsing top bar for contact info screen
 */
@OptIn(ExperimentalMotionApi::class)
@Composable
fun ContactInfoTopAppBar(
    onBackPress: () -> Unit,
    avatar: Bitmap?,
    primaryDisplayName: String,
    userChatStatus: UserChatStatus,
    defaultAvatarColor: String?,
    progress: Float,
    statusBarHeight: Float,
    headerMinHeight: Float,
    headerHeight: Float,
) {
    val backgroundColor = MaterialTheme.colors.surface
        .colorAtElevation(absoluteElevation = AppBarDefaults.TopAppBarElevation.value.dp)

    val primaryTextColorAlpha =
        lerp(white_alpha_087, MaterialTheme.colors.textColorPrimary, progress)
    val gradientColor = lerp(grey_alpha_070, Color.Transparent, progress)

    val backgroundColorAlpha = lerp(
        backgroundColor,
        Color.Transparent,
        1 - progress
    )


    MotionLayout(
        start = startConstraintSet(statusBarHeight),
        end = endConstraintSet(statusBarHeight, headerMinHeight),
        progress = progress,
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight.dp)
    ) {
        Box(
            modifier = Modifier
                .layoutId(BACKGROUND)
                .fillMaxSize()
                .shadow(2.dp)
        ) {
            Image(
                painter = avatar?.let { rememberAsyncImagePainter(model = it) }
                    ?: ColorPainter(
                        color = Color(defaultAvatarColor?.toColorInt() ?: -1)
                    ),
                contentDescription = BACKGROUND,
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColorAlpha)
                    .drawWithCache {
                        val gradient = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, gradientColor),
                            startY = size.height / 100,
                            endY = size.height
                        )
                        onDrawWithContent {
                            drawContent()
                            drawRect(gradient, blendMode = BlendMode.Multiply)
                        }
                    }
                    .graphicsLayer {
                        alpha = 1 - progress
                    },
                contentScale = ContentScale.Crop,
            )
        }
        UserStatusView(
            modifier = Modifier
                .layoutId(TITLE)
                .wrapContentHeight(),
            title = primaryDisplayName,
            userChatStatus = userChatStatus,
            progress = progress
        )


        IconButton(modifier = Modifier.layoutId(BACK_ARROW), onClick = { onBackPress() }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back_black),
                contentDescription = BACK_ARROW,
                tint = primaryTextColorAlpha
            )
        }
        Row(
            modifier = Modifier
                .layoutId(ACTIONS)
                .wrapContentWidth()
        ) {
            ActionItems(iconColor = primaryTextColorAlpha)
        }
    }
}

@Composable
private fun ActionItems(iconColor: Color) {
    IconButton(onClick = { /*TODO*/ }) {
        Icon(
            painter = painterResource(id = R.drawable.ic_send_to_contact),
            contentDescription = "",
            tint = iconColor
        )
    }
    IconButton(onClick = { /*TODO*/ }) {
        Icon(
            painter = painterResource(id = R.drawable.ic_share),
            contentDescription = "",
            tint = iconColor
        )
    }
    IconButton(onClick = { /*TODO*/ }) {
        Icon(
            painter = painterResource(id = R.drawable.ic_dots_vertical_white),
            contentDescription = "",
            tint = iconColor
        )
    }
}


// Constraint Sets defined by using Kotlin DSL option
private fun startConstraintSet(statusBarHeight: Float) = ConstraintSet {
    val poster = createRefFor(BACKGROUND)
    val title = createRefFor(TITLE)
    val actions = createRefFor(ACTIONS)
    val back = createRefFor(BACK_ARROW)

    constrain(poster) {
        width = Dimension.fillToConstraints
        start.linkTo(parent.start)
        end.linkTo(parent.end)
        top.linkTo(parent.top)
    }

    constrain(back) {
        start.linkTo(parent.start)
        top.linkTo(parent.top, statusBarHeight.dp)
    }

    constrain(actions) {
        end.linkTo(parent.end)
        top.linkTo(back.top)
        bottom.linkTo(back.bottom)
    }
    constrain(title) {
        width = Dimension.fillToConstraints
        start.linkTo(parent.start, 72.dp)
        end.linkTo(actions.end, 72.dp)
        bottom.linkTo(parent.bottom, 8.dp)
    }


}

private fun endConstraintSet(statusBarHeight: Float, headerMinHeight: Float) = ConstraintSet {
    val poster = createRefFor(BACKGROUND)
    val title = createRefFor(TITLE)
    val actions = createRefFor(ACTIONS)
    val back = createRefFor(BACK_ARROW)

    constrain(poster) {
        width = Dimension.fillToConstraints
        height = Dimension.value(headerMinHeight.dp)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
        top.linkTo(parent.top)
    }

    constrain(back) {
        start.linkTo(parent.start)
        top.linkTo(parent.top, statusBarHeight.dp)
    }
    constrain(actions) {
        end.linkTo(parent.end)
        top.linkTo(back.top)
        bottom.linkTo(back.bottom)
    }

    constrain(title) {
        width = Dimension.fillToConstraints
        start.linkTo(parent.start, 72.dp)
        top.linkTo(back.top, 12.dp)
        end.linkTo(actions.start)
        bottom.linkTo(back.bottom)
    }
}

private const val BACKGROUND = "background"
private const val BACK_ARROW = "back_arrow"
private const val TITLE = "title"
private const val ACTIONS = "actions"

@Composable
private fun Color.colorAtElevation(
    absoluteElevation: Dp,
): Color = LocalElevationOverlay.current?.apply(this, absoluteElevation) ?: this

