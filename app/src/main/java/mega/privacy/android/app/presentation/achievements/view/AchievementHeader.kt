package mega.privacy.android.app.presentation.achievements.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.theme.extensions.dark_blue_500_dark_blue_200
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary

@Composable
internal fun AchievementHeader(storageQuota: String?) {
    Card(shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
                text = stringResource(id = R.string.unlocked_rewards_title),
                style = MaterialTheme.typography.subtitle1,
            )

            storageQuota?.let {
                MegaSpannedText(
                    value = it,
                    baseStyle = MaterialTheme.typography.subtitle2.copy(
                        color = MaterialTheme.colors.dark_blue_500_dark_blue_200,
                        fontWeight = FontWeight.Bold
                    ),
                    styles = mapOf(SpanIndicator('A') to SpanStyle(fontSize = 2.5.em)),
                )
            } ?: Text(
                text = "...",
                style = MaterialTheme.typography.subtitle2.copy(
                    color = MaterialTheme.colors.dark_blue_500_dark_blue_200
                ),
            )

            Text(
                modifier = Modifier.padding(bottom = 30.dp),
                text = stringResource(id = R.string.unlocked_storage_title),
                style = MaterialTheme.typography.caption.copy(
                    color = MaterialTheme.colors.textColorPrimary
                ),
            )
        }
    }
}
