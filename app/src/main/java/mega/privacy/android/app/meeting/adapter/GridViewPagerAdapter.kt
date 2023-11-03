package mega.privacy.android.app.meeting.adapter

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.get
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import mega.privacy.android.app.R
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.utils.Util.getCurrentOrientation
import timber.log.Timber

class GridViewPagerAdapter(
    var data: List<List<Participant>>,
    private val inMeetingViewModel: InMeetingViewModel,
    private var maxWidth: Int,
    private var maxHeight: Int,
    private val onPageClickedCallback: () -> Unit,
) : RecyclerView.Adapter<GridViewPagerAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

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
        payloads: MutableList<Any>,
    ) {
        Timber.d("Bind view holder position $position")
        recyclerView = holder.itemView.findViewById(R.id.grid_view)

        val participantsForPage = data[position]

        participantsForPage.let {
            recyclerView?.let {
                it.apply {
                    adapter = null
                    itemAnimator = DefaultItemAnimator()
                    setOnTouchCallback {
                        onPageClickedCallback.invoke()
                    }
                    setHasFixedSize(true)
                    setParamsForGridView(position, participantsForPage, this)
                    setColumnWidth(position, this, participantsForPage.size)
                }
            }

            val adapter = VideoGridViewAdapter(
                inMeetingViewModel,
                recyclerView!!,
                maxWidth,
                maxHeight,
                position,
                onPageClickedCallback
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

    /**
     * Method for updating participants per page
     *
     * @param newData Updated list of participants on each page
     */
    @SuppressLint("NotifyDataSetChanged")
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
    @SuppressLint("NotifyDataSetChanged")
    fun participantAdded(
        previousList: List<List<Participant>>,
        currentList: List<List<Participant>>,
        position: Int,
    ) {
        val lastPageNum = currentList.size - 1
        var isANewPage = false
        if (currentList.size - previousList.size == 1) {
            isANewPage = true
        }

        if (isANewPage) {
            Timber.d("A new page is needed: lastPageNum $lastPageNum")
            notifyItemInserted(lastPageNum)
            return
        }

        val pageWithChange = position / PARTICIPANTS_PER_PAGE
        Timber.d("Update the currentPage : $pageWithChange")
        if (adapterList.isNotEmpty() && adapterList.size > pageWithChange) {
            val participantsInPage = currentList[pageWithChange]
            adapterList[pageWithChange]?.let {
                it.submitList(participantsInPage) {
                    when (pageWithChange) {
                        0 -> {
                            when {
                                it.currentList.size <= 3 -> {
                                    Timber.d("Update only the adapter in the pager")
                                    if (isLandscape()) {
                                        notifyItemChanged(pageWithChange)
                                    } else {
                                        it.notifyDataSetChanged()
                                    }
                                }
                                it.currentList.size == 4 -> {
                                    Timber.d("Update the current page, as the number of columns must be updated")
                                    notifyItemChanged(pageWithChange)
                                }
                                it.currentList.size == 5 -> {
                                    Timber.d("Update position only")
                                    if (isLandscape()) {
                                        notifyItemChanged(pageWithChange)
                                    } else {
                                        it.notifyItemInserted(position)
                                    }
                                }
                                else -> {
                                    Timber.d("update the position and the previous position")
                                    it.notifyItemInserted(position)
                                    if (isLandscape()) {
                                        it.notifyItemRangeChanged(position - 2, it.currentList.size)
                                    } else {
                                        it.notifyItemRangeChanged(position - 1, it.currentList.size)
                                    }

                                }
                            }
                        }
                        else -> {
                            Timber.d("Update position only")
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
        position: Int,
    ) {
        val lastPageNum = previousList.size - 1
        var removePage = false
        if (currentList.size - previousList.size == -1) {
            removePage = true
        }

        val pageWithChange = position / PARTICIPANTS_PER_PAGE
        Timber.d("The current page is $pageWithChange. The last page is $lastPageNum")

        if (removePage) {
            Timber.d("This page should be deleted $pageWithChange")
            if (adapterList.isNotEmpty() && adapterList.size > lastPageNum) {
                adapterList[lastPageNum]?.submitList(null)
                notifyItemRemoved(lastPageNum)
            }

            if (pageWithChange != lastPageNum) {
                Timber.d("Checking pages to update")
                checkPagesToUpdate(position, pageWithChange, lastPageNum)
            }
        } else if (pageWithChange == (currentList.size - 1)) {
            Timber.d("There is only the page with the change")
            updatePageWithChange(pageWithChange, position)
        } else {
            Timber.d("Checking pages to update")
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
    @SuppressLint("NotifyDataSetChanged")
    private fun checkPagesToUpdate(position: Int, numPage: Int, lastPage: Int) {
        Timber.d("Checking the rest of the pages to be updated ... ")
        for (i in numPage until lastPage + 1) {
            if (i == numPage) {
                Timber.d("Update the page with the participant removed")
                updatePageWithChange(numPage, position)
            } else if (data.isNotEmpty() && data.size > i) {
                val participantsForPage = data[i]
                if (adapterList.isNotEmpty() && adapterList.size > i) {
                    adapterList[i]?.let {
                        Timber.d("Completely update the page $i")
                        it.submitList(participantsForPage) {
                            it.notifyDataSetChanged()
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
    @SuppressLint("NotifyDataSetChanged")
    private fun updatePageWithChange(pageWithChange: Int, position: Int) {
        val participantsForPage = data[pageWithChange]
        adapterList[pageWithChange]?.let {
            it.submitList(participantsForPage) {
                when (pageWithChange) {
                    0 -> {
                        when {
                            it.currentList.size <= 2 -> {
                                Timber.d("Update only the adapter in the pager")
                                if (isLandscape()) {
                                    notifyItemChanged(pageWithChange)
                                } else {
                                    it.notifyDataSetChanged()
                                }
                            }
                            it.currentList.size == 3 -> {
                                Timber.d("Update the current page, as the number of columns must be updated.")
                                notifyItemChanged(pageWithChange)
                            }
                            it.currentList.size == 4 && isLandscape() -> {
                                notifyItemChanged(pageWithChange)
                            }
                            else -> {
                                when (position) {
                                    0 -> {
                                        Timber.d("First page, update the current page")
                                        notifyItemChanged(pageWithChange)
                                    }
                                    else -> {
                                        Timber.d("First page, update position only")
                                        var rangeToUpdate = position

                                        if (participantsForPage.size <= 5) {
                                            if (position == 5) {
                                                rangeToUpdate = if (isLandscape()) {
                                                    position - 2
                                                } else {
                                                    position - 1
                                                }
                                            } else if (position == 4 && isLandscape()) {
                                                rangeToUpdate = position - 1
                                            }
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
                        Timber.d("Another page, update the item removed")
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

    /**
     * Update participant name
     *
     * @param participant the target participant
     * @param currentPage the current page number
     * @param pager the ViewPager2
     * @param typeChange the type of change, name or avatar
     */
    fun updateParticipantNameOrAvatar(
        participant: Participant,
        currentPage: Int,
        pager: ViewPager2,
        typeChange: Int,
    ) {
        val holder = getHolder(currentPage, pager)
        holder?.let {
            adapterList.let {
                if (adapterList.isEmpty())
                    return

                for (i in 0 until adapterList.size) {
                    adapterList[i]?.updateParticipantNameOrAvatar(participant, typeChange)
                }

            }
            return
        }

        notifyItemChanged(currentPage)
    }

    /**
     * Method to control when the video listener should be added or removed.
     *
     * @param participant The participant whose listener of the video is to be added or deleted
     * @param shouldAddListener True, should add the listener. False, should remove the listener
     * @param isHiRes True, if is High resolution. False, if is Low resolution
     * @param currentPage the current page number
     * @param pager the ViewPager2
     */
    fun updateListener(
        participant: Participant, shouldAddListener: Boolean, isHiRes: Boolean, currentPage: Int,
        pager: ViewPager2,
    ) {
        val holder = getHolder(currentPage, pager)
        holder?.let {
            adapterList.let {
                for (i in 0 until adapterList.size) {
                    adapterList[i]?.updateListener(participant, shouldAddListener, isHiRes)
                }
            }
            return
        }

        notifyItemChanged(currentPage)
    }

    /**
     * Update participant when call is on hold
     *
     * @param participant
     * @param isOnHold True, it it's. False, otherwise.
     * @param currentPage the current page number
     * @param pager the ViewPager2
     */
    fun updateCallOnHold(
        participant: Participant,
        isOnHold: Boolean,
        currentPage: Int,
        pager: ViewPager2,
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

    /**
     * Update participant on hold session
     *
     * @param participant
     * @param isOnHold True, it it's. False, otherwise.
     * @param currentPage the current page number
     * @param pager the ViewPager2
     */
    fun updateSessionOnHold(
        participant: Participant,
        isOnHold: Boolean,
        currentPage: Int,
        pager: ViewPager2,
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

    /**
     * Method to activate or stop a participant's video whether it is visible or not
     *
     * @param shouldActivate True, if video should be activated. False, otherwise.
     * @param participant
     * @param currentPage the current page number
     * @param pager the ViewPager2
     */
    fun updateVideoWhenScroll(
        shouldActivate: Boolean,
        participant: Participant,
        currentPage: Int,
        pager: ViewPager2,
    ) {
        val holder = getHolder(currentPage, pager)
        holder?.let {
            adapterList.let {
                if (adapterList.isEmpty())
                    return

                for (i in 0 until adapterList.size) {
                    adapterList[i]?.updateVideoWhenScroll(shouldActivate, participant)
                }

            }
            return
        }

        notifyItemChanged(currentPage)
    }

    /**
     * Update participant audio or video flags
     *
     * @param typeChange TYPE_VIDEO or TYPE_AUDIO
     * @param participant
     * @param currentPage the current page number
     * @param pager the ViewPager2
     */
    fun updateParticipantAudioVideo(
        typeChange: Int,
        participant: Participant,
        currentPage: Int,
        pager: ViewPager2,
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
     *
     * @param participant
     * @param currentPage the current page number
     * @param pager the ViewPager2
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
        recyclerView: CustomizedGridCallRecyclerView,
    ) {
        val layoutParams = recyclerView.layoutParams as RecyclerView.LayoutParams
        var leftRightMargin = 0

        if (isLandscape()) {
            if (position == 0 && data.size < 4)
                leftRightMargin = 0

            if (position > 0 || (position == 0 && data.size > 4))
                leftRightMargin = maxWidth / 8

            if (position == 0 && data.size == 4)
                leftRightMargin = maxWidth / 4
        }

        layoutParams.setMargins(leftRightMargin, 0, leftRightMargin, 0)
    }

    /**
     * Method to get the correct column width
     *
     * @param position Position of the participant
     * @param gridView The Recycler view
     * @param size Number of participants on a given page
     */
    private fun setColumnWidth(
        position: Int,
        gridView: CustomizedGridCallRecyclerView,
        size: Int,
    ) {
        if (getCurrentOrientation() == Configuration.ORIENTATION_PORTRAIT) {
            if (position == 0) {
                gridView.setColumnWidth(
                    when (size) {
                        1, 2 -> maxWidth
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
     * @param widthPixels the new width
     * @param heightPixels the new height
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updateOrientation(widthPixels: Int, heightPixels: Int) {
        maxWidth = widthPixels
        maxHeight = heightPixels
        notifyDataSetChanged()
    }

    /**
     * Determine if current orientation is landscape
     */
    fun isLandscape() =
        getCurrentOrientation() == Configuration.ORIENTATION_LANDSCAPE

    companion object {
        private const val PARTICIPANTS_PER_PAGE = 6
    }
}