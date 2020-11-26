package mega.privacy.android.app.fragments.getLinkFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import mega.privacy.android.app.MimeTypeList.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentGetLinkBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.interfaces.GetLinkInterface
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.utils.MegaApiUtils.getInfoFolder
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.Util.getSizeString
import nz.mega.sdk.MegaAccountDetails.ACCOUNT_TYPE_FREE
import nz.mega.sdk.MegaNode

class GetLinkFragment(private val getLinkInterface: GetLinkInterface) : BaseFragment() {

    companion object {
        private const val ALPHA_VIEW_DISABLED = 0.3f
        private const val ALPHA_VIEW_ENABLED = 1.0f
    }

    private lateinit var binding: FragmentGetLinkBinding

    private lateinit var linkWithKey: String
    private lateinit var linkWithoutKey: String
    private lateinit var key: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGetLinkBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val node = getLinkInterface.getNode()

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

        binding.copyKeyButton.visibility = GONE
        binding.copyKeyButton.setOnClickListener { getLinkInterface.copyLinkOrKey(key, false) }

        updateLink()

        if (!node.isExported) {
            binding.linkText.text = getString(R.string.link_request_status)
            getLinkInterface.exportNode()
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
        val isPro = app.myAccountInfo.accountType > ACCOUNT_TYPE_FREE
        val alpha = if (isPro) ALPHA_VIEW_ENABLED else ALPHA_VIEW_DISABLED

        binding.expiryDateText.alpha = alpha
        binding.expiryDateSwitch.alpha = alpha
        binding.passwordProtectionText.alpha = alpha

        if (isExported) {
            binding.decryptedKeyText.alpha = ALPHA_VIEW_ENABLED
            binding.decryptedKeyLayout.setOnClickListener { clickDecryptedKeySeparately() }
            binding.decryptedKeySwitch.alpha = ALPHA_VIEW_ENABLED
            binding.decryptedKeySwitch.setOnClickListener { clickDecryptedKeySeparately() }
            binding.decryptedKeySwitch.isEnabled = true

            if (isPro) {
                binding.expiryDateProOnlyText.visibility = GONE
                binding.expiryDateLayout.setOnClickListener { showSelectDateDialog() }
                binding.expiryDateSwitch.setOnClickListener { showSelectDateDialog() }
                binding.expiryDateSwitch.isEnabled = true

                binding.passwordProtectionProOnlyText.visibility = GONE
                binding.passwordProtectionLayout.setOnClickListener { getLinkInterface.startSetPassword() }
            } else {
                binding.expiryDateProOnlyText.visibility = VISIBLE
                binding.expiryDateLayout.setOnClickListener(null)
                binding.expiryDateSwitch.setOnClickListener(null)
                binding.expiryDateSwitch.isEnabled = false

                binding.passwordProtectionProOnlyText.visibility = VISIBLE
                binding.passwordProtectionLayout.setOnClickListener(null)
            }
        } else {
            binding.decryptedKeyText.alpha = ALPHA_VIEW_DISABLED
            binding.decryptedKeyLayout.setOnClickListener(null)
            binding.decryptedKeySwitch.alpha = ALPHA_VIEW_DISABLED
            binding.decryptedKeySwitch.setOnClickListener(null)
            binding.decryptedKeySwitch.isEnabled = false

            binding.expiryDateProOnlyText.visibility = VISIBLE
            binding.expiryDateLayout.setOnClickListener(null)
            binding.expiryDateSwitch.setOnClickListener(null)
            binding.expiryDateSwitch.isEnabled = false

            binding.passwordProtectionProOnlyText.visibility = VISIBLE
            binding.passwordProtectionLayout.setOnClickListener(null)
        }
    }

    private fun clickDecryptedKeySeparately() {
       if (binding.decryptedKeySwitch.isChecked) {
           binding.copyKeyButton.visibility = VISIBLE
           binding.linkText.text = linkWithoutKey
       } else {
           binding.copyKeyButton.visibility = GONE
           binding.linkText.text = linkWithKey
       }
    }

    private fun clickLearnMore() {

    }

    private fun showSelectDateDialog() {

    }

    fun updateLink() {
        val node = getLinkInterface.getNode()

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
}