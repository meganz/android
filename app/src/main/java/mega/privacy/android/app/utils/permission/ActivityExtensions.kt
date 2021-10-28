package mega.privacy.android.app.utils.permission

import androidx.fragment.app.FragmentActivity

/**
 * Build a permissions requester for ordinary permissions that require a grant from the user.
 * Used by sub activity of [FragmentActivity]
 */

fun FragmentActivity.permissionsBuilder(permissions: ArrayList<String>
) = PermissionsRequesterImpl.Builder(
    activity = this,
    permissions
)