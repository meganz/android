package mega.privacy.android.app.presentation.node.model.mapper

import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * Mapper to get the icon of the access permission
 */
class NodeAccessPermissionIconMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param accessPermission
     */
    operator fun invoke(accessPermission: AccessPermission) = when (accessPermission) {
        AccessPermission.READ -> R.drawable.ic_shared_read
        AccessPermission.READWRITE -> R.drawable.ic_shared_read_write
        AccessPermission.FULL -> R.drawable.ic_shared_fullaccess
        else -> null
    }
}