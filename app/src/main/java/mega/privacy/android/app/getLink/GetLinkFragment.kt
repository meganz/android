package mega.privacy.android.app.getLink

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialStyledDatePickerDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MimeTypeList.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ListenScrollChangesHelper
import mega.privacy.android.app.databinding.FragmentGetLinkBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.MegaApiUtils.getMegaNodeFolderInfo
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop.getRoundedBitmap
import mega.privacy.android.app.utils.Util.*
import java.text.SimpleDateFormat
import java.util.*


class GetLinkFragment : BaseFragment(), DatePickerDialog.OnDateSetListener, Scrollable {

    companion object {
        private const val INVALID_EXPIRATION_TIME = -1L
        private const val LAST_MINUTE = "2359"
    }

    private val viewModel: GetLinkViewModel by activityViewModels()

    private lateinit var binding: FragmentGetLinkBinding

    private var passwordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGetLinkBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        setupObservers()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setupView() {
        ListenScrollChangesHelper().addViewToListen(
            binding.scrollViewGetLink
        ) { _, _, _, _, _ -> checkScroll() }

        checkScroll()
        setThumbnail()

        val node = viewModel.getNode()
        binding.nodeName.text = node.name
        binding.nodeInfo.text =
            if (node.isFolder) getMegaNodeFolderInfo(node)
            else getSizeString(node.size)

        binding.learnMoreTextButton.setOnClickListener {
            checkIfShouldHidePassword()
            findNavController().navigate(R.id.learn_more)
        }

        binding.passwordProtectionSetToggle.setOnClickListener { toggleClick() }

        binding.resetPasswordButton.setOnClickListener {
            checkIfShouldHidePassword()
            findNavController().navigate(GetLinkFragmentDirections.setPassword(true))
        }

        binding.removePasswordButton.setOnClickListener { removePasswordClick() }

        binding.keyLayout.isVisible = false
        binding.keySeparator.isVisible = false

        binding.copyLinkButton.setOnClickListener {
            checkIfShouldHidePassword()
            viewModel.copyLink { copyInfo -> copyToClipboard(copyInfo) }
        }

        binding.copyKeyButton.setOnClickListener {
            viewModel.copyLinkKey { copyInfo ->
                copyToClipboard(copyInfo)
            }
        }

        binding.copyKeyButton.isVisible = false

        binding.copyPasswordButton.setOnClickListener {
            checkIfShouldHidePassword()
            viewModel.copyLinkPassword { copyInfo -> copyToClipboard(copyInfo) }
        }

        if (viewModel.isPro()) {
            binding.expiryDateProOnlyText.isVisible = false
            binding.passwordProtectionProOnlyText.isVisible = false
        } else {
            binding.expiryDateProOnlyText.isVisible = true
            binding.passwordProtectionProOnlyText.isVisible = true
        }

        if (!node.isExported) {
            viewModel.export()
        }
    }

    private fun setupObservers() {
        viewModel.getLink().observe(viewLifecycleOwner, ::updateLink)
        viewModel.getExpiryDate().observe(viewLifecycleOwner, ::updateExpiryDate)
        viewModel.getPassword().observe(viewLifecycleOwner, ::updatePassword)
    }

    override fun onResume() {
        super.onResume()

        updateSendDecryptedKeySeparatelyLayouts()
    }

    /**
     * Disables the password protection.
     */
    private fun removePasswordClick() {
        checkIfShouldHidePassword()
        viewModel.removeLinkWithPassword()
    }

    /**
     * Updates the entire view depending on the enabled options.
     *
     * @param text Text to show as link text.
     */
    private fun updateLink(text: String) {
        binding.linkText.text = text

        val node = viewModel.getNode()
        val alpha = if (node.isExported) ALPHA_VIEW_ENABLED else ALPHA_VIEW_DISABLED

        binding.decryptedKeyLayout.alpha = alpha
        binding.expiryDateLayout.alpha = alpha
        binding.passwordProtectionLayout.alpha = alpha

        if (node.isExported) {
            binding.decryptedKeyLayout.setOnClickListener { sendDecryptedKeySeparatelyClick(false) }
            binding.decryptedKeySwitch.apply {
                setOnClickListener { sendDecryptedKeySeparatelyClick(true) }
                isEnabled = true
                setOnCheckedChangeListener { _, isChecked ->
                    viewModel.updateSendDecryptedKeySeparatelyEnabled(isChecked)
                }
            }

            binding.expiryDateLayout.setOnClickListener { setExpiryDateClick(false) }
            binding.expiryDateSwitch.setOnClickListener { setExpiryDateClick(true) }
            binding.expiryDateSwitch.isEnabled = true
            binding.copyLinkButton.isVisible = true
        } else {
            binding.decryptedKeyLayout.setOnClickListener(null)
            binding.decryptedKeySwitch.setOnClickListener(null)
            binding.decryptedKeySwitch.isEnabled = false

            binding.expiryDateLayout.setOnClickListener(null)
            binding.expiryDateSwitch.setOnClickListener(null)
            binding.expiryDateSwitch.isEnabled = false

            binding.keySeparator.isVisible = false

            binding.copyLinkButton.isVisible = false
            binding.copyKeyButton.isVisible = false
        }
    }

    /**
     * Updates the expiry date views.
     *
     * @param date Text to show as link text.
     */
    private fun updateExpiryDate(date: String) {
        if (date.isNotEmpty()) {
            binding.expiryDateSwitch.isChecked = true
            binding.expiryDateSetText.isVisible = true
            binding.expiryDateSetText.text = date
        } else {
            binding.expiryDateSwitch.isChecked = false
            binding.expiryDateSetText.isVisible = false
            binding.expiryDateSetText.text = null
        }
    }

    /**
     * Updates the UI of password protection option.
     *
     * @param password The link with password if set, null otherwise.
     */
    private fun updatePassword(password: String?) {
        val isPasswordSet = !isTextEmpty(password)
        val visibility = if (isPasswordSet) VISIBLE else GONE

        if (isPasswordSet) {
            if (binding.decryptedKeySwitch.isChecked) {
                sendDecryptedKeySeparatelyClick(false)
            }

            binding.passwordProtectionSetText.transformationMethod = PasswordTransformationMethod()
        }

        binding.passwordProtectionSetText.visibility = visibility
        binding.passwordProtectionSetText.text = if (isPasswordSet) password else null
        binding.passwordProtectionSetToggle.visibility = visibility

        binding.resetPasswordButton.visibility = visibility
        binding.removePasswordButton.visibility = visibility

        binding.copyPasswordButton.visibility = visibility

        if (isPasswordSet || !viewModel.getNode().isExported) {
            binding.passwordProtectionLayout.setOnClickListener(null)
        } else {
            binding.passwordProtectionLayout.setOnClickListener { setPasswordProtectionClick() }
        }
    }

    /**
     * Sets the thumbnail of the node to which is getting or managing the link.
     */
    private fun setThumbnail() {
        var thumb: Bitmap? = null
        val node = viewModel.getNode()

        if (node.isFolder) {
            binding.nodeThumbnail.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_folder_list,
                    null
                )
            )

            return
        } else if (node.hasThumbnail()) {
            thumb = ThumbnailUtils.getThumbnailFromCache(node)
            if (thumb == null) {
                thumb = ThumbnailUtils.getThumbnailFromFolder(node, context)
            }
        }

        if (thumb != null) {
            binding.nodeThumbnail.setImageBitmap(
                getRoundedBitmap(
                    context,
                    thumb,
                    dp2px(THUMB_CORNER_RADIUS_DP)
                )
            )
        } else {
            binding.nodeThumbnail.setImageResource(typeForName(node.name).iconResourceId)
        }
    }

    /**
     * Manages the click of send decrypted key separately option.
     *
     * @param isSwitchClick True if the click was in the switch, false if it was in other part of the view.
     */
    private fun sendDecryptedKeySeparatelyClick(isSwitchClick: Boolean) {
        if (!isSwitchClick) {
            binding.decryptedKeySwitch.isChecked = !binding.decryptedKeySwitch.isChecked
        }

        if (binding.decryptedKeySwitch.isChecked
            && !viewModel.getLinkWithPassword().isNullOrEmpty()
        ) {
            removePasswordClick()
        }

        updateSendDecryptedKeySeparatelyLayouts()
    }

    /**
     * Updates the UI of the send decrypted key separately option.
     */
    private fun updateSendDecryptedKeySeparatelyLayouts() {
        val visibility = if (binding.decryptedKeySwitch.isChecked) VISIBLE else GONE

        binding.keyLayout.visibility = visibility
        binding.keySeparator.visibility = visibility
        binding.copyKeyButton.visibility = visibility

        binding.keyText.text =
            if (binding.decryptedKeySwitch.isChecked) viewModel.getLinkKey() else null
    }

    /**
     * Manages the click of set expiry date option.
     *
     * @param isSwitchClick True if the click was in the switch, false if it was in other part of the view.
     */
    private fun setExpiryDateClick(isSwitchClick: Boolean) {
        checkIfShouldHidePassword()
        val node = viewModel.getNode()
        val isPro = viewModel.isPro()

        if (!isPro || (isSwitchClick && node.expirationTime <= 0)) {
            binding.expiryDateSwitch.isChecked = false
        }

        if (!isPro) {
            showUpgradeToProWarning()
            return
        }

        if (isSwitchClick && node.expirationTime > 0) {
            binding.expiryDateSetText.apply {
                isVisible = false
                text = null
            }

            viewModel.export()
        } else {
            showDatePicker()
        }
    }

    private fun showUpgradeToProWarning() {
        val upgradeToProDialogBuilder = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Mega_MaterialAlertDialog
        )

        upgradeToProDialogBuilder.setTitle(R.string.upgrade_pro)
            .setMessage(getString(R.string.link_upgrade_pro_explanation) + "\n")
            .setCancelable(false)
            .setPositiveButton(R.string.button_plans_almost_full_warning) { _, _ ->
                (requireActivity() as BaseActivity).apply {
                    navigateToUpgradeAccount()
                    finish()
                }
            }
            .setNegativeButton(R.string.verify_account_not_now_button) { dialog, _ ->
                dialog.dismiss()
            }

        upgradeToProDialogBuilder.create().show()
    }

    @SuppressLint("RestrictedApi")
    private fun showDatePicker() {
        val node = viewModel.getNode()
        val calendar =
            if (node.expirationTime == INVALID_EXPIRATION_TIME) Calendar.getInstance()
            else calculateDateFromTimestamp(node.expirationTime)

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = MaterialStyledDatePickerDialog(
            context,
            R.style.Widget_Mega_DatePickerDialog,
            this, year, month, day
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    /**
     * Manages the click on set password protection option.
     */
    private fun setPasswordProtectionClick() {
        if (viewModel.isPro()) {
            checkIfShouldHidePassword()
            findNavController().navigate(GetLinkFragmentDirections.setPassword(false))
        } else {
            showUpgradeToProWarning()
        }
    }

    /**
     * Checks if should hide the password each time the user clicks on some option.
     */
    private fun checkIfShouldHidePassword() {
        if (passwordVisible) {
            toggleClick()
        }
    }

    /**
     * Manages the click on password toggle by showing or hiding the password.
     */
    private fun toggleClick() {
        if (passwordVisible) {
            binding.passwordProtectionSetText.transformationMethod = PasswordTransformationMethod()
            binding.passwordProtectionSetToggle.setColorFilter(
                ContextCompat.getColor(context, R.color.grey_012_white_038), PorterDuff.Mode.SRC_IN
            )
        } else {
            binding.passwordProtectionSetText.transformationMethod = null
            binding.passwordProtectionSetToggle.setColorFilter(
                ColorUtils.getThemeColor(context, R.attr.colorSecondary), PorterDuff.Mode.SRC_IN
            )
        }

        passwordVisible = !passwordVisible
    }

    /**
     * Copies a link, decryption key or password into clipboard and shows a snackbar.
     *
     * @param copyInfo First is the  content to copy, second the text to show as confirmation.
     */
    private fun copyToClipboard(copyInfo: Pair<String, String>) {
        TextUtil.copyToClipboard(requireActivity(), copyInfo.first)
        (requireActivity() as SnackbarShower).showSnackbar(copyInfo.second)
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val cal = Calendar.getInstance()
        cal.set(year, month, dayOfMonth)

        val date = cal.time
        val dfTimestamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateString = dfTimestamp.format(date) + LAST_MINUTE
        val timestamp = calculateTimestamp(dateString).toInt()

        viewModel.exportWithTimestamp(timestamp)
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized) {
            return
        }

        val withElevation = binding.scrollViewGetLink.canScrollVertically(SCROLLING_UP_DIRECTION)
        viewModel.setElevation(withElevation)
    }
}