package mega.privacy.android.feature.sync.ui.mapper.exclusion

import androidx.annotation.StringRes
import mega.privacy.android.feature.sync.domain.exception.SyncExclusionEditError
import javax.inject.Inject
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The copy telling the user why an exclusion edit was refused.
 *
 * Only a duplicate and a collision with a bound already set are named, because those are the two a
 * user reaches by typing: a bound added from a form that states one direction meets the file's own
 * bound in the other, which no form can draw. The rest describe states the forms now prevent, left
 * reachable only by another client rewriting the file underneath, so they fall back to the general
 * failure rather than each carrying words for a race.
 */
internal class SyncExclusionEditErrorMapper @Inject constructor() {

    @StringRes
    operator fun invoke(error: SyncExclusionEditError): Int = when (error) {
        is SyncExclusionEditError.DuplicateRule ->
            sharedR.string.sync_exclusions_add_name_duplicate_error

        is SyncExclusionEditError.EqualSizeBounds ->
            sharedR.string.sync_exclusions_manage_snackbar_size_conflict

        is SyncExclusionEditError.StaleRuleTarget,
        is SyncExclusionEditError.UnsupportedTarget,
        is SyncExclusionEditError.UnusableSizeBound,
            -> sharedR.string.general_text_error
    }
}
