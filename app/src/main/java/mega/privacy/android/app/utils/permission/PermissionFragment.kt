package mega.privacy.android.app.utils.permission

import android.content.Context
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.util.*

/**
 * The Instance of PermissionRequestFragment is used to process permission request
 */
sealed class PermissionFragment : Fragment() {
    protected val requestCode = Random().nextInt(100)
    protected lateinit var viewModel: PermissionViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
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
        companion object {
            const val BUNDLE_MEGA_PERMISSIONS = "mega_permissions"

            fun newInstance(permissions: ArrayList<String>) =
                NormalRequestFragment().apply {
                    val bundle = Bundle()
                    bundle.putStringArrayList(BUNDLE_MEGA_PERMISSIONS, permissions)
                    arguments = bundle
                }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // Get Permissions
            val megaPermissions = arguments?.getStringArrayList(BUNDLE_MEGA_PERMISSIONS) ?: return

            // Create permissions launcher
            val permissionCaller = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                    val permissionResults = permissions.mapValues { permission ->
                        when (permission.value) {
                            true -> PermissionResult.GRANTED // Permissions is granted
                            false -> {
                                if (shouldShowRequestPermissionRationale(permission.key)) {
                                    PermissionResult.DENIED // The user denies without "Never Ask Again"
                                } else {
                                    PermissionResult.DENIED_AND_DISABLED // The user denies and tickets "Never Ask Again"
                                }
                            }
                        }
                    }
                    viewModel.postPermissionRequestResult(permissionResults.toMutableMap())
                    dismiss()
                }

            // Request permission
            permissionCaller.launch(megaPermissions.toTypedArray())
        }
    }

    /**
     * Subclass of PermissionRequestFragment
     */
    internal class CheckRequestFragment : PermissionFragment() {

        companion object {
            fun newInstance() = CheckRequestFragment()
        }
    }
}
