package mega.privacy.android.app.presentation.fileinfo.view

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.Dimension
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.layouts.CollapsibleHeaderWithTitle
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

@Composable
internal fun FileInfoHeader(
    title: String,
    iconResource: Int?,
    accessPermissionDescription: Int?,
) = CollapsibleHeaderWithTitle(
    appBarType = AppBarType.BACK_NAVIGATION,
    title = title
) { titleConstrainedLayoutReference ->
    val (icon, permission) = createRefs()
    if (iconResource != null) {
        //icon
        Image(
            modifier = Modifier
                .testTag(TEST_TAG_ICON)
                .constrainAs(icon) {
                    start.linkTo(parent.start, 16.dp)
                    top.linkTo(parent.top, 56.dp)
                    width = Dimension.value(24.dp)
                    height = Dimension.value(24.dp)
                },
            painter = painterResource(id = iconResource),
            contentDescription = "Icon"
        )

        //permission text
        accessPermissionDescription?.let { strRes ->
            Text(
                text = stringResource(id = strRes),
                style = MaterialTheme.typography.body2.copy(
                    color = MaterialTheme.colors.textColorSecondary,
                    letterSpacing = (-0.025).sp
                ),
                modifier = Modifier
                    .testTag(TEST_TAG_ACCESS)
                    .constrainAs(permission) {
                        start.linkTo(parent.start, paddingStartDefault.dp)
                        top.linkTo(titleConstrainedLayoutReference.bottom, 5.dp)
                    }

            )
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@CombinedTextAndThemePreviews
@Composable
private fun FileInfoHeaderPreview(
    @PreviewParameter(FileInfoViewStatePreviewsProvider::class) viewState: FileInfoViewState,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        FileInfoHeader(
            title = viewState.title,
            iconResource = viewState.iconResource,
            accessPermissionDescription = viewState.accessPermission.description(),
        )
    }
}

internal const val TEST_TAG_ICON = "TestTagIcon"
internal const val TEST_TAG_ACCESS = "TestTagAccess"