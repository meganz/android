package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetManageChatLinkBinding
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.utils.ChatUtil.showConfirmationRemoveChatLink

class ManageChatLinkBottomSheetDialogFragment() : BaseBottomSheetDialogFragment() {

    companion object {
        private const val CHAT_LINK = "CHAT_LINK"
        private const val IS_MODERATOR = "IS_MODERATOR"
    }

    private lateinit var binding: BottomSheetManageChatLinkBinding

    private var chatLink = ""
    private var isModerator = false

    fun setValues(chatLink: String, isModerator: Boolean) {
        this.chatLink = chatLink
        this.isModerator = isModerator
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            chatLink = savedInstanceState.getString(CHAT_LINK, "")
            isModerator = savedInstanceState.getBoolean(IS_MODERATOR, false)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        binding = BottomSheetManageChatLinkBinding.inflate(layoutInflater)
        contentView = binding.root.rootView
        mainLinearLayout = binding.manageChatLinkBottomSheet
        items_layout = binding.itemsLayout

        binding.copyManageChatLinkLayout.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", chatLink)
            clipboard.setPrimaryClip(clip)

            if (activity is GroupChatInfoActivityLollipop) {
                (activity as GroupChatInfoActivityLollipop).showSnackbar(getString(R.string.chat_link_copied_clipboard))
            }

            setStateBottomSheetBehaviorHidden()
        }

        binding.shareManageChatLinkLayout.setOnClickListener {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(Intent.EXTRA_TEXT, chatLink)
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.context_share)))

            setStateBottomSheetBehaviorHidden()
        }

        if (!isModerator) binding.deleteManageChatLinkLayout.visibility = View.GONE
        else binding.deleteManageChatLinkLayout.setOnClickListener {
            showConfirmationRemoveChatLink(
                activity
            )

            setStateBottomSheetBehaviorHidden()
        }

        dialog.setContentView(contentView)
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(CHAT_LINK, chatLink)
        outState.putBoolean(IS_MODERATOR, isModerator)

        super.onSaveInstanceState(outState)
    }
}