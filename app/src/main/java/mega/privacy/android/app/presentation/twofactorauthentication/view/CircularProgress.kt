package mega.privacy.android.app.presentation.twofactorauthentication.view

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun CircularProgress(testTag: String, modifier: Modifier = Modifier) =
    CircularProgressIndicator(
        modifier = modifier
            .padding(top = 10.dp)
            .size(72.dp)
            .testTag(testTag),
        color = MaterialTheme.colors.secondary
    )