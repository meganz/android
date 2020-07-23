package mega.privacy.android.app.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.lollipop.PinActivityLollipop


class GiphyActivity : PinActivityLollipop() {

    private var toolbar: Toolbar? = null
    private var giphyList: RecyclerView? = null

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
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            onBackPressed()

        return super.onOptionsItemSelected(item)
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

        searchMenuItem.setOnActionExpandListener(object  : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                TODO("Not yet implemented")
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                TODO("Not yet implemented")
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                TODO("Not yet implemented")
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                TODO("Not yet implemented")
            }

        })

        return super.onCreateOptionsMenu(menu)
    }
}