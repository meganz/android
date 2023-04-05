package mega.privacy.android.app.presentation.qrcode.mycode

import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentMycodeBinding
import mega.privacy.android.app.presentation.qrcode.QRCodeActivity
import mega.privacy.android.app.utils.ColorUtils.changeStatusBarColorForElevation
import mega.privacy.android.app.utils.MegaProgressDialogUtil.createProgressDialog
import timber.log.Timber

/**
 * The UI of showing current user's QR Code.
 */
@AndroidEntryPoint
class MyCodeFragment : Fragment() {

    private val viewModel: MyCodeViewModel by activityViewModels()

    private var _binding: FragmentMycodeBinding? = null
    private val binding get() = _binding!!

    private var abL: AppBarLayout? = null

    private var processingDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Timber.d("onCreateView")

        _binding = FragmentMycodeBinding.inflate(inflater, container, false)

        abL = requireActivity().findViewById(R.id.app_bar_layout)

        binding.qrCodeButtonCopyLink.setOnClickListener {
            viewModel.uiState.value.contactLink?.let {
                viewModel.copyContactLink()
            } ?: run {
                createQRCode()
            }
        }

        binding.myCodeScrollview.setOnScrollChangeListener { _: View?, _: Int, _: Int, _: Int, _: Int ->
            checkScroll()
        }

        val configuration = resources.configuration
        val width = getDP(RELATIVE_WIDTH)
        val params: LinearLayout.LayoutParams
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            params = LinearLayout.LayoutParams(width - 80, width - 80)
            params.gravity = Gravity.CENTER
            params.setMargins(0, 0, 0, getDP(20))
            binding.qrCodeRelativeContainer.layoutParams = params
            binding.qrCodeRelativeContainer.setPadding(0, -40, 0, 0)
        } else {
            params = LinearLayout.LayoutParams(width, width)
            params.gravity = Gravity.CENTER
            params.setMargins(0, getDP(55), 0, getDP(58))
            binding.qrCodeRelativeContainer.layoutParams = params
        }

        // With Compose Scaffold, Fragment is always destroyed and re-created after switching tab,
        // so check here to make sure no createQRCode if it is just been deleted
        if (!viewModel.uiState.value.hasQRCodeBeenDeleted) {
            createQRCode()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFlow()
    }

    private fun setupFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.uiState.collect { state ->
                    Timber.d("state = $state")
                    with(state) {

                        binding.qrCodeLink.text =
                            state.contactLink?.takeIf { it.isNotEmpty() } ?: ""

                        snackBarMessage?.let {
                            showSnackBar(it)
                            viewModel.setSnackBarMessage(null)
                        }

                        binding.qrCodeImage.setImageBitmap(qrCodeBitmap)

                        if (isInProgress) {
                            showProgressDialog()
                        } else {
                            dismissProgressDialog()
                        }
                        binding.qrCodeButtonCopyLink.isEnabled = isInProgress.not()

                        val buttonStringId = if (contactLink != null && !hasQRCodeBeenDeleted) {
                            R.string.button_copy_link
                        } else {
                            R.string.button_create_qr
                        }
                        binding.qrCodeButtonCopyLink.text =
                            getString(buttonStringId)
                    }
                }
            }
        }
    }

    private fun createQRCode() {
        val penColor = ContextCompat.getColor(requireContext(), R.color.dark_grey)
        val bgColor = ContextCompat.getColor(requireContext(), R.color.white_grey_700)
        val avatarBorderColor = ContextCompat.getColor(requireContext(), R.color.white_dark_grey)
        viewModel.createQRCode(
            width = QRCODE_WIDTH,
            height = QRCODE_WIDTH,
            penColor = penColor,
            bgColor = bgColor,
            avatarWidth = AVATAR_WIDTH,
            avatarBorderWidth = AVATAR_BORDER_WIDTH,
            avatarBorderColor = avatarBorderColor,
        )
    }

    private fun checkScroll() {
        val withElevation = binding.myCodeScrollview.canScrollVertically(-1)
        abL?.elevation =
            if (withElevation) resources.getDimension(R.dimen.toolbar_elevation) else 0f
        changeStatusBarColorForElevation(requireActivity(), withElevation)
    }

    private fun getDP(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun showProgressDialog() {
        if (processingDialog?.isShowing != true) {
            var temp: AlertDialog? = null
            try {
                temp = createProgressDialog(
                    requireContext(),
                    getString(R.string.generatin_qr)
                )
                temp.show()
            } catch (e: Exception) {
                Timber.e(e)
            }
            processingDialog = temp
        }
    }

    private fun dismissProgressDialog() {
        processingDialog?.dismiss()
        processingDialog = null
    }

    private fun showSnackBar(@StringRes textResId: Int) {
        (requireActivity() as QRCodeActivity).showSnackbar(
            binding.root,
            getString(textResId)
        )
    }

    companion object {
        /**
         * suffix in local QR code file name
         */
        const val QR_IMAGE_FILE_NAME = "QR_code_image.jpg"

        /**
         * Width of QR code
         */
        const val QRCODE_WIDTH = 500

        /**
         * Width of avatar
         */
        const val AVATAR_WIDTH = 135

        /**
         * Avatar's border width
         */
        const val AVATAR_BORDER_WIDTH = 3

        private const val RELATIVE_WIDTH = 280
    }
}