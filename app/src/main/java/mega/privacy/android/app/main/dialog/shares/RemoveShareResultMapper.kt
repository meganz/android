package mega.privacy.android.app.main.dialog.shares

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.node.RemoveShareResult
import javax.inject.Inject

/**
 * Remove share result mapper
 *
 * @property context
 */
class RemoveShareResultMapper @Inject constructor(@ApplicationContext private val context: Context) {
    /**
     * Invoke
     *
     * @param result
     * @return the string to show in the snackbar
     */
    operator fun invoke(result: RemoveShareResult): String =
        if (result.errorCount == 0) context.getString(R.string.context_share_correctly_removed)
        else context.resources.getQuantityString(
            R.plurals.shared_items_outgoing_shares_snackbar_remove_contact_access_failed,
            result.errorCount,
            result.errorCount
        )
}