package mega.privacy.android.app.presentation.fileinfo.view

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.constraintlayout.compose.Dimension
import coil.compose.rememberAsyncImagePainter
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.core.ui.theme.grey_alpha_026

@Composable
internal fun FileInfoHeader(
    viewState: FileInfoViewState,
    modifier: Modifier = Modifier,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
    ) {
        val (shadowTop, shadowBottom, icon, permission) = createRefs()

        //preview or icon
        if (viewState.hasPreview) {
            viewState.actualPreviewUriString?.let { uri ->
                PreviewWithShadow(shadowBottom, shadowTop, uri)
            }
        } else {
            viewState.iconResource?.let { icRes ->
                Image(
                    modifier = Modifier
                        .constrainAs(icon) {
                            start.linkTo(parent.start, 16.dp)
                            top.linkTo(parent.top, 44.dp)
                            width = Dimension.value(24.dp)
                            height = Dimension.value(24.dp)
                        },
                    painter = painterResource(id = icRes),
                    contentDescription = "Icon"
                )
            }


            //permission text
            if (viewState.isIncomingSharedNode) {
                viewState.accessPermission.description()?.let { strRes ->
                    Text(
                        text = stringResource(id = strRes),
                        style = MaterialTheme.typography.body2.copy(
                            color = MaterialTheme.colors.textColorSecondary,
                            letterSpacing = (-0.025).sp
                        ),
                        modifier = Modifier.constrainAs(permission) {
                            start.linkTo(parent.start, paddingStartDefault.dp)
                            bottom.linkTo(parent.bottom, 5.dp)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConstraintLayoutScope.PreviewWithShadow(
    shadowTop: ConstrainedLayoutReference,
    shadowBottom: ConstrainedLayoutReference,
    previewString: String,
) {

    Image(
        modifier = Modifier.fillMaxSize(),
        painter = rememberAsyncImagePainter(model = previewString),
        contentDescription = "Preview"
    )
    //shadow top
    Box(
        modifier = Modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        grey_alpha_026,
                        Color.Transparent,
                    )
                )
            )
            .constrainAs(shadowTop) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(parent.top)
                height = Dimension.percent(0.5f)
                width = Dimension.matchParent
            },
    )
    //shadow bottom
    Box(
        modifier = Modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        grey_alpha_026,
                    )
                )
            )
            .constrainAs(shadowBottom) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom)
                height = Dimension.percent(0.5f)
                width = Dimension.matchParent
            },
    )
}

@SuppressLint("UnrememberedMutableState")
@CombinedTextAndThemePreviews
@Composable
private fun FileInfoHeaderPreview(
    @PreviewParameter(FileInfoViewStatePreviewsProvider::class) viewState: FileInfoViewState,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FileInfoHeader(
            viewState = viewState,
            modifier = Modifier.height(182.dp)
        )
    }
}