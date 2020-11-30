package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class SearchByDateActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface, View.OnClickListener, DatePickerDialog.OnDateSetListener {

    float scaleH, scaleW;
    float density;
    DisplayMetrics outMetrics;
    Display display;

    public static String ACTION_SEARCH_BY_DATE = "ACTION_SEARCH_BY_DATE";

    RelativeLayout fragmentContainer;
    RelativeLayout relativeLayoutDay;
    RelativeLayout relativeLayoutFrom;
    RelativeLayout relativeLayoutTo;
    TextView textViewDay;
    TextView textViewFrom;
    TextView textViewTo;
    TextView textViewSetPeriod;
    TextView textViewTitleFrom;
    TextView textViewTitleTo;
    TextView textViewSetDay;
    Button buttonLastMonth;
    Button buttonLastYear;
    Button buttonCancel;
    Button buttonApply;
    ImageButton removeDay;
    ImageButton removePeriodFrom;
    ImageButton removePeriodTo;

    String optionSelected = null;
    boolean optionPeriodFrom = false;
    boolean optionPeriodTo = false;
    long tsDay = 0;
    long tsFrom = 0;
    long tsTo = 0;

    private long selectedDate[];
    DatePickerDialog datePickerDialog;

    ActionBar aB;
    Toolbar tB;

    public int visibleFragment= COPYRIGHT_FRAGMENT;

    static SearchByDateActivityLollipop searchByDateActivity;

    public long handle;
    public MegaNode selectedNode;
    public int accountType;

    DatabaseHandler dbH;

    Handler handler = new Handler();
    private MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    @SuppressLint("NewApi")
    @Override protected void onCreate(Bundle savedInstanceState) {
        logDebug("onCreate");
        super.onCreate(savedInstanceState);

        searchByDateActivity = this;

        display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        density  = getResources().getDisplayMetrics().density;

        scaleW = getScaleW(outMetrics, density);
        scaleH = getScaleH(outMetrics, density);

        dbH = DatabaseHandler.getDbHandler(getApplicationContext());
        if (megaApi == null){
            megaApi = ((MegaApplication) getApplication()).getMegaApi();
        }

        if(megaApi==null||megaApi.getRootNode()==null){
            logDebug("Refresh session - sdk");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
        }

        if (megaChatApi == null || megaChatApi.getInitState() == MegaChatApi.INIT_ERROR) {
            logDebug("Refresh session - karere");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.search_by_date_activity_layout);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
        }

        fragmentContainer = (RelativeLayout) findViewById(R.id.fragment_container_search_date);

        tB = (Toolbar) findViewById(R.id.toolbar_search);
        if(tB==null){
            logError("Tb is Null");
            return;
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        aB.setTitle(getString(R.string.action_search_by_date));
        aB.setSubtitle(null);

        textViewSetDay = (TextView) findViewById(R.id.title_set_day);

        relativeLayoutDay = (RelativeLayout) findViewById(R.id.relative_layout_day);
        textViewDay = (TextView) findViewById(R.id.text_view_day);
        removeDay = (ImageButton) findViewById(R.id.remove_day);

        buttonLastMonth = (Button) findViewById(R.id.button_last_month);
        buttonLastYear = (Button) findViewById(R.id.button_last_year);

        textViewSetPeriod = (TextView) findViewById(R.id.title_set_period);

        relativeLayoutFrom = (RelativeLayout) findViewById(R.id.relative_layout_from);
        textViewTitleFrom = (TextView) findViewById(R.id.title_text_view_from);
        textViewFrom = (TextView) findViewById(R.id.text_view_from);
        removePeriodFrom = (ImageButton) findViewById(R.id.remove_period_from);

        relativeLayoutTo = (RelativeLayout) findViewById(R.id.relative_layout_to);
        textViewTitleTo = (TextView) findViewById(R.id.title_text_view_to);
        textViewTo = (TextView) findViewById(R.id.text_view_to);
        removePeriodTo = (ImageButton) findViewById(R.id.remove_period_to);

        buttonCancel = (Button) findViewById(R.id.button_cancel);
        buttonApply = (Button) findViewById(R.id.button_apply);

        String weekDay = giveDate();
        textViewDay.setText(weekDay);
        textViewFrom.setText(weekDay);
        textViewTo.setText(weekDay);

        buttonApply.setEnabled(false);
        buttonApply.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));
        buttonApply.setBackgroundColor(ContextCompat.getColor(this, R.color.white));

        selectedDate = new long[5];
        selectedDate[0]=0;
        selectedDate[1]=0;
        selectedDate[2]=0;
        selectedDate[3]=0;
        selectedDate[4]=0;

        relativeLayoutDay.setOnClickListener(this);
        relativeLayoutFrom.setOnClickListener(this);
        relativeLayoutTo.setOnClickListener(this);
        buttonLastMonth.setOnClickListener(this);
        buttonLastYear.setOnClickListener(this);
        buttonCancel.setOnClickListener(this);
        buttonApply.setOnClickListener(this);
        removeDay.setOnClickListener(this);
        removePeriodFrom.setOnClickListener(this);
        removePeriodTo.setOnClickListener(this);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                finish();
                break;
            }
        }
        return true;
    }


    @Override
    public void onResume() {
        logDebug("onResume");
        super.onResume();
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {}

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {}

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {}

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {}

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.relative_layout_day:{
                optionSelected = "day";
                showDatePicker(optionSelected);
                break;
            }
            case R.id.remove_day:{
                optionSelected = "day";
                restoreOptions(optionSelected);
                break;
            }
            case R.id.button_last_month:{
                optionSelected = "month";
                applySelection();
                break;
            }
            case R.id.button_last_year:{
                optionSelected = "year";
                applySelection();
                break;
            }
            case R.id.relative_layout_from:{
                optionSelected = "from";
                showDatePicker(optionSelected);
                break;
            }
            case R.id.remove_period_from:{
                optionSelected = "from";
                restoreOptions(optionSelected);
                break;
            }
            case R.id.relative_layout_to:{
                optionSelected = "to";
                showDatePicker(optionSelected);
                break;
            }
            case R.id.remove_period_to:{
                optionSelected = "to";
                restoreOptions(optionSelected);
                break;
            }
            case R.id.button_cancel:{
                finish();
                break;
            }
            case R.id.button_apply:{
                applySelection();
                break;
            }
        }
    }

    public String giveDate() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMMM yyyy");
        return sdf.format(cal.getTime());
    }

    public void showDatePicker(String element){

        Calendar cal = Calendar.getInstance();
        int year, month, day;

        if((element.equals("from")) && (tsTo != 0)){
            cal.setTimeInMillis(tsTo);
            year = cal.get(Calendar.YEAR);
            month = cal.get(Calendar.MONTH);
            day = cal.get(Calendar.DAY_OF_MONTH);
        }else if((element.equals("to")) && (tsFrom != 0)){
            cal.setTimeInMillis(tsFrom);
            year = cal.get(Calendar.YEAR);
            month = cal.get(Calendar.MONTH);
            day = cal.get(Calendar.DAY_OF_MONTH);
        }else {
            year = cal.get(Calendar.YEAR);
            month = cal.get(Calendar.MONTH);
            day = cal.get(Calendar.DAY_OF_MONTH);
        }

        datePickerDialog = new DatePickerDialog(this, this, year, month, day);
        datePickerDialog.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMMM yyyy");

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        Date date = cal.getTime();
        String formattedDate = sdf.format(date);
        long ts = cal.getTimeInMillis();

        disableOptions(optionSelected, formattedDate, ts);
    }

    public void disableOptions(String option, String date, long ts){
        String weekDay = giveDate();

        if(option.equals("day")){
            tsDay = ts;

            textViewSetDay.setTextColor(ContextCompat.getColor(this, R.color.secondary_text));
            relativeLayoutDay.setEnabled(true);
            textViewDay.setText(date);
            textViewDay.setTextColor(ContextCompat.getColor(this, R.color.accentColor));
            removeDay.setVisibility(View.VISIBLE);

            textViewSetPeriod.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));

            relativeLayoutFrom.setEnabled(false);
            textViewTitleFrom.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));
            textViewFrom.setText(weekDay);
            textViewFrom.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));
            removePeriodFrom.setVisibility(View.GONE);

            relativeLayoutTo.setEnabled(false);
            textViewTitleTo.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));
            textViewTo.setText(weekDay);
            textViewTo.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));
            removePeriodTo.setVisibility(View.GONE);

        }else if (option.equals("from")){
            tsFrom = ts;

            textViewSetPeriod.setTextColor(ContextCompat.getColor(this, R.color.secondary_text));
            relativeLayoutFrom.setEnabled(true);
            textViewTitleFrom.setTextColor(ContextCompat.getColor(this, R.color.secondary_text));

            if((tsTo == 0) || (tsTo > ts)){

                textViewFrom.setText(date);
                textViewFrom.setTextColor(ContextCompat.getColor(this, R.color.accentColor));
                removePeriodFrom.setVisibility(View.VISIBLE);
                optionPeriodFrom = true;

            }else{
                showSnackbar(getString(R.string.snackbar_search_by_date));
                textViewFrom.setText(weekDay);
                textViewFrom.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));
                removePeriodFrom.setVisibility(View.GONE);
            }

            textViewSetDay.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));
            relativeLayoutDay.setEnabled(false);
            textViewDay.setText(weekDay);
            textViewDay.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));
            removeDay.setVisibility(View.GONE);

        }else if(option.equals("to")){
            tsTo = ts;

            textViewSetPeriod.setTextColor(ContextCompat.getColor(this, R.color.secondary_text));
            relativeLayoutTo.setEnabled(true);
            textViewTitleTo.setTextColor(ContextCompat.getColor(this, R.color.secondary_text));

            if((tsFrom ==0 ) || (tsFrom < ts)){

                textViewTo.setText(date);
                textViewTo.setTextColor(ContextCompat.getColor(this, R.color.accentColor));
                removePeriodTo.setVisibility(View.VISIBLE);
                optionPeriodTo = true;

            }else{
                showSnackbar(getString(R.string.snackbar_search_by_date));
                textViewTo.setText(weekDay);
                textViewTo.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));
                removePeriodTo.setVisibility(View.GONE);
            }

            textViewSetDay.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));
            relativeLayoutDay.setEnabled(false);
            textViewDay.setText(weekDay);
            textViewDay.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));
            removeDay.setVisibility(View.GONE);
        }

        buttonLastMonth.setEnabled(false);
        buttonLastMonth.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));

        buttonLastYear.setEnabled(false);
        buttonLastYear.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));

        if(((optionPeriodFrom == true) && (optionPeriodTo == true))|| (option.equals(("day")))){
            buttonApply.setEnabled(true);
            buttonApply.setTextColor(ContextCompat.getColor(this, R.color.white));
            buttonApply.setBackgroundColor(ContextCompat.getColor(this, R.color.accentColor));
        }else{
            buttonApply.setEnabled(false);
            buttonApply.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));
            buttonApply.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
        }

    }
    public void restoreOptions(String element){
        String weekDay = giveDate();
        optionSelected = null;

        if(element.equals("day")){
            optionPeriodFrom = false;
            optionPeriodTo = false;
            tsDay = 0;

            textViewDay.setText(weekDay);
            textViewDay.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));
            removeDay.setVisibility(View.GONE);

            buttonLastMonth.setEnabled(true);
            buttonLastMonth.setTextColor(ContextCompat.getColor(this, R.color.black));

            buttonLastYear.setEnabled(true);
            buttonLastYear.setTextColor(ContextCompat.getColor(this, R.color.black));

            textViewSetPeriod.setTextColor(ContextCompat.getColor(this, R.color.secondary_text));

            relativeLayoutFrom.setEnabled(true);
            textViewTitleFrom.setTextColor(ContextCompat.getColor(this, R.color.secondary_text));
            textViewFrom.setText(weekDay);
            textViewFrom.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));
            removePeriodFrom.setVisibility(View.GONE);

            relativeLayoutTo.setEnabled(true);
            textViewTitleTo.setTextColor(ContextCompat.getColor(this, R.color.secondary_text));
            textViewTo.setText(weekDay);
            textViewTo.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));
            removePeriodTo.setVisibility(View.GONE);

        }else if(element.equals("from")){
            optionPeriodFrom = false;
            tsFrom = 0;
            textViewFrom.setText(weekDay);
            textViewFrom.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));
            removePeriodFrom.setVisibility(View.GONE);

            if((optionPeriodFrom == false)&&(optionPeriodTo == false)){
                textViewSetDay.setTextColor(ContextCompat.getColor(this, R.color.secondary_text));
                relativeLayoutDay.setEnabled(true);

                buttonLastMonth.setEnabled(true);
                buttonLastMonth.setTextColor(ContextCompat.getColor(this, R.color.black));

                buttonLastYear.setEnabled(true);
                buttonLastYear.setTextColor(ContextCompat.getColor(this, R.color.black));
            }

        }else if(element.equals("to")){
            optionPeriodTo = false;
            tsTo = 0;
            textViewTo.setText(weekDay);
            textViewTo.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));
            removePeriodTo.setVisibility(View.GONE);

            if((optionPeriodFrom == false)&&(optionPeriodTo == false)){

                textViewSetDay.setTextColor(ContextCompat.getColor(this, R.color.secondary_text));
                relativeLayoutDay.setEnabled(true);

                buttonLastMonth.setEnabled(true);
                buttonLastMonth.setTextColor(ContextCompat.getColor(this, R.color.black));

                buttonLastYear.setEnabled(true);
                buttonLastYear.setTextColor(ContextCompat.getColor(this, R.color.black));
            }
        }

        buttonApply.setEnabled(false);
        buttonApply.setTextColor(ContextCompat.getColor(this, R.color.black_12_alpha));
        buttonApply.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
    }

    public void applySelection(){
        if(optionSelected.equals("day")){

            selectedDate[0] = 1;
            selectedDate[1] = tsDay;

        }else if((optionSelected.equals("month"))||(optionSelected.equals("year"))){

            selectedDate[0] = 2;

            if(optionSelected.equals("month")){
                selectedDate[2] = 1;
            }else{
                selectedDate[2] = 2;
            }
        }else if((optionSelected.equals("from"))||(optionSelected.equals("to"))) {

            selectedDate[0] = 3;
            selectedDate[3] = tsFrom;
            selectedDate[4] = tsTo;

        }

        if(selectedDate[0] != 0){
            Intent intent = new Intent();
            intent.putExtra("SELECTED_DATE",selectedDate);
            setResult(RESULT_OK, intent);
            finish();
        }else{
            finish();
        }
    }


    public void showSnackbar(String message){
        showSnackbar(fragmentContainer, message);
    }
}


