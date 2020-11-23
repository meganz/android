package mega.privacy.android.app.fragments.getLinkFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import mega.privacy.android.app.MimeTypeList
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
        private const val ALPHA_VIEW_DISABLED = 0.5f
        private const val ALPHA_VIEW_ENABLED = 1.0f
    }

    private lateinit var binding: FragmentGetLinkBinding

    private var nC: NodeController? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGetLinkBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        nC = NodeController(context)

        val node = getLinkInterface.getNode()

        setThumbnail(node)
        binding.nodeName.text = node.name
        binding.nodeInfo.text =
            if (node.isFolder) getInfoFolder(node, context)
            else getSizeString(node.size)

        binding.copyKeyButton.visibility = GONE

        setProProOnlyLayouts()

        if (getLinkInterface.getNode().isExported) {

        } else {

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

    private fun setProProOnlyLayouts() {
        val isPro = app.myAccountInfo.accountType > ACCOUNT_TYPE_FREE
        val alpha = if (isPro) ALPHA_VIEW_ENABLED else ALPHA_VIEW_DISABLED

        binding.expiryDateText.alpha = alpha
        binding.expiryDateSwitch.alpha = alpha
        binding.passwordProtectionText.alpha = alpha

        if (isPro) {
            binding.expiryDateProOnlyText.visibility = GONE
            binding.expiryDateLayout.setOnClickListener { showSelectDateDialog() }
            binding.expiryDateSwitch.setOnClickListener { showSelectDateDialog() }

            binding.passwordProtectionProOnlyText.visibility = GONE
            binding.passwordProtectionLayout.setOnClickListener { getLinkInterface.startSetPassword() }
        } else {
            binding.expiryDateProOnlyText.visibility = VISIBLE
            binding.expiryDateLayout.setOnClickListener(null)
            binding.expiryDateSwitch.setOnClickListener(null)

            binding.passwordProtectionProOnlyText.visibility = VISIBLE
            binding.passwordProtectionLayout.setOnClickListener(null)
        }
    }

    private fun showSelectDateDialog() {

    }
}