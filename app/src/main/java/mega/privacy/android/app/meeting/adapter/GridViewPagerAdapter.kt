package mega.privacy.android.app.meeting.adapter

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import mega.privacy.android.app.R
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView
import mega.privacy.android.app.meeting.fragments.InMeetingFragment
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.utils.LogUtil.logDebug

class GridViewPagerAdapter(
    var data: List<List<Participant>>,
    private val inMeetingViewModel: InMeetingViewModel,
    private val fragment: Fragment?,
    private var maxWidth: Int,
    private var maxHeight: Int,
) : RecyclerView.Adapter<GridViewPagerAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private var orientation = Configuration.ORIENTATION_PORTRAIT

    private val adapterList = mutableListOf<VideoGridViewAdapter?>()

    var recyclerView: CustomizedGridCallRecyclerView? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val root = inflater.inflate(R.layout.grid_view_call_item, parent, false)
        return ViewHolder(root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        recyclerView = holder.itemView.findViewById(R.id.grid_view)

        val participantsForPage = data[position]

        participantsForPage.let {
            recyclerView?.let {
                it.apply {
                    adapter = null
                    itemAnimator = DefaultItemAnimator()
                    setOnTouchCallback {
                        (fragment as InMeetingFragment).onPageClick()
                    }
                    clipToPadding = true
                    setHasFixedSize(true)
                    setParamsForGridView(position, participantsForPage, this)
                    setColumnWidth(position, this, participantsForPage.size, orientation)
                }
            }

            val adapter = VideoGridViewAdapter(
                inMeetingViewModel,
                recyclerView!!,
                maxWidth,
                maxHeight,
                position,
                orientation,
            )

            adapter.submitList(null)
            adapter.submitList(participantsForPage)

            if (adapterList.isNotEmpty() && adapterList.size > position) {
                adapterList.removeAt(position)
            }
            adapterList.add(position, adapter)

            recyclerView!!.adapter = adapter
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setNewData(newData: List<List<Participant>>) {
        if (data.isNotEmpty()) {
            data = newData
        } else {
            data = newData
            notifyDataSetChanged()
        }

        if (data.isNotEmpty() && adapterList.isNotEmpty()) {
            for (i in 0 until adapterList.size) {
                if (data.size > i) {
                    val participantsForPage = data[i]
                    if (participantsForPage.isNotEmpty()) {
                        adapterList[i]?.submitList(participantsForPage)
                    }
                }
            }
        }
    }

    /**
     * Control when a participant is added
     *
     * @param previousList the previous list of participants
     * @param currentList the current list of participants
     * @param position the position that has changed
     */
    fun participantAdded(
        previousList: List<List<Participant>>,
        currentList: List<List<Participant>>,
        position: Int
    ) {
        val lastPageNum = currentList.size - 1
        var isANewPage = false
        if (currentList.size - previousList.size == 1) {
            isANewPage = true
        }

        if (isANewPage) {
            logDebug("A new page is needed: lastPageNum $lastPageNum")
            notifyItemInserted(lastPageNum)
            return
        }

        val pageWithChange = position / PARTICIPANTS_PER_PAGE
        logDebug("Update the currentPage : $pageWithChange")
        if (adapterList.isNotEmpty() && adapterList.size > pageWithChange) {
            val participantsInPage = currentList[pageWithChange]
            adapterList[pageWithChange]?.let {
                it.submitList(participantsInPage) {
                    when (pageWithChange) {
                        0 -> {
                            when {
                                it.currentList.size <= 3 -> {
                                    logDebug("Update only the adapter in the pager")
                                    it.notifyDataSetChanged()
                                }
                                it.currentList.size == 4 -> {
                                    logDebug("Update the current page, as the number of columns must be updated.")
                                    notifyItemChanged(pageWithChange)
                                }
                                it.currentList.size == 5 -> {
                                    logDebug("Update position only")
                                    it.notifyItemInserted(position)
                                }
                                else -> {
                                    logDebug("update the position and the previous position")
                                    it.notifyItemInserted(position)
                                    it.notifyItemRangeChanged(position - 1, it.currentList.size)
                                }
                            }
                        }
                        else -> {
                            logDebug("Update position only")
                            val positionInPage = position - (PARTICIPANTS_PER_PAGE * pageWithChange)
                            it.notifyItemInserted(positionInPage)
                        }
                    }
                }
            }
        }
    }

    /**
     * Control when a participant is removed
     *
     * @param previousList the previous list of participants
     * @param currentList the current list of participants
     * @param position the position that has changed
     */
    fun participantRemoved(
        previousList: List<List<Participant>>,
        currentList: List<List<Participant>>,
        position: Int
    ) {
        val lastPageNum = previousList.size - 1
        var removePage = false
        if (currentList.size - previousList.size == -1) {
            removePage = true
        }

        val pageWithChange = position / PARTICIPANTS_PER_PAGE
        logDebug("The current page is $pageWithChange. The last page is $lastPageNum")

        if (removePage) {
            logDebug("This page should be deleted $pageWithChange")
            if (adapterList.isNotEmpty() && adapterList.size > lastPageNum) {
                adapterList[lastPageNum]?.submitList(null)

                notifyItemRemoved(lastPageNum)
            }

            if (pageWithChange != lastPageNum) {
                logDebug("Checking pages to update")
                checkPagesToUpdate(position, pageWithChange, lastPageNum)
            }
        } else if (pageWithChange == (currentList.size - 1)) {
            logDebug("There is only the page with the change")
            updatePageWithChange(pageWithChange, position)
        } else {
            logDebug("Checking pages to update")
            checkPagesToUpdate(position, pageWithChange, lastPageNum)
        }
    }

    /**
     * Method to check which pages need to be updated
     *
     * @param position Position of the change
     * @param numPage num of the page with the change
     * @param lastPage the last page
     */
    private fun checkPagesToUpdate(position: Int, numPage: Int, lastPage: Int) {
        logDebug("Checking the rest of the pages to be updated ... ")
        for (i in numPage until lastPage) {
            if (i == numPage) {
                logDebug("Update the page with the participant removed")
                updatePageWithChange(numPage, position)
            } else {
                if (data.isNotEmpty() && data.size > i) {
                    val participantsForPage = data[i]
                    if (adapterList.isNotEmpty() && adapterList.size > i) {
                        adapterList[i]?.let {
                            logDebug("Completely update the page $i")
                            it.submitList(participantsForPage) {
                                it.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Method for updating the page on which the participant has been deleted
     *
     * @param pageWithChange num of the Page
     * @param position position of participant removed
     */
    private fun updatePageWithChange(pageWithChange: Int, position: Int) {
        val participantsForPage = data[pageWithChange]
        adapterList[pageWithChange]?.let {
            it.submitList(participantsForPage) {
                when (pageWithChange) {
                    0 -> {
                        when {
                            it.currentList.size <= 2 -> {
                                logDebug("Update only the adapter in the pager")
                                it.notifyDataSetChanged()
                            }
                            it.currentList.size == 3 -> {
                                logDebug("Update the current page, as the number of columns must be updated.")
                                notifyItemChanged(pageWithChange)
                            }
                            else -> {
                                when (position) {
                                    0 -> {
                                        logDebug("First page, update the current page")
                                        notifyItemChanged(pageWithChange)
                                    }
                                    else -> {
                                        logDebug("First page, update position only")
                                        var rangeToUpdate = position
                                        if (participantsForPage.size <= 5 && position == 5) {
                                            rangeToUpdate = position - 1
                                        }
                                        it.notifyItemRemoved(position)
                                        it.notifyItemRangeRemoved(
                                            rangeToUpdate,
                                            it.currentList.size
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        val positionInPage = position - (PARTICIPANTS_PER_PAGE * pageWithChange)
                        logDebug("Another page, update the item removed")
                        it.notifyItemRemoved(positionInPage)
                        it.notifyItemRangeChanged(positionInPage, it.currentList.size)
                    }
                }
            }
        }
    }


    fun getHolder(currentPage: Int, pager: ViewPager2): ViewHolder? {
        (pager[0] as RecyclerView).findViewHolderForAdapterPosition(currentPage)?.let {
            return it as ViewHolder
        }

        return null
    }

    fun updateParticipantPrivileges(participant: Participant, currentPage: Int, pager: ViewPager2) {
        val holder = getHolder(currentPage, pager)
        holder?.let {
            adapterList.let {
                if (adapterList.isEmpty())
                    return

                for (i in 0 until adapterList.size) {
                    adapterList[i]?.updateParticipantPrivileges(participant)
                }

            }
            return
        }

        notifyItemChanged(currentPage)
    }

    fun updateParticipantName(participant: Participant, currentPage: Int, pager: ViewPager2) {
        val holder = getHolder(currentPage, pager)
        holder?.let {
            adapterList.let {
                if (adapterList.isEmpty())
                    return

                for (i in 0 until adapterList.size) {
                    adapterList[i]?.updateParticipantName(participant)
                }

            }
            return
        }

        notifyItemChanged(currentPage)
    }

    fun updateCallOnHold(
        participant: Participant,
        isOnHold: Boolean,
        currentPage: Int,
        pager: ViewPager2
    ) {
        val holder = getHolder(currentPage, pager)
        holder?.let {
            adapterList.let {
                if (adapterList.isEmpty())
                    return

                for (i in 0 until adapterList.size) {
                    adapterList[i]?.updateCallOnHold(participant, isOnHold)
                }

            }
            return
        }

        notifyItemChanged(currentPage)
    }

    fun updateSessionOnHold(
        participant: Participant,
        isOnHold: Boolean,
        currentPage: Int,
        pager: ViewPager2
    ) {
        val holder = getHolder(currentPage, pager)
        holder?.let {
            adapterList.let {
                if (adapterList.isEmpty())
                    return

                for (i in 0 until adapterList.size) {
                    adapterList[i]?.updateSessionOnHold(participant, isOnHold)
                }

            }
            return
        }

        notifyItemChanged(currentPage)
    }

    fun updateParticipantAudioVideo(
        typeChange: Int,
        participant: Participant,
        currentPage: Int,
        pager: ViewPager2
    ) {
        val holder = getHolder(currentPage, pager)
        holder?.let {
            adapterList.let {
                if (adapterList.isEmpty())
                    return

                for (i in 0 until adapterList.size) {
                    adapterList[i]?.updateParticipantAudioVideo(typeChange, participant)
                }

            }
            return
        }

        notifyItemChanged(currentPage)
    }

    /**
     * Method to destroy the texture view
     */
    fun removeTextureView(participant: Participant, currentPage: Int, pager: ViewPager2) {
        val holder = getHolder(currentPage, pager)
        holder?.let {
            adapterList.let {
                if (adapterList.isEmpty())
                    return

                for (i in 0 until adapterList.size) {
                    adapterList[i]?.removeTextureView(participant)
                }

            }
            return
        }
    }

    /**
     * Set new layout params for the grid view
     *
     * @param position current selected page
     * @param data the current data list
     * @param recyclerView the recycler view need to draw
     */
    private fun setParamsForGridView(
        position: Int,
        data: List<Participant>,
        recyclerView: CustomizedGridCallRecyclerView
    ) {
        val layoutParams = recyclerView.layoutParams as RecyclerView.LayoutParams
        when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                if (position == 0) {
                    if (data.size == 4) {
                        layoutParams.setMargins(maxWidth / 4, 0, maxWidth / 4, 0)
                    } else if (data.size > 4) {
                        layoutParams.setMargins(maxWidth / 8, 0, maxWidth / 8, 0)
                    }
                } else {
                    layoutParams.setMargins(maxWidth / 8, 0, maxWidth / 8, 0)
                }
            }
            else -> {
                layoutParams.setMargins(0, 0, 0, 0)
            }
        }
    }

    private fun setColumnWidth(
        position: Int,
        gridView: CustomizedGridCallRecyclerView,
        size: Int,
        orientation: Int
    ) {
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (position == 0) {
                gridView.setColumnWidth(
                    when (size) {
                        1 -> maxWidth
                        2 -> maxWidth
                        3 -> (maxWidth * 0.8).toInt()
                        else -> maxWidth / 2
                    }
                )
            } else {
                gridView.setColumnWidth(maxWidth / 2)
            }
        } else {
            // Landscape width
            if (position == 0) {
                gridView.setColumnWidth(
                    when (size) {
                        1, 2 -> maxWidth / 2
                        3 -> (maxWidth / 3)
                        else -> maxWidth / 4
                    }
                )
            } else {
                gridView.setColumnWidth(maxWidth / 4)
            }
        }
    }

    /**
     * Change the layout when the orientation is changing
     *
     * @param newOrientation the new orientation
     * @param widthPixels the new width
     * @param heightPixels the new height
     */
    fun updateOrientation(newOrientation: Int, widthPixels: Int, heightPixels: Int) {
        orientation = newOrientation
        maxWidth = widthPixels
        maxHeight = heightPixels
        notifyDataSetChanged()
    }

    companion object {
        private const val PARTICIPANTS_PER_PAGE = 6
    }
}