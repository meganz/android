package mega.privacy.android.app.meeting

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Extension for bottom sheet dialog fragment
 * set click listener for view, after that, will close fragment
 *
 * @param view the target view
 * @param action the listener
 */
fun BottomSheetDialogFragment.listenAction(view: View, action: () -> Unit) {
    view.setOnClickListener {
        action()
        dismiss()
    }
}
