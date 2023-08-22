package mega.privacy.android.app.presentation.fileinfo.view

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.subtitle1medium
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

@Composable
internal fun FileInfoHeader(
    title: String,
    tintColor: Color,
    backgroundAlpha: Float,
    titleAlpha: Float,
    previewUri: String?,
    iconResource: Int?,
    accessPermissionDescription: Int?,
    modifier: Modifier = Modifier,
    titleDisplacement: Dp = 0.dp,
    statusBarHeight: Dp = 0.dp,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
    ) {
        val (icon, permission, titlePlaceholder, titleVisible) = createRefs()

        //preview or icon
        if (previewUri != null) {
            PreviewWithShadow(
                previewUri,
                backgroundAlpha,
            )
        } else {
            iconResource?.let { icRes ->
                Image(
                    modifier = Modifier
                        .alpha(backgroundAlpha)
                        .testTag(TEST_TAG_ICON)
                        .constrainAs(icon) {
                            start.linkTo(parent.start, 16.dp)
                            top.linkTo(parent.top, appBarHeight.dp + statusBarHeight)
                            width = Dimension.value(24.dp)
                            height = Dimension.value(24.dp)
                        },
                    painter = painterResource(id = icRes),
                    contentDescription = "Icon"
                )
            }

            //permission text
            accessPermissionDescription?.let { strRes ->
                Text(
                    text = stringResource(id = strRes),
                    style = MaterialTheme.typography.body2.copy(
                        color = MaterialTheme.colors.textColorSecondary,
                        letterSpacing = (-0.025).sp
                    ),
                    modifier = Modifier
                        .alpha(backgroundAlpha)
                        .testTag(TEST_TAG_ACCESS)
                        .constrainAs(permission) {
                            start.linkTo(parent.start, paddingStartDefault.dp)
                            top.linkTo(titleVisible.bottom, 5.dp)
                        }

                )
            }
        }
        val appBarBottomGuideline = createGuidelineFromTop(statusBarHeight + appBarHeight.dp)
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1medium,
            textAlign = TextAlign.Start,
            maxLines = 1,
            modifier = Modifier
                .alpha(0f)
                .constrainAs(titlePlaceholder) {
                    //this is just a none visible text with 1 line to place the next text with first line in the center of the toolbar
                    start.linkTo(parent.start, 72.dp)
                    end.linkTo(parent.end, 8.dp)
                    top.linkTo(parent.top, statusBarHeight)
                    bottom.linkTo(appBarBottomGuideline)
                    baseline
                    height = Dimension.wrapContent
                    width = Dimension.fillToConstraints
                }
        )
        Text(
            modifier = Modifier
                .alpha(titleAlpha)
                .fillMaxWidth()
                .constrainAs(titleVisible) {
                    start.linkTo(titlePlaceholder.start)
                    end.linkTo(titlePlaceholder.end)
                    top.linkTo(titlePlaceholder.top, titleDisplacement)
                    height = Dimension.wrapContent
                    width = Dimension.fillToConstraints
                },
            text = title,
            style = MaterialTheme.typography.subtitle1medium.copy(color = tintColor),
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis,
            maxLines = 3,
        )
    }
}

@SuppressLint("UnrememberedMutableState")
@CombinedTextAndThemePreviews
@Composable
private fun FileInfoHeaderPreview(
    @PreviewParameter(FileInfoViewStatePreviewsProvider::class) viewState: FileInfoViewState,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FileInfoHeader(
            title = viewState.title,
            titleAlpha = 1f,
            previewUri = viewState.actualPreviewUriString,
            iconResource = viewState.iconResource,
            accessPermissionDescription = viewState.accessPermission.description(),
            tintColor = MaterialTheme.colors.onSurface,
            backgroundAlpha = 1f,
            modifier = Modifier.height(182.dp),
        )
    }
}

internal const val TEST_TAG_ICON = "TestTagIcon"
internal const val TEST_TAG_ACCESS = "TestTagAccess"