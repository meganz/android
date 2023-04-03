package mega.privacy.android.app.data.extensions

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util

/**
 * Get info
 *
 * @param context
 */
fun DocumentFile.getInfo(context: Context): String = if (isDirectory) {
    FileUtil.getFileFolderInfo(this, context)
} else {
    TextUtil.getFileInfo(
        Util.getSizeString(length(), context),
        TimeUtils.formatLongDateTime(lastModified() / 1000)
    )
}