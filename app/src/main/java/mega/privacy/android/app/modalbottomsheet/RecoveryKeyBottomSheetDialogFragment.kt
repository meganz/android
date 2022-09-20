package mega.privacy.android.app.modalbottomsheet

import mega.privacy.android.app.main.controllers.AccountController.Companion.saveRkToFileSystem
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.domain.qualifier.ApplicationScope
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import mega.privacy.android.app.R
import mega.privacy.android.app.main.controllers.AccountController
import mega.privacy.android.app.main.TwoFactorAuthenticationActivity

@AndroidEntryPoint
class RecoveryKeyBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    @ApplicationScope
    @Inject
    lateinit var sharingScope: CoroutineScope

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        contentView = View.inflate(requireContext(), R.layout.bottom_sheet_recovery_key, null)
        itemsLayout = contentView.findViewById(R.id.items_layout)
        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        contentView.apply {
            findViewById<LinearLayout>(R.id.recovery_key_print_layout).setOnClickListener {
                AccountController(requireActivity()).printRK()
                setStateBottomSheetBehaviorHidden()
            }

            findViewById<LinearLayout>(R.id.recovery_key_copytoclipboard_layout).setOnClickListener {
                AccountController(requireActivity()).copyRkToClipboard(sharingScope)

                if (requireActivity() is TwoFactorAuthenticationActivity) {
                    requireActivity().finish()
                }

                setStateBottomSheetBehaviorHidden()
            }

            findViewById<LinearLayout>(R.id.recovery_key_saveTo_fileSystem_layout).setOnClickListener {
                saveRkToFileSystem(requireActivity())
                setStateBottomSheetBehaviorHidden()
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }
}