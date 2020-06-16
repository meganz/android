package mega.privacy.android.app;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;

import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.listeners.GetAttrUserListener;
import mega.privacy.android.app.listeners.VerifyCredentialsListener;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class AuthenticityCredentialsActivity extends PinActivityLollipop {

    private static final String CONTACT_CREDENTIALS = "CONTACT_CREDENTIALS";
    private static final int LENGHT_CREDENTIALS_LIST = 10;
    private static final int LENGHT_CREDENTIALS_CHARACTERS = 4;

    private AuthenticityCredentialsActivity authenticityCredentialsActivity;

    private MegaUser contact;
    private String contactCredentials;

    private ActionBar aB;

    private LinearLayout authenticityCredentialsLayout;
    private ScrollView scrollView;

    private TextView contactCredentials00;
    private TextView contactCredentials01;
    private TextView contactCredentials02;
    private TextView contactCredentials03;
    private TextView contactCredentials04;
    private TextView contactCredentials10;
    private TextView contactCredentials11;
    private TextView contactCredentials12;
    private TextView contactCredentials13;
    private TextView contactCredentials14;

    private Button credentialsButton;

    private TextView myCredentials00;
    private TextView myCredentials01;
    private TextView myCredentials02;
    private TextView myCredentials03;
    private TextView myCredentials04;
    private TextView myCredentials10;
    private TextView myCredentials11;
    private TextView myCredentials12;
    private TextView myCredentials13;
    private TextView myCredentials14;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() == null || getIntent().getExtras() == null) {
            logError("Cannot init view, Intent is null");
            return;
        }

        String email = getIntent().getExtras().getString(EMAIL);
        if (isTextEmpty(email)) {
            logError("Cannot init view, contact' email is empty");
            return;
        }

        contact = megaApi.getContact(email);
        if (contact == null) {
            logError("Cannot init view, contactis null");
            return;
        }

        authenticityCredentialsActivity = this;

        if (savedInstanceState != null) {
            contactCredentials = savedInstanceState.getString(CONTACT_CREDENTIALS);
        }

        setContentView(R.layout.activity_authenticity_credentials);

        Toolbar tB = findViewById(R.id.credentials_toolbar);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        if (aB == null) {
            logError("Cannot init view, ActionBar is null");
            return;
        }

        aB.setElevation(0);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
        tB.setTitle(getString(R.string.authenticity_credentials_label).toUpperCase());
        setTitle(getString(R.string.authenticity_credentials_label).toUpperCase());

        authenticityCredentialsLayout = findViewById(R.id.authenticity_credentials_layout);
        scrollView = findViewById(R.id.credentials_scrollview);
        new ListenScrollChangesHelper().addViewToListen(scrollView, (v, scrollX, scrollY, oldScrollX, oldScrollY) -> changeViewElevation(aB, scrollView.canScrollVertically(-1), getOutMetrics()));

        TextView contactName = findViewById(R.id.contact_credentials_name);
        contactName.setText(getContactNameDB(contact.getHandle()));
        TextView contactEmail = findViewById(R.id.contact_credentials_email);
        contactEmail.setText(email);

        contactCredentials00 = findViewById(R.id.contact_credentials_0_0);
        contactCredentials01 = findViewById(R.id.contact_credentials_0_1);
        contactCredentials02 = findViewById(R.id.contact_credentials_0_2);
        contactCredentials03 = findViewById(R.id.contact_credentials_0_3);
        contactCredentials04 = findViewById(R.id.contact_credentials_0_4);
        contactCredentials10 = findViewById(R.id.contact_credentials_1_0);
        contactCredentials11 = findViewById(R.id.contact_credentials_1_1);
        contactCredentials12 = findViewById(R.id.contact_credentials_1_2);
        contactCredentials13 = findViewById(R.id.contact_credentials_1_3);
        contactCredentials14 = findViewById(R.id.contact_credentials_1_4);

        if (isTextEmpty(contactCredentials)) {
            megaApi.getUserCredentials(contact, new GetAttrUserListener(authenticityCredentialsActivity));
        }

        credentialsButton = findViewById(R.id.credentials_button);
        updateButtonText();

        VerifyCredentialsListener verifyCredentialsListener = new VerifyCredentialsListener(authenticityCredentialsActivity);
        credentialsButton.setOnClickListener(v -> {
            if (megaApi.areCredentialsVerified(contact)) {
                megaApi.resetCredentials(contact, verifyCredentialsListener);
            } else {
                megaApi.verifyCredentials(contact, verifyCredentialsListener);
            }
        });

        myCredentials00 = findViewById(R.id.my_credentials_0_0);
        myCredentials01 = findViewById(R.id.my_credentials_0_1);
        myCredentials02 = findViewById(R.id.my_credentials_0_2);
        myCredentials03 = findViewById(R.id.my_credentials_0_3);
        myCredentials04 = findViewById(R.id.my_credentials_0_4);
        myCredentials10 = findViewById(R.id.my_credentials_1_0);
        myCredentials11 = findViewById(R.id.my_credentials_1_1);
        myCredentials12 = findViewById(R.id.my_credentials_1_2);
        myCredentials13 = findViewById(R.id.my_credentials_1_3);
        myCredentials14 = findViewById(R.id.my_credentials_1_4);
        setMyCredentials();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(CONTACT_CREDENTIALS, contactCredentials);

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateButtonText() {
        credentialsButton.setText(megaApi.areCredentialsVerified(contact) ? R.string.action_reset : R.string.general_verify);
    }

    public void finishVerifyCredentialsAction(MegaRequest request, MegaError e) {
        if (request.getNodeHandle() != contact.getHandle()) return;

        if (e.getErrorCode() == MegaError.API_OK) {
            updateButtonText();

            if (megaApi.areCredentialsVerified(contact)) {
                showSnackbar(getString(R.string.label_verified));
            }

            return;
        }

        showSnackbar(MegaApiJava.getTranslatedErrorString(e));
    }

    private void setMyCredentials() {
        ArrayList<String> myCredentialsList = getCredentialsList(megaApi.getMyCredentials());
        if (myCredentialsList.size() != 10) {
            logWarning("Error, my credentials are wrong");
            return;
        }

        myCredentials00.setText(myCredentialsList.get(0));
        myCredentials01.setText(myCredentialsList.get(1));
        myCredentials02.setText(myCredentialsList.get(2));
        myCredentials03.setText(myCredentialsList.get(3));
        myCredentials04.setText(myCredentialsList.get(4));
        myCredentials10.setText(myCredentialsList.get(5));
        myCredentials11.setText(myCredentialsList.get(6));
        myCredentials12.setText(myCredentialsList.get(7));
        myCredentials13.setText(myCredentialsList.get(8));
        myCredentials14.setText(myCredentialsList.get(9));
    }

    private void setContactCredentials(String contactCredentials) {
        ArrayList<String> contactCredentialsList = getCredentialsList(contactCredentials);
        if (contactCredentialsList.size() != 10) {
            logWarning("Error, the contact's credentials are wrong");
            return;
        }

        contactCredentials00.setText(contactCredentialsList.get(0));
        contactCredentials01.setText(contactCredentialsList.get(1));
        contactCredentials02.setText(contactCredentialsList.get(2));
        contactCredentials03.setText(contactCredentialsList.get(3));
        contactCredentials04.setText(contactCredentialsList.get(4));
        contactCredentials10.setText(contactCredentialsList.get(5));
        contactCredentials11.setText(contactCredentialsList.get(6));
        contactCredentials12.setText(contactCredentialsList.get(7));
        contactCredentials13.setText(contactCredentialsList.get(8));
        contactCredentials14.setText(contactCredentialsList.get(9));
    }

    public void setContactCredentials(MegaRequest request, MegaError e) {
        if (e.getErrorCode() == MegaError.API_OK && request.getFlag()) {
            setContactCredentials(request.getPassword());
            return;
        }

        showSnackbar(MegaApiJava.getTranslatedErrorString(e));
    }

    private ArrayList<String> getCredentialsList(String credentials) {
        ArrayList<String> credentialsList = new ArrayList<>();

        if (isTextEmpty(credentials)) {
            logWarning("Error getting credentials list");
            return credentialsList;
        }

        int index = 0;
        for (int i = 0; i < LENGHT_CREDENTIALS_LIST; i++) {
            credentialsList.add(credentials.substring(index, index + LENGHT_CREDENTIALS_CHARACTERS));
            index += LENGHT_CREDENTIALS_CHARACTERS;
        }

        return credentialsList;
    }

    public void showSnackbar(String s) {
        showSnackbar(authenticityCredentialsLayout, s);
    }
}
