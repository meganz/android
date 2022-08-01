package mega.privacy.android.app.presentation.settings.reportissue.view

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.theme.AndroidTheme

@Composable
fun ErrorBanner(
    errorMessage: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = if (!MaterialTheme.colors.isLight) colorResource(id = R.color.yellow_700_alpha_015) else colorResource(
                    id = R.color.yellow_100
                )
            )
            .padding(8.dp)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = errorMessage,
            color = if (!MaterialTheme.colors.isLight) colorResource(id = R.color.yellow_700) else Color.Black,
        )
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview
@Composable
fun PreviewErrorBanner() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ErrorBanner(
            errorMessage = "This is an error message. If you broke it, " +
                    "please fix whatever is wrong, if we broke it,we are very sorry" +
                    " and will fix it as soon as possible. If someone else broke it, " +
                    "there is not much we can do."
        )
    }
}