package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaPricing;

public class ChooseAccountFragmentLollipop extends Fragment implements View.OnClickListener {

    Context context;
    static int HEIGHT_ACCOUNT_LAYOUT=109;

    float scaleH, scaleW;
    float density;
    DisplayMetrics outMetrics;
    Display display;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    ActionBar aB;

    public ArrayList<Product> accounts;

    private LinearLayout mainLinearLayout;
    private ScrollView scrollView;

    private RelativeLayout freeLayout;
    private RelativeLayout proLiteLayout;
    private RelativeLayout pro1Layout;
    private RelativeLayout pro3Layout;
    private RelativeLayout pro2Layout;

    private RelativeLayout freeTransparentLayout;
    private RelativeLayout proLiteTransparentLayout;
    private RelativeLayout pro1TransparentLayout;
    private RelativeLayout pro3TransparentLayout;
    private RelativeLayout pro2TransparentLayout;

    private RelativeLayout leftFreeLayout;
    private RelativeLayout leftProLiteLayout;
    private RelativeLayout leftPro1Layout;
    private RelativeLayout leftPro3Layout;
    private RelativeLayout leftPro2Layout;

    private TextView titleFree;
    private TextView titleProLite;
    private TextView titlePro1;
    private TextView titlePro2;
    private TextView titlePro3;

    private View verticalDividerFree;
    private View verticalDividerProLite;
    private View verticalDividerPro1;
    private View verticalDividerPro2;
    private View verticalDividerPro3;

    private RelativeLayout rightFreeLayout;
    private RelativeLayout rightProLiteLayout;
    private RelativeLayout rightPro1Layout;
    private RelativeLayout rightPro3Layout;
    private RelativeLayout rightPro2Layout;

    private TableRow tableRowFree;
    private TableRow tableRowProLite;
    private TableRow tableRowPro1;
    private TableRow tableRowPro2;
    private TableRow tableRowPro3;

    private TextView storageValueFree;
    private TextView storageValueProLite;
    private TextView storageValuePro1;
    private TextView storageValuePro2;
    private TextView storageValuePro3;

    private TextView emptyTextFree;
    private TextView emptyTextProLite;
    private TextView emptyTextPro1;
    private TextView emptyTextPro2;
    private TextView emptyTextPro3;

    private TextView bandwidthValueFree;
    private TextView bandwidthValueProLite;
    private TextView bandwidthValuePro1;
    private TextView bandwidthValuePro2;
    private TextView bandwidthValuePro3;

    private TextView emptyTextBandwidthFree;
    private TextView emptyTextBandwidthProLite;
    private TextView emptyTextBandwidthPro1;
    private TextView emptyTextBandwidthPro2;
    private TextView emptyTextBandwidthPro3;

    private TextView freePriceInteger;
    private TextView freePriceDecimal;
    private TextView freeStorageTitle;
    private TextView freeStorageInteger;
    private TextView freeStorageGb;
    private TextView freeBandwidthInteger;
    private TextView freeBandwidthTb;

    private TextView proLitePriceInteger;
    private TextView proLitePriceDecimal;
    private TextView proLiteStorageInteger;
    private TextView proLiteStorageGb;
    private TextView proLiteBandwidthInteger;
    private TextView proLiteBandwidthTb;

    private TextView pro1PriceInteger;
    private TextView pro1PriceDecimal;
    private TextView pro1StorageInteger;
    private TextView pro1StorageGb;
    private TextView pro1BandwidthInteger;
    private TextView pro1BandwidthTb;

    private TextView pro2PriceInteger;
    private TextView pro2PriceDecimal;
    private TextView pro2StorageInteger;
    private TextView pro2StorageGb;
    private TextView pro2BandwidthInteger;
    private TextView pro2BandwidthTb;

    private TextView pro3PriceInteger;
    private TextView pro3PriceDecimal;
    private TextView pro3StorageInteger;
    private TextView pro3StorageGb;
    private TextView pro3BandwidthInteger;
    private TextView pro3BandwidthTb;

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate");
        super.onCreate(savedInstanceState);

        if(context==null){
            log("context is null");
            return;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = getResources().getDisplayMetrics().density;

        float scaleW = Util.getScaleW(outMetrics, density);
        float scaleH = Util.getScaleH(outMetrics, density);

        if(megaApi==null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if(megaChatApi==null){
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
        }

        DatabaseHandler dbH = DatabaseHandler.getDbHandler(((Activity)context).getApplicationContext());
        if (dbH.getCredentials() == null){
//            megaApi.localLogout();
//            AccountController aC = new AccountController(context);
//            aC.logout(context, megaApi, megaChatApi, false);
            //Show Login Fragment
            ((LoginActivityLollipop)context).showFragment(Constants.LOGIN_FRAGMENT);
        }

        accounts = new ArrayList<Product>();

        View v = inflater.inflate(R.layout.fragment_choose_account, container, false);

        scrollView = (ScrollView) v.findViewById(R.id.scroll_view_choose_account);
        mainLinearLayout = (LinearLayout) v.findViewById(R.id.choose_account_main_linear_layout);

        //Replace elevation
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.grid_item_separator));
            mainLinearLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.grid_item_separator));
        }

        //FREE ACCOUNT
        freeLayout = (RelativeLayout) v.findViewById(R.id.choose_account_free_layout);
        freeLayout.setOnClickListener(this);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) freeLayout.getLayoutParams();
        layoutParams.setMargins(Util.scaleWidthPx(8, outMetrics), Util.scaleHeightPx(8, outMetrics), Util.scaleWidthPx(8, outMetrics), 0);
        freeLayout.setLayoutParams(layoutParams);

        //Replace elevation
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            freeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        }

        leftFreeLayout = (RelativeLayout) v.findViewById(R.id.choose_account_free_left_side);
        RelativeLayout.LayoutParams leftLayoutParams = (RelativeLayout.LayoutParams) leftFreeLayout.getLayoutParams();
        leftLayoutParams.width = Util.scaleWidthPx(125, outMetrics);
        leftLayoutParams.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
        leftFreeLayout.setLayoutParams(leftLayoutParams);

        titleFree= (TextView) v.findViewById(R.id.choose_account_free_title_text);
        RelativeLayout.LayoutParams titleParams = (RelativeLayout.LayoutParams) titleFree.getLayoutParams();
        titleParams.setMargins(0,0,0,Util.scaleHeightPx(11, outMetrics));
        titleFree.setLayoutParams(titleParams);

        verticalDividerFree = (View) v.findViewById(R.id.choose_account_free_vertical_divider);
        verticalDividerFree.getLayoutParams().width = Util.scaleWidthPx(2, outMetrics);
        verticalDividerFree.getLayoutParams().height = Util.scaleHeightPx(86, outMetrics);

        rightFreeLayout = (RelativeLayout) v.findViewById(R.id.choose_account_free_layout_right_side);
        RelativeLayout.LayoutParams rightLayoutParams = (RelativeLayout.LayoutParams) rightFreeLayout.getLayoutParams();
        rightLayoutParams.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
        rightLayoutParams.setMargins(Util.scaleWidthPx(12, outMetrics),0,0,0);
        rightFreeLayout.setLayoutParams(rightLayoutParams);

        tableRowFree = (TableRow) v.findViewById(R.id.table_row_free);
        TableLayout.LayoutParams tableRowParams = (TableLayout.LayoutParams) tableRowFree.getLayoutParams();
        tableRowParams.setMargins(0,0,0,Util.scaleHeightPx(25, outMetrics));
        tableRowFree.setLayoutParams(tableRowParams);

        storageValueFree = (TextView) v.findViewById(R.id.choose_account_free_storage_value_integer);
        TableRow.LayoutParams storageValueParams = (TableRow.LayoutParams) storageValueFree.getLayoutParams();
        storageValueParams.width = Util.scaleWidthPx(40, outMetrics);
        storageValueFree.setLayoutParams(storageValueParams);

        emptyTextFree = (TextView) v.findViewById(R.id.choose_account_free_empty_text);
        TableRow.LayoutParams emptyTextParams = (TableRow.LayoutParams) emptyTextFree.getLayoutParams();
        emptyTextParams.width = Util.scaleWidthPx(12, outMetrics);
        emptyTextFree.setLayoutParams(emptyTextParams);

        bandwidthValueFree = (TextView) v.findViewById(R.id.choose_account_free_bandwidth_value_integer);
        TableRow.LayoutParams bandwidthValueParams = (TableRow.LayoutParams) bandwidthValueFree.getLayoutParams();
        bandwidthValueParams.width = Util.scaleWidthPx(40, outMetrics);
        bandwidthValueFree.setLayoutParams(bandwidthValueParams);

        emptyTextBandwidthFree = (TextView) v.findViewById(R.id.choose_account_free_empty_text_bandwidth);
        TableRow.LayoutParams emptyTextBandwidthParams = (TableRow.LayoutParams) emptyTextBandwidthFree.getLayoutParams();
        emptyTextBandwidthParams.width = Util.scaleWidthPx(12, outMetrics);
        emptyTextBandwidthFree.setLayoutParams(emptyTextBandwidthParams);

        TextView perMonth = (TextView) v.findViewById(R.id.choose_account_free_per_month_text);
        perMonth.setText("/" + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));
        RelativeLayout.LayoutParams perMonthParams = (RelativeLayout.LayoutParams) perMonth.getLayoutParams();
        perMonthParams.setMargins(0,0,0,Util.scaleHeightPx(4, outMetrics));
        perMonth.setLayoutParams(perMonthParams);

        freeStorageTitle = (TextView) v.findViewById(R.id.choose_account_free_storage_label);
        String storageTitle = getString(R.string.general_storage)+" *";
        freeStorageTitle.setText(storageTitle.toUpperCase(Locale.getDefault()));

        freePriceInteger = (TextView) v.findViewById(R.id.choose_account_free_integer_text);
        freePriceDecimal = (TextView) v.findViewById(R.id.choose_account_free_decimal_text);
        RelativeLayout.LayoutParams priceDecimalParams = (RelativeLayout.LayoutParams) freePriceDecimal.getLayoutParams();
        priceDecimalParams.setMargins(0,0,0,Util.scaleHeightPx(4, outMetrics));
        freePriceDecimal.setLayoutParams(priceDecimalParams);

        freeStorageInteger = (TextView) v.findViewById(R.id.choose_account_free_storage_value_integer);
        freeStorageGb = (TextView) v.findViewById(R.id.choose_account_free_storage_value_gb);
        freeBandwidthInteger = (TextView) v.findViewById(R.id.choose_account_free_bandwidth_value_integer);
        freeBandwidthTb = (TextView) v.findViewById(R.id.choose_account_free_bandwith_value_tb);

        freeTransparentLayout = (RelativeLayout) v.findViewById(R.id.choose_account_free_layout_transparent);
        freeTransparentLayout.setVisibility(View.INVISIBLE);
        //END -- FREE ACCOUNT

        //PRO LITE ACCOUNT
        proLiteLayout = (RelativeLayout) v.findViewById(R.id.choose_account_prolite_layout);
        proLiteLayout.setOnClickListener(this);
        LinearLayout.LayoutParams layoutParamsProLite = (LinearLayout.LayoutParams) proLiteLayout.getLayoutParams();
        layoutParamsProLite.setMargins(Util.scaleWidthPx(8, outMetrics), Util.scaleHeightPx(8, outMetrics), Util.scaleWidthPx(8, outMetrics), 0);
        proLiteLayout.setLayoutParams(layoutParamsProLite);

        //Replace elevation
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            proLiteLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        }

        leftProLiteLayout = (RelativeLayout) v.findViewById(R.id.choose_account_prolite_left_side);
        RelativeLayout.LayoutParams leftLayoutParamsProLite = (RelativeLayout.LayoutParams) leftProLiteLayout.getLayoutParams();
        leftLayoutParamsProLite.width = Util.scaleWidthPx(125, outMetrics);
        leftLayoutParamsProLite.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
        leftProLiteLayout.setLayoutParams(leftLayoutParamsProLite);

        titleProLite = (TextView) v.findViewById(R.id.choose_account_prolite_title_text);
        RelativeLayout.LayoutParams titleParamsProLite = (RelativeLayout.LayoutParams) titleProLite.getLayoutParams();
        titleParamsProLite.setMargins(0,0,0,Util.scaleHeightPx(11, outMetrics));
        titleProLite.setLayoutParams(titleParamsProLite);

        verticalDividerProLite = (View) v.findViewById(R.id.choose_account_prolite_vertical_divider);
        verticalDividerProLite.getLayoutParams().width = Util.scaleWidthPx(2, outMetrics);
        verticalDividerProLite.getLayoutParams().height = Util.scaleHeightPx(86, outMetrics);

        rightProLiteLayout = (RelativeLayout) v.findViewById(R.id.choose_account_prolite_layout_right_side);
        RelativeLayout.LayoutParams rightLayoutParamsProLite = (RelativeLayout.LayoutParams) rightProLiteLayout.getLayoutParams();
        rightLayoutParamsProLite.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
        rightProLiteLayout.setLayoutParams(rightLayoutParamsProLite);

        tableRowProLite = (TableRow) v.findViewById(R.id.table_row_pro_lite);
        TableLayout.LayoutParams tableRowParamsProLite = (TableLayout.LayoutParams) tableRowProLite.getLayoutParams();
        tableRowParamsProLite.setMargins(0,0,0,Util.scaleHeightPx(25, outMetrics));
        tableRowProLite.setLayoutParams(tableRowParamsProLite);

        storageValueProLite = (TextView) v.findViewById(R.id.choose_account_prolite_storage_value_integer);
        TableRow.LayoutParams storageValueParamsProLite = (TableRow.LayoutParams) storageValueProLite.getLayoutParams();
        storageValueParamsProLite.width = Util.scaleWidthPx(40, outMetrics);
        storageValueProLite.setLayoutParams(storageValueParamsProLite);

        emptyTextProLite = (TextView) v.findViewById(R.id.choose_account_prolite_empty_text);
        TableRow.LayoutParams emptyTextParamsProLite = (TableRow.LayoutParams) emptyTextProLite.getLayoutParams();
        emptyTextParamsProLite.width = Util.scaleWidthPx(12, outMetrics);
        emptyTextProLite.setLayoutParams(emptyTextParamsProLite);

        bandwidthValueProLite = (TextView) v.findViewById(R.id.choose_account_prolite_bandwidth_value_integer);
        TableRow.LayoutParams bandwidthValueParamsProLite = (TableRow.LayoutParams) bandwidthValueProLite.getLayoutParams();
        bandwidthValueParamsProLite.width = Util.scaleWidthPx(40, outMetrics);
        bandwidthValueProLite.setLayoutParams(bandwidthValueParamsProLite);

        emptyTextBandwidthProLite = (TextView) v.findViewById(R.id.choose_account_prolite_empty_text_bandwidth);
        TableRow.LayoutParams emptyTextBandwidthParamsProLite = (TableRow.LayoutParams) emptyTextBandwidthProLite.getLayoutParams();
        emptyTextBandwidthParamsProLite.width = Util.scaleWidthPx(12, outMetrics);
        emptyTextBandwidthProLite.setLayoutParams(emptyTextBandwidthParamsProLite);

        TextView perMonthProLite = (TextView) v.findViewById(R.id.choose_account_prolite_per_month_text);
        perMonthProLite.setText("/" + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));
        RelativeLayout.LayoutParams perMonthParamsProLite = (RelativeLayout.LayoutParams) perMonthProLite.getLayoutParams();
        perMonthParamsProLite.setMargins(0,0,0,Util.scaleHeightPx(3, outMetrics));
        perMonthProLite.setLayoutParams(perMonthParamsProLite);

        proLitePriceInteger = (TextView) v.findViewById(R.id.choose_account_prolite_integer_text);
        proLitePriceDecimal = (TextView) v.findViewById(R.id.choose_account_prolite_decimal_text);
        RelativeLayout.LayoutParams priceDecimalParamsProLite = (RelativeLayout.LayoutParams) proLitePriceDecimal.getLayoutParams();
        priceDecimalParamsProLite.setMargins(0,0,0,Util.scaleHeightPx(3, outMetrics));
        proLitePriceDecimal.setLayoutParams(priceDecimalParamsProLite);

        proLiteStorageInteger = (TextView) v.findViewById(R.id.choose_account_prolite_storage_value_integer);
        proLiteStorageGb = (TextView) v.findViewById(R.id.choose_account_prolite_storage_value_gb);
        proLiteBandwidthInteger = (TextView) v.findViewById(R.id.choose_account_prolite_bandwidth_value_integer);
        proLiteBandwidthTb = (TextView) v.findViewById(R.id.choose_account_prolite_bandwith_value_tb);

        proLiteTransparentLayout = (RelativeLayout) v.findViewById(R.id.choose_account_prolite_layout_transparent);
        proLiteTransparentLayout.setVisibility(View.INVISIBLE);
        //END -- PRO LITE ACCOUNT

        //PRO I ACCOUNT
        pro1Layout = (RelativeLayout) v.findViewById(R.id.choose_account_pro_i_layout);
        pro1Layout.setOnClickListener(this);
        LinearLayout.LayoutParams pro1LayoutParams = (LinearLayout.LayoutParams) pro1Layout.getLayoutParams();
        pro1LayoutParams.setMargins(Util.scaleWidthPx(8, outMetrics), Util.scaleHeightPx(8, outMetrics), Util.scaleWidthPx(8, outMetrics), 0);
        pro1Layout.setLayoutParams(pro1LayoutParams);

        //Replace elevation
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            pro1Layout.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        }

        leftPro1Layout = (RelativeLayout) v.findViewById(R.id.choose_account_pro_i_left_side);
        RelativeLayout.LayoutParams leftPro1LayoutParams = (RelativeLayout.LayoutParams) leftPro1Layout.getLayoutParams();
        leftPro1LayoutParams.width = Util.scaleWidthPx(125, outMetrics);
        leftPro1LayoutParams.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
        leftPro1Layout.setLayoutParams(leftPro1LayoutParams);

        titlePro1 = (TextView) v.findViewById(R.id.choose_account_pro_i_title_text);
        RelativeLayout.LayoutParams titlePro1Params = (RelativeLayout.LayoutParams) titlePro1.getLayoutParams();
        titlePro1Params.setMargins(0,0,0,Util.scaleHeightPx(11, outMetrics));
        titlePro1.setLayoutParams(titlePro1Params);

        verticalDividerPro1 = (View) v.findViewById(R.id.choose_account_pro_i_vertical_divider);
        verticalDividerPro1.getLayoutParams().width = Util.scaleWidthPx(2, outMetrics);
        verticalDividerPro1.getLayoutParams().height = Util.scaleHeightPx(86, outMetrics);

        rightPro1Layout = (RelativeLayout) v.findViewById(R.id.choose_account_pro_i_layout_right_side);
        RelativeLayout.LayoutParams rightLayoutPro1Params = (RelativeLayout.LayoutParams) rightPro1Layout.getLayoutParams();
        rightLayoutPro1Params.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
        rightPro1Layout.setLayoutParams(rightLayoutPro1Params);

        tableRowPro1 = (TableRow) v.findViewById(R.id.table_row_pro_i);
        TableLayout.LayoutParams tableRowPro1Params = (TableLayout.LayoutParams) tableRowPro1.getLayoutParams();
        tableRowPro1Params.setMargins(0,0,0,Util.scaleHeightPx(25, outMetrics));
        tableRowPro1.setLayoutParams(tableRowPro1Params);

        storageValuePro1 = (TextView) v.findViewById(R.id.choose_account_pro_i_storage_value_integer);
        TableRow.LayoutParams storageValuePro1Params = (TableRow.LayoutParams) storageValuePro1.getLayoutParams();
        storageValuePro1Params.width = Util.scaleWidthPx(40, outMetrics);
        storageValuePro1.setLayoutParams(storageValuePro1Params);

        emptyTextPro1 = (TextView) v.findViewById(R.id.choose_account_pro_i_empty_text);
        TableRow.LayoutParams emptyTextPro1Params = (TableRow.LayoutParams) emptyTextPro1.getLayoutParams();
        emptyTextPro1Params.width = Util.scaleWidthPx(12, outMetrics);
        emptyTextPro1.setLayoutParams(emptyTextPro1Params);

        bandwidthValuePro1 = (TextView) v.findViewById(R.id.choose_account_pro_i_bandwidth_value_integer);
        TableRow.LayoutParams bandwidthValuePro1Params = (TableRow.LayoutParams) bandwidthValuePro1.getLayoutParams();
        bandwidthValuePro1Params.width = Util.scaleWidthPx(40, outMetrics);
        bandwidthValuePro1.setLayoutParams(bandwidthValuePro1Params);

        emptyTextBandwidthPro1 = (TextView) v.findViewById(R.id.choose_account_pro_i_empty_text_bandwidth);
        TableRow.LayoutParams emptyTextBandwidthPro1Params = (TableRow.LayoutParams) emptyTextBandwidthPro1.getLayoutParams();
        emptyTextBandwidthPro1Params.width = Util.scaleWidthPx(12, outMetrics);
        emptyTextBandwidthPro1.setLayoutParams(emptyTextBandwidthPro1Params);

        perMonth = (TextView) v.findViewById(R.id.choose_account_pro_i_per_month_text);
        perMonth.setText("/" + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));

        RelativeLayout.LayoutParams perMonthPro1Params = (RelativeLayout.LayoutParams) perMonth.getLayoutParams();
        perMonthPro1Params.setMargins(0,0,0,Util.scaleHeightPx(3, outMetrics));
        perMonth.setLayoutParams(perMonthPro1Params);

        pro1PriceInteger = (TextView) v.findViewById(R.id.choose_account_pro_i_integer_text);

        pro1PriceDecimal = (TextView) v.findViewById(R.id.choose_account_pro_i_decimal_text);
        RelativeLayout.LayoutParams priceDecimalPro1Params = (RelativeLayout.LayoutParams) pro1PriceDecimal.getLayoutParams();
        priceDecimalPro1Params.setMargins(0,0,0,Util.scaleHeightPx(3, outMetrics));
        pro1PriceDecimal.setLayoutParams(priceDecimalPro1Params);

        pro1StorageInteger = (TextView) v.findViewById(R.id.choose_account_pro_i_storage_value_integer);
        pro1StorageGb = (TextView) v.findViewById(R.id.choose_account_pro_i_storage_value_gb);
        pro1BandwidthInteger = (TextView) v.findViewById(R.id.choose_account_pro_i_bandwidth_value_integer);
        pro1BandwidthTb = (TextView) v.findViewById(R.id.choose_account_pro_i_bandwith_value_tb);

        pro1TransparentLayout = (RelativeLayout) v.findViewById(R.id.choose_account_pro_i_layout_transparent);
        pro1TransparentLayout.setVisibility(View.INVISIBLE);
        //END -- PRO I ACCOUNT

        //PRO II ACCOUNT
        pro2Layout = (RelativeLayout) v.findViewById(R.id.choose_account_pro_ii_layout);
        pro2Layout.setOnClickListener(this);
        LinearLayout.LayoutParams pro2LayoutParams = (LinearLayout.LayoutParams) pro2Layout.getLayoutParams();
        pro2LayoutParams.setMargins(Util.scaleWidthPx(8, outMetrics), Util.scaleHeightPx(8, outMetrics), Util.scaleWidthPx(8, outMetrics), 0);
        pro2Layout.setLayoutParams(pro2LayoutParams);

        leftPro2Layout = (RelativeLayout) v.findViewById(R.id.choose_account_pro_ii_left_side);
        RelativeLayout.LayoutParams leftPro2LayoutParams = (RelativeLayout.LayoutParams) leftPro2Layout.getLayoutParams();
        leftPro2LayoutParams.width = Util.scaleWidthPx(125, outMetrics);
        leftPro2LayoutParams.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
        leftPro2Layout.setLayoutParams(leftPro2LayoutParams);

        //Replace elevation
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            pro2Layout.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        }

        titlePro2 = (TextView) v.findViewById(R.id.choose_account_pro_ii_title_text);
        RelativeLayout.LayoutParams titlePro2Params = (RelativeLayout.LayoutParams) titlePro2.getLayoutParams();
        titlePro2Params.setMargins(0,0,0,Util.scaleHeightPx(11, outMetrics));
        titlePro2.setLayoutParams(titlePro2Params);

        verticalDividerPro2 = (View) v.findViewById(R.id.choose_account_pro_ii_vertical_divider);
        verticalDividerPro2.getLayoutParams().width = Util.scaleWidthPx(2, outMetrics);
        verticalDividerPro2.getLayoutParams().height = Util.scaleHeightPx(86, outMetrics);

        rightPro2Layout = (RelativeLayout) v.findViewById(R.id.choose_account_pro_ii_layout_right_side);
        RelativeLayout.LayoutParams rightLayoutPro2Params = (RelativeLayout.LayoutParams) rightPro2Layout.getLayoutParams();
        rightLayoutPro2Params.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
        rightPro2Layout.setLayoutParams(rightLayoutPro2Params);

        tableRowPro2 = (TableRow) v.findViewById(R.id.table_row_pro_ii);
        TableLayout.LayoutParams tableRowPro2Params = (TableLayout.LayoutParams) tableRowPro2.getLayoutParams();
        tableRowPro2Params.setMargins(0,0,0,Util.scaleHeightPx(25, outMetrics));
        tableRowPro2.setLayoutParams(tableRowPro2Params);

        storageValuePro2 = (TextView) v.findViewById(R.id.choose_account_pro_ii_storage_value_integer);
        TableRow.LayoutParams storageValuePro2Params = (TableRow.LayoutParams) storageValuePro2.getLayoutParams();
        storageValuePro2Params.width = Util.scaleWidthPx(40, outMetrics);
        storageValuePro2.setLayoutParams(storageValuePro2Params);

        emptyTextPro2 = (TextView) v.findViewById(R.id.choose_account_pro_ii_empty_text);
        TableRow.LayoutParams emptyTextPro2Params = (TableRow.LayoutParams) emptyTextPro2.getLayoutParams();
        emptyTextPro2Params.width = Util.scaleWidthPx(12, outMetrics);
        emptyTextPro2.setLayoutParams(emptyTextPro2Params);

        bandwidthValuePro2 = (TextView) v.findViewById(R.id.choose_account_pro_ii_bandwidth_value_integer);
        TableRow.LayoutParams bandwidthValuePro2Params = (TableRow.LayoutParams) bandwidthValuePro2.getLayoutParams();
        bandwidthValuePro2Params.width = Util.scaleWidthPx(40, outMetrics);
        bandwidthValuePro2.setLayoutParams(bandwidthValuePro2Params);
//
        emptyTextBandwidthPro2 = (TextView) v.findViewById(R.id.choose_account_pro_ii_empty_text_bandwidth);
        TableRow.LayoutParams emptyTextBandwidthPro2Params = (TableRow.LayoutParams) emptyTextBandwidthPro2.getLayoutParams();
        emptyTextBandwidthPro2Params.width = Util.scaleWidthPx(12, outMetrics);
        emptyTextBandwidthPro2.setLayoutParams(emptyTextBandwidthPro2Params);

        perMonth = (TextView) v.findViewById(R.id.choose_account_pro_ii_per_month_text);
        perMonth.setText("/" + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));

        RelativeLayout.LayoutParams perMonthPro2Params = (RelativeLayout.LayoutParams) perMonth.getLayoutParams();
        perMonthPro2Params.setMargins(0,0,0,Util.scaleHeightPx(3, outMetrics));
        perMonth.setLayoutParams(perMonthPro2Params);

        pro2PriceInteger = (TextView) v.findViewById(R.id.choose_account_pro_ii_integer_text);
        pro2PriceDecimal = (TextView) v.findViewById(R.id.choose_account_pro_ii_decimal_text);

        RelativeLayout.LayoutParams priceDecimalPro2Params = (RelativeLayout.LayoutParams) pro2PriceDecimal.getLayoutParams();
        priceDecimalPro2Params.setMargins(0,0,0,Util.scaleHeightPx(3, outMetrics));
        pro2PriceDecimal.setLayoutParams(priceDecimalPro2Params);

        pro2StorageInteger = (TextView) v.findViewById(R.id.choose_account_pro_ii_storage_value_integer);
        pro2StorageGb = (TextView) v.findViewById(R.id.choose_account_pro_ii_storage_value_gb);
        pro2BandwidthInteger = (TextView) v.findViewById(R.id.choose_account_pro_ii_bandwidth_value_integer);
        pro2BandwidthTb = (TextView) v.findViewById(R.id.choose_account_pro_ii_bandwith_value_tb);

        pro2TransparentLayout = (RelativeLayout) v.findViewById(R.id.choose_account_pro_ii_layout_transparent);
        pro2TransparentLayout.setVisibility(View.INVISIBLE);
        //END -- PRO II ACCOUNT

        //PRO III ACCOUNT
        pro3Layout = (RelativeLayout) v.findViewById(R.id.choose_account_pro_iii_layout);
        pro3Layout.setOnClickListener(this);

        LinearLayout.LayoutParams pro3LayoutParams = (LinearLayout.LayoutParams) pro3Layout.getLayoutParams();
        pro3LayoutParams.setMargins(Util.scaleWidthPx(8, outMetrics), Util.scaleHeightPx(8, outMetrics), Util.scaleWidthPx(8, outMetrics), Util.scaleHeightPx(5, outMetrics));
        pro3Layout.setLayoutParams(pro3LayoutParams);

        //Replace elevation
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            pro3Layout.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        }

        leftPro3Layout = (RelativeLayout) v.findViewById(R.id.choose_account_pro_iii_left_side);
        RelativeLayout.LayoutParams leftPro3LayoutParams = (RelativeLayout.LayoutParams) leftPro3Layout.getLayoutParams();
        leftPro3LayoutParams.width = Util.scaleWidthPx(125, outMetrics);
        leftPro3LayoutParams.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
        leftPro3Layout.setLayoutParams(leftPro3LayoutParams);

        titlePro3 = (TextView) v.findViewById(R.id.choose_account_pro_iii_title_text);
        RelativeLayout.LayoutParams titlePro3Params = (RelativeLayout.LayoutParams) titlePro3.getLayoutParams();
        titlePro3Params.setMargins(0,0,0,Util.scaleHeightPx(11, outMetrics));
        titlePro3.setLayoutParams(titlePro3Params);

        verticalDividerPro3 = (View) v.findViewById(R.id.choose_account_pro_iii_vertical_divider);
        verticalDividerPro3.getLayoutParams().width = Util.scaleWidthPx(2, outMetrics);
        verticalDividerPro3.getLayoutParams().height = Util.scaleHeightPx(86, outMetrics);

        rightPro3Layout = (RelativeLayout) v.findViewById(R.id.choose_account_pro_iii_layout_right_side);
        RelativeLayout.LayoutParams rightLayoutPro3Params = (RelativeLayout.LayoutParams) rightPro3Layout.getLayoutParams();
        rightLayoutPro3Params.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
        rightPro3Layout.setLayoutParams(rightLayoutPro3Params);

        tableRowPro3 = (TableRow) v.findViewById(R.id.table_row_pro_iii);
        TableLayout.LayoutParams tableRowPro3Params = (TableLayout.LayoutParams) tableRowPro3.getLayoutParams();
        tableRowPro3Params.setMargins(0,0,0,Util.scaleHeightPx(25, outMetrics));
        tableRowPro3.setLayoutParams(tableRowPro3Params);

        storageValuePro3 = (TextView) v.findViewById(R.id.choose_account_pro_iii_storage_value_integer);
        TableRow.LayoutParams storageValuePro3Params = (TableRow.LayoutParams) storageValuePro3.getLayoutParams();
        storageValuePro3Params.width = Util.scaleWidthPx(40, outMetrics);
        storageValuePro3.setLayoutParams(storageValuePro3Params);

        emptyTextPro3 = (TextView) v.findViewById(R.id.choose_account_pro_iii_empty_text);
        TableRow.LayoutParams emptyTextPro3Params = (TableRow.LayoutParams) emptyTextPro3.getLayoutParams();
        emptyTextPro3Params.width = Util.scaleWidthPx(12, outMetrics);
        emptyTextPro3.setLayoutParams(emptyTextPro3Params);

        bandwidthValuePro3 = (TextView) v.findViewById(R.id.choose_account_pro_iii_bandwidth_value_integer);
        TableRow.LayoutParams bandwidthValuePro3Params = (TableRow.LayoutParams) bandwidthValuePro3.getLayoutParams();
        bandwidthValuePro3Params.width = Util.scaleWidthPx(40, outMetrics);
        bandwidthValuePro3.setLayoutParams(bandwidthValuePro3Params);
//
        emptyTextBandwidthPro3 = (TextView) v.findViewById(R.id.choose_account_pro_iii_empty_text_bandwidth);
        TableRow.LayoutParams emptyTextBandwidthPro3Params = (TableRow.LayoutParams) emptyTextBandwidthPro3.getLayoutParams();
        emptyTextBandwidthPro3Params.width = Util.scaleWidthPx(12, outMetrics);
        emptyTextBandwidthPro3.setLayoutParams(emptyTextBandwidthPro3Params);

        perMonth = (TextView) v.findViewById(R.id.choose_account_pro_iii_per_month_text);
        perMonth.setText("/" + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));

        RelativeLayout.LayoutParams perMonthPro3Params = (RelativeLayout.LayoutParams) perMonth.getLayoutParams();
        perMonthPro3Params.setMargins(0,0,0,Util.scaleHeightPx(3, outMetrics));
        perMonth.setLayoutParams(perMonthPro3Params);

        pro3PriceInteger = (TextView) v.findViewById(R.id.choose_account_pro_iii_integer_text);
        pro3PriceDecimal = (TextView) v.findViewById(R.id.choose_account_pro_iii_decimal_text);

        RelativeLayout.LayoutParams priceDecimalPro3Params = (RelativeLayout.LayoutParams) pro3PriceDecimal.getLayoutParams();
        priceDecimalPro3Params.setMargins(0,0,0,Util.scaleHeightPx(3, outMetrics));
        pro3PriceDecimal.setLayoutParams(priceDecimalPro3Params);

        pro3StorageInteger = (TextView) v.findViewById(R.id.choose_account_pro_iii_storage_value_integer);
        pro3StorageGb = (TextView) v.findViewById(R.id.choose_account_pro_iii_storage_value_gb);
        pro3BandwidthInteger = (TextView) v.findViewById(R.id.choose_account_pro_iii_bandwidth_value_integer);
        pro3BandwidthTb = (TextView) v.findViewById(R.id.choose_account_pro_iii_bandwith_value_tb);

        pro3TransparentLayout = (RelativeLayout) v.findViewById(R.id.choose_account_pro_iii_layout_transparent);
        pro3TransparentLayout.setVisibility(View.INVISIBLE);
        //END -- PRO III ACCOUNT

        setPricingInfo();

        return v;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.choose_account_free_layout:{
                onFreeClick(v);
                break;
            }
            case R.id.choose_account_prolite_layout:{
                onUpgradeLiteClick(v);
                break;
            }
            case R.id.choose_account_pro_i_layout:{
                onUpgrade1Click(v);
                break;
            }
            case R.id.choose_account_pro_ii_layout:{
                onUpgrade2Click(v);
                break;
            }
            case R.id.choose_account_pro_iii_layout:{
                onUpgrade3Click(v);
                break;
            }
        }
    }


    public void onFreeClick (View view){
        log("onFreeClick");

        Intent intent = null;
        intent = new Intent(context,ManagerActivityLollipop.class);
        intent.putExtra("firstTimeCam", true);
        intent.putExtra("upgradeAccount", false);
        intent.putExtra("newAccount", true);
        startActivity(intent);
        ((LoginActivityLollipop)context).finish();
    }

    public void onUpgrade1Click(View view) {
//		((ManagerActivity)context).showpF(1, accounts);
        log("onUpgrade1Click");

        Intent intent = null;
        intent = new Intent(context,ManagerActivityLollipop.class);
        intent.putExtra("upgradeAccount", true);
        intent.putExtra("accountType", Constants.PRO_I);
        intent.putExtra("newAccount", true);
        startActivity(intent);
        ((LoginActivityLollipop)context).finish();
    }

    public void onUpgrade2Click(View view) {
//		((ManagerActivity)context).showpF(2, accounts);
        log("onUpgrade2Click");

        Intent intent = null;
        intent = new Intent(context,ManagerActivityLollipop.class);
        intent.putExtra("upgradeAccount", true);
        intent.putExtra("accountType", Constants.PRO_II);
        intent.putExtra("newAccount", true);
        startActivity(intent);
        ((LoginActivityLollipop)context).finish();
    }

    public void onUpgrade3Click(View view) {
//		((ManagerActivity)context).showpF(3, accounts);
        log("onUpgrade3Click");

        Intent intent = null;
        intent = new Intent(context,ManagerActivityLollipop.class);
        intent.putExtra("upgradeAccount", true);
        intent.putExtra("accountType", Constants.PRO_III);
        intent.putExtra("newAccount", true);
        startActivity(intent);
        ((LoginActivityLollipop)context).finish();
    }

    public void onUpgradeLiteClick(View view){
//		((ManagerActivity)context).showpF(4, accounts);
        log("onUpgradeLiteClick");

        Intent intent = null;
        intent = new Intent(context,ManagerActivityLollipop.class);
        intent.putExtra("upgradeAccount", true);
        intent.putExtra("accountType", Constants.PRO_LITE);
        intent.putExtra("newAccount", true);
        startActivity(intent);
        ((LoginActivityLollipop)context).finish();
    }

    public String sizeTranslation(long size, int type) {
        switch(type){
            case 0:{
                //From GB to TB
                if(size!=1024){
                    size=size/1024;
                }

                String value = new DecimalFormat("#").format(size);
                return value;
            }
        }
        return null;
    }

    public void setPricingInfo(){
        log("setPricingInfo");

        DecimalFormat df = new DecimalFormat("#.##");
        MyAccountInfo myAccountInfo = ((MegaApplication) ((Activity)context).getApplication()).getMyAccountInfo();

        if(myAccountInfo==null)
            return;

        MegaPricing p = myAccountInfo.getPricing();
        if(p==null){
            log("Return - getPricing NULL");
            return;
        }

        freePriceInteger.setText("0");
        freePriceDecimal.setText("." + "00 €");

        freeStorageInteger.setVisibility(View.GONE);
        freeStorageGb.setText("50 GB");

        freeBandwidthInteger.setVisibility(View.GONE);
        freeBandwidthTb.setText(getString(R.string.limited_bandwith));

        for (int i=0;i<p.getNumProducts();i++){
            log("p["+ i +"] = " + p.getHandle(i) + "__" + p.getAmount(i) + "___" + p.getGBStorage(i) + "___" + p.getMonths(i) + "___" + p.getProLevel(i) + "___" + p.getGBTransfer(i));

            Product account = new Product (p.getHandle(i), p.getProLevel(i), p.getMonths(i), p.getGBStorage(i), p.getAmount(i), p.getGBTransfer(i));

            if(account.getLevel()==1&&account.getMonths()==1){
                log("PRO1: "+account.getStorage());

                double price = account.getAmount()/100.00;
                String priceString = df.format(price);
                String [] s = priceString.split("\\.");
                if (s.length == 1){
                    String [] s1 = priceString.split(",");
                    if (s1.length == 1){
                        pro1PriceInteger.setText(s1[0]);
                        pro1PriceDecimal.setText("");
                    }
                    else if (s1.length == 2){
                        pro1PriceInteger.setText(s1[0]);
                        pro1PriceDecimal.setText("." + s1[1] + " €");
                    }
                }
                else if (s.length == 2){
                    pro1PriceInteger.setText(s[0]);
                    pro1PriceDecimal.setText("." + s[1] + " €");
                }

                pro1StorageInteger.setText(""+account.getStorage()/1024);
                pro1StorageGb.setText(" TB");

                pro1BandwidthInteger.setText(""+account.getTransfer()/1024);
                pro1BandwidthTb.setText(" TB");
            }
            else if(account.getLevel()==2&&account.getMonths()==1){
                log("PRO2: "+account.getStorage());

                double price = account.getAmount()/100.00;
                String priceString = df.format(price);
                String [] s = priceString.split("\\.");
                if (s.length == 1){
                    String [] s1 = priceString.split(",");
                    if (s1.length == 1){
                        pro2PriceInteger.setText(s1[0]);
                        pro2PriceDecimal.setText("");
                    }
                    else if (s1.length == 2){
                        pro2PriceInteger.setText(s1[0]);
                        pro2PriceDecimal.setText("." + s1[1] + " €");
                    }
                }
                else if (s.length == 2){
                    pro2PriceInteger.setText(s[0]);
                    pro2PriceDecimal.setText("." + s[1] + " €");
                }

                pro2StorageInteger.setText(sizeTranslation(account.getStorage(),0));
                pro2StorageGb.setText(" TB");

                pro2BandwidthInteger.setText(""+account.getTransfer()/1024);
                pro2BandwidthTb.setText(" TB");
            }
            else if(account.getLevel()==3&&account.getMonths()==1){
                log("PRO3: "+account.getStorage());

                double price = account.getAmount()/100.00;
                String priceString = df.format(price);
                String [] s = priceString.split("\\.");
                if (s.length == 1){
                    String [] s1 = priceString.split(",");
                    if (s1.length == 1){
                        pro3PriceInteger.setText(s1[0]);
                        pro3PriceDecimal.setText("");
                    }
                    else if (s1.length == 2){
                        pro3PriceInteger.setText(s1[0]);
                        pro3PriceDecimal.setText("." + s1[1] + " €");
                    }
                }
                else if (s.length == 2){
                    pro3PriceInteger.setText(s[0]);
                    pro3PriceDecimal.setText("." + s[1] + " €");
                }

                pro3StorageInteger.setText(sizeTranslation(account.getStorage(),0));
                pro3StorageGb.setText(" TB");

                pro3BandwidthInteger.setText(""+account.getTransfer()/1024);
                pro3BandwidthTb.setText(" TB");
            }
            else if (account.getLevel()==4&&account.getMonths()==1){
                log("Lite: "+account.getStorage());

                double price = account.getAmount()/100.00;
                String priceString = df.format(price);
                String [] s = priceString.split("\\.");
                if (s.length == 1){
                    String [] s1 = priceString.split(",");
                    if (s1.length == 1){
                        proLitePriceInteger.setText(s1[0]);
                        proLitePriceDecimal.setText("");
                    }
                    else if (s1.length == 2){
                        proLitePriceInteger.setText(s1[0]);
                        proLitePriceDecimal.setText("." + s1[1] + " €");
                    }
                }
                else if (s.length == 2){
                    proLitePriceInteger.setText(s[0]);
                    proLitePriceDecimal.setText("." + s[1] + " €");
                }

                proLiteStorageInteger.setText(""+account.getStorage());
                proLiteStorageGb.setText(" GB");

                proLiteBandwidthInteger.setText(""+account.getTransfer()/1024);
                proLiteBandwidthTb.setText(" TB");
            }
            accounts.add(account);
        }
        //			/*RESULTS
//            p[0] = 1560943707714440503__999___500___1___1___1024 - PRO 1 montly
//    		p[1] = 7472683699866478542__9999___500___12___1___12288 - PRO 1 annually
//    		p[2] = 7974113413762509455__1999___2048___1___2___4096  - PRO 2 montly
//    		p[3] = 370834413380951543__19999___2048___12___2___49152 - PRO 2 annually
//    		p[4] = -2499193043825823892__2999___4096___1___3___8192 - PRO 3 montly
//    		p[5] = 7225413476571973499__29999___4096___12___3___98304 - PRO 3 annually*/
    }



    @Override
    public void onAttach(Context context) {
        log("onAttach");
        super.onAttach(context);
        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    @Override
    public void onAttach(Activity context) {
        log("onAttach Activity");
        super.onAttach(context);
        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    public static void log(String message) {
        Util.log("ChooseAccountFragmentLollipop", message);
    }

}
