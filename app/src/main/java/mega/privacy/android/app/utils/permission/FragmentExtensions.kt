package mega.privacy.android.app.utils.permission

import androidx.fragment.app.Fragment

/**
 * Build a permissions requester for ordinary permissions that require a grant from the user.
 * Should apply after the activity is ready
 *
 * @param permissions array of requester permissions
 */

fun Fragment.permissionsBuilder(
    permissions: Array<String>
) = PermissionsRequesterImpl.Builder(
    activity = requireActivity(),
    permissions
)
