package mega.privacy.android.app.fragments.managerFragments

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.databinding.FragmentTransfersBinding
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.adapters.RotatableAdapter
import mega.privacy.android.app.main.managerSections.RotatableFragment
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Util
import javax.inject.Inject

/**
 * The base transfer fragment
 */
@AndroidEntryPoint
open class TransfersBaseFragment : RotatableFragment() {

    /**
     * [TransfersManagement] injection
     */
    @Inject
    lateinit var transfersManagement: TransfersManagement

    /**
     * The empty image
     */
    protected lateinit var emptyImage: ImageView

    /**
     * The empty text
     */
    protected lateinit var emptyText: TextView

    private var getMoreQuotaView: RelativeLayout? = null

    /**
     * The recycler view
     */
    protected var listView: RecyclerView? = null

    /**
     * LayoutManager
     */
    protected lateinit var mLayoutManager: LinearLayoutManager

    /**
     * [ManagerActivity]
     */
    protected lateinit var managerActivity: ManagerActivity

    /**
     * [SimpleDividerItemDecoration]
     */
    protected lateinit var itemDecoration: SimpleDividerItemDecoration

    private lateinit var binding: FragmentTransfersBinding


    /**
     * Initial the view
     *
     * @param inflater LayoutInflater
     * @param container ViewGroup
     *
     * @return view
     */
    protected open fun initView(inflater: LayoutInflater, container: ViewGroup?): View {
        binding = FragmentTransfersBinding.inflate(inflater, container, false)

        itemDecoration = SimpleDividerItemDecoration(requireContext())

        mLayoutManager = LinearLayoutManager(requireContext())
        listView = binding.transfersListView.apply {
            addItemDecoration(itemDecoration)
            layoutManager = mLayoutManager
            setHasFixedSize(true)
            itemAnimator = null
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    updateElevation()
                }
            })
        }

        emptyImage = binding.transfersEmptyImage
        emptyText = binding.transfersEmptyText
        getMoreQuotaView = binding.layoutGetMoreQuotaView.getMoreQuotaView
        binding.layoutGetMoreQuotaView.getMoreQuotaUpgradeButton.setOnClickListener {
            (requireActivity() as ManagerActivity).navigateToUpgradeAccount()
        }

        setGetMoreQuotaViewVisibility()
        managerActivity.invalidateOptionsMenu()

        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        managerActivity = context as ManagerActivity
    }

    /**
     * Update elevation when scrolling if needed.
     */
    open fun updateElevation() = managerActivity.changeAppBarElevation(
        listView?.canScrollVertically(DEFAULT_SCROLL_DIRECTION) == true)


    /**
     * Shows an empty view if there are not transfers
     * and the list if there are.
     *
     * @param size  the size of the list of transfers
     */
    protected open fun setEmptyView(size: Int) {
        if (size == 0) {
            emptyImage.visibility = VISIBLE
            emptyText.visibility = VISIBLE
            listView?.visibility = GONE
            updateElevation()
        } else {
            emptyImage.visibility = GONE
            emptyText.visibility = GONE
            listView?.visibility = VISIBLE
        }
    }

    /**
     * Sets the visibility of the view "Get more quota".
     */
    fun setGetMoreQuotaViewVisibility() {
        getMoreQuotaView?.let {
            if (transfersManagement.isOnTransferOverQuota()) {
                it.visibility = VISIBLE
                if (Util.isDarkMode(requireContext())) {
                    getMoreQuotaView?.setBackgroundColor(ColorUtils.getColorForElevation(
                        requireContext(),
                        DARK_MODE_ELEVATION))
                } else {
                    getMoreQuotaView?.setBackgroundResource(R.drawable.white_layout_with_broder_shadow)
                }
            } else {
                getMoreQuotaView?.visibility = GONE
            }
        }
    }

    override fun getAdapter(): RotatableAdapter? = null

    override fun activateActionMode() {
    }

    override fun multipleItemClick(position: Int) {
    }

    override fun reselectUnHandledSingleItem(position: Int) {
    }

    override fun updateActionModeTitle() {
    }

    companion object {

        /**
         * The default value for scroll direction
         */
        const val DEFAULT_SCROLL_DIRECTION = -1
        private const val DARK_MODE_ELEVATION = 6F
    }
}