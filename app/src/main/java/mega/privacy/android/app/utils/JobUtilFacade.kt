package mega.privacy.android.app.utils

import android.content.Context
import mega.privacy.android.app.utils.wrapper.JobUtilWrapper
import javax.inject.Inject

/**
 * The implementation of [mega.privacy.android.app.utils.wrapper.JobUtilWrapper]
 */
class JobUtilFacade @Inject constructor() : JobUtilWrapper {

    override fun isOverQuota(context: Context): Boolean = JobUtil.isOverQuota(context)
}
