package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.DBUtil;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

public class UpgradeAccountFragmentLollipop extends Fragment implements OnClickListener{

	static int HEIGHT_ACCOUNT_LAYOUT=109;

	static int HEIGHT_PAYMENT_METHODS_LAYOUT=50;

//	View v = null;
	private MegaApiAndroid megaApi;
	public MyAccountInfo myAccountInfo;

	DisplayMetrics outMetrics;

	private ScrollView scrollView;
	private LinearLayout linearLayoutMain;
	
	private RelativeLayout proLiteLayout;
	private RelativeLayout pro1Layout;
	private RelativeLayout pro3Layout;
	private RelativeLayout pro2Layout;

	private RelativeLayout proLiteLayoutContent;

	private RelativeLayout leftProLiteLayout;
	private RelativeLayout leftPro1Layout;
	private RelativeLayout leftPro3Layout;
	private RelativeLayout leftPro2Layout;

	private TextView titleProLite;
	private TextView titlePro1;
	private TextView titlePro2;
	private TextView titlePro3;

	private View verticalDividerProLite;
	private View verticalDividerPro1;
	private View verticalDividerPro2;
	private View verticalDividerPro3;

	private RelativeLayout rightProLiteLayout;
	private RelativeLayout rightPro1Layout;
	private RelativeLayout rightPro3Layout;
	private RelativeLayout rightPro2Layout;

	private TableRow tableRowProLite;
	private TableRow tableRowPro1;
	private TableRow tableRowPro2;
	private TableRow tableRowPro3;

	private TextView storageValueProLite;
	private TextView storageValuePro1;
	private TextView storageValuePro2;
	private TextView storageValuePro3;

	private TextView emptyTextProLite;
	private TextView emptyTextPro1;
	private TextView emptyTextPro2;
	private TextView emptyTextPro3;

	private TextView bandwidthValueProLite;
	private TextView bandwidthValuePro1;
	private TextView bandwidthValuePro2;
	private TextView bandwidthValuePro3;

	private TextView emptyTextBandwidthProLite;
	private TextView emptyTextBandwidthPro1;
	private TextView emptyTextBandwidthPro2;
	private TextView emptyTextBandwidthPro3;

	private RelativeLayout proLiteTransparentLayout;
	private RelativeLayout pro1TransparentLayout;
	private RelativeLayout pro3TransparentLayout;
	private RelativeLayout pro2TransparentLayout;
	
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

	//Payment layout
	View selectPaymentMethodLayoutLite;
	View selectPaymentMethodLayoutPro1;
	View selectPaymentMethodLayoutPro2;
	View selectPaymentMethodLayoutPro3;
	RelativeLayout closeLayout;
	private TextView selectPaymentMethod;

	RelativeLayout googlePlayLayout;
	RelativeLayout creditCardLayout;
	RelativeLayout fortumoLayout;
	RelativeLayout centiliLayout;

	ImageView closeIcon;
	ImageView fortumoIcon;
	TextView fortumoText;
	ImageView centiliIcon;
	TextView centiliText;
	ImageView creditCardIcon;
	TextView creditCardText;
	ImageView googleWalletIcon;
	TextView googleWalletText;

	Context context;

	@Override
	public void onDestroy(){				

		super.onDestroy();
	}
	
	@Override
	public void onCreate (Bundle savedInstanceState){
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		super.onCreate(savedInstanceState);
		log("onCreate");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		log("onCreateView");

		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}

		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		float density = ((Activity) context).getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);

		View v = inflater.inflate(R.layout.fragment_upgrade_account, container, false);

		scrollView = (ScrollView) v.findViewById(R.id.scroll_view_upgrade);
		linearLayoutMain = (LinearLayout) v.findViewById(R.id.linear_layout_upgrade);

		//Replace elevation
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			scrollView.setBackgroundColor(ContextCompat.getColor(context, R.color.grid_item_separator));
			linearLayoutMain.setBackgroundColor(ContextCompat.getColor(context, R.color.grid_item_separator));
		}

		//PRO LITE ACCOUNT
		proLiteLayout = (RelativeLayout) v.findViewById(R.id.upgrade_prolite_layout);
		proLiteLayout.setOnClickListener(this);
		LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) proLiteLayout.getLayoutParams();
		layoutParams.setMargins(Util.scaleWidthPx(8, outMetrics), Util.scaleHeightPx(8, outMetrics), Util.scaleWidthPx(8, outMetrics), 0);
		proLiteLayout.setLayoutParams(layoutParams);

		proLiteLayoutContent = (RelativeLayout) v.findViewById(R.id.upgrade_prolite_layout_content);

		leftProLiteLayout = (RelativeLayout) v.findViewById(R.id.upgrade_prolite_left_side);
		RelativeLayout.LayoutParams leftLayoutParams = (RelativeLayout.LayoutParams) leftProLiteLayout.getLayoutParams();
		leftLayoutParams.width = Util.scaleWidthPx(125, outMetrics);
		leftLayoutParams.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
		leftProLiteLayout.setLayoutParams(leftLayoutParams);

		//Replace elevation
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			proLiteLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
		}

		titleProLite = (TextView) v.findViewById(R.id.upgrade_prolite_title_text);
		RelativeLayout.LayoutParams titleParams = (RelativeLayout.LayoutParams) titleProLite.getLayoutParams();
		titleParams.setMargins(0,0,0,Util.scaleHeightPx(11, outMetrics));
		titleProLite.setLayoutParams(titleParams);

		verticalDividerProLite = (View) v.findViewById(R.id.upgrade_prolite_vertical_divider);
		verticalDividerProLite.getLayoutParams().width = Util.scaleWidthPx(2, outMetrics);
		verticalDividerProLite.getLayoutParams().height = Util.scaleHeightPx(86, outMetrics);

		rightProLiteLayout = (RelativeLayout) v.findViewById(R.id.upgrade_prolite_layout_right_side);
		RelativeLayout.LayoutParams rightLayoutParams = (RelativeLayout.LayoutParams) rightProLiteLayout.getLayoutParams();
		rightLayoutParams.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
		rightProLiteLayout.setLayoutParams(rightLayoutParams);

		tableRowProLite = (TableRow) v.findViewById(R.id.table_row_pro_lite);
		TableLayout.LayoutParams tableRowParams = (TableLayout.LayoutParams) tableRowProLite.getLayoutParams();
		tableRowParams.setMargins(0,0,0,Util.scaleHeightPx(25, outMetrics));
		tableRowProLite.setLayoutParams(tableRowParams);

		storageValueProLite = (TextView) v.findViewById(R.id.upgrade_prolite_storage_value_integer);
		TableRow.LayoutParams storageValueParams = (TableRow.LayoutParams) storageValueProLite.getLayoutParams();
		storageValueParams.width = Util.scaleWidthPx(40, outMetrics);
		storageValueProLite.setLayoutParams(storageValueParams);

		emptyTextProLite = (TextView) v.findViewById(R.id.upgrade_prolite_empty_text);
		TableRow.LayoutParams emptyTextParams = (TableRow.LayoutParams) emptyTextProLite.getLayoutParams();
		emptyTextParams.width = Util.scaleWidthPx(12, outMetrics);
		emptyTextProLite.setLayoutParams(emptyTextParams);

		bandwidthValueProLite = (TextView) v.findViewById(R.id.upgrade_prolite_bandwidth_value_integer);
		TableRow.LayoutParams bandwidthValueParams = (TableRow.LayoutParams) bandwidthValueProLite.getLayoutParams();
		bandwidthValueParams.width = Util.scaleWidthPx(40, outMetrics);
		bandwidthValueProLite.setLayoutParams(bandwidthValueParams);

		emptyTextBandwidthProLite = (TextView) v.findViewById(R.id.upgrade_prolite_empty_text_bandwidth);
		TableRow.LayoutParams emptyTextBandwidthParams = (TableRow.LayoutParams) emptyTextBandwidthProLite.getLayoutParams();
		emptyTextBandwidthParams.width = Util.scaleWidthPx(12, outMetrics);
		emptyTextBandwidthProLite.setLayoutParams(emptyTextBandwidthParams);

		TextView perMonth = (TextView) v.findViewById(R.id.upgrade_prolite_per_month_text);
		perMonth.setText("/" + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));
		RelativeLayout.LayoutParams perMonthParams = (RelativeLayout.LayoutParams) perMonth.getLayoutParams();
		perMonthParams.setMargins(0,0,0,Util.scaleHeightPx(3, outMetrics));
		perMonth.setLayoutParams(perMonthParams);

		proLitePriceInteger = (TextView) v.findViewById(R.id.upgrade_prolite_integer_text);
		proLitePriceDecimal = (TextView) v.findViewById(R.id.upgrade_prolite_decimal_text);
		RelativeLayout.LayoutParams priceDecimalParams = (RelativeLayout.LayoutParams) proLitePriceDecimal.getLayoutParams();
		priceDecimalParams.setMargins(0,0,0,Util.scaleHeightPx(3, outMetrics));
		proLitePriceDecimal.setLayoutParams(priceDecimalParams);

		proLiteStorageInteger = (TextView) v.findViewById(R.id.upgrade_prolite_storage_value_integer);
		proLiteStorageGb = (TextView) v.findViewById(R.id.upgrade_prolite_storage_value_gb);
		proLiteBandwidthInteger = (TextView) v.findViewById(R.id.upgrade_prolite_bandwidth_value_integer);
		proLiteBandwidthTb = (TextView) v.findViewById(R.id.upgrade_prolite_bandwith_value_tb);

		selectPaymentMethodLayoutLite =v.findViewById(R.id.available_payment_methods_prolite);

		proLiteTransparentLayout = (RelativeLayout) v.findViewById(R.id.upgrade_prolite_layout_transparent);
		proLiteTransparentLayout.setVisibility(View.INVISIBLE);
		//END -- PRO LITE ACCOUNT

		//PRO I ACCOUNT
		pro1Layout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_i_layout);
		pro1Layout.setOnClickListener(this);
		LinearLayout.LayoutParams pro1LayoutParams = (LinearLayout.LayoutParams) pro1Layout.getLayoutParams();
		pro1LayoutParams.setMargins(Util.scaleWidthPx(8, outMetrics), Util.scaleHeightPx(8, outMetrics), Util.scaleWidthPx(8, outMetrics), 0);
		pro1Layout.setLayoutParams(pro1LayoutParams);

		leftPro1Layout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_i_left_side);
		RelativeLayout.LayoutParams leftPro1LayoutParams = (RelativeLayout.LayoutParams) leftPro1Layout.getLayoutParams();
		leftPro1LayoutParams.width = Util.scaleWidthPx(125, outMetrics);
		leftPro1LayoutParams.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
		leftPro1Layout.setLayoutParams(leftPro1LayoutParams);

		//Replace elevation
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			pro1Layout.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
		}

		titlePro1 = (TextView) v.findViewById(R.id.upgrade_pro_i_title_text);
		RelativeLayout.LayoutParams titlePro1Params = (RelativeLayout.LayoutParams) titlePro1.getLayoutParams();
		titlePro1Params.setMargins(0,0,0,Util.scaleHeightPx(11, outMetrics));
		titlePro1.setLayoutParams(titlePro1Params);

		verticalDividerPro1 = (View) v.findViewById(R.id.upgrade_pro_i_vertical_divider);
		verticalDividerPro1.getLayoutParams().width = Util.scaleWidthPx(2, outMetrics);
		verticalDividerPro1.getLayoutParams().height = Util.scaleHeightPx(86, outMetrics);

		rightPro1Layout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_i_layout_right_side);
		RelativeLayout.LayoutParams rightLayoutPro1Params = (RelativeLayout.LayoutParams) rightPro1Layout.getLayoutParams();
		rightLayoutPro1Params.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
		rightPro1Layout.setLayoutParams(rightLayoutPro1Params);

		tableRowPro1 = (TableRow) v.findViewById(R.id.table_row_pro_i);
		TableLayout.LayoutParams tableRowPro1Params = (TableLayout.LayoutParams) tableRowPro1.getLayoutParams();
		tableRowPro1Params.setMargins(0,0,0,Util.scaleHeightPx(25, outMetrics));
		tableRowPro1.setLayoutParams(tableRowPro1Params);

		storageValuePro1 = (TextView) v.findViewById(R.id.upgrade_pro_i_storage_value_integer);
		TableRow.LayoutParams storageValuePro1Params = (TableRow.LayoutParams) storageValuePro1.getLayoutParams();
		storageValuePro1Params.width = Util.scaleWidthPx(40, outMetrics);
		storageValuePro1.setLayoutParams(storageValuePro1Params);

		emptyTextPro1 = (TextView) v.findViewById(R.id.upgrade_pro_i_empty_text);
		TableRow.LayoutParams emptyTextPro1Params = (TableRow.LayoutParams) emptyTextPro1.getLayoutParams();
		emptyTextPro1Params.width = Util.scaleWidthPx(12, outMetrics);
		emptyTextPro1.setLayoutParams(emptyTextPro1Params);

		bandwidthValuePro1 = (TextView) v.findViewById(R.id.upgrade_pro_i_bandwidth_value_integer);
		TableRow.LayoutParams bandwidthValuePro1Params = (TableRow.LayoutParams) bandwidthValuePro1.getLayoutParams();
		bandwidthValuePro1Params.width = Util.scaleWidthPx(40, outMetrics);
		bandwidthValuePro1.setLayoutParams(bandwidthValuePro1Params);

		emptyTextBandwidthPro1 = (TextView) v.findViewById(R.id.upgrade_pro_i_empty_text_bandwidth);
		TableRow.LayoutParams emptyTextBandwidthPro1Params = (TableRow.LayoutParams) emptyTextBandwidthPro1.getLayoutParams();
		emptyTextBandwidthPro1Params.width = Util.scaleWidthPx(12, outMetrics);
		emptyTextBandwidthPro1.setLayoutParams(emptyTextBandwidthPro1Params);

		perMonth = (TextView) v.findViewById(R.id.upgrade_pro_i_per_month_text);
		perMonth.setText("/" + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));

		RelativeLayout.LayoutParams perMonthPro1Params = (RelativeLayout.LayoutParams) perMonth.getLayoutParams();
		perMonthPro1Params.setMargins(0,0,0,Util.scaleHeightPx(3, outMetrics));
		perMonth.setLayoutParams(perMonthPro1Params);

		pro1PriceInteger = (TextView) v.findViewById(R.id.upgrade_pro_i_integer_text);

		pro1PriceDecimal = (TextView) v.findViewById(R.id.upgrade_pro_i_decimal_text);
		RelativeLayout.LayoutParams priceDecimalPro1Params = (RelativeLayout.LayoutParams) pro1PriceDecimal.getLayoutParams();
		priceDecimalPro1Params.setMargins(0,0,0,Util.scaleHeightPx(3, outMetrics));
		pro1PriceDecimal.setLayoutParams(priceDecimalPro1Params);

		pro1StorageInteger = (TextView) v.findViewById(R.id.upgrade_pro_i_storage_value_integer);
		pro1StorageGb = (TextView) v.findViewById(R.id.upgrade_pro_i_storage_value_gb);
		pro1BandwidthInteger = (TextView) v.findViewById(R.id.upgrade_pro_i_bandwidth_value_integer);
		pro1BandwidthTb = (TextView) v.findViewById(R.id.upgrade_pro_i_bandwith_value_tb);

		selectPaymentMethodLayoutPro1 =v.findViewById(R.id.available_payment_methods_pro_i);

		pro1TransparentLayout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_i_layout_transparent);
		pro1TransparentLayout.setVisibility(View.INVISIBLE);
		//END -- PRO I ACCOUNT

		//PRO II ACCOUNT
		pro2Layout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_ii_layout);
		pro2Layout.setOnClickListener(this);
		LinearLayout.LayoutParams pro2LayoutParams = (LinearLayout.LayoutParams) pro2Layout.getLayoutParams();
		pro2LayoutParams.setMargins(Util.scaleWidthPx(8, outMetrics), Util.scaleHeightPx(8, outMetrics), Util.scaleWidthPx(8, outMetrics), 0);
		pro2Layout.setLayoutParams(pro2LayoutParams);

		leftPro2Layout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_ii_left_side);
		RelativeLayout.LayoutParams leftPro2LayoutParams = (RelativeLayout.LayoutParams) leftPro2Layout.getLayoutParams();
		leftPro2LayoutParams.width = Util.scaleWidthPx(125, outMetrics);
		leftPro2LayoutParams.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
		leftPro2Layout.setLayoutParams(leftPro2LayoutParams);

		//Replace elevation
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			pro2Layout.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
		}

		titlePro2 = (TextView) v.findViewById(R.id.upgrade_pro_ii_title_text);
		RelativeLayout.LayoutParams titlePro2Params = (RelativeLayout.LayoutParams) titlePro2.getLayoutParams();
		titlePro2Params.setMargins(0,0,0,Util.scaleHeightPx(11, outMetrics));
		titlePro2.setLayoutParams(titlePro2Params);

		verticalDividerPro2 = (View) v.findViewById(R.id.upgrade_pro_ii_vertical_divider);
		verticalDividerPro2.getLayoutParams().width = Util.scaleWidthPx(2, outMetrics);
		verticalDividerPro2.getLayoutParams().height = Util.scaleHeightPx(86, outMetrics);

		rightPro2Layout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_ii_layout_right_side);
		RelativeLayout.LayoutParams rightLayoutPro2Params = (RelativeLayout.LayoutParams) rightPro2Layout.getLayoutParams();
		rightLayoutPro2Params.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
		rightPro2Layout.setLayoutParams(rightLayoutPro2Params);

		tableRowPro2 = (TableRow) v.findViewById(R.id.table_row_pro_ii);
		TableLayout.LayoutParams tableRowPro2Params = (TableLayout.LayoutParams) tableRowPro2.getLayoutParams();
		tableRowPro2Params.setMargins(0,0,0,Util.scaleHeightPx(25, outMetrics));
		tableRowPro2.setLayoutParams(tableRowPro2Params);

		storageValuePro2 = (TextView) v.findViewById(R.id.upgrade_pro_ii_storage_value_integer);
		TableRow.LayoutParams storageValuePro2Params = (TableRow.LayoutParams) storageValuePro2.getLayoutParams();
		storageValuePro2Params.width = Util.scaleWidthPx(40, outMetrics);
		storageValuePro2.setLayoutParams(storageValuePro2Params);

		emptyTextPro2 = (TextView) v.findViewById(R.id.upgrade_pro_ii_empty_text);
		TableRow.LayoutParams emptyTextPro2Params = (TableRow.LayoutParams) emptyTextPro2.getLayoutParams();
		emptyTextPro2Params.width = Util.scaleWidthPx(12, outMetrics);
		emptyTextPro2.setLayoutParams(emptyTextPro2Params);

		bandwidthValuePro2 = (TextView) v.findViewById(R.id.upgrade_pro_ii_bandwidth_value_integer);
		TableRow.LayoutParams bandwidthValuePro2Params = (TableRow.LayoutParams) bandwidthValuePro2.getLayoutParams();
		bandwidthValuePro2Params.width = Util.scaleWidthPx(40, outMetrics);
		bandwidthValuePro2.setLayoutParams(bandwidthValuePro2Params);
//
		emptyTextBandwidthPro2 = (TextView) v.findViewById(R.id.upgrade_pro_ii_empty_text_bandwidth);
		TableRow.LayoutParams emptyTextBandwidthPro2Params = (TableRow.LayoutParams) emptyTextBandwidthPro2.getLayoutParams();
		emptyTextBandwidthPro2Params.width = Util.scaleWidthPx(12, outMetrics);
		emptyTextBandwidthPro2.setLayoutParams(emptyTextBandwidthPro2Params);

		perMonth = (TextView) v.findViewById(R.id.upgrade_pro_ii_per_month_text);
		perMonth.setText("/" + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));

		RelativeLayout.LayoutParams perMonthPro2Params = (RelativeLayout.LayoutParams) perMonth.getLayoutParams();
		perMonthPro2Params.setMargins(0,0,0,Util.scaleHeightPx(3, outMetrics));
		perMonth.setLayoutParams(perMonthPro2Params);

		pro2PriceInteger = (TextView) v.findViewById(R.id.upgrade_pro_ii_integer_text);
		pro2PriceDecimal = (TextView) v.findViewById(R.id.upgrade_pro_ii_decimal_text);

		RelativeLayout.LayoutParams priceDecimalPro2Params = (RelativeLayout.LayoutParams) pro2PriceDecimal.getLayoutParams();
		priceDecimalPro2Params.setMargins(0,0,0,Util.scaleHeightPx(3, outMetrics));
		pro2PriceDecimal.setLayoutParams(priceDecimalPro2Params);

		pro2StorageInteger = (TextView) v.findViewById(R.id.upgrade_pro_ii_storage_value_integer);
		pro2StorageGb = (TextView) v.findViewById(R.id.upgrade_pro_ii_storage_value_gb);
		pro2BandwidthInteger = (TextView) v.findViewById(R.id.upgrade_pro_ii_bandwidth_value_integer);
		pro2BandwidthTb = (TextView) v.findViewById(R.id.upgrade_pro_ii_bandwith_value_tb);

		selectPaymentMethodLayoutPro2 =v.findViewById(R.id.available_payment_methods_pro_ii);

		pro2TransparentLayout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_ii_layout_transparent);
		pro2TransparentLayout.setVisibility(View.INVISIBLE);
		//END -- PRO II ACCOUNT

		//PRO III ACCOUNT
		pro3Layout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_iii_layout);
		pro3Layout.setOnClickListener(this);

		LinearLayout.LayoutParams pro3LayoutParams = (LinearLayout.LayoutParams) pro3Layout.getLayoutParams();
		pro3LayoutParams.setMargins(Util.scaleWidthPx(8, outMetrics), Util.scaleHeightPx(8, outMetrics), Util.scaleWidthPx(8, outMetrics), Util.scaleHeightPx(5, outMetrics));
		pro3Layout.setLayoutParams(pro3LayoutParams);

		leftPro3Layout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_iii_left_side);
		RelativeLayout.LayoutParams leftPro3LayoutParams = (RelativeLayout.LayoutParams) leftPro3Layout.getLayoutParams();
		leftPro3LayoutParams.width = Util.scaleWidthPx(125, outMetrics);
		leftPro3LayoutParams.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
		leftPro3Layout.setLayoutParams(leftPro3LayoutParams);

		//Replace elevation
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			pro3Layout.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
		}

		titlePro3 = (TextView) v.findViewById(R.id.upgrade_pro_iii_title_text);
		RelativeLayout.LayoutParams titlePro3Params = (RelativeLayout.LayoutParams) titlePro3.getLayoutParams();
		titlePro3Params.setMargins(0,0,0,Util.scaleHeightPx(11, outMetrics));
		titlePro2.setLayoutParams(titlePro3Params);

		verticalDividerPro3 = (View) v.findViewById(R.id.upgrade_pro_iii_vertical_divider);
		verticalDividerPro3.getLayoutParams().width = Util.scaleWidthPx(2, outMetrics);
		verticalDividerPro3.getLayoutParams().height = Util.scaleHeightPx(86, outMetrics);

		rightPro3Layout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_iii_layout_right_side);
		RelativeLayout.LayoutParams rightLayoutPro3Params = (RelativeLayout.LayoutParams) rightPro3Layout.getLayoutParams();
		rightLayoutPro3Params.height = Util.scaleHeightPx(HEIGHT_ACCOUNT_LAYOUT, outMetrics);
		rightPro3Layout.setLayoutParams(rightLayoutPro3Params);

		tableRowPro3 = (TableRow) v.findViewById(R.id.table_row_pro_iii);
		TableLayout.LayoutParams tableRowPro3Params = (TableLayout.LayoutParams) tableRowPro3.getLayoutParams();
		tableRowPro3Params.setMargins(0,0,0,Util.scaleHeightPx(25, outMetrics));
		tableRowPro3.setLayoutParams(tableRowPro3Params);

		storageValuePro3 = (TextView) v.findViewById(R.id.upgrade_pro_iii_storage_value_integer);
		TableRow.LayoutParams storageValuePro3Params = (TableRow.LayoutParams) storageValuePro3.getLayoutParams();
		storageValuePro3Params.width = Util.scaleWidthPx(40, outMetrics);
		storageValuePro3.setLayoutParams(storageValuePro3Params);

		emptyTextPro3 = (TextView) v.findViewById(R.id.upgrade_pro_iii_empty_text);
		TableRow.LayoutParams emptyTextPro3Params = (TableRow.LayoutParams) emptyTextPro3.getLayoutParams();
		emptyTextPro3Params.width = Util.scaleWidthPx(12, outMetrics);
		emptyTextPro3.setLayoutParams(emptyTextPro3Params);

		bandwidthValuePro3 = (TextView) v.findViewById(R.id.upgrade_pro_iii_bandwidth_value_integer);
		TableRow.LayoutParams bandwidthValuePro3Params = (TableRow.LayoutParams) bandwidthValuePro3.getLayoutParams();
		bandwidthValuePro3Params.width = Util.scaleWidthPx(40, outMetrics);
		bandwidthValuePro3.setLayoutParams(bandwidthValuePro3Params);
//
		emptyTextBandwidthPro3 = (TextView) v.findViewById(R.id.upgrade_pro_iii_empty_text_bandwidth);
		TableRow.LayoutParams emptyTextBandwidthPro3Params = (TableRow.LayoutParams) emptyTextBandwidthPro3.getLayoutParams();
		emptyTextBandwidthPro3Params.width = Util.scaleWidthPx(12, outMetrics);
		emptyTextBandwidthPro3.setLayoutParams(emptyTextBandwidthPro3Params);

		perMonth = (TextView) v.findViewById(R.id.upgrade_pro_iii_per_month_text);
		perMonth.setText("/" + getString(R.string.month_cc).toLowerCase(Locale.getDefault()));

		RelativeLayout.LayoutParams perMonthPro3Params = (RelativeLayout.LayoutParams) perMonth.getLayoutParams();
		perMonthPro3Params.setMargins(0,0,0,Util.scaleHeightPx(3, outMetrics));
		perMonth.setLayoutParams(perMonthPro3Params);

		pro3PriceInteger = (TextView) v.findViewById(R.id.upgrade_pro_iii_integer_text);
		pro3PriceDecimal = (TextView) v.findViewById(R.id.upgrade_pro_iii_decimal_text);

		RelativeLayout.LayoutParams priceDecimalPro3Params = (RelativeLayout.LayoutParams) pro3PriceDecimal.getLayoutParams();
		priceDecimalPro3Params.setMargins(0,0,0,Util.scaleHeightPx(3, outMetrics));
		pro3PriceDecimal.setLayoutParams(priceDecimalPro3Params);

		pro3StorageInteger = (TextView) v.findViewById(R.id.upgrade_pro_iii_storage_value_integer);
		pro3StorageGb = (TextView) v.findViewById(R.id.upgrade_pro_iii_storage_value_gb);
		pro3BandwidthInteger = (TextView) v.findViewById(R.id.upgrade_pro_iii_bandwidth_value_integer);
		pro3BandwidthTb = (TextView) v.findViewById(R.id.upgrade_pro_iii_bandwith_value_tb);

		selectPaymentMethodLayoutPro3 =v.findViewById(R.id.available_payment_methods_pro_iii);

		pro3TransparentLayout = (RelativeLayout) v.findViewById(R.id.upgrade_pro_iii_layout_transparent);
		pro3TransparentLayout.setVisibility(View.INVISIBLE);
		//END -- PRO III ACCOUNT

		setPricing();
		log("setPricing ENDS");
		showAvailableAccount();

		refreshAccountInfo();

		int displayedAccountType = ((ManagerActivityLollipop)context).getDisplayedAccountType();
		log("displayedAccountType: "+displayedAccountType);
		if(displayedAccountType!=-1){
			switch(displayedAccountType){
				case Constants.PRO_LITE:{
					onUpgradeClick(Constants.PRO_LITE);
					break;
				}
				case Constants.PRO_I:{
					onUpgradeClick(Constants.PRO_I);
					break;
				}
				case Constants.PRO_II:{
					onUpgradeClick(Constants.PRO_II);
					break;
				}
				case Constants.PRO_III:{
					onUpgradeClick(Constants.PRO_III);
					break;
				}
			}
		}

		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
		log("END onCreateView");
		return v;
	}

	public void refreshAccountInfo(){
		log("refreshAccountInfo");

		log("Check the last call to callToPricing");
		if(DBUtil.callToPricing(context)){
			log("megaApi.getPricing SEND");
			megaApi.getPricing(myAccountInfo);
		}

		log("Check the last call to callToPaymentMethods");
		if(DBUtil.callToPaymentMethods(context)){
			log("megaApi.getPaymentMethods SEND");
			megaApi.getPaymentMethods(myAccountInfo);
		}
	}

	public void setPricing(){
		log("setPricing");

		DecimalFormat df = new DecimalFormat("#.##");

		if(myAccountInfo!=null){
			ArrayList<Product> productAccounts = myAccountInfo.getProductAccounts();

			if (productAccounts == null){
				log("productAccounts == null");
				megaApi.getPricing(myAccountInfo);
				return;
			}

			for (int i = 0; i < productAccounts.size(); i++) {
				Product account = productAccounts.get(i);
				if (account.getLevel() == Constants.PRO_I && account.getMonths() == 1) {
					log("PRO1: " + account.getStorage());
					double price = account.getAmount() / 100.00;
					String priceString = df.format(price);
					String[] s = priceString.split("\\.");
					if (s.length == 1) {
						String[] s1 = priceString.split(",");
						if (s1.length == 1) {
							pro1PriceInteger.setText(s1[0]);
							pro1PriceDecimal.setText("");
						} else if (s1.length == 2) {
							pro1PriceInteger.setText(s1[0]);
							pro1PriceDecimal.setText("." + s1[1] + " €");
						}
					} else if (s.length == 2) {
						pro1PriceInteger.setText(s[0]);
						pro1PriceDecimal.setText("." + s[1] + " €");
					}

					pro1StorageInteger.setText("" + account.getStorage() / 1024);
					pro1StorageGb.setText(" TB");

					pro1BandwidthInteger.setText("" + account.getTransfer() / 1024);
					pro1BandwidthTb.setText(" TB");
				} else if (account.getLevel() == Constants.PRO_II && account.getMonths() == 1) {
					log("PRO2: " + account.getStorage());

					double price = account.getAmount() / 100.00;
					String priceString = df.format(price);
					String[] s = priceString.split("\\.");
					if (s.length == 1) {
						String[] s1 = priceString.split(",");
						if (s1.length == 1) {
							pro2PriceInteger.setText(s1[0]);
							pro2PriceDecimal.setText("");
						} else if (s1.length == 2) {
							pro2PriceInteger.setText(s1[0]);
							pro2PriceDecimal.setText("." + s1[1] + " €");
						}
					} else if (s.length == 2) {
						pro2PriceInteger.setText(s[0]);
						pro2PriceDecimal.setText("." + s[1] + " €");
					}

					pro2StorageInteger.setText(sizeTranslation(account.getStorage(), 0));
					pro2StorageGb.setText(" TB");

					pro2BandwidthInteger.setText("" + account.getTransfer() / 1024);
					pro2BandwidthTb.setText(" TB");
				} else if (account.getLevel() == Constants.PRO_III && account.getMonths() == 1) {
					log("PRO3: " + account.getStorage());

					double price = account.getAmount() / 100.00;
					String priceString = df.format(price);
					String[] s = priceString.split("\\.");
					if (s.length == 1) {
						String[] s1 = priceString.split(",");
						if (s1.length == 1) {
							pro3PriceInteger.setText(s1[0]);
							pro3PriceDecimal.setText("");
						} else if (s1.length == 2) {
							pro3PriceInteger.setText(s1[0]);
							pro3PriceDecimal.setText("." + s1[1] + " €");
						}
					} else if (s.length == 2) {
						pro3PriceInteger.setText(s[0]);
						pro3PriceDecimal.setText("." + s[1] + " €");
					}

					pro3StorageInteger.setText(sizeTranslation(account.getStorage(), 0));
					pro3StorageGb.setText(" TB");

					pro3BandwidthInteger.setText("" + account.getTransfer() / 1024);
					pro3BandwidthTb.setText(" TB");
				} else if (account.getLevel() == Constants.PRO_LITE && account.getMonths() == 1) {
					log("Lite: " + account.getStorage());

					double price = account.getAmount() / 100.00;
					String priceString = df.format(price);
					String[] s = priceString.split("\\.");
					if (s.length == 1) {
						String[] s1 = priceString.split(",");
						if (s1.length == 1) {
							proLitePriceInteger.setText(s1[0]);
							proLitePriceDecimal.setText("");
						} else if (s1.length == 2) {
							proLitePriceInteger.setText(s1[0]);
							proLitePriceDecimal.setText("." + s1[1] + " €");
						}
					} else if (s.length == 2) {
						proLitePriceInteger.setText(s[0]);
						proLitePriceDecimal.setText("." + s[1] + " €");
					}

					proLiteStorageInteger.setText("" + account.getStorage());
					proLiteStorageGb.setText(" GB");

					proLiteBandwidthInteger.setText("" + account.getTransfer() / 1024);
					proLiteBandwidthTb.setText(" TB");
				}
			}

			int displayedAccountType = ((ManagerActivityLollipop)context).getDisplayedAccountType();
			log("displayedAccountType: "+displayedAccountType);
			if(displayedAccountType!=-1){
				switch(displayedAccountType){
					case Constants.PRO_LITE:{
						onUpgradeClick(Constants.PRO_LITE);
						break;
					}
					case Constants.PRO_I:{
						onUpgradeClick(Constants.PRO_I);
						break;
					}
					case Constants.PRO_II:{
						onUpgradeClick(Constants.PRO_II);
						break;
					}
					case Constants.PRO_III:{
						onUpgradeClick(Constants.PRO_III);
						break;
					}
				}
			}
		}
		else{
			log("MyAccountInfo is Null");
		}
	}
	
	public void showAvailableAccount(){
		log("showAvailableAccount");

		if(myAccountInfo==null){
			log("MyAccountInfo is NULL");
			myAccountInfo = ((ManagerActivityLollipop)context).getMyAccountInfo();
		}

		log("showAvailableAccount: "+myAccountInfo.getAccountType());

		switch(myAccountInfo.getAccountType()){

			case Constants.PRO_I:{
				hideProLite();
				break;
			}
			case Constants.PRO_II:{
				hideProLite();
				hideProI();
				break;
			}
			case Constants.PRO_III:{
				hideProLite();
				hideProI();
				hideProII();
				break;
			}
			case Constants.PRO_LITE:{
				break;
			}
		}
	}

	public void onUpgradeClick(int account){
		log("onUpgradeClick: "+account);
		LinearLayout selectPaymentMethodClicked;

		switch (account){
			case Constants.PRO_LITE:{
				selectPaymentMethodClicked = (LinearLayout) selectPaymentMethodLayoutLite;
				break;
			}
			case Constants.PRO_I:{
				selectPaymentMethodClicked = (LinearLayout) selectPaymentMethodLayoutPro1;
				break;
			}
			case Constants.PRO_II:{
				selectPaymentMethodClicked = (LinearLayout) selectPaymentMethodLayoutPro2;
				break;
			}
			case Constants.PRO_III:{
				selectPaymentMethodClicked = (LinearLayout) selectPaymentMethodLayoutPro3;
				break;
			}
			default:{
				selectPaymentMethodClicked = (LinearLayout) selectPaymentMethodLayoutLite;
				break;
			}
		}

		if (myAccountInfo.getPaymentBitSet() != null){

			selectPaymentMethod = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_text_payment_method);
			RelativeLayout.LayoutParams titleParams = (RelativeLayout.LayoutParams) selectPaymentMethod.getLayoutParams();
			titleParams.setMargins(0,Util.scaleHeightPx(18, outMetrics),0,Util.scaleHeightPx(14, outMetrics));
			selectPaymentMethod.setLayoutParams(titleParams);

			googlePlayLayout = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_google_wallet);
			googlePlayLayout.setOnClickListener(this);

			LinearLayout.LayoutParams googlePlayParams = (LinearLayout.LayoutParams) googlePlayLayout.getLayoutParams();
			googlePlayParams.height = Util.scaleHeightPx(HEIGHT_PAYMENT_METHODS_LAYOUT, outMetrics);
			googlePlayLayout.setLayoutParams(googlePlayParams);

			googleWalletIcon = (ImageView) selectPaymentMethodClicked.findViewById(R.id.payment_method_google_wallet_icon);

			RelativeLayout.LayoutParams googleIconParams = (RelativeLayout.LayoutParams) googleWalletIcon.getLayoutParams();
			googleIconParams.height = Util.scaleHeightPx(40, outMetrics);
			googleIconParams.width = Util.scaleWidthPx(40, outMetrics);
			googleIconParams.setMargins(Util.scaleWidthPx(16, outMetrics),Util.scaleHeightPx(8, outMetrics),0,Util.scaleHeightPx(8, outMetrics));

			googleWalletIcon.setLayoutParams(googleIconParams);

			googleWalletText = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_method_google_wallet_text);
			RelativeLayout.LayoutParams googleTextParams = (RelativeLayout.LayoutParams) googleWalletText.getLayoutParams();
			googleTextParams.setMargins(Util.scaleWidthPx(16, outMetrics),0,0,0);
			googleWalletText.setLayoutParams(googleTextParams);

			creditCardLayout = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_credit_card);
			creditCardLayout.setOnClickListener(this);

			LinearLayout.LayoutParams creditCardParams = (LinearLayout.LayoutParams) creditCardLayout.getLayoutParams();
			creditCardParams.height = Util.scaleHeightPx(HEIGHT_PAYMENT_METHODS_LAYOUT, outMetrics);
			creditCardLayout.setLayoutParams(creditCardParams);

			creditCardIcon = (ImageView) selectPaymentMethodClicked.findViewById(R.id.payment_method_credit_card_icon);

			RelativeLayout.LayoutParams creditCardIconParams = (RelativeLayout.LayoutParams) creditCardIcon.getLayoutParams();
			creditCardIconParams.height = Util.scaleHeightPx(40, outMetrics);
			creditCardIconParams.width = Util.scaleWidthPx(40, outMetrics);
			creditCardIconParams.setMargins(Util.scaleWidthPx(16, outMetrics),Util.scaleHeightPx(8, outMetrics),0,Util.scaleHeightPx(8, outMetrics));

			creditCardIcon.setLayoutParams(creditCardIconParams);

			creditCardText = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_method_credit_card_text);
			RelativeLayout.LayoutParams creditCardTextParams = (RelativeLayout.LayoutParams) creditCardText.getLayoutParams();
			creditCardTextParams.setMargins(Util.scaleWidthPx(16, outMetrics),0,0,0);
			creditCardText.setLayoutParams(creditCardTextParams);

			fortumoLayout = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_fortumo);
			fortumoLayout.setOnClickListener(this);

			LinearLayout.LayoutParams fortumoParams = (LinearLayout.LayoutParams) fortumoLayout.getLayoutParams();
			fortumoParams.height = Util.scaleHeightPx(HEIGHT_PAYMENT_METHODS_LAYOUT, outMetrics);
			fortumoLayout.setLayoutParams(fortumoParams);

			fortumoIcon = (ImageView) selectPaymentMethodClicked.findViewById(R.id.payment_method_fortumo_icon);

			RelativeLayout.LayoutParams fortumoIconParams = (RelativeLayout.LayoutParams) fortumoIcon.getLayoutParams();
			fortumoIconParams.height = Util.scaleHeightPx(40, outMetrics);
			fortumoIconParams.width = Util.scaleWidthPx(40, outMetrics);
			fortumoIconParams.setMargins(Util.scaleWidthPx(16, outMetrics),Util.scaleHeightPx(8, outMetrics),0,Util.scaleHeightPx(8, outMetrics));

			fortumoIcon.setLayoutParams(fortumoIconParams);

			fortumoText = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_method_fortumo_text);
			RelativeLayout.LayoutParams fortumoTextParams = (RelativeLayout.LayoutParams) fortumoText.getLayoutParams();
			fortumoTextParams.setMargins(Util.scaleWidthPx(16, outMetrics),0,0,0);
			fortumoText.setLayoutParams(fortumoTextParams);

			centiliLayout = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.payment_method_centili);
			centiliLayout.setOnClickListener(this);

			LinearLayout.LayoutParams centiliParams = (LinearLayout.LayoutParams) centiliLayout.getLayoutParams();
			centiliParams.height = Util.scaleHeightPx(HEIGHT_PAYMENT_METHODS_LAYOUT, outMetrics);
			centiliLayout.setLayoutParams(centiliParams);

			centiliIcon = (ImageView) selectPaymentMethodClicked.findViewById(R.id.payment_method_centili_icon);

			RelativeLayout.LayoutParams centiliIconParams = (RelativeLayout.LayoutParams) centiliIcon.getLayoutParams();
			centiliIconParams.height = Util.scaleHeightPx(40, outMetrics);
			centiliIconParams.width = Util.scaleWidthPx(40, outMetrics);
			centiliIconParams.setMargins(Util.scaleWidthPx(16, outMetrics),Util.scaleHeightPx(8, outMetrics),0,Util.scaleHeightPx(8, outMetrics));

			centiliIcon.setLayoutParams(fortumoIconParams);

			centiliText = (TextView) selectPaymentMethodClicked.findViewById(R.id.payment_method_centili_text);
			RelativeLayout.LayoutParams centiliTextParams = (RelativeLayout.LayoutParams) centiliText.getLayoutParams();
			centiliTextParams.setMargins(Util.scaleWidthPx(16, outMetrics),0,0,0);
			centiliText.setLayoutParams(centiliTextParams);

			closeLayout = (RelativeLayout) selectPaymentMethodClicked.findViewById(R.id.close_layout);
			closeLayout.setOnClickListener(this);

			LinearLayout.LayoutParams closeLayoutParams = (LinearLayout.LayoutParams) closeLayout.getLayoutParams();
			closeLayoutParams.setMargins(0,Util.scaleHeightPx(5, outMetrics),0,0);
			closeLayout.setLayoutParams(closeLayoutParams);

			closeIcon = (ImageView) selectPaymentMethodClicked.findViewById(R.id.close_layout_icon);

			RelativeLayout.LayoutParams closeIconParams = (RelativeLayout.LayoutParams) closeIcon.getLayoutParams();
			closeIconParams.setMargins(0,0,Util.scaleWidthPx(16, outMetrics),Util.scaleHeightPx(8, outMetrics));
			closeIcon.setLayoutParams(closeIconParams);

			closeLayout.setVisibility(View.VISIBLE);
			googlePlayLayout.setVisibility(View.GONE);
			creditCardLayout.setVisibility(View.GONE);
			fortumoLayout.setVisibility(View.GONE);
			centiliLayout.setVisibility(View.GONE);

			showPaymentMethods(account);

			refreshAccountInfo();
			log("END refreshAccountInfo");
			if (!myAccountInfo.isInventoryFinished()){
				log("if (!myAccountInfo.isInventoryFinished())");
				googlePlayLayout.setVisibility(View.GONE);
			}
			log("Just before show the layout");

			selectPaymentMethodClicked.setVisibility(View.VISIBLE);

			switch (account){
				case Constants.PRO_I:{

					new Handler().post(new Runnable() {
						@Override
						public void run() {
							log("smeasure: "+pro2Layout.getTop());
							log("scroll to: "+pro2Layout.getBottom());
							scrollView.smoothScrollTo(0, pro1Layout.getTop());

						}
					});
					break;
				}
				case Constants.PRO_II:{
					new Handler().post(new Runnable() {
						@Override
						public void run() {
							scrollView.smoothScrollTo(0, pro3Layout.getBottom());
						}
					});
					break;
				}
				case Constants.PRO_III:{
					new Handler().post(new Runnable() {
						@Override
						public void run() {
							scrollView.smoothScrollTo(0, pro3Layout.getBottom());
						}
					});
					break;
				}
			}
		}
		else{
			log("PaymentBitSet Null");
		}
	}

	private void hideProLite(){
		proLiteTransparentLayout.setVisibility(View.VISIBLE);
	}
	
	private void hideProI(){
		pro1TransparentLayout.setVisibility(View.VISIBLE);
		
//		AlphaAnimation alpha = new AlphaAnimation(0.5F, 0.5F);
//		alpha.setDuration(0); 
//		alpha.setFillAfter(true); 
//		pro1.startAnimation(alpha);
		
	}
	
	private void hideProII(){
		pro2TransparentLayout.setVisibility(View.VISIBLE);
	}
	
	private void hideProIII(){
		pro3TransparentLayout.setVisibility(View.VISIBLE);
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

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		context = activity;
	}
	
	public static void log(String log) {
		Util.log("UpgradeAccountFragmentLollipop", log);
	}

	public MyAccountInfo getMyAccountInfo() {
		return myAccountInfo;
	}

	public void setMyAccountInfo(MyAccountInfo myAccountInfo) {
		this.myAccountInfo = myAccountInfo;
	}

	public void showNextPaymentFragment(int paymentMethod){
		log("showNextPaymentFragment: paymentMethod: "+paymentMethod);

		int parameterType;
		if(selectPaymentMethodLayoutLite.getVisibility()==View.VISIBLE){
			parameterType=Constants.PRO_LITE;
		}
		else if(selectPaymentMethodLayoutPro1.getVisibility()==View.VISIBLE){
			parameterType=Constants.PRO_I;
		}
		else if(selectPaymentMethodLayoutPro2.getVisibility()==View.VISIBLE){
			parameterType=Constants.PRO_II;
		}
		else if(selectPaymentMethodLayoutPro3.getVisibility()==View.VISIBLE){
			parameterType=Constants.PRO_III;
		}
		else{
			parameterType=0;
		}
		((ManagerActivityLollipop)context).setSelectedAccountType(parameterType);
		((ManagerActivityLollipop)context).setSelectedPaymentMethod(paymentMethod);
		((ManagerActivityLollipop)context).showmyF(paymentMethod, parameterType);
	}

	@Override
	public void onClick(View v) {
		((ManagerActivityLollipop)context).setDisplayedAccountType(-1);
		((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
		switch (v.getId()){
			case R.id.upgrade_prolite_layout:{
				if(selectPaymentMethodLayoutLite.getVisibility()==View.VISIBLE){
					selectPaymentMethodLayoutLite.setVisibility(View.GONE);
				}
				else{
					selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
					onUpgradeClick(Constants.PRO_LITE);
				}

				break;
			}
			case R.id.close_layout:{
				log("onClick close layout");
				selectPaymentMethodLayoutLite.setVisibility(View.GONE);
				selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
				selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
				selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
				break;
			}
			case R.id.upgrade_pro_i_layout:{
				if(selectPaymentMethodLayoutPro1.getVisibility()==View.VISIBLE){
					selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
				}
				else{
					selectPaymentMethodLayoutLite.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
					onUpgradeClick(Constants.PRO_I);
				}
				break;
			}
			case R.id.upgrade_pro_ii_layout:{
				if(selectPaymentMethodLayoutPro2.getVisibility()==View.VISIBLE){
					selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
				}
				else{
					selectPaymentMethodLayoutLite.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
					onUpgradeClick(Constants.PRO_II);
				}
				break;
			}
			case R.id.upgrade_pro_iii_layout:{
				if(selectPaymentMethodLayoutPro3.getVisibility()==View.VISIBLE){
					selectPaymentMethodLayoutPro3.setVisibility(View.GONE);
				}
				else{
					selectPaymentMethodLayoutLite.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro1.setVisibility(View.GONE);
					selectPaymentMethodLayoutPro2.setVisibility(View.GONE);
					onUpgradeClick(Constants.PRO_III);
				}
				break;
			}
			case R.id.payment_method_google_wallet:{
				showNextPaymentFragment(MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET);
				break;
			}
			case R.id.payment_method_credit_card:{
				showNextPaymentFragment(MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD);
				break;
			}
			case R.id.payment_method_fortumo:{
				showNextPaymentFragment(MegaApiAndroid.PAYMENT_METHOD_FORTUMO);
				break;
			}
			case R.id.payment_method_centili:{
				showNextPaymentFragment(MegaApiAndroid.PAYMENT_METHOD_CENTILI);
				break;
			}
		}
	}

	public void showPaymentMethods(int parameterType){
		log("showPaymentMethods");

		ArrayList<Product> accounts = myAccountInfo.getProductAccounts();

		if (accounts == null){
			log("accounts == null");
			megaApi.getPricing(myAccountInfo);
			return;
		}

		switch(parameterType){
			case Constants.PRO_I:{
				if (myAccountInfo.getPaymentBitSet() != null){
					if (!myAccountInfo.isInventoryFinished()){
						log("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else{
						if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
							if ((myAccountInfo.getProIMonthly() != null) && (myAccountInfo.getProIYearly() != null)) {
								log("PROI monthly: " + myAccountInfo.getProIMonthly().getOriginalJson());
								log("PROI annualy: " + myAccountInfo.getProIYearly().getOriginalJson());
								googlePlayLayout.setVisibility(View.GONE);
							}
							else{
								googlePlayLayout.setVisibility(View.VISIBLE);
							}
						}
					}

					if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
					}
					fortumoLayout.setVisibility(View.GONE);
					centiliLayout.setVisibility(View.GONE);

					if(!Util.isPaymentMethod(myAccountInfo.getPaymentBitSet(), parameterType)){
						selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					}
					else{
						selectPaymentMethod.setText(getString(R.string.select_payment_method));
					}
				}
				else{
					log("not payment bit set received!!!");
					selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					googlePlayLayout.setVisibility(View.GONE);
					fortumoLayout.setVisibility(View.GONE);
					centiliLayout.setVisibility(View.GONE);
				}

				break;
			}
			case Constants.PRO_II:{

				if (myAccountInfo.getPaymentBitSet() != null){
					if (!myAccountInfo.isInventoryFinished()){
						log("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else{
						if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
							if ((myAccountInfo.getProIIMonthly() != null) && (myAccountInfo.getProIIYearly() != null)) {
								log("PROII monthly: " + myAccountInfo.getProIIMonthly().getOriginalJson());
								log("PROII annualy: " + myAccountInfo.getProIIYearly().getOriginalJson());
								googlePlayLayout.setVisibility(View.GONE);
							}
							else{
								googlePlayLayout.setVisibility(View.VISIBLE);
							}
						}
					}

					if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
					}
					fortumoLayout.setVisibility(View.GONE);
					centiliLayout.setVisibility(View.GONE);

					if(!Util.isPaymentMethod(myAccountInfo.getPaymentBitSet(), parameterType)){
						selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					}
					else{
						selectPaymentMethod.setText(getString(R.string.select_payment_method));
					}
				}
				else{
					log("not payment bit set received!!!");
				}

				break;
			}
			case Constants.PRO_III:{

				if (myAccountInfo.getPaymentBitSet() != null){
					if (!myAccountInfo.isInventoryFinished()){
						log("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else{
						if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
							if ((myAccountInfo.getProIIIMonthly() != null) && (myAccountInfo.getProIIIYearly() != null)) {
								log("PROIII monthly: " + myAccountInfo.getProIIIMonthly().getOriginalJson());
								log("PROIII annualy: " + myAccountInfo.getProIIIYearly().getOriginalJson());
								googlePlayLayout.setVisibility(View.GONE);
							}
							else{
								googlePlayLayout.setVisibility(View.VISIBLE);
							}
						}
					}

					if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
					}
					fortumoLayout.setVisibility(View.GONE);
					centiliLayout.setVisibility(View.GONE);

					if(!Util.isPaymentMethod(myAccountInfo.getPaymentBitSet(), parameterType)){
						selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					}
					else{
						selectPaymentMethod.setText(getString(R.string.select_payment_method));
					}
				}

				break;
			}
			case Constants.PRO_LITE:{

				if (myAccountInfo.getPaymentBitSet() != null){
					if (!myAccountInfo.isInventoryFinished()){
						log("if (!myAccountInfo.isInventoryFinished())");
						googlePlayLayout.setVisibility(View.GONE);
					}
					else {
						if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)) {
							if ((myAccountInfo.getProLiteMonthly() != null) && (myAccountInfo.getProLiteYearly() != null)) {
								log("PRO Lite monthly: " + myAccountInfo.getProLiteMonthly().getOriginalJson());
								log("PRO Lite annualy: " + myAccountInfo.getProLiteYearly().getOriginalJson());
								googlePlayLayout.setVisibility(View.GONE);
							}
							else{
								googlePlayLayout.setVisibility(View.VISIBLE);
							}
						}
					}

					if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
						creditCardLayout.setVisibility(View.VISIBLE);
					}
					if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
						fortumoLayout.setVisibility(View.VISIBLE);
					}
					if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CENTILI)){
						centiliLayout.setVisibility(View.VISIBLE);
					}

					if(!Util.isPaymentMethod(myAccountInfo.getPaymentBitSet(), parameterType)){
						selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
					}
					else{
						selectPaymentMethod.setText(getString(R.string.select_payment_method));
					}
				}

				break;
			}
		}

	}

//	public void setPaymentMethods(int parameterType){
//		log("setPaymentMethods");
//
//		if (!myAccountInfo.isInventoryFinished()){
//			log("if (!myAccountInfo.isInventoryFinished())");
//			googlePlayLayout.setVisibility(View.GONE);
//		}
//		else{
//			if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_GOOGLE_WALLET)){
//				switch (parameterType){
//					case 1:{
//						if ((myAccountInfo.getProIMonthly() != null) && (myAccountInfo.getProIYearly() != null)) {
//							googlePlayLayout.setVisibility(View.GONE);
//						}
//						else{
//							googlePlayLayout.setVisibility(View.VISIBLE);
//						}
//						break;
//					}
//					case 2:{
//						if ((myAccountInfo.getProIIMonthly() != null) && (myAccountInfo.getProIIYearly() != null)) {
//							googlePlayLayout.setVisibility(View.GONE);
//						}
//						else{
//							googlePlayLayout.setVisibility(View.VISIBLE);
//						}
//						break;
//					}
//					case 3:{
//						if ((myAccountInfo.getProIIIMonthly() != null) && (myAccountInfo.getProIIIYearly() != null)) {
//							googlePlayLayout.setVisibility(View.GONE);
//						}
//						else{
//							googlePlayLayout.setVisibility(View.VISIBLE);
//						}
//						break;
//					}
//					case 4:{
//						if ((myAccountInfo.getProLiteMonthly() != null) && (myAccountInfo.getProLiteYearly() != null)) {
//							googlePlayLayout.setVisibility(View.GONE);
//						}
//						else{
//							googlePlayLayout.setVisibility(View.VISIBLE);
//						}
//						break;
//					}
//				}
//
//			}
//		}
//
//		if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CREDIT_CARD)){
//			creditCardLayout.setVisibility(View.VISIBLE);
//		}
//		if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_FORTUMO)){
//			if (parameterType == 4){
//				fortumoLayout.setVisibility(View.VISIBLE);
//			}
//		}
//		if (Util.checkBitSet(myAccountInfo.getPaymentBitSet(), MegaApiAndroid.PAYMENT_METHOD_CENTILI)){
//			if (parameterType == 4){
//				centiliLayout.setVisibility(View.VISIBLE);
//			}
//		}
//		if(!Util.isPaymentMethod(myAccountInfo.getPaymentBitSet(), parameterType)){
//			selectPaymentMethod.setText(getString(R.string.no_available_payment_method));
//		}
//		else{
//			selectPaymentMethod.setText(getString(R.string.select_payment_method));
//		}
//
//	}

}
