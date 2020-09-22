package mega.privacy.android.app.activities

import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import mega.privacy.android.app.R
import mega.privacy.android.app.adapters.GiphyAdapter
import mega.privacy.android.app.interfaces.GiphyEndPointsInterface
import mega.privacy.android.app.lollipop.PinActivityLollipop
import mega.privacy.android.app.objects.Data
import mega.privacy.android.app.objects.GiphyResponse
import mega.privacy.android.app.services.GiphyService
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.Util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GiphyActivity : PinActivityLollipop() {

    companion object {
        private const val NUM_COLUMNS_PORTRAIT = 2
        private const val NUM_COLUMNS_LANDSCAPE = 4
        private const val GIF_MARGIN = 4F
    }

    private var toolbar: Toolbar? = null
    private var gifList: RecyclerView? = null

    private var giphyAdapter: GiphyAdapter? = null

    private var giphyService: GiphyEndPointsInterface? = null

    private var screenOrientation = 0
    private var numColumns = 0
    private var screenGifWidth = 0

    private var previousRequest: Call<GiphyResponse>? = null
    private var trendingData: ArrayList<Data>? = null
    private var searchData = HashMap<String, ArrayList<Data>?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_giphy)
        window.statusBarColor = resources.getColor(R.color.dark_primary_color)

        toolbar = findViewById(R.id.giphy_toolbar)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_grey)
        toolbar?.title = getString(R.string.search_giphy_title)

        screenOrientation = if (isScreenInPortrait(this@GiphyActivity)) ORIENTATION_PORTRAIT else ORIENTATION_LANDSCAPE
        updateView()

        gifList = findViewById(R.id.giphy_list)
        gifList?.apply {
            setHasFixedSize(true)
            clipToPadding = false
            layoutManager = StaggeredGridLayoutManager(numColumns, RecyclerView.VERTICAL)
            itemAnimator = DefaultItemAnimator()
        }

        giphyService = GiphyService.buildService()
        requestTrendingData()
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
        numColumns = if (screenOrientation == ORIENTATION_PORTRAIT) NUM_COLUMNS_PORTRAIT else NUM_COLUMNS_LANDSCAPE


        screenGifWidth = (widthScreen / numColumns) - (px2dp(GIF_MARGIN, outMetrics) * numColumns)
        gifList?.layoutManager = StaggeredGridLayoutManager(numColumns, RecyclerView.VERTICAL)
        giphyAdapter?.notifyDataSetChanged()
    }

    /**
     * Requests trending data to the Giphy API.
     */
    private fun requestTrendingData() {
        if (trendingData == null) getAndSetData(giphyService?.getGiphyTrending(), true)
        else updateAdapter(trendingData)
    }

    /**
     * Requests search data to the Giphy API.
     *
     * @param query The search String.
     */
    private fun requestSearchData(query: String) {
        val searchValue = searchData[query]

        if (searchValue == null) {
            getAndSetData(giphyService?.getGiphySearch(query), false)
        } else {
            updateAdapter(searchValue)
        }
    }

    /**
     *  Launches a request to the Giphy API and manages the response when it finishes,
     *  showing the result on screen if everything was correct.
     *  If there is already a request in progress, cancels it to launch the new one.
     *
     * @param call  Indicates the type of the request. It can be Trending or Search.
     */
    private fun getAndSetData(call: Call<GiphyResponse>?, isTrendingData: Boolean) {
        if (previousRequest?.isCanceled == false) {
            previousRequest?.cancel()
        }

        call?.enqueue(object : Callback<GiphyResponse> {
            override fun onResponse(call: Call<GiphyResponse>, response: Response<GiphyResponse>) {
                if (response.isSuccessful) {
                    val gifData = response.body()?.data

                    if (isTrendingData) trendingData = gifData
                    else {
                        val url = call.request().url().url().toString()
                        searchData[url.split("q=").last()] = gifData
                    }

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
     * Updates the adapter with new data.
     */
    private fun updateAdapter(gifsData: ArrayList<Data>?) {
        if (giphyAdapter == null) {
            giphyAdapter = GiphyAdapter(gifsData, this@GiphyActivity)
            gifList?.adapter = giphyAdapter
        } else {
            giphyAdapter?.setGifs(gifsData)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_giphy, menu)

        val searchMenuItem = menu?.findItem(R.id.action_search)
        val searchView = searchMenuItem?.actionView as SearchView
        val line = searchView.findViewById(androidx.appcompat.R.id.search_plate) as View
        line.setBackgroundColor(resources.getColor(android.R.color.transparent))

        val searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text) as SearchView.SearchAutoComplete
        searchAutoComplete.setTextColor(resources.getColor(R.color.giphy_search_text))
        searchAutoComplete.hint = ""

        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
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
                if (!isTextEmpty(newText)) requestSearchData(newText.toString())
                return true
            }

        })

        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Gets the height of a GIF to display on screen from its real width, real height and width available on the screen.
     * The width available on the screen will depend on the dimensions of the current device and the current columns displayed.
     *
     * @param gifWidth  Real width of the GIF.
     * @param gifHeight Real height of the GIF.
     * @return The height to display on screen.
     */
    fun getScreenGifHeight(gifWidth: Int, gifHeight: Int): Int {
        if (gifWidth == gifHeight) {
            return screenGifWidth
        }

        val factor = screenGifWidth.toFloat() / gifWidth.toFloat()
        val screenGifHeight = gifHeight * factor

        return screenGifHeight.toInt()
    }
}