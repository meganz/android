package mega.privacy.android.feature.sharelink.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.feature.sharelink.session.ShareLinkSession
import javax.inject.Inject

/**
 * Owns the [ShareLinkSession] for one visit to the Share link flow.
 *
 * Retrieved with `sharedViewModel` from both the Share link and Link settings entries, so both get
 * the same session while either is on the back stack. The Share link entry owns the navigation
 * scope this lives in, so popping it clears the store and the session with it.
 */
@HiltViewModel
class ShareLinkSessionViewModel @Inject constructor() : ViewModel() {

    /** The session both screens of this flow share. */
    val session = ShareLinkSession()
}
