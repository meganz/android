package mega.privacy.android.feature.photos.presentation.albums.copyright

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.TextOnlyButton
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.photos.R
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun CopyRightScreen(
    onAgree: () -> Unit,
    onDisagree: () -> Unit,
    modifier: Modifier = Modifier,
    disableBackPress: Boolean = true
) {
    BackHandler(disableBackPress) {
        // Disable back press - user must choose Agree or Disagree
    }

    MegaScaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(
                modifier = Modifier
                    .height(48.dp)
                    .fillMaxWidth()
            )

            // Legacy icon for now, will be updated later
            MegaIcon(
                painter = painterResource(R.drawable.ic_copyright),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
            )

            Spacer(modifier = Modifier.height(33.dp))

            MegaText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(sharedR.string.copyright_screen_title),
                style = AppTheme.typography.titleMedium,
                textColor = TextColor.Brand,
            )

            Spacer(modifier = Modifier.height(23.dp))

            MegaText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(sharedR.string.copyright_screen_description_first_paragraph),
                style = AppTheme.typography.titleSmall,
                textColor = TextColor.Secondary,
            )

            Spacer(modifier = Modifier.height(33.dp))

            MegaText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(sharedR.string.copyright_screen_description_second_paragraph),
                style = AppTheme.typography.titleSmall,
                textColor = TextColor.Secondary,
            )

            Spacer(modifier = Modifier.height(46.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                TextOnlyButton(
                    modifier = Modifier.wrapContentSize(),
                    onClick = onDisagree,
                    text = stringResource(sharedR.string.copyright_action_disagree)
                )

                PrimaryFilledButton(
                    modifier = Modifier.wrapContentSize(),
                    onClick = onAgree,
                    text = stringResource(sharedR.string.copyright_action_agree)
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun CopyRightScreenPreview() {
    AndroidThemeForPreviews {
        CopyRightScreen(
            onAgree = {},
            onDisagree = {}
        )
    }
}
