package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;

public class GetLinkFragmentLollipop extends Fragment implements View.OnClickListener, DatePickerDialog.OnDateSetListener, android.widget.CompoundButton.OnCheckedChangeListener {

    private final static float ALPHA_VIEW_DISABLED = 0.5f;
    private final static float ALPHA_VIEW_ENABLED = 1.0f;

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
    SwitchCompat switchButtonExpiry;
    Button expiryDateButton;
    RelativeLayout expiryDateLayout;
    RelativeLayout passwordProtectionLayout;

    ImageView advancedOptionsImage;
    RelativeLayout advancedOptionsLayout;

    DatePickerDialog datePickerDialog;

    private LinearLayout mainLinearLayout;
    private ScrollView scrollView;

    String link;
    boolean isExpiredDateLink;
    private boolean isInPasswordProtectionMode;

    NodeController nC;

    CheckedTextView linkWithoutKeyCheck;
    CheckedTextView linkDecryptionKeyCheck;
    CheckedTextView linkWithKeyCheck;
    TextView subtitleProOnlyExpiry;
    TextView linkText;

    RelativeLayout linkWithoutKeyLayout;
    RelativeLayout linkDecryptionKeyLayout;
    RelativeLayout linkWithKeyLayout;

    TextView subtitleProOnlyProtection;
    SwitchCompat switchButtonProtection;
    TextView passwordProtectionEditText;

    RelativeLayout transparentKeyLayoutExpiry;
    RelativeLayout transparentKeyLayoutProtection;

    LinearLayout separatorExpiry;
    LinearLayout separatorPass;

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
        expiryDateLayout = (RelativeLayout) v.findViewById(R.id.expiry_date_layout);
        expiryDateLayout.setVisibility(View.GONE);

        switchButtonExpiry = (SwitchCompat) v.findViewById(R.id.switch_set_expiry_date);
        expiryDateButton = (Button) v.findViewById(R.id.expiry_date_button);

        advancedOptionsLayout = (RelativeLayout) v.findViewById(R.id.advanced_options_layout);
        advancedOptionsImage = (ImageView) v.findViewById(R.id.advanced_options_image);

        //Set by default, link with key
        linkWithoutKeyLayout = (RelativeLayout) v.findViewById(R.id.link_without_key_layout);
        linkDecryptionKeyLayout= (RelativeLayout) v.findViewById(R.id.link_decryption_key_layout);
        linkWithKeyLayout = (RelativeLayout) v.findViewById(R.id.link_with_key_layout);

        linkWithoutKeyCheck = (CheckedTextView) v.findViewById(R.id.link_without_key);
        linkDecryptionKeyCheck = (CheckedTextView) v.findViewById(R.id.link_decryption_key);
        linkWithKeyCheck = (CheckedTextView) v.findViewById(R.id.link_with_key);

        linkText = (TextView)v.findViewById(R.id.link);
        subtitleProOnlyExpiry = (TextView)v.findViewById(R.id.subtitle_set_expiry_date);

        advancedOptionsLayout.setOnClickListener(this);

        linkWithoutKeyLayout.setOnClickListener(this);
        linkDecryptionKeyLayout.setOnClickListener(this);
        linkWithKeyLayout.setOnClickListener(this);

        linkWithoutKeyLayout.setVisibility(View.GONE);
        linkDecryptionKeyLayout.setVisibility(View.GONE);
        linkWithKeyLayout.setVisibility(View.GONE);

        sendButton = (Button) v.findViewById(R.id.send_button);
        copyButton = (Button) v.findViewById(R.id.copy_button);

        sendButton.setOnClickListener(this);
        copyButton.setOnClickListener(this);

        sendButton.setEnabled(false);
        copyButton.setEnabled(false);

        transparentKeyLayoutExpiry = (RelativeLayout) v.findViewById(R.id.transparent_key_layout_expiry_date);

        passwordProtectionLayout = (RelativeLayout) v.findViewById(R.id.password_protection_layout);
        passwordProtectionLayout.setVisibility(View.GONE);
        subtitleProOnlyProtection = (TextView)v.findViewById(R.id.subtitle_set_password_protection);
        switchButtonProtection = (SwitchCompat) v.findViewById(R.id.switch_set_password_protection);
        passwordProtectionEditText = (TextView) v.findViewById(R.id.password_protection_edit);
        passwordProtectionEditText.setOnClickListener(this);

        transparentKeyLayoutProtection = (RelativeLayout) v.findViewById(R.id.transparent_key_layout_password_protection);

        separatorExpiry = (LinearLayout) v.findViewById(R.id.separator_expiry);
        separatorPass= (LinearLayout) v.findViewById(R.id.separator_password);
        separatorExpiry.setVisibility(View.GONE);
        separatorPass.setVisibility(View.GONE);

        if(((GetLinkActivityLollipop)context).selectedNode.isExported()){
            log("node is already exported: "+((GetLinkActivityLollipop)context).selectedNode.getName());
            log("node link: "+((GetLinkActivityLollipop)context).selectedNode.getPublicLink());
            link = ((GetLinkActivityLollipop)context).selectedNode.getPublicLink();

            linkWithoutKeyCheck.setChecked(false);
            linkDecryptionKeyCheck.setChecked(false);
            linkWithKeyCheck.setChecked(true);
            linkText.setText(link);
            copyButton.setEnabled(true);
            sendButton.setEnabled(true);
        }
        else{
            nC.exportLink(((GetLinkActivityLollipop)context).selectedNode);
            linkText.setText(getString(R.string.link_request_status));
            copyButton.setEnabled(false);
            sendButton.setEnabled(false);
        }

		if(((GetLinkActivityLollipop)context).accountType > MegaAccountDetails.ACCOUNT_TYPE_FREE){
			log("The user is PRO - enable expiration date");

            transparentKeyLayoutExpiry.setVisibility(View.GONE);

			if(((GetLinkActivityLollipop)context).selectedNode.getExpirationTime()<=0){
                switchButtonExpiry.setChecked(false);
				expiryDateButton.setVisibility(View.GONE);
                subtitleProOnlyExpiry.setVisibility(View.VISIBLE);
			}
			else{
				switchButtonExpiry.setChecked(true);
				java.text.DateFormat df = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault());
				Calendar cal = Util.calculateDateFromTimestamp(((GetLinkActivityLollipop)context).selectedNode.getExpirationTime());
				TimeZone tz = cal.getTimeZone();
				df.setTimeZone(tz);
				Date date = cal.getTime();
				String formattedDate = df.format(date);
				expiryDateButton.setText(formattedDate);
				expiryDateButton.setVisibility(View.VISIBLE);
                subtitleProOnlyExpiry.setVisibility(View.GONE);
                
                //if previously selected expiry date then we need to expand advance section
                showAdvanceSection();
			}

            switchButtonExpiry.setEnabled(true);
            switchButtonExpiry.setOnCheckedChangeListener(this);
            expiryDateButton.setOnClickListener(this);

            //Check if the link has password
            transparentKeyLayoutProtection.setVisibility(View.GONE);

            switchButtonProtection.setChecked(false);
            passwordProtectionEditText.setVisibility(View.GONE);
            subtitleProOnlyProtection.setVisibility(View.VISIBLE);

            switchButtonProtection.setEnabled(true);
            switchButtonProtection.setOnCheckedChangeListener(this);
            switchButtonProtection.setOnClickListener(this);
		}
		else{
			log("The is user is not PRO");
            transparentKeyLayoutExpiry.setVisibility(View.VISIBLE);

            switchButtonExpiry.setEnabled(false);
			expiryDateButton.setVisibility(View.GONE);
            subtitleProOnlyExpiry.setVisibility(View.VISIBLE);

            transparentKeyLayoutProtection.setVisibility(View.VISIBLE);

            switchButtonProtection.setEnabled(false);
            passwordProtectionEditText.setVisibility(View.GONE);
            subtitleProOnlyProtection.setVisibility(View.VISIBLE);
		}

        return v;
    }

    public void showDatePicker(long expirationTimestamp){
        log("showDatePicker: "+expirationTimestamp);
        int year;
        int month;
        int day;

        if(expirationTimestamp!=-1){
            java.text.DateFormat df = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault());
            Calendar c = Util.calculateDateFromTimestamp(expirationTimestamp);
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            day = c.get(Calendar.DAY_OF_MONTH);
        }
        else{
            Calendar c = Calendar.getInstance();
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            day = c.get(Calendar.DAY_OF_MONTH);
        }

        datePickerDialog = new DatePickerDialog(context, this, year, month, day);
        datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_NEGATIVE) {
                    MegaNode node = ((GetLinkActivityLollipop)context).selectedNode;
                    if(node != null && node.getExpirationTime() > 0){
                        switchButtonExpiry.setChecked(true);
                    }else{
                        switchButtonExpiry.setChecked(false);
                    }
                }
            }
        });
        //disable past date
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    @Override
    public void onClick(View v) {
        log("onClick");
        switch (v.getId()){
            case R.id.expiry_date_button:{
                showDatePicker(((GetLinkActivityLollipop)context).selectedNode.getExpirationTime());
                break;
            }
            case R.id.disagree_button:{
                log("DISAgree button");
                ((GetLinkActivityLollipop)context).finish();
                break;
            }
            case R.id.agree_button:{
                log("Agree button");
                break;
            }
            case R.id.advanced_options_layout:{
                if(linkWithKeyLayout.isShown()){
                    hideAdvanceSection();
                }else{
                   showAdvanceSection();
                }
                break;
            }
            case R.id.link_without_key_layout:{
                if(isInPasswordProtectionMode){
                    return;
                }
                linkWithoutKeyCheck.setChecked(true);
				linkDecryptionKeyCheck.setChecked(false);
				linkWithKeyCheck.setChecked(false);

                expiryDateLayout.setAlpha(ALPHA_VIEW_ENABLED);
                passwordProtectionLayout.setAlpha(ALPHA_VIEW_ENABLED);
				
				//disable expiry/password options for decryption key option
                if(((GetLinkActivityLollipop)context).accountType > MegaAccountDetails.ACCOUNT_TYPE_FREE) {
                    switchButtonExpiry.setEnabled(true);
                    switchButtonProtection.setEnabled(true);
                }
                if(link!=null){
                    String urlString="";
                    String [] s = link.split("!");
                    if (s.length == 3){
                        urlString = s[0] + "!" + s[1];
                    }
                    linkText.setText(urlString);
                    copyButton.setEnabled(true);
                    sendButton.setEnabled(true);
                }
                break;
            }
            case R.id.link_decryption_key_layout:{
                if(isInPasswordProtectionMode){
                    return;
                }
                
                switchButtonProtection.setEnabled(false);
                expiryDateLayout.setAlpha(ALPHA_VIEW_DISABLED);
                passwordProtectionLayout.setAlpha(ALPHA_VIEW_DISABLED);
                linkWithoutKeyCheck.setChecked(false);
				linkDecryptionKeyCheck.setChecked(true);
				linkWithKeyCheck.setChecked(false);
                if(link!=null){
                    String keyString="";
                    String [] s = link.split("!");
                    if (s.length == 3){
                        keyString = s[2];
                    }
                    linkText.setText(keyString);
                    copyButton.setEnabled(true);
                    sendButton.setEnabled(true);
                }
                break;
            }
            case R.id.link_with_key_layout:{
                if(isInPasswordProtectionMode){
                    return;
                }
                linkWithoutKeyCheck.setChecked(false);
				linkDecryptionKeyCheck.setChecked(false);
				linkWithKeyCheck.setChecked(true);
                expiryDateLayout.setAlpha(ALPHA_VIEW_ENABLED);
                passwordProtectionLayout.setAlpha(ALPHA_VIEW_ENABLED);
				linkText.setText(link);
                if(((GetLinkActivityLollipop)context).accountType > MegaAccountDetails.ACCOUNT_TYPE_FREE) {
                    switchButtonExpiry.setEnabled(true);
                    switchButtonProtection.setEnabled(true);
                }
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
            case R.id.password_protection_edit:{
                ((GetLinkActivityLollipop)context).showSetPasswordDialog(null, link);
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

    public void enablePassProtection(boolean enable){
        switchButtonProtection.setChecked(enable);
    }

    public void processingPass(){
        linkText.setText(getString(R.string.link_request_status));
        copyButton.setEnabled(false);
        sendButton.setEnabled(false);
    }

    public void requestFinish(MegaRequest request, MegaError e){
        log("requestFinish");

        if (request.getType() == MegaRequest.TYPE_EXPORT) {
            log("export request finished");
            MegaNode node = ((GetLinkActivityLollipop)context).selectedNode;
            log("EXPIRATION DATE: " + node.getExpirationTime());
            if (isExpiredDateLink) {
                log("change the expiration date");
                if (node.getExpirationTime() <= 0) {
                    switchButtonExpiry.setChecked(false);
                    expiryDateButton.setVisibility(View.GONE);
                    subtitleProOnlyExpiry.setVisibility(View.VISIBLE);
                } else {
                    switchButtonExpiry.setChecked(true);
                    java.text.DateFormat df = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault());
                    Calendar cal = Util.calculateDateFromTimestamp(node.getExpirationTime());
                    TimeZone tz = cal.getTimeZone();
                    df.setTimeZone(tz);
                    Date date = cal.getTime();
                    String formattedDate = df.format(date);
                    expiryDateButton.setText(formattedDate);
                    expiryDateButton.setVisibility(View.VISIBLE);
                    subtitleProOnlyExpiry.setVisibility(View.GONE);
                }
            } else {
                link = request.getLink();

                if(linkWithoutKeyCheck.isChecked()){
                    if(link!=null){
                        String urlString="";
                        String [] s = link.split("!");
                        if (s.length == 3){
                            urlString = s[0] + "!" + s[1];
                        }
                        linkText.setText(urlString);
                        copyButton.setEnabled(true);
                        sendButton.setEnabled(true);
                    }
                }
                else if(linkDecryptionKeyCheck.isChecked()){
                    if(link!=null){
                        String keyString="!";
                        String [] s = link.split("!");
                        if (s.length == 3){
                            keyString = keyString+s[2];
                        }
                        linkText.setText(keyString);
                        copyButton.setEnabled(true);
                        sendButton.setEnabled(true);
                    }
                }
                else{
                    linkText.setText(link);
                    copyButton.setEnabled(true);
                    sendButton.setEnabled(true);
                }
            }
            log("link: " + request.getLink());

            isExpiredDateLink = false;
        }
        else if(request.getType() == MegaRequest.TYPE_PASSWORD_LINK){
            log("password link request finished");
            linkText.setText(request.getText());
            copyButton.setEnabled(true);
            sendButton.setEnabled(true);
            linkWithoutKeyCheck.setChecked(false);
            linkDecryptionKeyCheck.setChecked(false);
            linkWithKeyCheck.setChecked(true);
            passwordProtectionEditText.setText(request.getPassword());
            passwordProtectionEditText.setVisibility(View.VISIBLE);
            subtitleProOnlyProtection.setVisibility(View.GONE);
    
            //disable the other two option if password enabled
            isInPasswordProtectionMode = true;
            disableCheckedTextView();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        log("onCheckedChanged");

        switch (buttonView.getId()){
            case R.id.switch_set_expiry_date:{
                log("Set expiry date");
                if(switchButtonExpiry.isChecked()){
                    showDatePicker(-1);
                }
                else{
                    isExpiredDateLink=true;
                    nC.exportLink(((GetLinkActivityLollipop)context).selectedNode);
                }
                break;
            }
            case R.id.switch_set_password_protection:{
                log("Set password protection");
                if(switchButtonProtection.isChecked()){
                    ((GetLinkActivityLollipop)context).showSetPasswordDialog(null, link);
                }
                else{
                    log("Remove pass protection");
                    if (linkWithKeyCheck.isChecked()) {
                        linkText.setText(link);
                    } else if (linkWithoutKeyCheck.isChecked()) {
                        if(link!=null){
                            String urlString="";
                            String [] s = link.split("!");
                            if (s.length == 3){
                                urlString = s[0] + "!" + s[1];
                            }
                            linkText.setText(urlString);
                        }
                    }
                    copyButton.setEnabled(true);
                    sendButton.setEnabled(true);
                    isInPasswordProtectionMode = false;
                    enableCheckedTextView();
                    passwordProtectionEditText.setVisibility(View.GONE);
                    subtitleProOnlyProtection.setVisibility(View.VISIBLE);
                }

                break;
            }
        }
    }
    
    private void showAdvanceSection(){
        advancedOptionsImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_expand));
        linkWithoutKeyLayout.setVisibility(View.VISIBLE);
        linkDecryptionKeyLayout.setVisibility(View.VISIBLE);
        linkWithKeyLayout.setVisibility(View.VISIBLE);
        expiryDateLayout.setVisibility(View.VISIBLE);
        passwordProtectionLayout.setVisibility(View.VISIBLE);
        separatorExpiry.setVisibility(View.VISIBLE);
        separatorPass.setVisibility(View.VISIBLE);
    }
    
    private void hideAdvanceSection(){
        advancedOptionsImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_collapse_acc));
        linkWithoutKeyLayout.setVisibility(View.GONE);
        linkDecryptionKeyLayout.setVisibility(View.GONE);
        linkWithKeyLayout.setVisibility(View.GONE);
        expiryDateLayout.setVisibility(View.GONE);
        passwordProtectionLayout.setVisibility(View.GONE);
        separatorExpiry.setVisibility(View.GONE);
        separatorPass.setVisibility(View.GONE);
    }
    
    private void disableCheckedTextView(){
        linkWithoutKeyCheck.setAlpha(ALPHA_VIEW_DISABLED);
        linkDecryptionKeyCheck.setAlpha(ALPHA_VIEW_DISABLED);
    }
    
    private void enableCheckedTextView(){
        linkWithoutKeyCheck.setAlpha(ALPHA_VIEW_ENABLED);
        linkDecryptionKeyCheck.setAlpha(ALPHA_VIEW_ENABLED);
    }
}
