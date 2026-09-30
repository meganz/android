package mega.privacy.android.app.presentation.offline.offlinefileinfocompose.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.Dimension
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.layouts.CollapsibleHeaderWithTitle
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorSecondary

@Composable
internal fun FileInfoHeader(
    title: String,
    iconResource: Int?,
    accessPermissionDescription: Int?,
) = CollapsibleHeaderWithTitle(
    appBarType = AppBarType.BACK_NAVIGATION,
    title = title,
    modifier = Modifier.testTag(TEST_TAG_FILE_INFO_HEADER)
) { titleConstrainedLayoutReference ->
    val (icon, permission) = createRefs()
    if (iconResource != null) {
        //icon
        Image(
            modifier = Modifier
                .testTag(TEST_TAG_ICON)
                .constrainAs(icon) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(titleConstrainedLayoutReference.bottom, 16.dp)
                    width = Dimension.value(96.dp)
                    height = Dimension.value(96.dp)
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
                        start.linkTo(parent.start, 72.dp)
                        top.linkTo(titleConstrainedLayoutReference.bottom, 5.dp)
                    }

            )
        }
    }
}

@CombinedTextAndThemePreviews
@Composable
private fun FileInfoHeaderPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        FileInfoHeader(
            title = "Example file.txt",
            iconResource = iconPackR.drawable.ic_text_medium_solid,
            accessPermissionDescription = null,
        )
    }
}

internal const val TEST_TAG_ICON = "TestTagIcon"
internal const val TEST_TAG_ACCESS = "TestTagAccess"
internal const val TEST_TAG_FILE_INFO_HEADER = "file_info_header"