package mega.privacy.android.feature.example.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.TextColor

class ExampleLegacyActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = intent.getStringExtra(LEGACY_SCREEN_PARAM) ?: "No content provided"
        setContent {
            AndroidTheme(isDark = isSystemInDarkTheme()) {
                MegaScaffold { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        MegaText(
                            text = "This is a fake legacy screen.",
                            textColor = TextColor.Primary
                        )
                        MegaText(
                            text = content,
                            textColor = TextColor.Warning
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val LEGACY_SCREEN_PARAM = "LEGACY_SCREEN_PARAM"

        fun getIntent(context: Context, content: String): Intent {
            return Intent(context, ExampleLegacyActivity::class.java).apply {
                putExtra(LEGACY_SCREEN_PARAM, content)
            }
        }
    }
}