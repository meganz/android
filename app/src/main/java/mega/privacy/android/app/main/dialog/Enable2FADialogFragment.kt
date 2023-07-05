package mega.privacy.android.app.main.dialog

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.IntentConstants
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.twofactorauthentication.TwoFactorAuthenticationActivity
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class Enable2FADialogFragment : DialogFragment() {
    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Timber.d("showClearRubbishBinDialog")
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                AndroidTheme(isDark = themeMode.isDarkMode()) {
                    Enable2FADialogView(onDismissRequest = {
                        dismissAllowingStateLoss()
                    }, onEnable2FA = {
                        open2FAScreen()
                    })
                }
            }
        }
    }

    private fun open2FAScreen() {
        val intent = Intent(requireContext(), TwoFactorAuthenticationActivity::class.java)
        intent.putExtra(IntentConstants.EXTRA_NEW_ACCOUNT, true)
        startActivity(intent)
        dismissAllowingStateLoss()
    }

    companion object {
        const val TAG = "Enable2FADialogFragment"
    }
}

@Composable
private fun Enable2FADialogView(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onEnable2FA: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .padding(horizontal = 40.dp)
                .fillMaxWidth(),
            elevation = 24.dp,
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.title_enable_2fa),
                    style = MaterialTheme.typography.h6,
                    textAlign = TextAlign.Center,
                )
                Image(
                    modifier = Modifier.padding(top = 8.dp),
                    painter = painterResource(id = R.drawable.ic_2fa),
                    contentDescription = "Icon 2 FA"
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(id = R.string.two_factor_authentication_explain),
                    style = MaterialTheme.typography.subtitle1,
                    textAlign = TextAlign.Center,
                )

                RaisedDefaultMegaButton(
                    modifier = Modifier.padding(top = 32.dp),
                    textId = R.string.general_enable,
                    onClick = onEnable2FA
                )
                TextMegaButton(
                    modifier = Modifier.padding(top = 8.dp),
                    textId = R.string.general_skip,
                    onClick = onDismissRequest
                )
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "HorizontalButtonDialog")
@Composable
private fun Enable2FADialogViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        Enable2FADialogView(
            onDismissRequest = { },
            onEnable2FA = {},
        )
    }
}