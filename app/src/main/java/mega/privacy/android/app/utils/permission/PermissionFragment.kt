package mega.privacy.android.app.utils.permission

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import mega.privacy.android.app.utils.permission.PermissionUtils.verifyPermissions
import java.util.*

/**
 * The Instance of PermissionRequestFragment is used to process permission request
 */
sealed class PermissionFragment : Fragment() {
    protected val requestCode = Random().nextInt(100)
    protected lateinit var viewModel: PermissionViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        retainInstance = true
        viewModel = ViewModelProvider(requireActivity()).get(PermissionViewModel::class.java)
    }

    /**
     * Remove current Fragment
     */
    protected fun dismiss() =
        parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()

    /**
     * Subclass of PermissionRequestFragment
     */
    internal class NormalRequestFragment : PermissionFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // Get Permissions
            val permissions = arguments?.getStringArrayList(BUNDLE_MEGA_PERMISSIONS) ?: return
            // request permission
            requestPermissions(permissions.toTypedArray(), requestCode)
        }


        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == this.requestCode) {
                var i = 0
                val map = HashMap<String, PermissionResult>()
                while (i < grantResults.size) {
                    if (verifyPermissions(grantResults[i])) {
                        // All permissions are granted
                        map[permissions[i]] = PermissionResult.GRANTED
                    } else {
                        val arrayListPermission = ArrayList<String>()
                        arrayListPermission.add(permissions[i])
                        if (!PermissionUtils.shouldShowRequestPermissionRationale(
                                this,
                                arrayListPermission
                            )
                        ) {
                            // The user denies and tickets "Never Ask Again"
                            map[permissions[i]] = PermissionResult.DENIED_AND_DISABLED
                        } else {
                            // The user denies without "Never Ask Again"
                            map[permissions[i]] = PermissionResult.DENIED
                        }
                    }
                    i++
                }
                viewModel.postPermissionRequestResult(map)
            }
            dismiss()
        }

        companion object {
            const val BUNDLE_MEGA_PERMISSIONS = "mega_permissions"

            fun newInstance(permissions: ArrayList<String>) =
                NormalRequestFragment().apply {
                    val bundle = Bundle()
                    bundle.putStringArrayList(BUNDLE_MEGA_PERMISSIONS, permissions)
                    arguments = bundle
                }
        }
    }

    /**
     * Subclass of PermissionRequestFragment
     */
    internal class CheckRequestFragment : PermissionFragment() {


        companion object {

            fun newInstance() =
                CheckRequestFragment()
        }
    }

}
