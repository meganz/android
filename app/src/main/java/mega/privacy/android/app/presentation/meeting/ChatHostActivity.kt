package mega.privacy.android.app.presentation.meeting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * Host Activity for new chat room
 */
@AndroidEntryPoint
class ChatHostActivity : AppCompatActivity() {

    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val mode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            AndroidTheme(isDark = mode.isDarkMode()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Hello Chat Activity",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.textColorPrimary
                    )
                }
            }
        }
    }
}