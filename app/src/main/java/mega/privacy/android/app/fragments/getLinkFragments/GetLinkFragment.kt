package mega.privacy.android.app.fragments.getLinkFragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.core.content.res.ResourcesCompat
import mega.privacy.android.app.MimeTypeList.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentGetLinkBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.interfaces.GetLinkInterface
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.utils.MegaApiUtils.getInfoFolder
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.Util.*
import nz.mega.sdk.MegaAccountDetails.ACCOUNT_TYPE_FREE
import nz.mega.sdk.MegaNode
import java.text.SimpleDateFormat
import java.util.*


class GetLinkFragment(private val getLinkInterface: GetLinkInterface) : BaseFragment(),
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

        binding.learnMoreTextButton.setOnClickListener { clickLearnMore() }

        binding.copyLinkButton.setOnClickListener {
            getLinkInterface.copyLinkOrKey(
                if (binding.decryptedKeySwitch.isChecked) linkWithoutKey
                else linkWithKey, true
            )
        }

        binding.keyLayout.visibility = GONE
        binding.keySeparator.visibility = GONE

        binding.copyKeyButton.visibility = GONE
        binding.copyKeyButton.setOnClickListener { getLinkInterface.copyLinkOrKey(key, false) }

        updateLink()

        if (!node.isExported) {
            binding.linkText.text = getString(R.string.link_request_status)
            nC.exportLink(node)
        }

        super.onViewCreated(view, savedInstanceState)
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
     *
     * @param isExported True if the node already has link, false if it is being request.
     */
    private fun setAvailableLayouts(isExported: Boolean) {
        val alpha = if (isExported) ALPHA_VIEW_ENABLED else ALPHA_VIEW_DISABLED

        binding.decryptedKeyText.alpha = alpha
        binding.expiryDateText.alpha = alpha
        binding.passwordProtectionText.alpha = alpha

        if (isExported) {
            binding.decryptedKeyLayout.setOnClickListener { sendDecryptedKeySeparatelyClick(false) }
            binding.decryptedKeySwitch.setOnClickListener { sendDecryptedKeySeparatelyClick(true) }
            binding.decryptedKeySwitch.isEnabled = true

            binding.expiryDateLayout.setOnClickListener { setExpiryDateClick(false) }
            binding.expiryDateSwitch.setOnClickListener { setExpiryDateClick(true) }

            if (node.expirationTime > 0) {
                binding.expiryDateSwitch.isChecked = true
                binding.expiryDateSetText.visibility = VISIBLE
                binding.expiryDateSetText.text = getExpiredDateText()
            } else {
                binding.expiryDateSwitch.isChecked = false
                binding.expiryDateSetText.visibility = GONE
                binding.expiryDateSetText.text = null
            }

            binding.passwordProtectionLayout.setOnClickListener { setPasswordProtectionClick() }

            binding.copyLinkButton.visibility = VISIBLE
        } else {
            binding.decryptedKeyLayout.setOnClickListener(null)
            binding.decryptedKeySwitch.setOnClickListener(null)
            binding.decryptedKeySwitch.isEnabled = false

            binding.expiryDateLayout.setOnClickListener(null)
            binding.expiryDateSwitch.setOnClickListener(null)

            binding.passwordProtectionLayout.setOnClickListener(null)

            binding.copyLinkButton.visibility = GONE
        }

        if (isPro) {
            binding.expiryDateProOnlyText.visibility = GONE
            binding.expiryDateSwitch.isEnabled = isExported

            binding.passwordProtectionProOnlyText.visibility = GONE
        } else {
            binding.expiryDateProOnlyText.visibility = VISIBLE
            binding.expiryDateSwitch.isEnabled = false

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

        val visibility = if (binding.decryptedKeySwitch.isChecked) VISIBLE else GONE

        binding.keyLayout.visibility = visibility
        binding.keySeparator.visibility = visibility
        binding.copyKeyButton.visibility = visibility

        if (binding.decryptedKeySwitch.isChecked) {
            binding.linkText.text = linkWithoutKey
            binding.keyText.text = key
        } else {
            binding.linkText.text = linkWithKey
            binding.keyText.text = null
        }
    }

    private fun clickLearnMore() {

    }

    private fun setExpiryDateClick(isSwitchClick: Boolean) {
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
            showUpgradeToProDialog()
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

    private fun showUpgradeToProDialog() {

    }

    private fun setPasswordProtectionClick() {
        if (isPro) {
            getLinkInterface.startSetPassword()
        } else {
            showUpgradeToProDialog()
        }
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

            binding.linkText.text = linkWithKey
        }

        setAvailableLayouts(node.isExported)
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