package mega.privacy.android.app.presentation.chat.dialog

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.main.InviteContactActivity
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * Fragment to display a custom two buttons alert dialog when user is trying to add participants
 * to a chat/meeting but has no contacts
 */
@AndroidEntryPoint
class AddParticipantsNoContactsDialogFragment : DialogFragment() {

    @Inject
    /** Current theme */
    lateinit var getThemeMode: GetThemeMode

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext(),
            R.style.ThemeOverlay_Mega_MaterialAlertDialog)
            .apply {
                setTitle(StringResourcesUtils.getString(R.string.chat_add_participants_no_contacts_title))
                setMessage(StringResourcesUtils.getString(R.string.chat_add_participants_no_contacts_message))
                setNegativeButton(StringResourcesUtils.getString(R.string.button_cancel)) { _, _ ->
                    dismiss()
                }
                setPositiveButton(StringResourcesUtils.getString(R.string.contact_invite)) { _, _ ->
                    startActivity(Intent(requireContext(), InviteContactActivity::class.java))
                    dismiss()
                }
            }.create()

    companion object {
        /**
         * Creates an instance of this class
         *
         * @return AddParticipantsNoContactsDialogFragment new instance
         */
        @JvmStatic
        fun newInstance() = AddParticipantsNoContactsDialogFragment()
    }
}