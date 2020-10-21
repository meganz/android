package mega.privacy.android.app.activities

import android.content.Intent
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import mega.privacy.android.app.R
import mega.privacy.android.app.adapters.GiphyAdapter
import mega.privacy.android.app.databinding.ActivityGiphyBinding
import mega.privacy.android.app.interfaces.GiphyEndPointsInterface
import mega.privacy.android.app.interfaces.GiphyInterface
import mega.privacy.android.app.lollipop.PinActivityLollipop
import mega.privacy.android.app.objects.Data
import mega.privacy.android.app.objects.GifData
import mega.privacy.android.app.objects.GiphyResponse
import mega.privacy.android.app.services.GiphyService
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_PICK_GIF
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.Util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GiphyActivity : PinActivityLollipop(), GiphyInterface {

    companion object {
        private const val NUM_COLUMNS_PORTRAIT = 2
        private const val NUM_COLUMNS_LANDSCAPE = 4
        private const val GIF_MARGIN = 4F

        const val GIF_DATA = "GIF_DATA"
    }

    private lateinit var binding: ActivityGiphyBinding

    private var giphyAdapter: GiphyAdapter? = null

    private var giphyService: GiphyEndPointsInterface? = null

    private var screenOrientation = 0
    private var numColumns = 0
    private var screenGifWidth = 0

    private var previousRequest: Call<GiphyResponse>? = null
    private var trendingData: ArrayList<Data>? = null
    private var searchData = HashMap<String, ArrayList<Data>?>()

    private var searchMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGiphyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = resources.getColor(R.color.dark_primary_color)

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
            layoutManager = StaggeredGridLayoutManager(numColumns, RecyclerView.VERTICAL)
            itemAnimator = DefaultItemAnimator()
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

        binding.giphyEndList.text = getSpannedHtmlText(endOfList)
        binding.emptyGiphyText.text = getSpannedHtmlText(emptyTextSearch)

        giphyService = GiphyService.buildService()
        requestTrendingData()
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

        screenOrientation = newOrientation
        updateView()
    }

    /**
     * Updates the view after an orientation change or onCreate execution.
     */
    private fun updateView() {
        var widthScreen = 0

        if (screenOrientation == ORIENTATION_PORTRAIT) {
            numColumns = NUM_COLUMNS_PORTRAIT
            widthScreen = outMetrics.widthPixels
        } else if (screenOrientation == ORIENTATION_LANDSCAPE) {
            numColumns = NUM_COLUMNS_LANDSCAPE
            widthScreen = outMetrics.heightPixels
        }

        screenGifWidth = (widthScreen / numColumns) - (px2dp(GIF_MARGIN, outMetrics) * numColumns)
        binding.giphyList.layoutManager =
            StaggeredGridLayoutManager(numColumns, RecyclerView.VERTICAL)
        giphyAdapter?.notifyDataSetChanged()
    }

    /**
     * Requests trending data to the Giphy API.
     */
    private fun requestTrendingData() {
        if (trendingData == null) getAndSetData(giphyService?.getGiphyTrending(), null)
        else {
            cancelPreviousRequests()
            updateAdapter(trendingData)
        }
    }

    /**
     * Requests search data to the Giphy API.
     *
     * @param query The search String.
     */
    private fun requestSearchData(query: String) {
        val searchValue = searchData[query]

        if (searchValue == null) {
            getAndSetData(giphyService?.getGiphySearch(query), query)
        } else {
            cancelPreviousRequests()
            updateAdapter(searchValue)
        }
    }

    /**
     *  Launches a request to the Giphy API and manages the response when it finishes,
     *  showing the result on screen if everything was correct.
     *  If there is already a request in progress, cancels it to launch the new one.
     *
     * @param call              Indicates the type of the request. It can be Trending or Search.
     * @param query             Search query if is a search request, null otherwise.
     */
    private fun getAndSetData(call: Call<GiphyResponse>?, query: String?) {
        cancelPreviousRequests()

        call?.enqueue(object : Callback<GiphyResponse> {
            override fun onResponse(call: Call<GiphyResponse>, response: Response<GiphyResponse>) {
                if (response.isSuccessful) {
                    val gifData = response.body()?.data

                    if (query == null) trendingData = gifData else searchData[query] = gifData

                    updateAdapter(gifData)
                } else {
                    logError("GiphyResponse failed.")
                }
            }

            override fun onFailure(call: Call<GiphyResponse>, t: Throwable) {
                logError("GiphyResponse failed: " + t.message)
            }
        })

        previousRequest = call
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
     */
    private fun updateAdapter(gifsData: ArrayList<Data>?) {
        if (giphyAdapter == null) {
            giphyAdapter = GiphyAdapter(gifsData, this@GiphyActivity)
            binding.giphyList.adapter = giphyAdapter
            binding.giphyListView.visibility = VISIBLE
        } else {
            giphyAdapter?.setGifs(gifsData)
        }

        binding.giphyList.scrollToPosition(0)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_giphy, menu)

        searchMenuItem = menu?.findItem(R.id.action_search)
        val searchView = searchMenuItem?.actionView as SearchView
        searchView.maxWidth = Int.MAX_VALUE

        val line = searchView.findViewById(androidx.appcompat.R.id.search_plate) as View
        line.setBackgroundColor(resources.getColor(android.R.color.transparent))

        val searchAutoComplete =
            searchView.findViewById(androidx.appcompat.R.id.search_src_text) as SearchView.SearchAutoComplete
        searchAutoComplete.setTextColor(resources.getColor(R.color.giphy_search_text))
        searchAutoComplete.hint = getString(R.string.search_giphy_title)

        searchMenuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                requestTrendingData()
                return true
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                hideKeyboard(this@GiphyActivity)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (isTextEmpty(newText)) {
                    requestTrendingData()
                } else {
                    requestSearchData(newText.toString())
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
            Intent(this@GiphyActivity, GiphyViewerActivity::class.java)
                .putExtra(GIF_DATA, gifData),
            REQUEST_CODE_PICK_GIF
        )
    }

    override fun setEmptyState(emptyState: Boolean) {
        if (emptyState) {
            binding.emptyGiphyView.visibility = VISIBLE
            binding.giphyListView.visibility = GONE
        } else {
            binding.emptyGiphyView.visibility = GONE
            binding.giphyListView.visibility = VISIBLE
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