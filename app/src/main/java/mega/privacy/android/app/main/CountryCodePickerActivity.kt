package mega.privacy.android.app.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.databinding.ActivityCountryCodePickerBinding
import mega.privacy.android.app.main.adapters.CountryListAdapter
import mega.privacy.android.app.utils.ColorUtils.tintIcon
import mega.privacy.android.app.utils.Util
import timber.log.Timber
import java.util.Locale

/**
 * Country Code Picker Activity
 */
class CountryCodePickerActivity : PasscodeActivity() {
    private lateinit var binding: ActivityCountryCodePickerBinding
    private lateinit var adapter: CountryListAdapter
    private var countries: List<Country>? = null
    private val selectedCountries = mutableListOf<Country>()
    private var searchInput: String? = null
    private var searchAutoComplete: SearchView.SearchAutoComplete? = null
    private val receivedCountryCodes by lazy(LazyThreadSafetyMode.NONE) {
        intent.extras?.getStringArrayList("country_code")
    }
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (psaWebBrowser != null && psaWebBrowser?.consumeBack() == true) return
            finish()
        }
    }

    /**
     * OnCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountryCodePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        countries = loadCountries()

        supportActionBar?.apply {
            title = getString(R.string.action_search_country)
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(
                tintIcon(
                    this@CountryCodePickerActivity,
                    R.drawable.ic_arrow_back_white
                )
            )
        }

        adapter = CountryListAdapter(countries).apply {
            setCallback { country ->
                onCountrySelected(country)
            }
        }

        with(binding) {
            countryList.layoutManager = LinearLayoutManager(this@CountryCodePickerActivity)
            countryList.adapter = adapter
            countryList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    Util.changeActionBarElevation(
                        this@CountryCodePickerActivity,
                        binding.appBarLayout,
                        recyclerView.canScrollVertically(-1)
                    )
                }
            })
        }

        if (savedInstanceState != null) {
            searchInput = savedInstanceState.getString(SAVED_QUERY_STRING)
        }
    }

    /**
     * onCreateOptionsMenu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_country_picker, menu)
        val searchMenuItem = menu.findItem(R.id.action_search)
        val searchView = searchMenuItem.actionView as? SearchView
        searchView?.setIconifiedByDefault(true)
        searchView?.maxWidth = Int.MAX_VALUE
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                Timber.d("onQueryTextSubmit: %s", query)
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                val view = View(this@CountryCodePickerActivity)
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                search(newText)
                return true
            }
        })
        searchView?.findViewById<View>(androidx.appcompat.R.id.search_plate)?.apply {
            setBackgroundColor(
                ContextCompat.getColor(
                    this@CountryCodePickerActivity,
                    android.R.color.transparent
                )
            )
        }

        searchAutoComplete = searchView?.findViewById(androidx.appcompat.R.id.search_src_text)
        searchAutoComplete?.hint = getString(R.string.hint_action_search)

        if (searchInput != null) {
            searchMenuItem.expandActionView()
            searchView?.setQuery(searchInput, true)
            search(searchInput!!)
        }

        return super.onCreateOptionsMenu(menu)
    }

    private fun search(query: String) {
        selectedCountries.clear()
        val lowerCaseQuery = query.lowercase(Locale.getDefault())
        val filteredCountries = countries.orEmpty().filter { country ->
            country.name.lowercase(Locale.getDefault())
                .contains(lowerCaseQuery)
        }.partition { country ->
            country.name.lowercase(Locale.getDefault())
                .startsWith(lowerCaseQuery)
        }

        selectedCountries.addAll(filteredCountries.first.sortedBy { it.name })
        selectedCountries.addAll(filteredCountries.second.sortedBy { it.name })
        adapter.refresh(selectedCountries)
    }

    private fun loadCountries(): List<Country> {
        //To decode received country codes from SMSVerificationActivity
        // Each string in ArrayList is "DO:1809,1829,1849,"
        val countryCodeList = ArrayList<Country>()
        if (receivedCountryCodes != null) {
            for (countryString in receivedCountryCodes.orEmpty()) {
                val splitIndex = countryString.indexOf(":")
                val countryCode = countryString.substring(0, countryString.indexOf(":"))
                val dialCodes = countryString.substring(splitIndex + 1).split(",".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                for (dialCode in dialCodes) {
                    val locale = Locale("", countryCode)
                    val countryName = locale.displayName
                    countryCodeList.add(Country(countryName, "+$dialCode", countryCode))
                }
            }
        }
        countryCodeList.sortWith(Comparator.comparing { obj: Country -> obj.name })
        return countryCodeList
    }

    private fun onCountrySelected(country: Country) {
        val intent = Intent().apply {
            putExtra(COUNTRY_NAME, country.name)
            putExtra(DIAL_CODE, country.code)
            putExtra(COUNTRY_CODE, country.countryCode)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    /**
     * onOptionsItemSelected
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Country Class
     * @param name as country name
     * @param code as code
     * @param countryCode as country code
     */
    class Country(
        val name: String = "",
        val code: String = "",
        val countryCode: String = "",
    ) {
        override fun toString(): String {
            return "Country{name='$name', countryCode='$countryCode', code='$code'}"
        }
    }

    /**
     * onSaveInstanceState
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val query = searchAutoComplete?.text.toString()
        if (searchAutoComplete?.hasFocus() == true || query.isNotEmpty()) {
            outState.putString(SAVED_QUERY_STRING, query)
        }
    }

    companion object {
        private const val SAVED_QUERY_STRING = "SAVED_QUERY_STRING"

        /**
         * Country Name Key
         */
        const val COUNTRY_NAME = "name"

        /**
         * Dial Code Key
         */

        const val DIAL_CODE = "dial_code"

        /**
         * Country Code Key
         */
        const val COUNTRY_CODE = "code"
    }
}