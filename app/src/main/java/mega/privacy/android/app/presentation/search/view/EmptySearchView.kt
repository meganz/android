package mega.privacy.android.app.presentation.search.view

import androidx.compose.foundation.Image
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.presentation.theme.AndroidTheme
import mega.privacy.android.presentation.theme.grey_300
import mega.privacy.android.presentation.theme.grey_600

@Composable
fun EmptySearchView() {
    Column(modifier = Modifier
        .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally) {
        val isPortrait =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

        Spacer(modifier = Modifier.height(if (isPortrait) 200.dp else 100.dp))

        Image(painter = painterResource(id = R.drawable.ic_empty_search),
            contentDescription = "Empty search image",
            alpha = if (MaterialTheme.colors.isLight) 1f else 0.16f)

        Spacer(modifier = Modifier.height(29.dp))

        Text(modifier = Modifier.padding(11.dp),
            text = stringResource(id = R.string.no_results_found).uppercase(),
            style = MaterialTheme.typography.subtitle2,
            color = if (MaterialTheme.colors.isLight) grey_300 else grey_600)

        Spacer(modifier = Modifier.height(50.dp))
    }
}

@Preview
@Composable
fun PreviewEmptySearchView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        EmptySearchView()
    }
}