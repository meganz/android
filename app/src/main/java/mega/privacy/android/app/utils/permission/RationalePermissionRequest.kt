package mega.privacy.android.app.utils.permission

import java.lang.ref.WeakReference

/**
 * This instance is for onShowRationale methods
 * to allow it for continuation by calling proceed()
 * or cancellation of a permission request by calling cancel()
 */
internal class RationalePermissionRequest(
    private val requestPermission: WeakReference<() -> Unit>,
    private val permissionDenied: WeakReference<(ArrayList<String>) -> Unit>?
) : PermissionRequest {
    override fun proceed() {
        requestPermission.get()?.invoke()
    }

    override fun cancel() {
        permissionDenied?.get()?.invoke(arrayListOf())
    }

    companion object {
        fun create(
            onPermissionDenied: ((ArrayList<String>) -> Unit)?,
            requestPermission: () -> Unit
        ) = RationalePermissionRequest(
            requestPermission = WeakReference(requestPermission),
            permissionDenied = onPermissionDenied?.let { WeakReference(it) }
        )
    }
}
