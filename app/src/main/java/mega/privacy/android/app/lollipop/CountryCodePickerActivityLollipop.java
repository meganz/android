package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.adapters.CountryListAdapter;
import mega.privacy.android.app.utils.Util;

public class CountryCodePickerActivityLollipop extends PinActivityLollipop implements CountryListAdapter.CountrySelectedCallback {
    private final String SAVED_QUERY_STRING = "SAVED_QUERY_STRING";
    private static List<Country> countries;

    private List<Country> selectedCountries = new ArrayList<>();

    private AppBarLayout abL;

    private RecyclerView countryList;

    private CountryListAdapter adapter;

    private ActionBar actionBar;

    private DisplayMetrics outMetrics;

    private Toolbar toolbar;
    private String searchInput;
    private SearchView.SearchAutoComplete searchAutoComplete;
    public static final String COUNTRY_NAME = "name";
    public static final String DIAL_CODE = "dial_code";
    public static final String COUNTRY_CODE = "code";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contry_code_picker);
        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        changeStatusBarColor();
        if (countries == null) {
            countries = loadCountries();
        }

        abL = findViewById(R.id.app_bar_layout);
        countryList = findViewById(R.id.country_list);
        countryList.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(recyclerView.canScrollVertically(-1)) {
                    changeActionBarElevation(true);
                } else {
                    changeActionBarElevation(false);
                }
            }
        });
        adapter = new CountryListAdapter(countries);
        adapter.setCallback(this);
        countryList.setAdapter(adapter);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        countryList.setLayoutManager(manager);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.action_search_country));
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(Util.mutateIcon(this,R.drawable.ic_arrow_back_white,R.color.black));
    
        if(savedInstanceState != null){
            searchInput = savedInstanceState.getString(SAVED_QUERY_STRING);
        }
    }

    private void changeStatusBarColor() {
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(ContextCompat.getColor(this,R.color.status_bar_search));
    }

    public void changeActionBarElevation(boolean withElevation) {
        if(withElevation) {
            final float elevation = Util.px2dp(4,outMetrics);
            abL.postDelayed(new Runnable() {

                @Override
                public void run() {
                    abL.setElevation(elevation);
                }
            },100);
        } else {
            abL.postDelayed(new Runnable() {

                @Override
                public void run() {
                    abL.setElevation(0);
                }
            },100);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_country_picker,menu);
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        searchMenuItem.setIcon(Util.mutateIcon(this,R.drawable.ic_menu_search,R.color.black));
        SearchView searchView = (SearchView)searchMenuItem.getActionView();
        if (searchView != null) {
            searchView.setIconifiedByDefault(true);
        }

        View v = searchView.findViewById(android.support.v7.appcompat.R.id.search_plate);
        searchAutoComplete = searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchAutoComplete.setTextColor(ContextCompat.getColor(this,R.color.black));
        searchAutoComplete.setHintTextColor(ContextCompat.getColor(this,R.color.status_bar_login));
        searchAutoComplete.setHint(getString(R.string.hint_action_search));
        if (searchInput != null) {
            searchMenuItem.expandActionView();
            searchView.setQuery(searchInput,true);
            search(searchInput);
        }
        v.setBackgroundColor(ContextCompat.getColor(this,android.R.color.transparent));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                log("onQueryTextSubmit: " + query);
                InputMethodManager imm = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
                View view = getCurrentFocus();
                if (view == null) {
                    view = new View(CountryCodePickerActivityLollipop.this);
                }
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(),0);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                search(newText);
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    private void search(String query) {
        selectedCountries.clear();
        List<Country> l1 = new ArrayList<>();
        List<Country> l2 = new ArrayList<>();

        for (Country country : countries) {
            if (country.name.toLowerCase().contains(query.toLowerCase())) {
                if(country.name.toLowerCase().startsWith(query.toLowerCase())) {
                    l1.add(country);
                } else {
                    l2.add(country);
                }
            }
        }
        //the countries with name starts with the query string should be put first
        Collections.sort(l1,new Comparator<Country>() {

            @Override
            public int compare(Country o1,Country o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        Collections.sort(l2,new Comparator<Country>() {

            @Override
            public int compare(Country o1,Country o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        selectedCountries.addAll(l1);
        selectedCountries.addAll(l2);
        adapter.refresh(selectedCountries);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public List<Country> loadCountries() {
        List<Country> countries = new ArrayList<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(getResources().getAssets().open("countries.json")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            JSONArray ja = new JSONArray(sb.toString());
            for (int i = 0;i < ja.length();i++) {
                JSONObject jo = ja.getJSONObject(i);
                countries.add(new Country(jo.getString(COUNTRY_NAME),jo.getString(DIAL_CODE),jo.getString(COUNTRY_CODE)));
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(countries,new Comparator<Country>() {
            
            @Override
            public int compare(Country o1,Country o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        return countries;
    }

    @Override
    public void onCountrySelected(Country country) {
        Intent result = new Intent();
        result.putExtra(COUNTRY_NAME,country.getName());
        result.putExtra(DIAL_CODE,country.getCode());
        result.putExtra(COUNTRY_CODE,country.getCountryCode());
        setResult(Activity.RESULT_OK,result);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public class Country {

        private String name = "";
        private String countryCode = "";
        private String code = "";
        private Context context;
        private final String prefix = "country_";

        public Country() {

        }

        public Country(String name,String code,String countryCode) {
            this.context = getApplicationContext();
            this.code = code;
            this.countryCode = countryCode;
            this.name = getTranslatableCountryName(countryCode, name);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = getTranslatableCountryName(this.countryCode,name);
        }
        
        public String getTranslatableCountryName(String countryCode,String name){
            int stringId = context.getResources().getIdentifier(prefix + countryCode.toLowerCase(), "string", context.getPackageName());
            if(stringId > 0){
                return context.getString(stringId);
            }else{
                return name;
            }
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    
        public String getCountryCode() {
            return countryCode;
        }
    
        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }
    }

    public static void countrySelected() {

    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String query = searchAutoComplete.getText().toString();
        if(searchAutoComplete.hasFocus() || (query != null && !query.isEmpty())){
            outState.putString(SAVED_QUERY_STRING, query);
        }
    }
}
