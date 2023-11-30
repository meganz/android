package mega.privacy.android.app.main.dialog.businessgrace

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class BusinessGraceDialogFragment : DialogFragment() {
    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var myAccountInfo: MyAccountInfo

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Timber.d("showBusinessGraceAlert")
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                MegaAppTheme(isDark = themeMode.isDarkMode()) {
                    BusinessGraceView(onDismissRequest = {
                        myAccountInfo.isBusinessAlertShown = false
                        dismissAllowingStateLoss()
                    })
                }
            }
        }
    }

    companion object {
        const val TAG = "BusinessGraceDialogFragment"
    }
}

@Composable
private fun BusinessGraceView(modifier: Modifier = Modifier, onDismissRequest: () -> Unit = {}) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
        )
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            elevation = 24.dp,
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.general_something_went_wrong_error),
                    style = MaterialTheme.typography.h6,
                )
                Image(
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .size(100.dp),
                    painter = painterResource(id = R.drawable.ic_account_expired),
                    contentDescription = "Image Account Expired",
                )
                Text(
                    modifier = Modifier.padding(top = 20.dp, start = 24.dp, end = 24.dp),
                    text = stringResource(id = R.string.grace_period_admin_alert),
                    style = MaterialTheme.typography.subtitle1,
                )

                TextMegaButton(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .align(Alignment.End),
                    textId = R.string.general_dismiss,
                    onClick = onDismissRequest,
                )
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "HorizontalButtonDialog")
@Composable
private fun BusinessGraceViewViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        BusinessGraceView(
            onDismissRequest = { },
        )
    }
}