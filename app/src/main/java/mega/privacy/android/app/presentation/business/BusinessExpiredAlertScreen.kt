package mega.privacy.android.app.presentation.business

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.business.model.BusinessExpiredAlertUiState
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.resources.R as sharedR

private val AdminGradientStart = Color(0xFF880E4F)
private val AdminGradientEnd = Color(0xFFAD1457)
private val UserGradientStart = Color(0xFF0D47A1)
private val UserGradientEnd = Color(0xFF1565C0)

/**
 * Business Expired Alert Screen
 *
 * @param uiState the UI state
 * @param onDismiss callback when dismiss button is clicked
 */
@Composable
fun BusinessExpiredAlertScreen(
    uiState: BusinessExpiredAlertUiState,
    onDismiss: () -> Unit,
) {
    val isPortrait =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val imageHeight = if (isPortrait) 284.dp else 136.dp

    val isAdminOrProFlexi = uiState.isProFlexiAccount || uiState.isMasterBusinessAccount

    val gradientBrush = if (isAdminOrProFlexi) {
        Brush.verticalGradient(colors = listOf(AdminGradientStart, AdminGradientEnd))
    } else {
        Brush.verticalGradient(colors = listOf(UserGradientStart, UserGradientEnd))
    }

    val imageRes = when {
        isAdminOrProFlexi -> if (isPortrait) {
            R.drawable.ic_account_expired_admin_portrait
        } else {
            R.drawable.ic_account_expired_admin_landscape
        }

        else -> if (isPortrait) {
            R.drawable.ic_account_expired_user_portrait
        } else {
            R.drawable.ic_account_expired_user_landscape
        }
    }

    val titleText = if (uiState.isProFlexiAccount) {
        stringResource(sharedR.string.account_pro_flexi_account_deactivated_dialog_title)
    } else {
        stringResource(R.string.account_business_account_deactivated_dialog_title)
    }

    val bodyText = when {
        uiState.isProFlexiAccount -> stringResource(sharedR.string.account_pro_flexi_account_deactivated_dialog_body)
        uiState.isMasterBusinessAccount -> stringResource(R.string.account_business_account_deactivated_dialog_admin_body)
        else -> stringResource(R.string.account_business_account_deactivated_dialog_sub_user_body)
    }

    val showSubtext = !uiState.isProFlexiAccount && !uiState.isMasterBusinessAccount

    val buttonText = if (uiState.isProFlexiAccount) {
        stringResource(sharedR.string.general_ok)
    } else {
        stringResource(R.string.account_business_account_deactivated_dialog_button)
    }

    MegaScaffold(
        modifier = Modifier.navigationBarsPadding(),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .background(gradientBrush),
                contentAlignment = if (isAdminOrProFlexi) Alignment.Center else Alignment.BottomCenter,
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MegaText(
                text = titleText,
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )

            MegaText(
                text = bodyText,
                textColor = TextColor.Secondary,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )

            if (showSubtext) {
                MegaText(
                    text = stringResource(R.string.expired_user_business_text_2),
                    textColor = TextColor.Secondary,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            RaisedDefaultMegaButton(
                text = buttonText,
                onClick = onDismiss,
                modifier = Modifier.padding(top = 64.dp, bottom = 64.dp),
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun BusinessExpiredAlertScreenProFlexiPreview() {
    AndroidTheme(isDark = false) {
        BusinessExpiredAlertScreen(
            uiState = BusinessExpiredAlertUiState(
                isProFlexiAccount = true,
                isMasterBusinessAccount = false,
            ),
            onDismiss = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun BusinessExpiredAlertScreenMasterBusinessPreview() {
    AndroidTheme(isDark = false) {
        BusinessExpiredAlertScreen(
            uiState = BusinessExpiredAlertUiState(
                isProFlexiAccount = false,
                isMasterBusinessAccount = true,
            ),
            onDismiss = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun BusinessExpiredAlertScreenSubUserPreview() {
    AndroidTheme(isDark = false) {
        BusinessExpiredAlertScreen(
            uiState = BusinessExpiredAlertUiState(
                isProFlexiAccount = false,
                isMasterBusinessAccount = false,
            ),
            onDismiss = {},
        )
    }
}
