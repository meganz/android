package mega.privacy.android.feature.example.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.MegaOutlinedButton
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.TextColor
import kotlin.random.Random

class ExampleLegacyResultActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                        val result = Random.nextInt(1, 101)
                        MegaText(
                            text = "This is a fake legacy screen that returns a result.",
                            textColor = TextColor.Primary
                        )
                        MegaText(
                            text = "The result to return is: $result",
                            textColor = TextColor.Warning
                        )
                        MegaOutlinedButton(
                            modifier = Modifier,
                            text = "Return result",
                            onClick = {
                                val resultIntent = Intent().apply {
                                    putExtra(LEGACY_SCREEN_RESULT_KEY, result)
                                }
                                setResult(RESULT_OK, resultIntent)
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    companion object : ActivityResultContract<Unit, Int?>() {
        private const val LEGACY_SCREEN_RESULT_KEY = "LEGACY_SCREEN_RESULT_KEY"
        override fun createIntent(
            context: Context,
            input: Unit,
        ) = Intent(context, ExampleLegacyResultActivity::class.java)

        override fun parseResult(
            resultCode: Int,
            intent: Intent?,
        ) = if (resultCode == RESULT_OK && intent != null) {
            intent.getIntExtra(LEGACY_SCREEN_RESULT_KEY, -1)
        } else null
    }
}