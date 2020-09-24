package mega.privacy.android.app.modalbottomsheet

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetPsaBinding
import mega.privacy.android.app.lollipop.WebViewActivityLollipop
import mega.privacy.android.app.psa.PsaViewModel
import mega.privacy.android.app.psa.PsaViewModelFactory

class PsaBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {
    private lateinit var psaViewModel: PsaViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        // disable drag to dismiss
        dialog.setOnShowListener {
            val bottomSheet = (it as BottomSheetDialog).findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return@setOnShowListener

            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED

            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
        }
        // disable click outside to dismiss
        dialog.setCanceledOnTouchOutside(false)

        setupView(dialog)

        return dialog
    }

    private fun setupView(dialog: BottomSheetDialog) {
        val binding = BottomSheetPsaBinding.inflate(LayoutInflater.from(context), null, false)

        contentView = binding.root
        mainLinearLayout = binding.root
        items_layout = binding.root
        dialog.setContentView(contentView)
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, false)

        psaViewModel = ViewModelProvider(requireActivity(), PsaViewModelFactory(megaApi))
            .get(PsaViewModel::class.java)

        val psa = psaViewModel.psa.value?.peekContent() ?: return
        if (!TextUtils.isEmpty(psa.imageUrl)) {
            binding.image.visibility = View.VISIBLE
            binding.image.setImageURI(Uri.parse(psa.imageUrl))
        }

        binding.title.text = psa.title
        binding.text.text = psa.text

        if (!TextUtils.isEmpty(psa.positiveText) && !TextUtils.isEmpty(psa.positiveLink)) {
            binding.leftButton.text = psa.positiveText
            binding.leftButton.setOnClickListener {
                val intent = Intent(requireContext(), WebViewActivityLollipop::class.java)
                intent.data = Uri.parse(psa.positiveLink)
                startActivity(intent)
                dismissPsa(psa.id)
            }

            binding.rightButton.visibility = View.VISIBLE
            binding.rightButton.setOnClickListener { dismissPsa(psa.id) }
        } else {
            binding.leftButton.setText(R.string.general_dismiss)
            binding.leftButton.setOnClickListener { dismissPsa(psa.id) }
        }
    }

    private fun dismissPsa(id: Int) {
        psaViewModel.dismissPsa(id)
        dismiss()
    }
}
