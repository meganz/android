package mega.privacy.android.app.presentation.favourites.facade

import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import javax.inject.Inject

/**
 * The implementation of StringUtilWrapper
 */
class StringUtilFacade @Inject constructor() : StringUtilWrapper {

    override fun getSizeString(size: Long): String = Util.getSizeString(size)

    override fun formatLongDateTime(timestamp: Long): String =
        TimeUtils.formatLongDateTime(timestamp)

    override fun getFolderInfo(numChildFolders: Int, numChildFiles: Int): String =
        TextUtil.getFolderInfo(numChildFolders, numChildFiles)
}
