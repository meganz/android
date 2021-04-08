package mega.privacy.android.app.modalbottomsheet

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.settingsActivities.PasscodeLockActivity
import mega.privacy.android.app.databinding.BottomSheetPasscodeOptionsBinding
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants.*

class PasscodeOptionsBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    companion object {
        private const val PASSCODE_TYPE = "PASSCODE_TYPE"

        @JvmStatic
        fun newInstance(
            passcodeType: String
        ): PasscodeOptionsBottomSheetDialogFragment {
            val fragment = PasscodeOptionsBottomSheetDialogFragment()
            val args = Bundle()

            args.putString(PASSCODE_TYPE, passcodeType)

            fragment.arguments = args

            return fragment
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        val binding =
            BottomSheetPasscodeOptionsBinding.inflate(LayoutInflater.from(context), null, false)

        setSelectedColor(
            when (arguments?.getString(PASSCODE_TYPE)) {
                PIN_4 -> {
                    binding.fourDigitsOption
                }
                PIN_6 -> {
                    binding.sixDigitsOption
                }
                else -> {
                    binding.alphanumericOption
                }
            }
        )

        binding.fourDigitsOption.setOnClickListener {
            changePasscodeType(PIN_4)
        }

        binding.sixDigitsOption.setOnClickListener {
            changePasscodeType(PIN_6)
        }

        binding.alphanumericOption.setOnClickListener {
            changePasscodeType(PIN_ALPHANUMERIC)
        }

        contentView = binding.root
        mainLinearLayout = binding.parentLayout
        items_layout = mainLinearLayout
        dialog.setContentView(contentView)
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false)
    }

    private fun setSelectedColor(text: TextView) =
        text.setTextColor(ColorUtils.getThemeColor(context, R.attr.colorSecondary))

    private fun changePasscodeType(type: String) {
        (requireActivity() as PasscodeLockActivity).setPasscodeType(type)
        setStateBottomSheetBehaviorHidden()
    }
}