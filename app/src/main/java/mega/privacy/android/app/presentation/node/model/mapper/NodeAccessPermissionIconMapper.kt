package mega.privacy.android.app.presentation.node.model.mapper

import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.icon.pack.R as iconPackR
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
        AccessPermission.READ -> iconPackR.drawable.ic_eye_medium_thin_outline
        AccessPermission.READWRITE -> iconPackR.drawable.ic_edit_medium_thin_outline
        AccessPermission.FULL -> iconPackR.drawable.ic_star_medium_thin_outline
        else -> null
    }
}