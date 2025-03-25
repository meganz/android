package mega.privacy.android.app.presentation.settings.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.compose.AndroidFragment
import mega.privacy.android.app.presentation.settings.QASettingsFragment

@Composable
internal fun QASettingsHomeView(){
    AndroidFragment<QASettingsFragment>(
        modifier = Modifier.fillMaxSize()
    )
}