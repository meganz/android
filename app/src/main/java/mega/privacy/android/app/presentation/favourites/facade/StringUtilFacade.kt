package mega.privacy.android.app.presentation.favourites.facade

import android.content.Context
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import javax.inject.Inject

/**
 * The implementation of StringUtilWrapper
 */
class StringUtilFacade @Inject constructor() : StringUtilWrapper {

    override fun getSizeString(size: Long, context: Context): String =
        Util.getSizeString(size, context)

    override fun formatLongDateTime(timestamp: Long): String =
        TimeUtils.formatLongDateTime(timestamp)

    override fun getFolderInfo(numChildFolders: Int, numChildFiles: Int, context: Context): String =
        TextUtil.getFolderInfo(numChildFolders, numChildFiles, context)

}
