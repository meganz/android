package mega.privacy.android.app.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.GiphyService
import mega.privacy.android.app.lollipop.PinActivityLollipop
import mega.privacy.android.app.objects.GiphyResponse
import mega.privacy.android.app.utils.LogUtil.logError
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GiphyActivity : PinActivityLollipop() {
    val TEST_URL = "https://giphy-sandbox3.developers.mega.co.nz/"
    val BASE_URL = "https://giphy.mega.nz/"

    private var toolbar: Toolbar? = null
    private var giphyList: RecyclerView? = null

    private var giphyService: GiphyService? = null

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

        giphyList = findViewById(R.id.giphy_list)

        val retrofit = Retrofit.Builder()
                .baseUrl(TEST_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        giphyService = retrofit.create(GiphyService::class.java)
        getRandomData()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            onBackPressed()

        return super.onOptionsItemSelected(item)
    }

    private fun getRandomData() {
        getAndSetData(giphyService?.getGiphyRandom())
    }

    private fun getSearchData(query: String? = null) {
        getAndSetData(giphyService?.getGiphySearch(query))
    }

    private fun getAndSetData(call: Call<GiphyResponse>?) {
        call?.enqueue(object : Callback<GiphyResponse> {
            override fun onResponse(call: Call<GiphyResponse>, response: Response<GiphyResponse>) {
                if (response.code() == 200) {
                    //Set data
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
                TODO("Not yet implemented")
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                TODO("Not yet implemented")
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                getSearchData(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                TODO("Not yet implemented")
            }

        })

        return super.onCreateOptionsMenu(menu)
    }
}