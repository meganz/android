package mega.privacy.android.app.presentation.view.extension

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.intl.Locale
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.domain.entity.node.FileNode

@Composable
internal fun FileNode.fileInfo(): String {
    val modifiedDate = formatModifiedDate(
        java.util.Locale(Locale.current.language, Locale.current.region),
        modificationTime
    )
    val fileSize = formatFileSize(size, LocalContext.current)
    return "$fileSize · $modifiedDate"
}