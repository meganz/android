package mega.privacy.android.app.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.main.ui.theme.AndroidTheme

@AndroidEntryPoint
class FeatureFlagActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SearchBar()
            previewFeatureFlagList()
        }
    }
}
