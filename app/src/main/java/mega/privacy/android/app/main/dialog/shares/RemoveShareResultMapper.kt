package mega.privacy.android.app.main.dialog.shares

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.node.ResultCount
import javax.inject.Inject

/**
 * Remove share result mapper
 *
 * @property successString
 * @property errorString
 */
class RemoveShareResultMapper(
    private val successString: () -> String,
    private val errorString: (Int) -> String,
) {

    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(
        successString = { context.getString(R.string.context_share_correctly_removed) },
        errorString = { errorCount ->
            context.resources.getQuantityString(
                R.plurals.shared_items_outgoing_shares_snackbar_remove_contact_access_failed,
                errorCount,
                errorCount
            )
        }
    )

    /**
     * Invoke
     *
     * @param result
     * @return the string to show in the snackbar
     */
    operator fun invoke(result: ResultCount): String =
        if (result.errorCount == 0) successString()
        else errorString(result.errorCount)
}