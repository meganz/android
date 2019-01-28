package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.adapters.CountryListAdapter;
import mega.privacy.android.app.utils.Util;

public class CountryCodePickerActivityLollipop extends PinActivityLollipop {

    private static List<Country> countries;

    private List<Country> selectedCountries = new ArrayList<>();

    private RecyclerView countryList;

    private CountryListAdapter adapter;

    private ActionBar actionBar;

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contry_code_picker);
        if (countries == null) {
            countries = loadCountries();
        }

        countryList = findViewById(R.id.country_list);
        adapter = new CountryListAdapter(countries);
        countryList.setAdapter(adapter);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        countryList.setLayoutManager(manager);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setTitle("SELECT COUNTRY");
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(Util.mutateIcon(this,R.drawable.ic_arrow_back_white,R.color.black));
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
        SearchView.SearchAutoComplete searchAutoComplete = searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchAutoComplete.setTextColor(ContextCompat.getColor(this,R.color.black));
        searchAutoComplete.setHintTextColor(ContextCompat.getColor(this,R.color.status_bar_login));
        searchAutoComplete.setHint(getString(R.string.action_search) + "...");
        v.setBackgroundColor(ContextCompat.getColor(this,android.R.color.transparent));
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
//                search(query);
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
        for (Country country : countries) {
            if (country.name.toLowerCase().contains(query.toLowerCase())) {
                selectedCountries.add(country);
            }
        }
        adapter.refresh(selectedCountries);
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
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
                countries.add(new Country(jo.getString("name"),jo.getString("dial_code")));
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
        return countries;
    }

    public class Country {

        private String name;

        private String code;

        public Country() {

        }

        public Country(String name,String code) {
            this.name = name;
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
