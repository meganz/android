package mega.privacy.android.app.modalbottomsheet

import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.testpassword.BackupRecoveryKeyAction

/**
 * Recovery Bottom Sheet Dialog, to select options of actions for Recovery Key
 */
class RecoveryKeyBottomSheetDialogFragment(
    private val action: BackupRecoveryKeyAction,
) : BaseBottomSheetDialogFragment() {
    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        contentView = View.inflate(requireContext(), R.layout.bottom_sheet_recovery_key, null)
        itemsLayout = contentView.findViewById(R.id.items_layout)
        return contentView
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        contentView.apply {
            findViewById<LinearLayout>(R.id.recovery_key_print_layout).setOnClickListener {
                action.print()
                setStateBottomSheetBehaviorHidden()
            }

            findViewById<LinearLayout>(R.id.recovery_key_copytoclipboard_layout).setOnClickListener {
                action.copyToClipboard()
                setStateBottomSheetBehaviorHidden()
            }

            findViewById<LinearLayout>(R.id.recovery_key_saveTo_fileSystem_layout).setOnClickListener {
                action.saveToFile()
                setStateBottomSheetBehaviorHidden()
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }
}