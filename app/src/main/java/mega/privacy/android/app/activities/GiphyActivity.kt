package mega.privacy.android.app.activities

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import mega.privacy.android.app.R
import mega.privacy.android.app.adapters.GiphyAdapter
import mega.privacy.android.app.interfaces.GiphyEndPointsInterface
import mega.privacy.android.app.lollipop.PinActivityLollipop
import mega.privacy.android.app.objects.GiphyResponse
import mega.privacy.android.app.objects.GiphySingleResponse
import mega.privacy.android.app.services.GiphyService
import mega.privacy.android.app.utils.FrescoUtils.loadGif
import mega.privacy.android.app.utils.LogUtil.logError
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GiphyActivity : PinActivityLollipop() {

    private var toolbar: Toolbar? = null
    private var gifList: RecyclerView? = null
    private var gifImgDisplay: SimpleDraweeView? = null

    private var giphyAdapter: GiphyAdapter? = null

    private var giphyService: GiphyEndPointsInterface? = null

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

        gifList = findViewById(R.id.giphy_list)
        gifList?.apply {
            setHasFixedSize(true)
            clipToPadding = false
            layoutManager = LinearLayoutManager(this@GiphyActivity)
            itemAnimator = DefaultItemAnimator()
        }

        gifImgDisplay = findViewById(R.id.gif_view)

        giphyService = GiphyService.buildService()
        requestRandomData()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            onBackPressed()

        return super.onOptionsItemSelected(item)
    }

    private fun requestRandomData() {
        getAndSetSingleData(giphyService?.getGiphyRandom())
    }

    private fun requestSearchData(query: String? = null) {
        getAndSetData(giphyService?.getGiphySearch(query))
    }

    private fun getAndSetSingleData(call: Call<GiphySingleResponse>?) {
        call?.enqueue(object : Callback<GiphySingleResponse> {
            override fun onResponse(call: Call<GiphySingleResponse>, response: Response<GiphySingleResponse>) {
                if (response.isSuccessful) {
                    gifImgDisplay!!.visibility = View.VISIBLE
                    gifList!!.visibility = View.GONE

                    val url = response.body()!!.data!!.images!!.fixed_height?.webp
                    loadGif(gifImgDisplay, Uri.parse(url))
                }
            }

            override fun onFailure(call: Call<GiphySingleResponse>, t: Throwable) {
                logError("GiphyResponse failed" + t.message)
            }
        })
    }

    private fun getAndSetData(call: Call<GiphyResponse>?) {
        call?.enqueue(object : Callback<GiphyResponse> {
            override fun onResponse(call: Call<GiphyResponse>, response: Response<GiphyResponse>) {
                if (response.isSuccessful) {
                    gifImgDisplay!!.visibility = View.GONE
                    gifList!!.visibility = View.VISIBLE

                    giphyAdapter = GiphyAdapter(response.body()!!.data)
                    gifList!!.adapter = giphyAdapter
                }
            }

            override fun onFailure(call: Call<GiphyResponse>, t: Throwable) {
                logError("GiphyResponse failed" + t.message)
            }
        })
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
                return true
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                requestSearchData(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }

        })

        return super.onCreateOptionsMenu(menu)
    }
}