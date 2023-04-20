package mega.privacy.android.app.modalbottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import mega.privacy.android.app.activities.settingsActivities.PasscodeLockActivity
import mega.privacy.android.app.databinding.BottomSheetPasscodeOptionsBinding
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants.PIN_4
import mega.privacy.android.app.utils.Constants.PIN_6
import mega.privacy.android.app.utils.Constants.PIN_ALPHANUMERIC

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

    private lateinit var binding: BottomSheetPasscodeOptionsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetPasscodeOptionsBinding
            .inflate(LayoutInflater.from(context), null, false)

        contentView = binding.root
        itemsLayout = binding.parentLayout

        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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

        super.onViewCreated(view, savedInstanceState)
    }

    private fun setSelectedColor(text: TextView) =
        text.setTextColor(ColorUtils.getThemeColor(requireContext(), com.google.android.material.R.attr.colorSecondary))

    private fun changePasscodeType(type: String) {
        (requireActivity() as PasscodeLockActivity).setPasscodeType(type)
        setStateBottomSheetBehaviorHidden()
    }
}