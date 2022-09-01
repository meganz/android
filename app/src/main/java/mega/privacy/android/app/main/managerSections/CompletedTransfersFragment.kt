package mega.privacy.android.app.main.managerSections

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.AndroidCompletedTransfer
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.DatabaseHandler.Companion.MAX_TRANSFERS
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.managerFragments.TransfersBaseFragment
import mega.privacy.android.app.main.adapters.MegaCompletedTransfersAdapter
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

/**
 * The Fragment is used for displaying the finished items of transfer.
 */
@AndroidEntryPoint
class CompletedTransfersFragment : TransfersBaseFragment() {
    private var adapter: MegaCompletedTransfersAdapter? = null

    val tL = mutableListOf<AndroidCompletedTransfer?>()

    @Inject
    lateinit var dbH: DatabaseHandler

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val v = initView(inflater, container)

        emptyImage.setImageResource(
            if (Util.isScreenInPortrait(requireContext())) {
                R.drawable.empty_transfer_portrait
            } else R.drawable.empty_transfer_landscape)

        var textToShow = StringResourcesUtils.getString(R.string.completed_transfers_empty_new)

        try {
            textToShow = textToShow.replace("[A]",
                "<font color=\'${
                    ColorUtils.getColorHexString(requireContext(),
                        R.color.grey_900_grey_100)
                }\'>")
            textToShow = textToShow.replace("[/A]", "</font>")
            textToShow = textToShow.replace("[B]",
                "<font color=\'${
                    ColorUtils.getColorHexString(requireContext(),
                        R.color.grey_300_grey_600)
                }\'>")
            textToShow = textToShow.replace("[/B]", "</font>")
        } catch (e: Exception) {
            Timber.w(e, "Exception formatting string")
        }

        emptyText.text = HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY)

        setCompletedTransfers()

        adapter = MegaCompletedTransfersAdapter(requireActivity(), tL)
        listView.adapter = adapter
        return v
    }

    private fun setCompletedTransfers() {
        tL.clear()
        tL.addAll(dbH.completedTransfers)
        setEmptyView(tL.size)
    }

    /**
     * Adds new completed transfer.
     *
     * @param transfer the transfer to add
     */
    fun transferFinish(transfer: AndroidCompletedTransfer) {
        tL.add(0, transfer)
        if (tL.size >= MAX_TRANSFERS) {
            tL.removeAt(tL.size - 1)
        }

        setEmptyView(tL.size)
        adapter?.notifyDataSetChanged()
        managerActivity.invalidateOptionsMenu()
    }

    /**
     * Checks if there is any completed transfer.
     *
     * @return True if there is any completed transfer, false otherwise.
     */
    fun isAnyTransferCompleted(): Boolean = tL.isNotEmpty()

    /**
     * Removes a completed transfer.
     *
     * @param transfer transfer to remove
     */
    fun transferRemoved(transfer: AndroidCompletedTransfer) {
        val index = tL.mapNotNull {
            it
        }.indexOfFirst { completedTransfer ->
            areTheSameTransfer(transfer, completedTransfer)
        }

        tL.removeAt(index)
        adapter?.removeItemData(index)
    }

    /**
     * Removes all completed transfers.
     */
    fun clearCompletedTransfers() {
        tL.clear()
        adapter?.setTransfers(tL)
        setEmptyView(tL.size)
    }

    private fun areTheSameTransfer(
        transfer1: AndroidCompletedTransfer,
        transfer2: AndroidCompletedTransfer,
    ) =
        transfer1.id == transfer2.id ||
                (isValidHandle(transfer1) && isValidHandle(transfer2) &&
                        transfer1.nodeHandle == transfer2.nodeHandle) ||
                (transfer1.error == transfer2.error && transfer1.fileName == transfer2.fileName &&
                        transfer1.size == transfer2.size)


    /**
     * Checks if a transfer has a valid handle.
     *
     * @param transfer AndroidCompletedTransfer to check.
     * @return True if the transfer has a valid handle, false otherwise.
     */
    private fun isValidHandle(transfer: AndroidCompletedTransfer) =
        !TextUtil.isTextEmpty(transfer.nodeHandle) && transfer.nodeHandle == INVALID_HANDLE.toString()

    companion object {

        /**
         * Generate a new instance for [CompletedTransfersFragment]
         *
         * @return new [CompletedTransfersFragment] instance
         */
        @JvmStatic
        fun newInstance(): CompletedTransfersFragment = CompletedTransfersFragment()
    }
}