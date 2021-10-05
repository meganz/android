package mega.privacy.android.app.modalbottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import mega.privacy.android.app.R

class PhoneNumberBottomSheetDialogFragment: BaseBottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        contentView = View.inflate(requireContext(), R.layout.bottom_sheet_phonenumber, null)
        itemsLayout = contentView.findViewById(R.id.items_layout)
        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val phoneNumberCallback = requireActivity() as PhoneNumberCallback

        contentView.findViewById<View>(R.id.modify_phonenumber_layout).setOnClickListener {
            phoneNumberCallback.showRemovePhoneNumberConfirmation(true)
            setStateBottomSheetBehaviorHidden()
        }

        contentView.findViewById<View>(R.id.remove_phonenumber_layout).setOnClickListener {
            phoneNumberCallback.showRemovePhoneNumberConfirmation(false)
            setStateBottomSheetBehaviorHidden()
        }

        super.onViewCreated(view, savedInstanceState)
    }

    interface PhoneNumberCallback {
        fun showRemovePhoneNumberConfirmation(isModify: Boolean)
    }
}