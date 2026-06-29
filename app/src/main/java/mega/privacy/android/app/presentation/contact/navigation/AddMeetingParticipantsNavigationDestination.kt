package mega.privacy.android.app.presentation.contact.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.contact.navigation.AddMeetingParticipantsEntry
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.AddMeetingParticipantsNavKey

/**
 * Registers the [AddMeetingParticipantsNavKey] destination, rendering the Compose
 * [AddMeetingParticipantsEntry] picker (the in-call/meeting variant, with the user-limit warning).
 *
 * The flag gate for this flow lives at the legacy launch site ([AddParticipantsComposeActivity] is
 * only launched when `ContactsComposeUI` is on), so this destination renders the Compose picker
 * directly. The selected emails are published as a `List<String>` under [AddMeetingParticipantsNavKey.KEY].
 *
 * TODO: Move this entry to the feature module once the feature flag is removed
 */
fun EntryProviderScope<NavKey>.addMeetingParticipantsDestination(navigationHandler: NavigationHandler) {
    entry<AddMeetingParticipantsNavKey> { key ->
        AddMeetingParticipantsEntry(
            navigationHandler = navigationHandler,
            chatId = key.chatId,
        )
    }
}
