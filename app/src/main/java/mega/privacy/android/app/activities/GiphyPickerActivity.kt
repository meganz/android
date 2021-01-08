package mega.privacy.android.app.activities

import android.content.Intent
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.text.Spanned
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RelativeLayout
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import mega.privacy.android.app.R
import mega.privacy.android.app.adapters.GiphyAdapter
import mega.privacy.android.app.databinding.ActivityGiphyBinding
import mega.privacy.android.app.interfaces.GiphyEndPointsInterface
import mega.privacy.android.app.interfaces.GiphyInterface
import mega.privacy.android.app.interfaces.GiphyInterface.Companion.EMPTY_DOWN_SERVER
import mega.privacy.android.app.interfaces.GiphyInterface.Companion.EMPTY_SEARCH
import mega.privacy.android.app.interfaces.GiphyInterface.Companion.NON_EMPTY
import mega.privacy.android.app.lollipop.PinActivityLollipop
import mega.privacy.android.app.objects.Data
import mega.privacy.android.app.objects.GifData
import mega.privacy.android.app.objects.GiphyResponse
import mega.privacy.android.app.services.GiphyService
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_PICK_GIF
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.Util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GiphyPickerActivity : PinActivityLollipop(), GiphyInterface {

    companion object {
        private const val NUM_COLUMNS_PORTRAIT = 2
        private const val NUM_COLUMNS_LANDSCAPE = 4
        private const val GIF_MARGIN = 4F
        private const val DEFAULT_LIMIT = 25
        private const val EMPTY_IMAGE_MARGIN_TOP_PORTRAIT = 152F
        private const val EMPTY_TEXT_MARGIN_TOP_PORTRAIT = 12F
        private const val EMPTY_IMAGE_MARGIN_TOP_LANDSCAPE = 20F
        private const val EMPTY_TEXT_MARGIN_TOP_LANDSCAPE = 0

        const val GIF_DATA = "GIF_DATA"
    }

    private lateinit var binding: ActivityGiphyBinding

    private var giphyAdapter: GiphyAdapter? = null
    private var staggeredGridLayoutManager: StaggeredGridLayoutManager? = null

    private var giphyService: GiphyEndPointsInterface? = null

    private var screenOrientation = 0
    private var numColumns = 0
    private var screenGifWidth = 0

    private var previousRequest: Call<GiphyResponse>? = null
    private var trendingData: ArrayList<Data> = ArrayList()
    private var searchData = hashMapOf<String, ArrayList<Data>>()

    private var searchMenuItem: MenuItem? = null

    private var currentQuery: String? = null
    private var queryOffset = 0
    private var isEndOfList = false

    private lateinit var emptyText: Spanned

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGiphyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor =
            ContextCompat.getColor(applicationContext, R.color.dark_primary_color)

        setSupportActionBar(binding.giphyToolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_grey)
        binding.giphyToolbar.title = getString(R.string.search_giphy_title)
        binding.giphyToolbar.setOnClickListener { searchMenuItem?.expandActionView() }

        screenOrientation = resources.configuration.orientation
        updateView()

        binding.giphyList.apply {
            setHasFixedSize(true)
            staggeredGridLayoutManager =
                StaggeredGridLayoutManager(numColumns, RecyclerView.VERTICAL)
            layoutManager = staggeredGridLayoutManager
            itemAnimator = DefaultItemAnimator()
            addScrollBehaviour()
        }

        binding.giphyListView.visibility = GONE
        binding.emptyGiphyView.visibility = GONE

        var endOfList = getString(R.string.end_of_results_giphy)
        var emptyTextSearch = getString(R.string.empty_search_giphy)

        try {
            endOfList = endOfList.replace("[A]", "<font color=\'#999999\'>")
            endOfList = endOfList.replace("[/A]", "</font>")
            emptyTextSearch = emptyTextSearch.replace("[A]", "<font color=\'#000000\'>")
            emptyTextSearch = emptyTextSearch.replace("[/A]", "</font>")
        } catch (e: Exception) {
            logWarning("Exception formatting string", e)
        }

        binding.giphyEndList.text = HtmlCompat.fromHtml(endOfList, HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.giphyEndList.visibility = GONE
        emptyText = HtmlCompat.fromHtml(emptyTextSearch, HtmlCompat.FROM_HTML_MODE_LEGACY)

        giphyService = GiphyService.buildService()
        requestTrendingData(false)
    }

    private fun addScrollBehaviour() {
        binding.giphyList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val latestPosition = staggeredGridLayoutManager?.itemCount?.minus(1)
                val lastVisibleItems =
                    staggeredGridLayoutManager?.findLastVisibleItemPositions(IntArray(numColumns))
                        ?: return

                var latestIsVisible = false
                for (visibleItem in lastVisibleItems) {
                    if (latestPosition == visibleItem) {
                        latestIsVisible = true

                        if (!isEndOfList) {
                            if (searchMenuItem?.isActionViewExpanded == true && !isTextEmpty(
                                    currentQuery
                                )
                            ) requestSearchData(
                                currentQuery.toString(),
                                true
                            ) else requestTrendingData(true)
                        }

                        break
                    }
                }

                binding.giphyEndList.visibility =
                    if (latestIsVisible && isEndOfList) VISIBLE else GONE
            }
        })
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    override fun onDestroy() {
        cancelPreviousRequests()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            onBackPressed()

        return super.onOptionsItemSelected(item)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val newOrientation = newConfig.orientation
        if (newOrientation == screenOrientation) return

        val firstVisibleItems =
            staggeredGridLayoutManager?.findFirstCompletelyVisibleItemPositions(IntArray(numColumns))

        screenOrientation = newOrientation
        updateView()

        staggeredGridLayoutManager?.scrollToPosition(firstVisibleItems?.get(0) ?: 0)
    }

    /**
     * Updates the view after an orientation change or onCreate execution.
     */
    private fun updateView() {
        var widthScreen = 0

        var paramsEmptyImage = binding.emptyGiphyImage.layoutParams as RelativeLayout.LayoutParams
        var paramsEmptyText = binding.emptyGiphyText.layoutParams as RelativeLayout.LayoutParams

        if (screenOrientation == ORIENTATION_PORTRAIT) {
            numColumns = NUM_COLUMNS_PORTRAIT
            widthScreen = outMetrics.widthPixels

            paramsEmptyImage.topMargin =
                dp2px(EMPTY_IMAGE_MARGIN_TOP_PORTRAIT, resources.displayMetrics)
            paramsEmptyText.topMargin =
                dp2px(EMPTY_TEXT_MARGIN_TOP_PORTRAIT, resources.displayMetrics)
        } else if (screenOrientation == ORIENTATION_LANDSCAPE) {
            numColumns = NUM_COLUMNS_LANDSCAPE
            widthScreen = outMetrics.heightPixels

            paramsEmptyImage.topMargin =
                dp2px(EMPTY_IMAGE_MARGIN_TOP_LANDSCAPE, resources.displayMetrics)
            paramsEmptyText.topMargin = EMPTY_TEXT_MARGIN_TOP_LANDSCAPE
        }

        binding.emptyGiphyImage.layoutParams = paramsEmptyImage
        binding.emptyGiphyText.layoutParams = paramsEmptyText

        screenGifWidth = (widthScreen / numColumns) - (dp2px(GIF_MARGIN, outMetrics) * numColumns)
        staggeredGridLayoutManager = StaggeredGridLayoutManager(numColumns, RecyclerView.VERTICAL)
        binding.giphyList.layoutManager = staggeredGridLayoutManager

        giphyAdapter?.notifyDataSetChanged()
    }

    /**
     * Requests trending data to the Giphy API.
     *
     * @param scrolling True if the query corresponds to an scrolling action to the user,
     *                  false otherwise
     */
    private fun requestTrendingData(scrolling: Boolean) {
        if (scrolling && isEndOfList) return

        if (trendingData.isEmpty() || scrolling) {
            if (trendingData.isEmpty()) resetQueryValues()

            getAndSetData(giphyService?.getGiphyTrending(DEFAULT_LIMIT, queryOffset), null)
        } else {
            cancelPreviousRequests()
            queryOffset = trendingData.size
            updateAdapter(trendingData, true)
        }
    }

    /**
     * Requests search data to the Giphy API.
     *
     * @param query     The search String.
     * @param scrolling True if the query corresponds to an scrolling action to the user,
     *                  false otherwise
     */
    private fun requestSearchData(query: String, scrolling: Boolean) {
        if (scrolling && isEndOfList) return

        val searchValue = searchData[query]

        if (searchValue == null || scrolling) {
            if (searchValue == null) resetQueryValues()

            getAndSetData(giphyService?.getGiphySearch(query, DEFAULT_LIMIT, queryOffset), query)
        } else {
            cancelPreviousRequests()
            queryOffset = searchValue.size
            updateAdapter(searchValue, true)
        }
    }

    private fun resetQueryValues() {
        binding.giphyEndList.visibility = GONE
        queryOffset = 0
        isEndOfList = false
    }

    /**
     *  Launches a request to the Giphy API and manages the response when it finishes,
     *  showing the result on screen if everything was correct.
     *  If there is already a request in progress, cancels it to launch the new one.
     *
     * @param call  Indicates the type of the request. It can be Trending or Search.
     * @param query Search query if is a search request, null otherwise.
     */
    private fun getAndSetData(call: Call<GiphyResponse>?, query: String?) {
        cancelPreviousRequests()

        call?.enqueue(object : Callback<GiphyResponse> {
            override fun onResponse(call: Call<GiphyResponse>, response: Response<GiphyResponse>) {
                if (response.isSuccessful) {
                    isEndOfList = false

                    val gifData = response.body()?.data
                    if (gifData == null || gifData.isEmpty()) {
                        if (query == null || (searchData[query] != null && searchData[query]?.isNotEmpty() == true)) {
                            isEndOfList = true
                        } else {
                            searchData[query] = ArrayList()
                            updateAdapter(searchData[query], false)
                        }

                        return
                    }

                    if (gifData.size < DEFAULT_LIMIT) isEndOfList = true
                    else queryOffset += DEFAULT_LIMIT

                    if (query == null) {
                        trendingData.addAll(gifData)
                        updateAdapter(trendingData, false)
                    } else {
                        var searchDataValue = searchData[query]

                        if (searchDataValue == null) searchDataValue = gifData
                        else searchDataValue.addAll(gifData)

                        searchData[query] = searchDataValue
                        updateAdapter(searchData[query], false)
                    }
                } else {
                    logError("GiphyResponse failed.")
                    checkIfShouldShowDownServerState()
                }
            }

            override fun onFailure(call: Call<GiphyResponse>, t: Throwable) {
                logError("GiphyResponse failed: " + t.message)
                checkIfShouldShowDownServerState()
            }
        })

        previousRequest = call
    }

    /**
     *Shows down server state if the response of server failed and there is no GIF in the adapter.
     */
    private fun checkIfShouldShowDownServerState() {
        if (giphyAdapter == null || giphyAdapter?.itemCount == 0) {
            setEmptyState(EMPTY_DOWN_SERVER)
        }
    }

    /**
     * Cancels the previous request if exist and is in progress.
     */
    private fun cancelPreviousRequests() {
        if (previousRequest?.isCanceled == false) {
            previousRequest?.cancel()
        }
    }

    /**
     * Updates the adapter with new data.
     *
     * @param gifsData          Data to set in the adapter
     * @param isChangeOfType    True if the adapter has to change from trending to search data
     *                          or vice versa, false otherwise
     */
    private fun updateAdapter(gifsData: ArrayList<Data>?, isChangeOfType: Boolean) {
        when {
            giphyAdapter == null -> {
                giphyAdapter = GiphyAdapter(gifsData, this@GiphyPickerActivity)
                binding.giphyList.adapter = giphyAdapter
                binding.giphyListView.visibility = VISIBLE
            }
            isChangeOfType || gifsData?.size!! <= DEFAULT_LIMIT -> {
                giphyAdapter?.setGifs(gifsData)
                binding.giphyList.scrollToPosition(0)
            }
            else -> {
                giphyAdapter?.addGifs(gifsData)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_giphy, menu)

        searchMenuItem = menu?.findItem(R.id.action_search)
        val searchView = searchMenuItem?.actionView as SearchView
        searchView.maxWidth = Int.MAX_VALUE

        val line = searchView.findViewById(androidx.appcompat.R.id.search_plate) as View
        line.setBackgroundColor(
            ContextCompat.getColor(
                applicationContext,
                android.R.color.transparent
            )
        )

        val searchAutoComplete =
            searchView.findViewById(androidx.appcompat.R.id.search_src_text) as SearchView.SearchAutoComplete
        searchAutoComplete.setTextColor(
            ContextCompat.getColor(
                applicationContext,
                R.color.giphy_search_text
            )
        )
        searchAutoComplete.hint = getString(R.string.search_giphy_title)

        searchMenuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                requestTrendingData(false)
                return true
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                hideKeyboard(this@GiphyPickerActivity)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText
                if (isTextEmpty(newText)) {
                    requestTrendingData(false)
                } else {
                    requestSearchData(newText.toString(), false)
                }

                return true
            }

        })

        return super.onCreateOptionsMenu(menu)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_PICK_GIF) {
            val gifData = data?.getParcelableExtra(GIF_DATA) as GifData
            setResult(RESULT_OK, Intent().putExtra(GIF_DATA, gifData))
            finish()
        }
    }

    override fun openGifViewer(gifData: GifData?) {
        startActivityForResult(
            Intent(this@GiphyPickerActivity, GiphyViewerActivity::class.java)
                .putExtra(GIF_DATA, gifData),
            REQUEST_CODE_PICK_GIF
        )
    }

    override fun setEmptyState(emptyState: Int) {
        when (emptyState) {
            NON_EMPTY -> {
                binding.emptyGiphyView.visibility = GONE
                binding.giphyListView.visibility = VISIBLE
            }
            EMPTY_SEARCH -> {
                binding.emptyGiphyView.visibility = VISIBLE
                binding.emptyGiphyText.text = emptyText
                binding.giphyListView.visibility = GONE
            }
            EMPTY_DOWN_SERVER -> {
                binding.emptyGiphyView.visibility = VISIBLE
                binding.emptyGiphyText.text = StringResourcesUtils.getString(R.string.server_down_giphy)
                binding.giphyListView.visibility = GONE
            }
        }
    }

    override fun getScreenGifHeight(gifWidth: Int, gifHeight: Int): Int {
        if (gifWidth == gifHeight) {
            return screenGifWidth
        }

        val factor = screenGifWidth.toFloat() / gifWidth.toFloat()

        return (gifHeight * factor).toInt()
    }
}