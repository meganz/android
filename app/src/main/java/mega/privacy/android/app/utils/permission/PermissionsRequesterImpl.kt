package mega.privacy.android.app.utils.permission

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider

/**
 * Implement of PermissionsRequester
 *
 * @param permissions the permissions that user requires.
 * @param activity the activity that the fragment attached
 * @param onShowRationale the method explains why the permissions are required.
 * @param onPermissionDenied the method invoked if the user deny the permissions.
 * @param onNeverAskAgain the method invoked if the user denies the permissions and tickets "never ask again" option.
 * @param requiresPermission the action requires [permissions].
 * @see PermissionsRequester
 */

class PermissionsRequesterImpl private constructor(
    private val permissions: ArrayList<String>,
    private val activity: FragmentActivity,
    private val onShowRationale: ((PermissionRequest) -> Unit)?,
    private val onPermissionDenied: ((ArrayList<String>) -> Unit)?,
    private val requiresPermission: (ArrayList<String>) -> Unit,
    private val onNeverAskAgain: ((ArrayList<String>) -> Unit)?,
    private val permissionEducation: (() -> Unit)?,
    private val permissionRequestType: PermissionType
) : PermissionsRequester {

    /**
     * Build a n instance of PermissionsRequesterImpl
     */
    class Builder(val activity: FragmentActivity, val permissions: ArrayList<String>) {
        private var onShowRationale: ((PermissionRequest) -> Unit)? = null
        private var onPermissionDenied: ((ArrayList<String>) -> Unit)? = null
        private lateinit var requiresPermission: (ArrayList<String>) -> Unit
        private var onNeverAskAgain: ((ArrayList<String>) -> Unit)? = null
        private var permissionEducation: (() -> Unit)? = null
        private var permissionRequestType: PermissionType =
            PermissionType.NormalPermission

        constructor(activity: FragmentActivity, permissions: Array<String>) : this(activity, permissions.toCollection(ArrayList()))

        /**
         * If set permissionEducation, It will be called when a permission is not GRANTED
         *
         * @param permissionEducation The callback function that show education
         * @return This Builder object to allow for chaining of calls to set methods
         */
        fun setPermissionEducation(permissionEducation: (() -> Unit)?): Builder {
            this.permissionEducation = permissionEducation
            return this
        }

        /**
         * Set callback function and it will be called when the user has denied the permissions that be requested
         *
         * @param onShowRationale Callback function that indicate the user deny the permissions that be requested. It allow for continuation
         * or cancellation of a permission request.
         * @return This Builder object to allow for chaining of calls to set methods
         */
        fun setOnShowRationale(onShowRationale: ((PermissionRequest) -> Unit)?): Builder {
            this.onShowRationale = onShowRationale
            return this
        }

        /**
         * Set callback function and it will be called when the user denies the permissions after calls requestPermissions()
         *
         * @param onPermissionDenied Callback function for denying permissions
         * @return This Builder object to allow for chaining of calls to set methods
         */
        fun setOnPermissionDenied(onPermissionDenied: ((ArrayList<String>) -> Unit)?): Builder {
            this.onPermissionDenied = onPermissionDenied
            return this
        }

        /**
         * Set callback function and it will be called when the user grants the permissions after calls requestPermissions()
         *
         * @param requiresPermission Callback function for granting permissions
         * @return This Builder object to allow for chaining of calls to set methods
         */
        fun setOnRequiresPermission(requiresPermission: (ArrayList<String>) -> Unit): Builder {
            this.requiresPermission = requiresPermission
            return this
        }

        /**
         * Set callback function and it will be called when the user denies the permissions and tickets "Never Ask Again" after calls requestPermissions()
         *
         * @param onNeverAskAgain Callback function for the user denies the permissions and tickets "Never Ask Again"
         * @return This Builder object to allow for chaining of calls to set methods
         */
        fun setOnNeverAskAgain(onNeverAskAgain: ((ArrayList<String>) -> Unit)?): Builder {
            this.onNeverAskAgain = onNeverAskAgain
            return this
        }

        /**
         * Set permission request type, NormalPermissionRequest by default
         */
        fun setPermissionRequestType(permissionRequestType: PermissionType = PermissionType.NormalPermission): Builder {
            this.permissionRequestType = permissionRequestType
            return this
        }

        /**
         * Create the instance of PermissionRequestImpl
         */
        fun build(): PermissionsRequesterImpl {
            return PermissionsRequesterImpl(
                permissions,
                activity,
                onShowRationale,
                onPermissionDenied,
                requiresPermission,
                onNeverAskAgain,
                permissionEducation,
                permissionRequestType
            )
        }
    }

    override fun launch(showEducation: Boolean) {
        when (permissionRequestType) {
            PermissionType.CheckPermission -> {
                for (permission in permissions) {
                    val arrayList = ArrayList<String>()
                    arrayList.add(permission)
                    if (permissionRequestType.checkPermissions(activity, arrayList)) {
                        requiresPermission(arrayList)
                    } else {
                        onPermissionDenied?.let { it(arrayList) }
                    }
                }
            }
            PermissionType.NormalPermission -> {
                if (permissionRequestType.checkPermissions(activity, permissions)) {
                    requiresPermission(permissions)
                } else {
                    if (showEducation) {
                        permissionEducation?.let { it() }
                    } else {
                        ViewModelProvider(activity).get(PermissionViewModel::class.java)
                            .removeObservers(activity)
                        ViewModelProvider(activity).get(PermissionViewModel::class.java).permissionRequestResult.observe(
                            activity
                        ) { map ->
                            map.forEach {
                                when (it.value) {
                                    PermissionResult.GRANTED -> requiresPermission.invoke(
                                        arrayListOf(it.key)
                                    )
                                    PermissionResult.DENIED -> onPermissionDenied?.invoke(
                                        arrayListOf(it.key)
                                    )
                                    PermissionResult.DENIED_AND_DISABLED -> onNeverAskAgain?.invoke(
                                        arrayListOf(it.key)
                                    )
                                }
                            }
                        }

                        val requestFun: () -> Unit = {
                            activity.supportFragmentManager
                                .beginTransaction()
                                .replace(
                                    android.R.id.content,
                                    permissionRequestType.fragment(permissions)
                                )
                                .commitAllowingStateLoss()
                        }
                        if (PermissionUtils.shouldShowRequestPermissionRationale(
                                activity,
                                permissions
                            )
                        ) {
                            onShowRationale?.invoke(
                                RationalePermissionRequest.create(
                                    onPermissionDenied,
                                    requestFun
                                )
                            )
                        } else {
                            requestFun.invoke()
                        }
                    }
                }
            }
        }
    }
}
