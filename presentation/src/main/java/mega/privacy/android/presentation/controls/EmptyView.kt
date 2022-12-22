package mega.privacy.android.presentation.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import mega.privacy.android.presentation.theme.grey_100
import mega.privacy.android.presentation.theme.grey_300
import mega.privacy.android.presentation.theme.grey_600
import mega.privacy.android.presentation.theme.grey_900

/**
 * Reusable EmptyView with Icon & Text
 */
@Composable
fun EmptyView(
    imageResId: Int,
    textResId: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = imageResId),
            contentDescription = "Empty",
            modifier = Modifier.padding(bottom = 30.dp),
            tint = if (!MaterialTheme.colors.isLight) {
                Color.Gray
            } else {
                Color.Unspecified
            }
        )

        val placeHolderStart = "[B]"
        val placeHolderEnd = "[/B]"
        val boldPlaceHolderStart = "[A]"
        val boldPlaceHolderEnd = "[/A]"

        val text: String =
            stringResource(id = textResId)

        Text(
            color = if (MaterialTheme.colors.isLight) grey_600 else grey_300,
            text = buildAnnotatedString {
                append(text.substring(0, text.indexOf(placeHolderStart)))

                append(
                    text.substring(
                        text.indexOf(placeHolderStart),
                        text.indexOf(placeHolderEnd)
                    ).replace("[B]", "")
                )

                append(
                    text.substring(
                        text.indexOf(placeHolderEnd),
                        text.indexOf(boldPlaceHolderStart)
                    ).replace("[/B]", "")
                )

                withStyle(SpanStyle(if (MaterialTheme.colors.isLight) grey_900 else grey_100)) {
                    append(
                        text.substring(
                            text.indexOf(boldPlaceHolderStart),
                            text.indexOf(boldPlaceHolderEnd)
                        ).replace("[A]", "")
                    )
                }

                append(
                    text.substring(text.indexOf(boldPlaceHolderEnd)).replace("[/A]", "")
                )
            }
        )
    }
}