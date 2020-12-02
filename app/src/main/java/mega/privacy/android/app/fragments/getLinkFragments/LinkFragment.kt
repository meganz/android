package mega.privacy.android.app.fragments.getLinkFragments

import android.app.DatePickerDialog
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
import mega.privacy.android.app.utils.Util.*
import nz.mega.sdk.MegaAccountDetails.ACCOUNT_TYPE_FREE
import nz.mega.sdk.MegaNode
import org.jetbrains.anko.applyRecursively
import java.text.SimpleDateFormat
import java.util.*


class LinkFragment(private val getLinkInterface: GetLinkInterface) : BaseFragment(),
    DatePickerDialog.OnDateSetListener {

    companion object {
        private const val ALPHA_VIEW_DISABLED = 0.3f
        private const val ALPHA_VIEW_ENABLED = 1.0f

        private const val INVALID_EXPIRATION_TIME = -1L
    }

    private lateinit var binding: FragmentGetLinkBinding

    private lateinit var linkWithKey: String
    private lateinit var linkWithoutKey: String
    private lateinit var key: String

    private var isPro: Boolean = false
    private lateinit var node: MegaNode

    private lateinit var nC: NodeController

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
        isPro = app.myAccountInfo.accountType > ACCOUNT_TYPE_FREE

        node = getLinkInterface.getNode()

        nC = NodeController(activity)

        setThumbnail(node)
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

        binding.copyKeyButton.setOnClickListener { getLinkInterface.copyLinkKey(key) }
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

    private fun checkIfShouldHidePassword() {
        if (passwordVisible) {
            toggleClick()
        }
    }

    private fun removePasswordClick() {
        checkIfShouldHidePassword()
        getLinkInterface.removeLinkWithPassword()
        updatePasswordLayouts()
        updateLinkText()
    }

    private fun updateLinkText() {
        val linkWithPassword = getLinkInterface.getLinkWithPassword();

        binding.linkText.text =
            if (!getLinkInterface.getNode().isExported) getString(R.string.link_request_status)
            else if (binding.decryptedKeySwitch.isChecked) linkWithoutKey
            else if (!isTextEmpty(linkWithPassword)) linkWithPassword
            else linkWithKey
    }

    private fun setThumbnail(node: MegaNode) {
        if (node.isFolder) {
            binding.nodeThumbnail.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_folder_list,
                    null
                )
            )
        } else if (node.hasThumbnail()) {
            var thumb = ThumbnailUtils.getThumbnailFromCache(node)
            if (thumb != null) {
                binding.nodeThumbnail.setImageBitmap(thumb)
            } else {
                thumb = ThumbnailUtils.getThumbnailFromFolder(node, context)

                if (thumb != null) {
                    binding.nodeThumbnail.setImageBitmap(thumb)
                } else {
                    binding.nodeThumbnail.setImageResource(typeForName(node.name).iconResourceId)
                }
            }
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

    private fun getExpiredDateText(): String {
        val df = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault())
        val cal = calculateDateFromTimestamp(node.expirationTime)
        val tz = cal.timeZone
        df.timeZone = tz
        val date = cal.time
        return df.format(date)
    }

    private fun sendDecryptedKeySeparatelyClick(isSwitchClick: Boolean) {
        if (!isSwitchClick) {
            binding.decryptedKeySwitch.isChecked = !binding.decryptedKeySwitch.isChecked
        }

        if (binding.decryptedKeySwitch.isChecked && !isTextEmpty(getLinkInterface.getLinkWithPassword())) {
            removePasswordClick()
        }

        updateSendDecryptedKeySeparatelyLayouts()
    }

    private fun updateSendDecryptedKeySeparatelyLayouts() {
        val visibility = if (binding.decryptedKeySwitch.isChecked) VISIBLE else GONE

        binding.keyLayout.visibility = visibility
        binding.keySeparator.visibility = visibility
        binding.copyKeyButton.visibility = visibility

        updateLinkText()

        binding.keyText.text = if (binding.decryptedKeySwitch.isChecked) key else null
    }

    private fun setExpiryDateClick(isSwitchClick: Boolean) {
        checkIfShouldHidePassword()

        if (!isPro || (isSwitchClick && node.expirationTime <= 0)) {
            binding.expiryDateSwitch.isChecked = false
        }

        if (isPro && node.expirationTime > 0) {
            if (!isSwitchClick) {
                binding.expiryDateSwitch.isChecked = false
            }

            binding.expiryDateSetText.visibility = GONE
            binding.expiryDateSetText.text = null

            nC.exportLink(node)
        } else if (isPro) {
            showDatePicker()
        } else {
            getLinkInterface.showUpgradeToProWarning()
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

    private fun setPasswordProtectionClick() {
        if (isPro) {
            checkIfShouldHidePassword()
            getLinkInterface.showFragment(PASSWORD_FRAGMENT)
        } else {
            getLinkInterface.showUpgradeToProWarning()
        }
    }

    fun updatePasswordLayouts() {
        val password = getLinkInterface.getPasswordLink();
        val isPasswordSet = !isTextEmpty(password)
        val visibility = if (isPasswordSet) VISIBLE else GONE

        if (isPasswordSet) {
            if (binding.decryptedKeySwitch.isChecked) {
                sendDecryptedKeySeparatelyClick(false)
            }

            binding.passwordProtectionSetText.transformationMethod = PasswordTransformationMethod()

            binding.copyLinkButton.applyRecursively { R.style.AccentButton }
        } else {
            binding.copyLinkButton.applyRecursively { R.style.AccentBorderBackgroundBorderlessButton }
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

            if (linkWithKey.contains("#!") || linkWithKey.contains("#F!")) {
                //old file or folder link format
                val s = linkWithKey.split("!")
                if (s.size == 3) {
                    linkWithoutKey = s[0] + "!" + s[1]
                    key = s[2];
                }
            } else {
                // new file or folder link format
                val s = linkWithKey.split("#")
                if (s.size == 2) {
                    linkWithoutKey = s[0]
                    key = s[1];
                }
            }

            updateLinkText()
        }

        setAvailableLayouts()
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val cal = Calendar.getInstance()
        cal.set(year, month, dayOfMonth)

        val date = cal.time
        val dfTimestamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateString = dfTimestamp.format(date) + "2359"
        val timestamp = calculateTimestamp(dateString).toInt()

        nC.exportLinkTimestamp(node, timestamp)
    }
}