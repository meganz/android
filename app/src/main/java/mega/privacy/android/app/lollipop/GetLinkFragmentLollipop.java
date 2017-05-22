package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SwitchCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class GetLinkFragmentLollipop extends Fragment implements View.OnClickListener, DatePickerDialog.OnDateSetListener, android.widget.CompoundButton.OnCheckedChangeListener {

    Context context;

    float scaleH, scaleW;
    float density;
    DisplayMetrics outMetrics;
    Display display;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    ActionBar aB;
    Button sendButton;
    Button copyButton;
    SwitchCompat switchButton;
    Button expiryDateButton;

    DatePickerDialog datePickerDialog;

    private LinearLayout mainLinearLayout;
    private ScrollView scrollView;

    String link;
    long expirationTimestamp;
    boolean isExpiredDateLink;

    NodeController nC;

    CheckedTextView linkWithoutKeyCheck;
    CheckedTextView linkDecryptionKeyCheck;
    CheckedTextView linkWithKeyCheck;

    TextView linkText;

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate");
        super.onCreate(savedInstanceState);

        if(context==null){
            log("context is null");
            return;
        }

        nC = new NodeController(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        if(megaApi==null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        View v = inflater.inflate(R.layout.fragment_get_link, container, false);

        scrollView = (ScrollView) v.findViewById(R.id.scroll_view_get_link);
        mainLinearLayout = (LinearLayout) v.findViewById(R.id.get_link_main_linear_layout);
        switchButton = (SwitchCompat) v.findViewById(R.id.switch_set_expiry_date);
        expiryDateButton = (Button) v.findViewById(R.id.expiry_date);

    //Set by default, link with key
        linkWithoutKeyCheck = (CheckedTextView) v.findViewById(R.id.link_without_key);
        linkDecryptionKeyCheck = (CheckedTextView) v.findViewById(R.id.link_decryption_key);
        linkWithKeyCheck = (CheckedTextView) v.findViewById(R.id.link_with_key);

        linkText = (TextView)v.findViewById(R.id.link);

        linkWithoutKeyCheck.setOnClickListener(this);
        linkDecryptionKeyCheck.setOnClickListener(this);
        linkWithKeyCheck.setOnClickListener(this);

        sendButton = (Button) v.findViewById(R.id.send_button);
        copyButton = (Button) v.findViewById(R.id.copy_button);

        sendButton.setOnClickListener(this);
        copyButton.setOnClickListener(this);


        if(((GetLinkActivityLollipop)context).selectedNode.isExported()){
            log("node is already exported: "+((GetLinkActivityLollipop)context).selectedNode.getName());
            log("node link: "+((GetLinkActivityLollipop)context).selectedNode.getPublicLink());
            link = ((GetLinkActivityLollipop)context).selectedNode.getPublicLink();
            expirationTimestamp = ((GetLinkActivityLollipop)context).selectedNode.getExpirationTime();

            linkWithoutKeyCheck.setChecked(false);
            linkDecryptionKeyCheck.setChecked(false);
            linkWithKeyCheck.setChecked(true);
            linkText.setText(link);
        }
        else{
            nC.exportLink(((GetLinkActivityLollipop)context).selectedNode);
            linkText.setText("Processing...");
        }

		if(((GetLinkActivityLollipop)context).accountType > MegaAccountDetails.ACCOUNT_TYPE_FREE){
			log("The user is PRO - enable expiration date");

			if(expirationTimestamp<=0){
                switchButton.setChecked(false);
				expiryDateButton.setVisibility(View.INVISIBLE);
			}
			else{
				switchButton.setChecked(true);
				java.text.DateFormat df = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault());
				Calendar cal = Util.calculateDateFromTimestamp(expirationTimestamp);
				TimeZone tz = cal.getTimeZone();
				df.setTimeZone(tz);
				Date date = cal.getTime();
				String formattedDate = df.format(date);
				expiryDateButton.setText(formattedDate);
				expiryDateButton.setVisibility(View.VISIBLE);
			}

            switchButton.setOnCheckedChangeListener(this);
            expiryDateButton.setOnClickListener(this);
		}
		else{
			log("The is user is not PRO");
            switchButton.setEnabled(false);
			expiryDateButton.setVisibility(View.INVISIBLE);
		}

        return v;
    }

    public void showDatePicker(){
        datePickerDialog.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.expiry_date:{
                datePickerDialog.show();
            }
            case R.id.disagree_button:{
                ((GetLinkActivityLollipop)context).finish();
                break;
            }
            case R.id.agree_button:{
                break;
            }
            case R.id.switch_set_expiry_date:{

                final Calendar c = Calendar.getInstance();
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);

                datePickerDialog = new DatePickerDialog(context, this, year, month, day);
                datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.general_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_NEGATIVE) {
                            log("Negative button of DatePicker clicked");
                            switchButton.setChecked(false);
                            expiryDateButton.setVisibility(View.INVISIBLE);
                        }
                    }
                });
                datePickerDialog.show();
                break;
            }
            case R.id.link_without_key:{
                linkWithoutKeyCheck.setChecked(true);
				linkDecryptionKeyCheck.setChecked(false);
				linkWithKeyCheck.setChecked(false);
				String urlString="";
				String [] s = link.split("!");
				if (s.length == 3){
					urlString = s[0] + "!" + s[1];
				}
				linkText.setText(urlString);
                break;
            }
            case R.id.link_decryption_key:{
                linkWithoutKeyCheck.setChecked(false);
				linkDecryptionKeyCheck.setChecked(true);
				linkWithKeyCheck.setChecked(false);
				String keyString="!";
				String [] s = link.split("!");
				if (s.length == 3){
					keyString = keyString+s[2];
				}
				linkText.setText(keyString);
                break;
            }
            case R.id.link_with_key:{
                linkWithoutKeyCheck.setChecked(false);
				linkDecryptionKeyCheck.setChecked(false);
				linkWithKeyCheck.setChecked(true);
				linkText.setText(link);
                break;
            }
            case R.id.copy_button:{
                ((GetLinkActivityLollipop)context).copyLink(linkText.getText().toString());
                break;
            }
            case R.id.send_button:{
                ((GetLinkActivityLollipop)context).sendLink(linkText.getText().toString());
                break;
            }
        }
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
        Util.log("GetLinkFragmentLollipop", message);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

        Calendar cal = Calendar.getInstance();
        cal.set(year, monthOfYear, dayOfMonth);
        Date date = cal.getTime();
        SimpleDateFormat dfTimestamp = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String dateString = dfTimestamp.format(date);
        dateString = dateString + "2359";
        log("the date string is: "+dateString);
        int timestamp = (int) Util.calculateTimestamp(dateString);
        log("the TIMESTAMP is: "+timestamp);
        isExpiredDateLink=true;
        nC.exportLinkTimestamp(((GetLinkActivityLollipop)context).selectedNode, timestamp);

    }

    public void requestFinish(MegaRequest request, MegaError e){
        log("requestFinish");
        if (e.getErrorCode() == MegaError.API_OK) {
            MegaNode node = megaApi.getNodeByHandle(request.getNodeHandle());
            log("EXPIRATION DATE: " + node.getExpirationTime());
            if (isExpiredDateLink) {
                log("change the expiration date");
                if (node.getExpirationTime() <= 0) {
                    switchButton.setChecked(false);
                    expiryDateButton.setVisibility(View.INVISIBLE);
                } else {
                    switchButton.setChecked(true);
                    java.text.DateFormat df = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault());
                    Calendar cal = Util.calculateDateFromTimestamp(node.getExpirationTime());
                    TimeZone tz = cal.getTimeZone();
                    df.setTimeZone(tz);
                    Date date = cal.getTime();
                    String formattedDate = df.format(date);
                    expiryDateButton.setText(formattedDate);
                    expiryDateButton.setVisibility(View.VISIBLE);
                }
            } else {
                link = request.getLink();
                expirationTimestamp = node.getExpirationTime();
                linkText.setText(link);
            }
            log("link: " + request.getLink());
        } else {
            log("Error: " + e.getErrorString());
            ((GetLinkActivityLollipop)context).showSnackbar(getString(R.string.context_no_link));
        }
        isExpiredDateLink = false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(switchButton.isChecked()){
            datePickerDialog.show();
        }
        else{
            isExpiredDateLink=true;
            nC.exportLink(((GetLinkActivityLollipop)context).selectedNode);
        }
    }
}
