package mega.privacy.android.app.fragments.getLinkFragments

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
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
import mega.privacy.android.app.MimeTypeList.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.GetLinkActivity.Companion.DECRYPTION_KEY_FRAGMENT
import mega.privacy.android.app.activities.GetLinkActivity.Companion.PASSWORD_FRAGMENT
import mega.privacy.android.app.databinding.FragmentGetLinkBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.interfaces.GetLinkInterface
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.utils.MegaApiUtils.getInfoFolder
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop.getRoundedBitmap
import mega.privacy.android.app.utils.Util.*
import nz.mega.sdk.MegaAccountDetails.ACCOUNT_TYPE_FREE
import nz.mega.sdk.MegaNode
import java.text.SimpleDateFormat
import java.util.*


class LinkFragment(private val getLinkInterface: GetLinkInterface) : BaseFragment(),
    DatePickerDialog.OnDateSetListener {

    companion object {
        private const val ALPHA_VIEW_DISABLED = 0.3f
        private const val ALPHA_VIEW_ENABLED = 1.0f
        private const val INVALID_EXPIRATION_TIME = -1L
        private const val THUMBNAIL_CORNER = 4F
        private const val LAST_MINUTE = "2359"
    }

    private lateinit var binding: FragmentGetLinkBinding

    private lateinit var linkWithKey: String
    private lateinit var linkWithoutKey: String
    private lateinit var key: String

    private var isPro = false
    private lateinit var node: MegaNode

    private lateinit var nC: NodeController

    private var passwordVisible = false

    private lateinit var accentDrawable: Drawable
    private lateinit var transparentDrawable: Drawable
    private var accentColor = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGetLinkBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        accentDrawable = ContextCompat.getDrawable(context, R.drawable.background_accent_button)!!
        transparentDrawable =
            ContextCompat.getDrawable(context, R.drawable.background_button_border_accent)!!

        accentColor = ContextCompat.getColor(context, R.color.accentColor)

        isPro = app.myAccountInfo.accountType > ACCOUNT_TYPE_FREE

        node = getLinkInterface.getNode()

        nC = NodeController(activity)

        setThumbnail()
        binding.nodeName.text = node.name
        binding.nodeInfo.text =
            if (node.isFolder) getInfoFolder(node, context)
            else getSizeString(node.size)

        binding.learnMoreTextButton.setOnClickListener {
            checkIfShouldHidePassword()
            getLinkInterface.showFragment(
                DECRYPTION_KEY_FRAGMENT
            )
        }

        binding.passwordProtectionSetToggle.setOnClickListener { toggleClick() }

        binding.resetPasswordButton.setOnClickListener {
            checkIfShouldHidePassword()
            getLinkInterface.showFragment(
                PASSWORD_FRAGMENT
            )
        }

        binding.removePasswordButton.setOnClickListener { removePasswordClick() }

        binding.keyLayout.visibility = GONE
        binding.keySeparator.visibility = GONE

        binding.copyLinkButton.setOnClickListener {
            checkIfShouldHidePassword()
            val linkWithPassword = getLinkInterface.getLinkWithPassword()

            getLinkInterface.copyLink(
                if (binding.decryptedKeySwitch.isChecked) linkWithoutKey
                else if (!isTextEmpty(linkWithPassword)) linkWithPassword!!
                else linkWithKey
            )
        }

        binding.copyKeyButton.setOnClickListener { getLinkInterface.copyLinkKey() }
        binding.copyKeyButton.visibility = GONE

        binding.copyPasswordButton.setOnClickListener {
            checkIfShouldHidePassword()
            getLinkInterface.copyLinkPassword()
        }

        updateLink()

        if (!node.isExported) {
            nC.exportLink(node)
        } else {
            updateLinkText()
        }

        super.onViewCreated(view, savedInstanceState)
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
        getLinkInterface.removeLinkWithPassword()
        updatePasswordLayouts()
        updateLinkText()
    }

    /**
     * Updates the text of the link depending on the enabled options.
     */
    private fun updateLinkText() {
        val linkWithPassword = getLinkInterface.getLinkWithPassword();

        binding.linkText.text =
            if (!getLinkInterface.getNode().isExported) getString(R.string.link_request_status)
            else if (binding.decryptedKeySwitch.isChecked) linkWithoutKey
            else if (!isTextEmpty(linkWithPassword)) linkWithPassword
            else linkWithKey
    }

    /**
     * Sets the thumbnail of the node to which is getting or managing the link.
     */
    private fun setThumbnail() {
        var thumb: Bitmap? = null

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
                    dp2px(THUMBNAIL_CORNER, outMetrics)
                )
            )
        } else {
            binding.nodeThumbnail.setImageResource(typeForName(node.name).iconResourceId)
        }
    }

    /**
     * Sets the options enabled/disabled depending on the type of the account
     * and if the node is exported or not.
     */
    private fun setAvailableLayouts() {
        val alpha = if (node.isExported) ALPHA_VIEW_ENABLED else ALPHA_VIEW_DISABLED

        binding.decryptedKeyLayout.alpha = alpha
        binding.expiryDateLayout.alpha = alpha
        binding.passwordProtectionLayout.alpha = alpha

        if (node.isExported) {
            binding.decryptedKeyLayout.setOnClickListener { sendDecryptedKeySeparatelyClick(false) }
            binding.decryptedKeySwitch.setOnClickListener { sendDecryptedKeySeparatelyClick(true) }
            binding.decryptedKeySwitch.isEnabled = true

            binding.expiryDateLayout.setOnClickListener { setExpiryDateClick(false) }
            binding.expiryDateSwitch.setOnClickListener { setExpiryDateClick(true) }
            binding.expiryDateSwitch.isEnabled = true

            if (node.expirationTime > 0) {
                binding.expiryDateSwitch.isChecked = true
                binding.expiryDateSetText.visibility = VISIBLE
                binding.expiryDateSetText.text = getExpiredDateText()
            } else {
                binding.expiryDateSwitch.isChecked = false
                binding.expiryDateSetText.visibility = GONE
                binding.expiryDateSetText.text = null
            }

            binding.copyLinkButton.visibility = VISIBLE
        } else {
            binding.decryptedKeyLayout.setOnClickListener(null)
            binding.decryptedKeySwitch.setOnClickListener(null)
            binding.decryptedKeySwitch.isEnabled = false

            binding.expiryDateLayout.setOnClickListener(null)
            binding.expiryDateSwitch.setOnClickListener(null)
            binding.expiryDateSwitch.isEnabled = false

            binding.keySeparator.visibility = GONE

            binding.copyLinkButton.visibility = GONE
            binding.copyKeyButton.visibility = GONE
        }

        updatePasswordLayouts()

        if (isPro) {
            binding.expiryDateProOnlyText.visibility = GONE

            binding.passwordProtectionProOnlyText.visibility = GONE
        } else {
            binding.expiryDateProOnlyText.visibility = VISIBLE

            binding.passwordProtectionProOnlyText.visibility = VISIBLE
        }
    }

    /**
     * Gets the expired date formatted.
     *
     * @return The formatted date.
     */
    private fun getExpiredDateText(): String {
        val df = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault())
        val cal = calculateDateFromTimestamp(node.expirationTime)
        val tz = cal.timeZone
        df.timeZone = tz
        val date = cal.time
        return df.format(date)
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

        if (binding.decryptedKeySwitch.isChecked && !isTextEmpty(getLinkInterface.getLinkWithPassword())) {
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

        updateLinkText()

        binding.keyText.text = if (binding.decryptedKeySwitch.isChecked) key else null
    }

    /**
     * Manages the click of set expiry date option.
     *
     * @param isSwitchClick True if the click was in the switch, false if it was in other part of the view.
     */
    private fun setExpiryDateClick(isSwitchClick: Boolean) {
        checkIfShouldHidePassword()

        if (!isPro || (isSwitchClick && node.expirationTime <= 0)) {
            binding.expiryDateSwitch.isChecked = false
        }

        if (!isPro) {
            getLinkInterface.showUpgradeToProWarning()
            return
        }

        if (isSwitchClick && node.expirationTime > 0) {
            binding.expiryDateSetText.apply {
                visibility = GONE
                text = null
            }

            nC.exportLink(node)
        } else {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar =
            if (node.expirationTime == INVALID_EXPIRATION_TIME) Calendar.getInstance()
            else calculateDateFromTimestamp(node.expirationTime)

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(context, this, year, month, day)
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    /**
     * Manages the click on set password protection option.
     */
    private fun setPasswordProtectionClick() {
        if (isPro) {
            checkIfShouldHidePassword()
            getLinkInterface.showFragment(PASSWORD_FRAGMENT)
        } else {
            getLinkInterface.showUpgradeToProWarning()
        }
    }

    /**
     * Updates the UI of password protection option.
     */
    fun updatePasswordLayouts() {
        val password = getLinkInterface.getLinkPassword();
        val isPasswordSet = !isTextEmpty(password)
        val visibility = if (isPasswordSet) VISIBLE else GONE

        if (isPasswordSet) {
            if (binding.decryptedKeySwitch.isChecked) {
                sendDecryptedKeySeparatelyClick(false)
            }

            binding.passwordProtectionSetText.transformationMethod = PasswordTransformationMethod()

            binding.copyLinkButton.background = accentDrawable
            binding.copyLinkButton.setTextColor(Color.WHITE)
        } else {
            binding.copyLinkButton.background = transparentDrawable
            binding.copyLinkButton.setTextColor(accentColor)
        }

        binding.passwordProtectionSetText.visibility = visibility
        binding.passwordProtectionSetText.text = if (isPasswordSet) password else null
        binding.passwordProtectionSetToggle.visibility = visibility

        binding.resetPasswordButton.visibility = visibility
        binding.removePasswordButton.visibility = visibility

        binding.copyPasswordButton.visibility = visibility

        if (isPasswordSet || !node.isExported) {
            binding.passwordProtectionLayout.setOnClickListener(null)
        } else {
            binding.passwordProtectionLayout.setOnClickListener { setPasswordProtectionClick() }
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
            binding.passwordProtectionSetToggle.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_b_shared_read
                )
            )
        } else {
            binding.passwordProtectionSetText.transformationMethod = null
            binding.passwordProtectionSetToggle.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_b_see
                )
            )
        }

        passwordVisible = !passwordVisible
    }

    fun updateLink() {
        node = getLinkInterface.getNode()

        if (node.isExported) {
            linkWithKey = node.publicLink
            linkWithoutKey = getLinkInterface.getLinkWithoutKey()
            key = getLinkInterface.getLinkKey()

            updateLinkText()
        }

        setAvailableLayouts()
    }

    fun isSendDecryptedKeySeparatelyEnabled(): Boolean {
        return binding.decryptedKeySwitch.isChecked
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val cal = Calendar.getInstance()
        cal.set(year, month, dayOfMonth)

        val date = cal.time
        val dfTimestamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateString = dfTimestamp.format(date) + LAST_MINUTE
        val timestamp = calculateTimestamp(dateString).toInt()

        nC.exportLinkTimestamp(node, timestamp)
    }
}