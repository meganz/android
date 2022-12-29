package mega.privacy.android.app.utils.greeter

import android.app.Activity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

interface AndroidComponentCreation {
    fun onActivityCreated(activity: Activity)

    fun onFragmentCreated(fragment: Fragment)

    fun onDialogFragmentCreated(fragment: DialogFragment)

    fun onBottomSheetDialogFragmentCreated(fragment: BottomSheetDialogFragment)
}
