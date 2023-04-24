package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetManageChatLinkBinding
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.utils.ChatUtil.showConfirmationRemoveChatLink
import mega.privacy.android.app.utils.Constants.COPIED_TEXT_LABEL
import mega.privacy.android.app.utils.Constants.TYPE_TEXT_PLAIN
import mega.privacy.android.app.utils.Util.showSnackbar

class ManageChatLinkBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    companion object {
        private const val CHAT_LINK = "CHAT_LINK"
        private const val IS_MODERATOR = "IS_MODERATOR"
        private const val CHAT_TITLE = "CHAT_TITLE"
    }

    private lateinit var binding: BottomSheetManageChatLinkBinding

    private var chatLink = ""
    private var isModerator = false
    private var chatTitle: String? = null

    fun setValues(chatLink: String, isModerator: Boolean, chatTitle: String?) {
        this.chatLink = chatLink
        this.isModerator = isModerator
        this.chatTitle = chatTitle
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = BottomSheetManageChatLinkBinding.inflate(layoutInflater)
        contentView = binding.root.rootView
        itemsLayout = binding.itemsLayout

        if (savedInstanceState != null) {
            chatLink = savedInstanceState.getString(CHAT_LINK, "")
            isModerator = savedInstanceState.getBoolean(IS_MODERATOR, false)
            chatTitle = savedInstanceState.getString(CHAT_TITLE, null)
        }

        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.copyManageChatLinkOption.setOnClickListener {
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(COPIED_TEXT_LABEL, chatLink)
            clipboard.setPrimaryClip(clip)

            showSnackbar(
                requireActivity(),
                getString(R.string.chat_link_copied_clipboard)
            )

            setStateBottomSheetBehaviorHidden()
        }

        binding.shareManageChatLinkOption.setOnClickListener {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = TYPE_TEXT_PLAIN
            sharingIntent.putExtra(Intent.EXTRA_TEXT, chatLink)
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, chatTitle)
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.context_share)))

            setStateBottomSheetBehaviorHidden()
        }

        if (!isModerator) binding.deleteManageChatLinkOption.visibility = View.GONE
        else binding.deleteManageChatLinkOption.setOnClickListener {
            showConfirmationRemoveChatLink(requireActivity())
            setStateBottomSheetBehaviorHidden()
        }

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(CHAT_LINK, chatLink)
        outState.putBoolean(IS_MODERATOR, isModerator)
        outState.putString(CHAT_TITLE, chatTitle)

        super.onSaveInstanceState(outState)
    }
}