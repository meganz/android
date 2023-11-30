package mega.privacy.android.app.presentation.twofactorauthentication.view.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_800
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.subtitle1medium
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.legacy.core.ui.controls.text.MegaSpannedAlignedText

@Composable
internal fun AuthenticationCompletedScreen(
    isMasterKeyExported: Boolean,
    onExportRkClicked: () -> Unit,
    onDismissClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.grey_020_grey_800)
        ) {
            Column(horizontalAlignment = CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.ic_2fa),
                    contentDescription = "",
                    modifier = Modifier
                        .padding(top = 24.dp)
                )
                Spacer(modifier = Modifier.padding(top = 12.dp))
                Text(
                    text = stringResource(id = R.string.title_2fa_enabled),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.textColorPrimary,
                    style = MaterialTheme.typography.subtitle1,
                )
                Spacer(modifier = Modifier.padding(top = 20.dp))
                Text(
                    text = stringResource(id = R.string.description_2fa_enabled),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.textColorSecondary,
                    style = MaterialTheme.typography.subtitle1
                )
                Spacer(modifier = Modifier.padding(top = 20.dp))

            }
        }

        Spacer(modifier = Modifier.padding(top = 20.dp))

        MegaSpannedAlignedText(
            modifier = Modifier
                .testTag(RK_EXPORT_INSTRUCTION_TEST_TAG)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            value = stringResource(id = R.string.recommendation_2fa_enabled),
            baseStyle = MaterialTheme.typography.subtitle1.copy(
                color = MaterialTheme.colors.textColorPrimary,
            ),
            textAlign = TextAlign.Center,
            styles = hashMapOf(
                SpanIndicator('A') to SpanStyle(
                    color = MaterialTheme.colors.textColorPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
        )

        Spacer(modifier = Modifier.padding(top = 30.dp))

        RecoveryKeyBox(
            testTag = RK_EXPORT_BOX_TEST_TAG,
            modifier = Modifier.align(CenterHorizontally),
            onExportRkClicked = onExportRkClicked
        )

        Spacer(modifier = Modifier.padding(top = 40.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            RaisedDefaultMegaButton(
                modifier = Modifier
                    .wrapContentSize(),
                textId = R.string.general_export,
                onClick = onExportRkClicked
            )

            Spacer(modifier = Modifier.width(20.dp))

            if (isMasterKeyExported) {
                TextMegaButton(
                    modifier = Modifier
                        .wrapContentSize(),
                    textId = R.string.general_dismiss,
                    onClick = onDismissClicked
                )
            }
        }
    }
}

@Composable
fun RecoveryKeyBox(
    testTag: String,
    modifier: Modifier = Modifier,
    onExportRkClicked: () -> Unit,
) {
    Box(
        modifier = modifier
            .wrapContentSize()
            .testTag(testTag)
            .background(color = MaterialTheme.colors.grey_020_grey_800)
            .border(
                1.dp,
                color = MaterialTheme.colors.grey_alpha_012_white_alpha_012,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onExportRkClicked() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 40.dp, vertical = 20.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_text_thumbnail),
                contentDescription = "",
                modifier = Modifier.size(size = 24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            val rkFileName = "${stringResource(id = R.string.general_rk)}.txt"

            Text(
                text = rkFileName,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colors.textColorPrimary,
                style = MaterialTheme.typography.subtitle1medium,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewAuthenticationCompletedScreen() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        AuthenticationCompletedScreen(
            isMasterKeyExported = false,
            onExportRkClicked = {},
            onDismissClicked = {})
    }
}

internal const val RK_EXPORT_BOX_TEST_TAG = "RK_EXPORT_BOX"
internal const val RK_EXPORT_INSTRUCTION_TEST_TAG = "RK_EXPORT_INSTRUCTION"


