package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.adapters.AddContactsLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.MegaAddContactsLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.MegaContactsLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.PhoneContactsLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.ShareContactsAdapter;
import mega.privacy.android.app.lollipop.adapters.ShareContactsHeaderAdapter;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;


public class AddContactActivityLollipop extends PinActivityLollipop implements View.OnClickListener, RecyclerView.OnItemTouchListener{

    public static String ACTION_PICK_CONTACT_SHARE_FOLDER = "ACTION_PICK_CONTACT_SHARE_FOLDER";
    public static String ACTION_PICK_CONTACT_SEND_FILE = "ACTION_PICK_CONTACT_SEND_FILE";

    DisplayMetrics outMetrics;
    MegaApplication app;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    DatabaseHandler dbH = null;
    int contactType = 0;
    int multipleSelectIntent;
    int sendToInbox;
    long nodeHandle = -1;
    long[] nodeHandles;
    Handler handler;
    long chatId = -1;

    AddContactActivityLollipop addContactActivityLollipop;

    Toolbar tB;
    ActionBar aB;

    RelativeLayout containerContacts;
    RecyclerView recyclerViewList;
    LinearLayoutManager linearLayoutManager;
    ImageView emptyImageView;
    TextView emptyTextView;
    ProgressBar progressBar;
    private RelativeLayout contactErrorLayout;
    private RelativeLayout notPermitedAddContacts;
    RecyclerView addedContactsRecyclerView;
    RelativeLayout containerAddedContactsRecyclerView;
    LinearLayout separator;
    LinearLayoutManager mLayoutManager;
    String inputString  ="";

//    Adapter list MEGA contacts
    MegaContactsLollipopAdapter adapterMEGA;
//    Adapter list chips MEGA contacts
    MegaAddContactsLollipopAdapter adapterMEGAContacts;

    ArrayList<MegaUser> contactsMEGA;
    ArrayList<MegaContactAdapter> visibleContactsMEGA = new ArrayList<>();
    ArrayList<MegaContactAdapter> filteredContactMEGA = new ArrayList<>();
    ArrayList<MegaContactAdapter> addedContactsMEGA = new ArrayList<>();


//    Adapter list Phone contacts
    PhoneContactsLollipopAdapter adapterPhone;
//    Adapter list chips Phone contacts
    AddContactsLollipopAdapter adapterContacts;

    ArrayList<PhoneContactInfo> phoneContacts = new ArrayList<>();
    ArrayList<PhoneContactInfo> addedContactsPhone = new ArrayList<>();
    ArrayList<PhoneContactInfo> filteredContactsPhone = new ArrayList<>();

//    Adapter list Share contacts
    ShareContactsHeaderAdapter adapterShareHeader;
//    Adapter list chips MEGA/Phone contacts
    ShareContactsAdapter adapterShare;

    ArrayList<ShareContactInfo> addedContactsShare = new ArrayList<>();
    ArrayList<ShareContactInfo> shareContacts = new ArrayList<>();
    ArrayList<ShareContactInfo> filteredShareContacts = new ArrayList<>();

    RelativeLayout relativeLayout;

    ArrayList<String> savedaddedContacts = new ArrayList<>();

    boolean itemClickPressed = false;

    public static String EXTRA_MEGA_CONTACTS = "mega_contacts";
    public static String EXTRA_CONTACTS = "extra_contacts";
    public static String EXTRA_NODE_HANDLE = "node_handle";

    private MenuItem sendInvitationMenuItem;

    private boolean comesFromChat;

    private RelativeLayout headerContacts;
    private TextView textHeader;

    private boolean filteredContactsPhoneIsEmpty  = false;

    private boolean fromAchievements = false;
    private ArrayList<String> mailsFromAchievements;

    private MenuItem searchMenuItem;
    private SearchView.SearchAutoComplete searchAutoComplete;
    private boolean searchExpand = false;

    private FilterContactsTask filterContactsTask;
    private GetContactsTask getContactsTask;
    private RecoverContactsTask recoverContactsTask;

    private class GetContactsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            if (contactType == Constants.CONTACT_TYPE_MEGA) {
                getVisibleMEGAContacts();
            }
            else if (contactType == Constants.CONTACT_TYPE_DEVICE) {
                phoneContacts.clear();
                filteredContactsPhone.clear();
                phoneContacts = getPhoneContacts();
                for (int i = 0; i < phoneContacts.size(); i++) {
                    filteredContactsPhone.add(phoneContacts.get(i));
                }
            }
            else {
                phoneContacts.clear();
                filteredContactsPhone.clear();
                phoneContacts = getPhoneContacts();
                for (int i = 0; i < phoneContacts.size(); i++) {
                    filteredContactsPhone.add(phoneContacts.get(i));
                }
                getVisibleMEGAContacts();

                MegaContactAdapter contactMEGA;
                PhoneContactInfo contactPhone;
                ShareContactInfo contact;
                boolean found;
                shareContacts.clear();
                if (filteredContactMEGA != null && !filteredContactMEGA.isEmpty()) {
                    shareContacts.add(new ShareContactInfo(true, true, false));
                    for (int i = 0; i<filteredContactMEGA.size(); i++) {
                        contactMEGA = filteredContactMEGA.get(i);
                        contact = new ShareContactInfo(null, contactMEGA, null);
                        shareContacts.add(contact);
                    }
                    shareContacts.get(shareContacts.size()-1).setLastItem(true);
                }
                if (filteredContactsPhone != null && !filteredContactsPhone.isEmpty()) {
                    shareContacts.add(new ShareContactInfo(true, false, true));
                    for (int i=0; i<filteredContactsPhone.size(); i++) {
                        found = false;
                        contactPhone = filteredContactsPhone.get(i);
                        for (int j=0; j<filteredContactMEGA.size(); j++) {
                            contactMEGA = filteredContactMEGA.get(j);
                            if (contactPhone.getEmail().equals(getMegaContactMail(contactMEGA))){
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            shareContacts.add(new ShareContactInfo(contactPhone, null, null));
                        }
                        else {
                            filteredContactsPhone.remove(contactPhone);
                            i--;
                        }
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void avoid) {
            progressBar.setVisibility(View.GONE);

            if (contactType == Constants.CONTACT_TYPE_MEGA) {
                if (adapterMEGA == null) {
                    adapterMEGA = new MegaContactsLollipopAdapter(addContactActivityLollipop, null, filteredContactMEGA, recyclerViewList, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
                } else {
                    adapterMEGA.setContacts(filteredContactMEGA);
                    adapterMEGA.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
                }

                adapterMEGA.setPositionClicked(-1);
                recyclerViewList.setAdapter(adapterMEGA);

                if (adapterMEGA.getItemCount() == 0) {

                    emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
                    String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
                    try{
                        textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                    }
                    catch (Exception e){}
                    Spanned result = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(textToShow);
                    }
                    emptyTextView.setText(result);
                    headerContacts.setVisibility(View.GONE);
                    recyclerViewList.setVisibility(View.GONE);
                    emptyImageView.setVisibility(View.VISIBLE);
                    emptyTextView.setVisibility(View.VISIBLE);
                } else {
                    headerContacts.setVisibility(View.VISIBLE);
                    recyclerViewList.setVisibility(View.VISIBLE);
                    emptyImageView.setVisibility(View.GONE);
                    emptyTextView.setVisibility(View.GONE);
                }
            }
            else if (contactType == Constants.CONTACT_TYPE_DEVICE){
                if(filteredContactsPhone!=null){
                    if (filteredContactsPhone.size() == 0){
                        headerContacts.setVisibility(View.GONE);
                        String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
                        try{
                            textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                            textToShow = textToShow.replace("[/A]", "</font>");
                            textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                            textToShow = textToShow.replace("[/B]", "</font>");
                        }
                        catch (Exception e){}
                        Spanned result = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                        } else {
                            result = Html.fromHtml(textToShow);
                        }
                        emptyTextView.setText(result);
                    }
                    else {
                        emptyTextView.setText(R.string.contacts_list_empty_text_loading);
                    }

                    if (adapterPhone == null){
                        adapterPhone = new PhoneContactsLollipopAdapter(addContactActivityLollipop, filteredContactsPhone);

                        recyclerViewList.setAdapter(adapterPhone);

                        adapterPhone.SetOnItemClickListener(new PhoneContactsLollipopAdapter.OnItemClickListener() {

                            @Override
                            public void onItemClick(View view, int position) {
                                itemClick(view, position);
                            }
                        });
                    }
                    else{
                        adapterPhone.setContacts(filteredContactsPhone);
                    }

                    if(adapterPhone!=null){
                        if (adapterPhone.getItemCount() == 0){
                            filteredContactsPhoneIsEmpty = true;
                            headerContacts.setVisibility(View.GONE);
                            recyclerViewList.setVisibility(View.GONE);
                            if (contactType == Constants.CONTACT_TYPE_BOTH) {
                                if (adapterMEGA != null) {
                                    if (adapterMEGA.getItemCount() == 0) {
                                        emptyImageView.setVisibility(View.VISIBLE);
                                        emptyTextView.setVisibility(View.VISIBLE);
                                    }
                                    else {
                                        emptyImageView.setVisibility(View.GONE);
                                        emptyTextView.setVisibility(View.GONE);
                                    }
                                }
                            }
                            else {
                                emptyImageView.setVisibility(View.VISIBLE);
                                emptyTextView.setVisibility(View.VISIBLE);
                            }
                        }
                        else{
                            filteredContactsPhoneIsEmpty = false;
                            headerContacts.setVisibility(View.VISIBLE);
                            recyclerViewList.setVisibility(View.VISIBLE);
                            emptyImageView.setVisibility(View.GONE);
                            emptyTextView.setVisibility(View.GONE);
                        }
                    }
                }
                else{
                    log("PhoneContactsTask: Phone contacts null");
                    boolean hasReadContactsPermission = (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
                    if (!hasReadContactsPermission) {
                        log("PhoneContactsTask: No read contacts permission");
                    }
                }
            }
            else {
                if (!shareContacts.isEmpty() && shareContacts.size() > 2){
                    filteredShareContacts.clear();
                    for (int i=0; i<shareContacts.size(); i++) {
                        filteredShareContacts.add(shareContacts.get(i));
                    }
                    if (adapterShareHeader == null){
                        adapterShareHeader = new ShareContactsHeaderAdapter(addContactActivityLollipop, filteredShareContacts);
                        recyclerViewList.setAdapter(adapterShareHeader);
                        adapterShareHeader.SetOnItemClickListener(new ShareContactsHeaderAdapter.OnItemClickListener() {

                            @Override
                            public void onItemClick(View view, int position) {
                                itemClick(view, position);
                            }
                        });
                    }
                    else{
                        adapterShareHeader.setContacts(filteredShareContacts);
                    }
                    emptyImageView.setVisibility(View.GONE);
                    emptyTextView.setVisibility(View.GONE);
                }
                else {
                    emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
                    String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
                    try{
                        textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                    }
                    catch (Exception e){}
                    Spanned result = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(textToShow);
                    }
                    emptyTextView.setText(result);
                }
            }
            setTitleAB();
            setRecyclersVisibility();
            setSeparatorVisibility();
        }
    }

    private class FilterContactsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (contactType == Constants.CONTACT_TYPE_MEGA) {

            }
            else if (contactType == Constants.CONTACT_TYPE_DEVICE) {
                //                    }
//                    else {
//                        for (int i = 0; i < phoneContacts.size(); i++) {
//                            boolean found = false;
//                            for (int j = 0; j < addedContactsPhone.size(); j++) {
//                                if (phoneContacts.get(i).getEmail().equals(addedContactsPhone.get(j).getEmail())) {
//                                    log("found true");
//                                    found = true;
//                                    break;
//                                }
//                            }
//                            if (!found) {
//                                log("!found -> filteredContacts.add(visibleContacts.get(" + i + ") = " + phoneContacts.get(i).getEmail());
//                                filteredContactsPhone.add(phoneContacts.get(i));
//                            }
//                        }
//                    }


//                    if (filteredContactsPhone.size() == 0) {
//                        String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
//                        try{
//                            textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
//                            textToShow = textToShow.replace("[/A]", "</font>");
//                            textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
//                            textToShow = textToShow.replace("[/B]", "</font>");
//                        }
//                        catch (Exception e){}
//                        Spanned result = null;
//                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
//                            result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
//                        } else {
//                            result = Html.fromHtml(textToShow);
//                        }
//                        emptyTextView.setText(result);
//                    }
            }
            else {

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {
            if (contactType == Constants.CONTACT_TYPE_MEGA) {

            }
            else if (contactType == Constants.CONTACT_TYPE_DEVICE) {

            }
            else {

            }
        }
    }

    private class RecoverContactsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (contactType == Constants.CONTACT_TYPE_MEGA) {
                getVisibleMEGAContacts();
                String contactToAddMail = null;
                filteredContactMEGA.clear();
                addedContactsMEGA.clear();
                MegaContactAdapter contactToAdd, contact;
                for (int i=0; i<savedaddedContacts.size(); i++){
                    String mail = savedaddedContacts.get(i);
                    log("mail["+i+"]: "+mail);
                    for (int j=0;j<visibleContactsMEGA.size(); j++){
                        contact = visibleContactsMEGA.get(j);
                        contactToAddMail = getMegaContactMail(contact);
                        if (contactToAddMail != null && contactToAddMail.equals(mail)){
                            if (!addedContactsMEGA.contains(contact)) {
                                addedContactsMEGA.add(contact);
                            }
                            break;
                        }
                        else if (!filteredContactMEGA.contains(contact) && !addedContactsMEGA.contains(contact)) {
                            log("filteredContactMEGA: "+i);
                            filteredContactMEGA.add(contact);
                        }
                    }
                    if (contactToAddMail != null && !contactToAddMail.equals(mail)){
                        contactToAdd = new MegaContactAdapter(null, null, mail);
                        if (!addedContactsMEGA.contains(contactToAdd)) {
                            addedContactsMEGA.add(contactToAdd);
                        }
                    }
                }
                for (int i=0; i<visibleContactsMEGA.size(); i++){
                    contact = visibleContactsMEGA.get(i);
                    if (!addedContactsMEGA.contains(contact) && !filteredContactMEGA.contains(contact)){
                        filteredContactMEGA.add(contact);
                    }
                }
                for (int i=0; i<filteredContactMEGA.size(); i++){
                    contact = filteredContactMEGA.get(i);
                    if (addedContactsMEGA.contains(contact)){
                        filteredContactMEGA.remove(contact);
                        i--;
                    }
                }
            }
            else {
                phoneContacts.clear();
                filteredContactsPhone.clear();
                phoneContacts = getPhoneContacts();
                for (int i = 0; i < phoneContacts.size(); i++) {
                    filteredContactsPhone.add(phoneContacts.get(i));
                }
                getVisibleMEGAContacts();

                MegaContactAdapter contactMEGA;
                PhoneContactInfo contactPhone;
                ShareContactInfo contact = null;
                boolean found;
                shareContacts.clear();

                if (filteredContactMEGA != null && !filteredContactMEGA.isEmpty()) {
                    for (int i = 0; i<filteredContactMEGA.size(); i++) {
                        contactMEGA = filteredContactMEGA.get(i);
                        contact = new ShareContactInfo(null, contactMEGA, null);
                        shareContacts.add(contact);
                    }
                    shareContacts.get(shareContacts.size()-1).setLastItem(true);
                }
                if (filteredContactsPhone != null && !filteredContactsPhone.isEmpty()) {
                    for (int i=0; i<filteredContactsPhone.size(); i++) {
                        found = false;
                        contactPhone = filteredContactsPhone.get(i);
                        for (int j=0; j<filteredContactMEGA.size(); j++) {
                            contactMEGA = filteredContactMEGA.get(j);
                            if (contactPhone.getEmail().equals(getMegaContactMail(contactMEGA))){
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            shareContacts.add(new ShareContactInfo(contactPhone, null, null));
                        }
                        else {
                            filteredContactsPhone.remove(contactPhone);
                            i--;
                        }
                    }
                }
                filteredShareContacts.clear();
                addedContactsShare.clear();
                String contactToAddMail = null;
                ArrayList<PhoneContactInfo> temporalContactPhone = new ArrayList<>();
                ArrayList<MegaContactAdapter> temporalContactMega = new ArrayList<>();

                for (int i=0; i<savedaddedContacts.size(); i++){
                    String mail = savedaddedContacts.get(i);
                    log("mail["+i+"]: "+mail);
                    for (int j=0;j<shareContacts.size(); j++){
                        contact = shareContacts.get(j);
                        if (contact.isMegaContact()){
                            contactToAddMail = getMegaContactMail(contact.getMegaContactAdapter());
                        }
                        else {
                            contactToAddMail = contact.getPhoneContactInfo().getEmail();
                        }
                        if (contactToAddMail != null && contactToAddMail.equals(mail)){
                            if (!addedContactsShare.contains(contact)) {
                                addedContactsShare.add(contact);
                            }
                            break;
                        }
                        else if (!filteredShareContacts.contains(contact) && !addedContactsShare.contains(contact)) {
                            log("filteredContactMEGA: "+i);
                            if (contact.isMegaContact()) {
                                temporalContactMega.add(contact.getMegaContactAdapter());
                            }
                            else {
                                temporalContactPhone.add(contact.getPhoneContactInfo());
                            }
                        }
                    }
                    if (contactToAddMail != null && !contactToAddMail.equals(mail)){
                        contact = new ShareContactInfo(null, null, mail);
                        if (!addedContactsShare.contains(contact)) {
                            addedContactsShare.add(contact);
                        }
                    }
                }
                for (int i=0; i<shareContacts.size(); i++){
                    contact = shareContacts.get(i);
                    if (!addedContactsShare.contains(contact) && !filteredShareContacts.contains(contact)){
                        if (contact.isMegaContact()){
                            temporalContactMega.add(contact.getMegaContactAdapter());
                        }
                        else {
                            temporalContactPhone.add(contact.getPhoneContactInfo());
                        }
                    }
                }

                filteredShareContacts.clear();
                if (temporalContactMega.size() > 0) {
                    filteredContactMEGA.clear();
                    Collections.sort(temporalContactMega, new Comparator<MegaContactAdapter>() {

                        public int compare(MegaContactAdapter c1, MegaContactAdapter c2) {
                            String name1 = c1.getFullName();
                            String name2 = c2.getFullName();
                            int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
                            if (res == 0) {
                                res = name1.compareTo(name2);
                            }
                            return res;
                        }
                    });
                    filteredShareContacts.add(new ShareContactInfo(true, true, false));
                    for (int i=0; i<temporalContactMega.size(); i++){
                        filteredContactMEGA.add(temporalContactMega.get(i));
                        contact = new ShareContactInfo(null, temporalContactMega.get(i), null);
                        filteredShareContacts.add(contact);
                    }
                    contact.setLastItem(true);
                }
                if (temporalContactPhone.size() > 0) {
                    filteredContactsPhone.clear();
                    Collections.sort(temporalContactPhone);
                    filteredShareContacts.add(new ShareContactInfo(true, false, true));
                    for (int i=0; i<temporalContactPhone.size(); i++) {
                        filteredContactsPhone.add(temporalContactPhone.get(i));
                        filteredShareContacts.add(new ShareContactInfo(temporalContactPhone.get(i), null, null));
                    }
                }
                for (int i = 0; i < filteredShareContacts.size(); i++) {
                    contact = filteredShareContacts.get(i);
                    if (addedContactsShare.contains(contact)) {
                        filteredShareContacts.remove(contact);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (contactType == Constants.CONTACT_TYPE_MEGA) {
                if (adapterMEGAContacts == null){
                    adapterMEGAContacts = new MegaAddContactsLollipopAdapter(addContactActivityLollipop, addedContactsMEGA);
                }
                else {
                    adapterMEGAContacts.setContacts(addedContactsMEGA);
                }

                if (addedContactsMEGA.size() == 0){
                    containerAddedContactsRecyclerView.setVisibility(View.GONE);
                }
                else {
                    containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
                }

                addedContactsRecyclerView.setAdapter(adapterMEGAContacts);

                setSendInvitationVisibility();

                if (adapterMEGA == null) {
                    adapterMEGA = new MegaContactsLollipopAdapter(addContactActivityLollipop, null, filteredContactMEGA, recyclerViewList, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
                } else {
                    adapterMEGA.setContacts(filteredContactMEGA);
                    adapterMEGA.setAdapterType(MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
                }

                adapterMEGA.setPositionClicked(-1);
                recyclerViewList.setAdapter(adapterMEGA);

                if (adapterMEGA.getItemCount() == 0) {

                    emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
                    String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
                    try{
                        textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                    }
                    catch (Exception e){}
                    Spanned result = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(textToShow);
                    }
                    emptyTextView.setText(result);
                    headerContacts.setVisibility(View.GONE);
                    recyclerViewList.setVisibility(View.GONE);
                    emptyImageView.setVisibility(View.VISIBLE);
                    emptyTextView.setVisibility(View.VISIBLE);
                } else {
                    headerContacts.setVisibility(View.VISIBLE);
                    recyclerViewList.setVisibility(View.VISIBLE);
                    emptyImageView.setVisibility(View.GONE);
                    emptyTextView.setVisibility(View.GONE);
                }
            }
            else {
                if (adapterShare == null) {
                    adapterShare = new ShareContactsAdapter(addContactActivityLollipop, addedContactsShare);
                }
                else {
                    adapterShare.setContacts(addedContactsShare);
                }

                if (addedContactsShare.size() == 0){
                    containerAddedContactsRecyclerView.setVisibility(View.GONE);
                }
                else {
                    containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
                }

                addedContactsRecyclerView.setAdapter(adapterShare);

                setSendInvitationVisibility();

                if (adapterShareHeader == null){
                    adapterShareHeader = new ShareContactsHeaderAdapter(addContactActivityLollipop, filteredShareContacts);
                    recyclerViewList.setAdapter(adapterShareHeader);
                    adapterShareHeader.SetOnItemClickListener(new ShareContactsHeaderAdapter.OnItemClickListener() {

                        @Override
                        public void onItemClick(View view, int position) {
                            itemClick(view, position);
                        }
                    });
                }
                else{
                    adapterShareHeader.setContacts(filteredShareContacts);
                }

                if (adapterShareHeader.getItemCount() == 0){
                    emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
                    String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
                    try{
                        textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                        textToShow = textToShow.replace("[/A]", "</font>");
                        textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                        textToShow = textToShow.replace("[/B]", "</font>");
                    }
                    catch (Exception e){}
                    Spanned result = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(textToShow);
                    }
                    emptyTextView.setText(result);
                }
                else {
                    emptyImageView.setVisibility(View.GONE);
                    emptyTextView.setVisibility(View.GONE);
                }
            }
            setTitleAB();
            setRecyclersVisibility();
            setSeparatorVisibility();
        }
    }



    public final static boolean isValidEmail(CharSequence target) {
        if (target == null) {
            return false;
        } else {
            log("isValid");
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    public void setSendInvitationVisibility() {
        if (sendInvitationMenuItem != null) {
            if (contactType == Constants.CONTACT_TYPE_MEGA && adapterMEGAContacts != null) {
                if (adapterMEGAContacts.getItemCount() > 0 && !searchExpand){
                    sendInvitationMenuItem.setVisible(true);
                }
                else {
                    sendInvitationMenuItem.setVisible(false);
                }
            }
            else if (contactType == Constants.CONTACT_TYPE_DEVICE && adapterContacts != null) {
                if (adapterContacts.getItemCount() > 0  && !searchExpand){
                    sendInvitationMenuItem.setVisible(true);
                }
                else {
                    sendInvitationMenuItem.setVisible(false);
                }
            }
            else if (adapterShare != null){
                if (adapterShare.getItemCount() > 0  && !searchExpand){
                    sendInvitationMenuItem.setVisible(true);
                }
                else {
                    sendInvitationMenuItem.setVisible(false);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_add_contact, menu);

        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchView.setIconifiedByDefault(true);

        searchAutoComplete = (SearchView.SearchAutoComplete) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchAutoComplete.setTextColor(ContextCompat.getColor(this, R.color.mail_my_account));
        searchAutoComplete.setHintTextColor(ContextCompat.getColor(this, R.color.mail_my_account));
        searchAutoComplete.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

        ImageView closeIcon = (ImageView) searchView.findViewById(android.support.v7.appcompat.R.id.search_close_btn);
        closeIcon.setImageDrawable(Util.mutateIcon(this, R.drawable.ic_close_white, R.color.add_contact_icons));

        searchAutoComplete.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                refreshKeyboard();
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String s = v.getText().toString();
                    log("s: "+s);
                    if (s.isEmpty() || s.equals("null") || s.equals("")) {
                        hideKeyboard();
                    }
                    else {
                        if (contactType == Constants.CONTACT_TYPE_MEGA) {
                            if (!comesFromChat) {
                                boolean isValid = isValidEmail(s.trim());
                                if (isValid) {
                                    MegaContactAdapter contact = new MegaContactAdapter(null, null, s.trim());
                                    addContactMEGA(contact);
                                    searchAutoComplete.getText().clear();
                                    inputString = "";
                                }
                                else {
                                    setError();
                                }
                            }
                            else {
                                setError();
                            }
                        }
                        else if (contactType == Constants.CONTACT_TYPE_DEVICE) {
                            boolean isValid = isValidEmail(s.trim());
                            if (isValid) {
                                PhoneContactInfo contact = new PhoneContactInfo(0, null, s.trim(), null);
                                addContact(contact);
                                searchAutoComplete.getText().clear();
                                inputString = "";
                            }
                            else {
                                setError();
                            }
                        }
                        else {
                            boolean isValid = isValidEmail(s.trim());
                            if (isValid) {
                                ShareContactInfo contact = new ShareContactInfo(null, null, s.trim());
                                addShareContact(contact);
                                searchAutoComplete.getText().clear();
                                inputString = "";
                            }
                            else {
                                setError();
                            }
                        }
                        if (filterContactsTask.getStatus() == AsyncTask.Status.RUNNING){
                            filterContactsTask.cancel(true);
                        }
                        filterContactsTask = new FilterContactsTask();
                        filterContactsTask.execute();

                    }
                    return true;
                }
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_SEND)) {
                    if (contactType == Constants.CONTACT_TYPE_DEVICE){
                        if (addedContactsPhone.isEmpty() || addedContactsPhone == null) {
                            hideKeyboard();
                        }
                        else {
                            inviteContacts(addedContactsPhone);
                        }
                    }
                    else if (contactType == Constants.CONTACT_TYPE_MEGA){
                        if (addedContactsMEGA.isEmpty() || addedContactsMEGA == null) {
                            hideKeyboard();
                        }
                        else {
                            setResultContacts(addedContactsMEGA, true);
                        }
                    }
                    else {
                        if (addedContactsShare.isEmpty() || addedContactsShare == null) {
                            hideKeyboard();
                        }
                        else {
                            shareWith(addedContactsShare);
                        }
                    }
                    return true;
                }
                return false;
            }
        });
        searchAutoComplete.setImeOptions(EditorInfo.IME_ACTION_DONE);
        searchAutoComplete.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                quitError();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                log("onTextChanged: " + s.toString() + "_ " + start + "__" + before + "__" + count);
                if (contactType == Constants.CONTACT_TYPE_MEGA){
                    if (s != null){
                        if (s.length() > 0) {
                            inputString = s.toString();
                            String temp = s.toString();
                            char last = s.charAt(s.length()-1);
                            if(last == ' '){
                                if (!comesFromChat){
                                    boolean isValid = isValidEmail(temp.trim());
                                    if(isValid){
                                        MegaContactAdapter contact = new MegaContactAdapter(null, null, temp.trim());
                                        addContactMEGA(contact);
                                        searchAutoComplete.getText().clear();
                                        inputString = "";
                                    }
                                    else{
                                        setError();
                                    }
                                }
                                else {
                                    setError();
                                }
                            }
                            else{
                                log("Last character is: "+last);
                            }
                        }
                        else{
                            inputString = "";
                        }
                    }
                }
                else if (contactType == Constants.CONTACT_TYPE_DEVICE){
                    if (s != null) {
                        if (s.length() > 0) {
                            inputString = s.toString();
                            String temp = s.toString();
                            char last = s.charAt(s.length()-1);
                            if(last == ' '){
                                boolean isValid = isValidEmail(temp.trim());
                                if(isValid){
                                    PhoneContactInfo contact = new PhoneContactInfo(0, null, temp.trim(), null);
                                    addContact(contact);
                                    searchAutoComplete.getText().clear();
                                    inputString = "";
                                }
                                else{
                                    setError();
                                }
                            }
                            else{
                                log("Last character is: "+last);
                            }
                        }
                        else{
                            inputString = "";
                        }
                    }
                }
                else {
                    if (s != null) {
                        if (s.length() > 0) {
                            inputString = s.toString();
                            String temp = s.toString();
                            char last = s.charAt(s.length()-1);
                            if (last == ' '){
                                boolean isValid = isValidEmail(temp.trim());
                                if (isValid){
                                    ShareContactInfo contact = new ShareContactInfo(null, null, temp.trim());
                                    addShareContact(contact);
                                    searchAutoComplete.getText().clear();
                                    inputString = "";
                                }
                                else {
                                    setError();
                                }
                            }
                            else{
                                log("Last character is: "+last);
                            }
                        }
                        else {
                            inputString = "";
                        }
                    }
                }
                if (filterContactsTask.getStatus() == AsyncTask.Status.RUNNING){
                    filterContactsTask.cancel(true);
                }
                filterContactsTask = new FilterContactsTask();
                filterContactsTask.execute();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                refreshKeyboard();
            }
        });

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                log("onMenuItemActionExpand");
                searchExpand = true;
                setSendInvitationVisibility();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                log("onMenuItemActionCollapse");
                searchExpand = false;
                setSendInvitationVisibility();
                setTitleAB();
                return true;
            }
        });
        searchView.setIconifiedByDefault(true);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }

        });

        sendInvitationMenuItem = menu.findItem(R.id.action_send_invitation);
        sendInvitationMenuItem.setIcon(Util.mutateIcon(this, R.drawable.ic_send_white, R.color.accentColor));
        setSendInvitationVisibility();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        log("onPrepareOptionsMenu");

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");
        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

        int id = item.getItemId();
        switch(id) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
            case R.id.action_send_invitation:{
                if (contactType == Constants.CONTACT_TYPE_DEVICE){
                    inviteContacts(addedContactsPhone);
                }
                else if (contactType == Constants.CONTACT_TYPE_MEGA){
                    setResultContacts(addedContactsMEGA, true);
                }
                else {
                    shareWith(addedContactsShare);
                }
                hideKeyboard();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void hideKeyboard () {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void refreshKeyboard() {

        String s = inputString;
        if (s != null) {
            if (s.length() == 0 && (!addedContactsMEGA.isEmpty() || !addedContactsPhone.isEmpty() || !addedContactsShare.isEmpty())){
                searchAutoComplete.setImeOptions(EditorInfo.IME_ACTION_SEND);
            }
            else {
                searchAutoComplete.setImeOptions(EditorInfo.IME_ACTION_DONE);
            }
        }
        else if (!addedContactsMEGA.isEmpty() || !addedContactsPhone.isEmpty() || !addedContactsShare.isEmpty()) {
            searchAutoComplete.setImeOptions(EditorInfo.IME_ACTION_SEND);
        }
        else {
            searchAutoComplete.setImeOptions(EditorInfo.IME_ACTION_DONE);
        }

        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            //inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            inputMethodManager.restartInput(view);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("fromAchievements", fromAchievements);
        outState.putStringArrayList("mailsFromAchievements", mailsFromAchievements);

        if (getContactsTask != null && getContactsTask.getStatus() == AsyncTask.Status.RUNNING){
            getContactsTask.cancel(true);
        }
        else if (filterContactsTask != null && filterContactsTask.getStatus() == AsyncTask.Status.RUNNING){
            filterContactsTask.cancel(true);
        }
        else if (recoverContactsTask != null && recoverContactsTask.getStatus() == AsyncTask.Status.RUNNING) {
            recoverContactsTask.cancel(true);
        }
        else {

        }
        savedaddedContacts.clear();
        if (contactType == Constants.CONTACT_TYPE_MEGA) {
            for (int i=0; i<addedContactsMEGA.size(); i++){
                if (getMegaContactMail(addedContactsMEGA.get(i)) != null) {
                    savedaddedContacts.add(getMegaContactMail(addedContactsMEGA.get(i)));
                }
            }
            outState.putStringArrayList("savedaddedContacts", savedaddedContacts);
        }
        else if (contactType == Constants.CONTACT_TYPE_DEVICE) {
            outState.putParcelableArrayList("addedContactsPhone", addedContactsPhone);
            outState.putParcelableArrayList("filteredContactsPhone", filteredContactsPhone);
            outState.putParcelableArrayList("phoneContacts", phoneContacts);
        }
        else {
            for (int i=0; i<addedContactsShare.size(); i++){
                if (addedContactsShare.get(i).isMegaContact()) {
                    savedaddedContacts.add(getMegaContactMail(addedContactsShare.get(i).getMegaContactAdapter()));
                }
                else if (addedContactsShare.get(i).isPhoneContact()) {
                    savedaddedContacts.add(addedContactsShare.get(i).getPhoneContactInfo().getEmail());
                }
            }
            outState.putStringArrayList("savedaddedContacts", savedaddedContacts);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");

        super.onCreate(savedInstanceState);

        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

        if (getIntent() != null){
            contactType = getIntent().getIntExtra("contactType", Constants.CONTACT_TYPE_MEGA);
            chatId = getIntent().getLongExtra("chatId", -1);
            fromAchievements = getIntent().getBooleanExtra("fromAchievements", false);
            if (fromAchievements){
                mailsFromAchievements = getIntent().getStringArrayListExtra(EXTRA_CONTACTS);
            }
            comesFromChat = getIntent().getBooleanExtra("chat", false);
            if (contactType == Constants.CONTACT_TYPE_MEGA || contactType == Constants.CONTACT_TYPE_BOTH){
                multipleSelectIntent = getIntent().getIntExtra("MULTISELECT", -1);
                if(multipleSelectIntent==0){
                    nodeHandle =  getIntent().getLongExtra(EXTRA_NODE_HANDLE, -1);
                }
                else if(multipleSelectIntent==1){
                    log("onCreate multiselect YES!");
                    nodeHandles=getIntent().getLongArrayExtra(EXTRA_NODE_HANDLE);
                }
                sendToInbox= getIntent().getIntExtra("SEND_FILE", -1);
            }
        }

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        app = (MegaApplication)getApplication();
        megaApi = app.getMegaApi();
        if(megaApi==null||megaApi.getRootNode()==null){
            log("Refresh session - sdk");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }
        if(Util.isChatEnabled()){
            if (megaChatApi == null){
                megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
            }

            if(megaChatApi==null||megaChatApi.getInitState()== MegaChatApi.INIT_ERROR){
                log("Refresh session - karere");
                Intent intent = new Intent(this, LoginActivityLollipop.class);
                intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return;
            }
        }

        addContactActivityLollipop = this;

        log("retryPendingConnections()");
        if (megaApi != null){
            log("---------retryPendingConnections");
            megaApi.retryPendingConnections();
        }

        dbH = DatabaseHandler.getDbHandler(this);
        handler = new Handler();
        setContentView(R.layout.activity_add_contact);

        tB = (Toolbar) findViewById(R.id.add_contact_toolbar);
        if(tB==null){
            log("Tb is Null");
            return;
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);

        relativeLayout = (RelativeLayout) findViewById(R.id.relative_container_add_contact);

        contactErrorLayout = (RelativeLayout) findViewById(R.id.add_contact_email_error);
        contactErrorLayout.setVisibility(View.GONE);
        notPermitedAddContacts = (RelativeLayout) findViewById(R.id.not_permited_add_contact_error);
        notPermitedAddContacts.setVisibility(View.GONE);
        addedContactsRecyclerView = (RecyclerView) findViewById(R.id.contact_adds_recycler_view);
        containerAddedContactsRecyclerView = (RelativeLayout) findViewById(R.id.contacts_adds_container);
        separator = (LinearLayout) findViewById(R.id.separator);
        containerAddedContactsRecyclerView.setVisibility(View.GONE);

        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        addedContactsRecyclerView.setLayoutManager(mLayoutManager);
        addedContactsRecyclerView.setItemAnimator(new DefaultItemAnimator());

        headerContacts = (RelativeLayout) findViewById(R.id.header_list);
        textHeader = (TextView) findViewById(R.id.text_header_list);

        linearLayoutManager = new LinearLayoutManager(this);
        recyclerViewList = (RecyclerView) findViewById(R.id.add_contact_list);
        recyclerViewList.setClipToPadding(false);
        recyclerViewList.setHasFixedSize(true);
        recyclerViewList.setLayoutManager(linearLayoutManager);
        recyclerViewList.addOnItemTouchListener(this);
        recyclerViewList.setItemAnimator(new DefaultItemAnimator());

        if (contactType == Constants.CONTACT_TYPE_MEGA) {
            headerContacts.setVisibility(View.VISIBLE);
            textHeader.setText(getString(R.string.contacts_mega));
            recyclerViewList.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
        }
        else if(contactType == Constants.CONTACT_TYPE_DEVICE) {
            headerContacts.setVisibility(View.VISIBLE);
            textHeader.setText(getString(R.string.contacts_phone));
            recyclerViewList.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
        }
        else {
            headerContacts.setVisibility(View.GONE);
        }

        containerContacts = (RelativeLayout) findViewById(R.id.container_list_contacts);

        emptyImageView = (ImageView) findViewById(R.id.add_contact_list_empty_image);
        emptyTextView = (TextView) findViewById(R.id.add_contact_list_empty_text);
        emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
        emptyTextView.setText(R.string.contacts_list_empty_text_loading_share);

        progressBar = (ProgressBar) findViewById(R.id.add_contact_progress_bar);

        //Get MEGA contacts and phone contacts: first name, last name and email
        if (savedInstanceState != null) {
            if (contactType == Constants.CONTACT_TYPE_MEGA || contactType == Constants.CONTACT_TYPE_BOTH) {
                savedaddedContacts = savedInstanceState.getStringArrayList("savedaddedContacts");

                if (savedaddedContacts == null) {
                    getContactsTask = new GetContactsTask();
                    getContactsTask.execute();
                }
                else {
                    recoverContactsTask = new RecoverContactsTask();
                    recoverContactsTask.execute();
                }
            }
            else if (contactType == Constants.CONTACT_TYPE_DEVICE){
                addedContactsPhone = savedInstanceState.getParcelableArrayList("addedContactsPhone");
                filteredContactsPhone = savedInstanceState.getParcelableArrayList("filteredContactsPhone");
                phoneContacts = savedInstanceState.getParcelableArrayList("phoneContacts");

                if (adapterContacts == null){
                    adapterContacts = new AddContactsLollipopAdapter(this, addedContactsPhone);
                }
                else {
                    adapterContacts.setContacts(addedContactsPhone);
                }

                if (addedContactsPhone.size() == 0){
                    containerAddedContactsRecyclerView.setVisibility(View.GONE);
                }
                else {
                    containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
                }

                addedContactsRecyclerView.setAdapter(adapterContacts);

                setSendInvitationVisibility();

                if (phoneContacts != null && !phoneContacts.isEmpty()) {
                    if (filteredContactsPhone == null || filteredContactsPhone.isEmpty()){
                        for (int i= 0; i<phoneContacts.size(); i++) {
                            filteredContactsPhone.add(phoneContacts.get(i));
                        }
                    }
                    if (adapterPhone == null) {
                        adapterPhone = new PhoneContactsLollipopAdapter(addContactActivityLollipop, filteredContactsPhone);
                        recyclerViewList.setAdapter(adapterPhone);
                        adapterPhone.SetOnItemClickListener(new PhoneContactsLollipopAdapter.OnItemClickListener() {

                            @Override
                            public void onItemClick(View view, int position) {
                                itemClick(view, position);
                            }
                        });
                    }
                    else{
                        adapterPhone.setContacts(filteredContactsPhone);
                    }

                    if(adapterPhone!=null){
                        if (adapterPhone.getItemCount() == 0){
                            filteredContactsPhoneIsEmpty = true;
                            headerContacts.setVisibility(View.GONE);
                            recyclerViewList.setVisibility(View.GONE);
                            if (contactType == Constants.CONTACT_TYPE_BOTH) {
                                if (adapterMEGA != null) {
                                    if (adapterMEGA.getItemCount() == 0) {
                                        emptyImageView.setVisibility(View.VISIBLE);
                                        emptyTextView.setVisibility(View.VISIBLE);
                                    }
                                    else {
                                        emptyImageView.setVisibility(View.GONE);
                                        emptyTextView.setVisibility(View.GONE);
                                    }
                                }
                            }
                            else {
                                emptyImageView.setVisibility(View.VISIBLE);
                                emptyTextView.setVisibility(View.VISIBLE);
                            }
                        }
                        else{
                            filteredContactsPhoneIsEmpty = false;
                            headerContacts.setVisibility(View.VISIBLE);
                            recyclerViewList.setVisibility(View.VISIBLE);
                            emptyImageView.setVisibility(View.GONE);
                            emptyTextView.setVisibility(View.GONE);
                        }
                    }
                }
                else {
                    emptyImageView.setVisibility(View.VISIBLE);
                    emptyTextView.setVisibility(View.VISIBLE);

                    progressBar.setVisibility(View.VISIBLE);
                    getContactsTask = new GetContactsTask();
                    getContactsTask.execute();
                }
            }
        }
        else {
            if (contactType == Constants.CONTACT_TYPE_MEGA) {

                if (adapterMEGAContacts == null){
                    adapterMEGAContacts = new MegaAddContactsLollipopAdapter(this, addedContactsMEGA);
                }

                addedContactsRecyclerView.setAdapter(adapterMEGAContacts);

                setSendInvitationVisibility();

                progressBar.setVisibility(View.VISIBLE);
                getContactsTask = new GetContactsTask();
                getContactsTask.execute();
            }
            else if (contactType == Constants.CONTACT_TYPE_DEVICE){
                if (adapterContacts == null){
                    adapterContacts = new AddContactsLollipopAdapter(this, addedContactsPhone);
                }

                addedContactsRecyclerView.setAdapter(adapterContacts);

                setSendInvitationVisibility();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    boolean hasReadContactsPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
                    if (!hasReadContactsPermission) {
                        log("No read contacts permission");
                        ActivityCompat.requestPermissions((AddContactActivityLollipop) this,
                                new String[]{Manifest.permission.READ_CONTACTS},
                                Constants.REQUEST_READ_CONTACTS);
                    }
                    else {
                        emptyImageView.setVisibility(View.VISIBLE);
                        emptyTextView.setVisibility(View.VISIBLE);

                        progressBar.setVisibility(View.VISIBLE);
                        getContactsTask = new GetContactsTask();
                        getContactsTask.execute();
                    }
                }
                else{
                    emptyImageView.setVisibility(View.VISIBLE);
                    emptyTextView.setVisibility(View.VISIBLE);

                    progressBar.setVisibility(View.VISIBLE);
                    getContactsTask = new GetContactsTask();
                    getContactsTask.execute();
                }
            }
            else {
                if (adapterShare == null) {
                    adapterShare = new ShareContactsAdapter(this, addedContactsShare);
                }

                addedContactsRecyclerView.setAdapter(adapterShare);

                setSendInvitationVisibility();

                progressBar.setVisibility(View.VISIBLE);
                emptyTextView.setText(R.string.contacts_list_empty_text_loading_share);

                getContactsTask = new GetContactsTask();
                getContactsTask.execute();
            }
        }
    }

    private void filterContactsMEGA(){
        log("filterContactsMEGA");
        boolean found;

        filteredContactMEGA.clear();
        if (contactType == Constants.CONTACT_TYPE_MEGA){
            if (inputString != null && inputString.equals("")){
                for (int i=0; i<visibleContactsMEGA.size(); i++){
                    found = false;
                    for (int j=0; j<addedContactsMEGA.size(); j++){
                        if (visibleContactsMEGA.get(i).equals(addedContactsMEGA.get(j))){
                            found = true;
                            break;
                        }
                    }
                    if (!found){
                        filteredContactMEGA.add(visibleContactsMEGA.get(i));
                    }
                }
            }
            else {
                for (int i=0; i<visibleContactsMEGA.size(); i++){
                    found = false;
                    if ((visibleContactsMEGA.get(i).getFullName().toUpperCase().contains(inputString.toUpperCase()) || visibleContactsMEGA.get(i).getMegaUser().getEmail().toUpperCase().contains(inputString.toUpperCase()))){
                        for (int j=0; j<addedContactsMEGA.size(); j++){
                            if (visibleContactsMEGA.get(i).equals(addedContactsMEGA.get(j))){
                                found = true;
                                break;
                            }
                        }
                        if (!found){
                            filteredContactMEGA.add(visibleContactsMEGA.get(i));
                        }
                    }
                }
            }
        }
        else  if (contactType == Constants.CONTACT_TYPE_BOTH){
            if (inputString != null && inputString.equals("")){
                for (int i=0; i<visibleContactsMEGA.size(); i++){
                    found = false;
                    for (int j=0; j<addedContactsShare.size(); j++){
                        if (addedContactsShare.get(j).isMegaContact()){
                            if (visibleContactsMEGA.get(i).equals(addedContactsShare.get(j).getMegaContactAdapter())){
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found){
                        filteredContactMEGA.add(visibleContactsMEGA.get(i));
                    }
                }
            }
            else {
                for (int i=0; i<visibleContactsMEGA.size(); i++){
                    found = false;
                    if ((visibleContactsMEGA.get(i).getFullName().toUpperCase().contains(inputString.toUpperCase()) || visibleContactsMEGA.get(i).getMegaUser().getEmail().toUpperCase().contains(inputString.toUpperCase()))){
                        for (int j=0; j<addedContactsShare.size(); j++){
                            if (addedContactsShare.get(j).isMegaContact()){
                                if (visibleContactsMEGA.get(i).equals(addedContactsShare.get(j).getMegaContactAdapter())){
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found){
                            filteredContactMEGA.add(visibleContactsMEGA.get(i));
                        }
                    }
                }
            }
        }

        adapterMEGA.setContacts(filteredContactMEGA);

        if (adapterMEGA.getItemCount() == 0){
            headerContacts.setVisibility(View.GONE);
            recyclerViewList.setVisibility(View.GONE);
        }
        else {
            headerContacts.setVisibility(View.VISIBLE);
            recyclerViewList.setVisibility(View.VISIBLE);
        }
    }

    private void setTitleAB() {
        log("setTitleAB");
        if (aB != null) {
            if (contactType == Constants.CONTACT_TYPE_MEGA){
                aB.setTitle(getString(R.string.group_chat_start_conversation_label));
                if (addedContactsMEGA.size() > 0){
                    aB.setSubtitle(addedContactsMEGA.size() + " " + getResources().getQuantityString(R.plurals.general_num_contacts, addedContactsMEGA.size()));
                }
                else {
                    aB.setSubtitle(null);
                }
            }
            else if (contactType == Constants.CONTACT_TYPE_DEVICE){
                aB.setTitle(getString(R.string.invite_contacts));
                if (addedContactsPhone.size() > 0){
                    aB.setSubtitle(addedContactsPhone.size() + " " + getResources().getQuantityString(R.plurals.general_num_contacts, addedContactsPhone.size()));
                }
                else {
                    aB.setSubtitle(null);
                }
            }
            else {
                aB.setTitle(getString(R.string.share_with));
                if (addedContactsShare.size() > 0){
                    aB.setSubtitle(addedContactsShare.size() + " " + getResources().getQuantityString(R.plurals.general_num_contacts, addedContactsShare.size()));
                }
                else {
                    aB.setSubtitle(null);
                }
            }
        }
    }

    private void setError(){
        log("setError");
        if (comesFromChat){
            notPermitedAddContacts.setVisibility(View.VISIBLE);
        }
        else {
            contactErrorLayout.setVisibility(View.VISIBLE);
        }
    }

    private void quitError(){
        if(contactErrorLayout.getVisibility() != View.GONE){
            log("quitError");
            contactErrorLayout.setVisibility(View.GONE);
        }
        if(notPermitedAddContacts.getVisibility() != View.GONE){
            log("quitError");
            notPermitedAddContacts.setVisibility(View.GONE);
        }
    }

    public void addShareContact (ShareContactInfo contact) {
        log("addShareContact");

        addedContactsShare.add(contact);
        adapterShare.setContacts(addedContactsShare);
        mLayoutManager.scrollToPosition(adapterShare.getItemCount()-1);
        setSendInvitationVisibility();
        containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
        setTitleAB();

        if (adapterShareHeader != null){
            if (adapterShareHeader.getItemCount() == 0){
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);

                String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
                try{
                    textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                }
                catch (Exception e){}
                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                emptyTextView.setText(result);
            }
            else {
                emptyImageView.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.GONE);
            }
        }
        setRecyclersVisibility();
        setSeparatorVisibility();
        refreshKeyboard();
    }

    public void addContactMEGA (MegaContactAdapter contact) {
        log("addContactMEGA: " + contact.getFullName());

        addedContactsMEGA.add(contact);
        adapterMEGAContacts.setContacts(addedContactsMEGA);
        mLayoutManager.scrollToPosition(adapterMEGAContacts.getItemCount()-1);
        setSendInvitationVisibility();
        containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
        setTitleAB();
        if (adapterMEGA != null){
            if (adapterMEGA.getItemCount() == 1){
                headerContacts.setVisibility(View.GONE);
                recyclerViewList.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);

                String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
                try{
                    textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                }
                catch (Exception e){}
                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                emptyTextView.setText(result);
            }
        }
        setRecyclersVisibility();
        setSeparatorVisibility();
        refreshKeyboard();
    }

    public void addContact (PhoneContactInfo contact){
        log("addContact: " + contact.getName()+" mail: " + contact.getEmail());

        addedContactsPhone.add(contact);
        adapterContacts.setContacts(addedContactsPhone);
        mLayoutManager.scrollToPosition(adapterContacts.getItemCount()-1);
        setSendInvitationVisibility();
        containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
        setTitleAB();

        if(adapterPhone!=null){
            if (adapterPhone.getItemCount() == 0){
                headerContacts.setVisibility(View.GONE);
                recyclerViewList.setVisibility(View.GONE);
                emptyImageView.setVisibility(View.VISIBLE);
                emptyTextView.setVisibility(View.VISIBLE);

                String textToShow = String.format(getString(R.string.context_empty_contacts), getString(R.string.section_contacts));
                try{
                    textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                }
                catch (Exception e){}
                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                emptyTextView.setText(result);
            }
        }
        setRecyclersVisibility();
        setSeparatorVisibility();
        refreshKeyboard();
    }

    public void deleteContact (int position){
        log("deleteContact: " +position);

        if (contactType == Constants.CONTACT_TYPE_MEGA){
            MegaContactAdapter deleteContact = addedContactsMEGA.get(position);
            addMEGAFilteredContact(deleteContact);
            addedContactsMEGA.remove(deleteContact);
            setSendInvitationVisibility();
            adapterMEGAContacts.setContacts(addedContactsMEGA);
            if (addedContactsMEGA.size() == 0){
                containerAddedContactsRecyclerView.setVisibility(View.GONE);
            }
        }
        else if (contactType == Constants.CONTACT_TYPE_DEVICE){
            PhoneContactInfo deleteContact = addedContactsPhone.get(position);
            addFilteredContact(deleteContact);
            addedContactsPhone.remove(deleteContact);
            setSendInvitationVisibility();
            adapterContacts.setContacts(addedContactsPhone);
            if (addedContactsPhone.size() == 0){
                containerAddedContactsRecyclerView.setVisibility(View.GONE);
            }
        }
        else {
            ShareContactInfo deleteContact = addedContactsShare.get(position);

            if (deleteContact.isPhoneContact()) {
                addFilteredContact(deleteContact.getPhoneContactInfo());
            }
            else if (deleteContact.isMegaContact()) {
                addMEGAFilteredContact(deleteContact.getMegaContactAdapter());
            }

            addedContactsShare.remove(deleteContact);
            setSendInvitationVisibility();
            adapterShare.setContacts(addedContactsShare);
            if (addedContactsShare.size() == 0){
                containerAddedContactsRecyclerView.setVisibility(View.GONE);
            }
        }
        setTitleAB();
        setRecyclersVisibility();
        setSeparatorVisibility();
        refreshKeyboard();
    }

    public void showSnackbar(String message) {
        hideKeyboard();
        Snackbar snackbar = Snackbar.make(relativeLayout, message, Snackbar.LENGTH_LONG);
        TextView snackbarTextView = (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setMaxLines(5);
        snackbar.show();
    }

    private void addMEGAFilteredContact (MegaContactAdapter contact) {

        filteredContactMEGA.add(contact);

        Collections.sort(filteredContactMEGA, new Comparator<MegaContactAdapter>(){

            public int compare(MegaContactAdapter c1, MegaContactAdapter c2) {
                String name1 = c1.getFullName();
                String name2 = c2.getFullName();
                int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
                if (res == 0) {
                    res = name1.compareTo(name2);
                }
                return res;
            }
        });

        if (contactType == Constants.CONTACT_TYPE_BOTH) {
            int i = filteredContactMEGA.indexOf(contact);
            ShareContactInfo contactToAdd = new ShareContactInfo(null, contact, null);
            if (filteredContactMEGA.size() == 1) {
                contactToAdd.setLastItem(true);
                filteredShareContacts.add(0, new ShareContactInfo(true, true, false));
                filteredShareContacts.add(1, contactToAdd);
            }
            else if (i == filteredContactMEGA.size() -1){
                contactToAdd.setLastItem(true);
                filteredShareContacts.get(i).setLastItem(false);
                filteredShareContacts.add(i+1, contactToAdd);
            }
            else {
                filteredShareContacts.add(i+1, contactToAdd);
            }
            adapterShareHeader.setContacts(filteredShareContacts);
        }
        else {
            adapterMEGA.setContacts(filteredContactMEGA);

            if (adapterMEGA.getItemCount() != 0){
                headerContacts.setVisibility(View.VISIBLE);
                recyclerViewList.setVisibility(View.VISIBLE);
                emptyImageView.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.GONE);
            }
        }
    }

    private void addFilteredContact(PhoneContactInfo contact) {
        log("addFilteredContact");
        filteredContactsPhone.add(contact);
        Collections.sort(filteredContactsPhone);

        log("Size filteredContactsPhone: " +filteredContactsPhone.size());

        if (contactType == Constants.CONTACT_TYPE_BOTH) {
            int i = filteredContactsPhone.indexOf(contact);
            if (filteredContactsPhone.size() == 1){
                filteredShareContacts.add(filteredShareContacts.size(), new ShareContactInfo(true, false, true));
                filteredShareContacts.add(filteredShareContacts.size(), new ShareContactInfo(contact, null, null));
            }
            else {
                int position = (adapterShareHeader.getItemCount()-filteredContactsPhone.size())+i+1;
                filteredShareContacts.add(position, new ShareContactInfo(contact, null, null));
            }
            adapterShareHeader.setContacts(filteredShareContacts);
        }
        else {
            adapterPhone.setContacts(filteredContactsPhone);
            if(adapterPhone!=null){
                if (adapterPhone.getItemCount() != 0){
                    headerContacts.setVisibility(View.VISIBLE);
                    recyclerViewList.setVisibility(View.VISIBLE);
                    emptyImageView.setVisibility(View.GONE);
                    emptyTextView.setVisibility(View.GONE);
                }
            }
        }
    }

    private void getVisibleMEGAContacts () {
        contactsMEGA = megaApi.getContacts();
        visibleContactsMEGA.clear();

        if(chatId!=-1){
            log("Add participant to chat");
            if(megaChatApi!=null){
                MegaChatRoom chat = megaChatApi.getChatRoom(chatId);
                if(chat!=null){
                    long participantsCount = chat.getPeerCount();

                    for (int i=0;i<contactsMEGA.size();i++){
                        if (contactsMEGA.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){

                            boolean found = false;

                            for(int j=0;j<participantsCount;j++) {

                                long peerHandle = chat.getPeerHandle(j);

                                if(peerHandle == contactsMEGA.get(i).getHandle()){
                                    found = true;
                                    break;
                                }
                            }

                            if(!found){
                                MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contactsMEGA.get(i).getHandle()+""));
                                String fullName = "";
                                if(contactDB!=null){
                                    ContactController cC = new ContactController(this);
                                    fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contactsMEGA.get(i).getEmail());
                                }
                                else{
                                    //No name, ask for it and later refresh!!
                                    fullName = contactsMEGA.get(i).getEmail();
                                }

                                log("Added to list: " + contactsMEGA.get(i).getEmail() + "_" + contactsMEGA.get(i).getVisibility());
                                MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contactsMEGA.get(i), fullName);
                                visibleContactsMEGA.add(megaContactAdapter);
                            }
                            else{
                                MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contactsMEGA.get(i).getHandle()+""));
                                String fullName = "";
                                if(contactDB!=null){
                                    ContactController cC = new ContactController(this);
                                    fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contactsMEGA.get(i).getEmail());
                                }
                                else{
                                    //No name, ask for it and later refresh!!
                                    fullName = contactsMEGA.get(i).getEmail();
                                }

                                log("Removed from list - already included on chat: "+fullName);
                            }
                        }
                    }
                }
                else{
                    for (int i=0;i<contactsMEGA.size();i++){
                        log("contact: " + contactsMEGA.get(i).getEmail() + "_" + contactsMEGA.get(i).getVisibility());
                        if (contactsMEGA.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){

                            MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contactsMEGA.get(i).getHandle()+""));
                            String fullName = "";
                            if(contactDB!=null){
                                ContactController cC = new ContactController(this);
                                fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contactsMEGA.get(i).getEmail());
                            }
                            else{
                                //No name, ask for it and later refresh!!
                                fullName = contactsMEGA.get(i).getEmail();
                            }

                            MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contactsMEGA.get(i), fullName);
                            visibleContactsMEGA.add(megaContactAdapter);

                        }
                    }
                }
            }
        }
        else{
            for (int i=0;i<contactsMEGA.size();i++){
                log("contact: " + contactsMEGA.get(i).getEmail() + "_" + contactsMEGA.get(i).getVisibility());
                if (contactsMEGA.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){

                    MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contactsMEGA.get(i).getHandle()+""));
                    String fullName = "";
                    if(contactDB!=null){
                        ContactController cC = new ContactController(this);
                        fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contactsMEGA.get(i).getEmail());
                    }
                    else{
                        //No name, ask for it and later refresh!!
                        fullName = contactsMEGA.get(i).getEmail();
                    }

                    MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contactsMEGA.get(i), fullName);
                    visibleContactsMEGA.add(megaContactAdapter);

                }
            }
        }

        Collections.sort(visibleContactsMEGA, new Comparator<MegaContactAdapter>(){

            public int compare(MegaContactAdapter c1, MegaContactAdapter c2) {
                String name1 = c1.getFullName();
                String name2 = c2.getFullName();
                int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
                if (res == 0) {
                    res = name1.compareTo(name2);
                }
                return res;
            }
        });

        for (int i= 0; i<visibleContactsMEGA.size(); i++){
            filteredContactMEGA.add(visibleContactsMEGA.get(i));
        }
    }

    @SuppressLint("InlinedApi")
    //Get the contacts explicitly added
    private ArrayList<PhoneContactInfo> getPhoneContacts() {
        log("getPhoneContacts");
        ArrayList<PhoneContactInfo> contactList = new ArrayList<>();
        log("inputString empty");
        String filter = ContactsContract.CommonDataKinds.Email.DATA + " NOT LIKE ''  AND " + ContactsContract.Contacts.IN_VISIBLE_GROUP + "=1";

        try {
            ContentResolver cr = getBaseContext().getContentResolver();
            String SORT_ORDER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? ContactsContract.Contacts.SORT_KEY_PRIMARY : ContactsContract.Contacts.DISPLAY_NAME;

            Cursor c = cr.query(
                    ContactsContract.Data.CONTENT_URI,
                    null,filter,
                    null, SORT_ORDER);

            while (c.moveToNext()){
                long id = c.getLong(c.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                String name = c.getString(c.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                String emailAddress = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                log("ID: " + id + "___ NAME: " + name + "____ EMAIL: " + emailAddress);

                if ((!emailAddress.equalsIgnoreCase("")) && (emailAddress.contains("@")) && (!emailAddress.contains("s.whatsapp.net"))) {
                    if (inputString == ""){
                        log("VALID Contact: "+ name + " ---> "+ emailAddress);
                        PhoneContactInfo contactPhone = new PhoneContactInfo(id, name, emailAddress, null);
                        contactList.add(contactPhone);
                    }
                    else if (!inputString.isEmpty() && (name.toUpperCase().contains(inputString.toUpperCase()) || emailAddress.toUpperCase().contains(inputString.toUpperCase()))){
                        log("VALID Contact: "+ name + " ---> "+ emailAddress + " inputString: " + inputString);
                        PhoneContactInfo contactPhone = new PhoneContactInfo(id, name, emailAddress, null);
                        contactList.add(contactPhone);
                    }
                }
            }
            c.close();

            log("contactList.size() = " + contactList.size());

            return contactList;

        } catch (Exception e) { log ("Exception: " + e.getMessage()); }

        return null;
    }

    public void onTextChanged(CharSequence s, int start, int before, int count){

    }

    public void itemClick(String email, int position){

        log("itemClick");
        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

        if (contactType == Constants.CONTACT_TYPE_MEGA) {
            for (int i = 0; i < filteredContactMEGA.size(); i++) {
                if (getMegaContactMail(filteredContactMEGA.get(i)).equals(email)) {
                    addContactMEGA(filteredContactMEGA.get(i));
                    filteredContactMEGA.remove(i);
                    adapterMEGA.setContacts(filteredContactMEGA);
                    break;
                }
            }

            if (addedContactsMEGA.size() == 0){
                setSendInvitationVisibility();
            }
        }
    }

    public void itemClick(View view, int position) {
        log("on item click");

        if (contactType == Constants.CONTACT_TYPE_DEVICE){

            if(adapterPhone==null){
                return;
            }

            final PhoneContactInfo contact = adapterPhone.getItem(position);
            if(contact == null) {
                return;
            }

            for (int i = 0; i < filteredContactsPhone.size(); i++) {
                if (filteredContactsPhone.get(i).getEmail().compareTo(contact.getEmail()) == 0) {
                    filteredContactsPhone.remove(i);
                    adapterPhone.setContacts(filteredContactsPhone);
                    break;
                }
            }
            addContact(contact);
        }
        else if (contactType == Constants.CONTACT_TYPE_BOTH){
            if(adapterShareHeader==null){
                return;
            }

            final ShareContactInfo contact = adapterShareHeader.getItem(position);
            if(contact == null || contact.isHeader()) {
                return;
            }
            if (contact.isMegaContact()) {
                if (filteredContactMEGA.size() == 1){
                    filteredShareContacts.remove(0);
                }
                else if (position == filteredContactMEGA.size()) {
                    filteredShareContacts.get(position-1).setLastItem(true);
                }
                filteredContactMEGA.remove(contact.getMegaContactAdapter());
            }
            else if (contact.isPhoneContact()) {
                filteredContactsPhone.remove(contact.getPhoneContactInfo());
                if (filteredContactsPhone.size() == 0) {
                    filteredShareContacts.remove(filteredShareContacts.size()-2);
                }
            }
            filteredShareContacts.remove(contact);
            adapterShareHeader.setContacts(filteredShareContacts);

            addShareContact(contact);
        }

    }

    public String getShareContactMail(ShareContactInfo contact) {
        String mail = null;

        if (contact.isMegaContact() && !contact.isHeader()) {
            if (contact.getMegaContactAdapter().getMegaUser() != null && contact.getMegaContactAdapter().getMegaUser().getEmail() != null) {
                mail = contact.getMegaContactAdapter().getMegaUser().getEmail();
            } else if (contact.getMegaContactAdapter().getMegaContactDB() != null && contact.getMegaContactAdapter().getMegaContactDB().getMail() != null) {
                mail = contact.getMegaContactAdapter().getMegaContactDB().getMail();
            }
        }
        else if (contact.isPhoneContact() && !contact.isHeader()){
            mail = contact.getPhoneContactInfo().getEmail();
        }
        else{
            mail = contact.getMail();
        }

        return mail;
    }

    public String getMegaContactMail (MegaContactAdapter contact) {
        String mail = null;
        if (contact.getMegaUser() != null && contact.getMegaUser().getEmail() != null) {
            mail = contact.getMegaUser().getEmail();
        } else if (contact.getMegaContactDB() != null && contact.getMegaContactDB().getMail() != null) {
            mail = contact.getMegaContactDB().getMail();
        }
        return mail;
    }

    @Override
    public void onClick(View v) {
        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

    }

    private void setResultContacts(ArrayList<MegaContactAdapter> addedContacts, boolean megaContacts){
        log("setResultContacts");
        ArrayList<String> contactsSelected = new ArrayList<String>();
        String contactEmail;

        if (addedContacts != null) {
            for (int i = 0; i < addedContacts.size(); i++) {
                if (addedContacts.get(i).getMegaUser() != null && addedContacts.get(i).getMegaContactDB() != null) {
                    contactEmail = addedContacts.get(i).getMegaUser().getEmail();
                } else {
                    contactEmail = addedContacts.get(i).getFullName();
                }
                if (contactEmail != null) {
                    contactsSelected.add(contactEmail);
                }
            }
        }
        log("contacts selected: "+contactsSelected.size());
        Intent intent = new Intent();
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contactsSelected);

        if(multipleSelectIntent==0){
            intent.putExtra(EXTRA_NODE_HANDLE, nodeHandle);
            intent.putExtra("MULTISELECT", 0);
        }
        else if(multipleSelectIntent==1){
            intent.putExtra(EXTRA_NODE_HANDLE, nodeHandles);
            intent.putExtra("MULTISELECT", 1);
        }

        if(sendToInbox==0){
            intent.putExtra("SEND_FILE",0);
        } else {
            intent.putExtra("SEND_FILE",1);
        }
        intent.putExtra(EXTRA_MEGA_CONTACTS, megaContacts);
        setResult(RESULT_OK, intent);
        hideKeyboard();
        finish();
    }

    private void shareWith (ArrayList<ShareContactInfo> addedContacts){
        log("shareWith");

        ArrayList<String> contactsSelected = new ArrayList<>();
        if (addedContacts != null){
            for (int i=0; i<addedContacts.size(); i++){
                contactsSelected.add(getShareContactMail(addedContacts.get(i)));
            }
        }

        Intent intent = new Intent();
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contactsSelected);
        if(multipleSelectIntent==0){
            intent.putExtra(EXTRA_NODE_HANDLE, nodeHandle);
            intent.putExtra("MULTISELECT", 0);
        }
        else if(multipleSelectIntent==1){
            intent.putExtra(EXTRA_NODE_HANDLE, nodeHandles);
            intent.putExtra("MULTISELECT", 1);
        }

        if(sendToInbox==0){
            intent.putExtra("SEND_FILE",0);
        } else {
            intent.putExtra("SEND_FILE",1);
        }
        intent.putExtra(EXTRA_MEGA_CONTACTS, false);
        setResult(RESULT_OK, intent);
        hideKeyboard();
        finish();
    }

    private void inviteContacts(ArrayList<PhoneContactInfo> addedContacts){
        log("inviteContacts");

        String contactEmail = null;
        ArrayList<String> contactsSelected = new ArrayList<>();
        if (addedContacts != null) {
            for (int i=0;i<addedContacts.size();i++) {
                contactEmail = addedContacts.get(i).getEmail();
                if (fromAchievements){
                    if (contactEmail != null && !mailsFromAchievements.contains(contactEmail)) {
                        contactsSelected.add(contactEmail);
                    }
                }
                else {
                    if (contactEmail != null) {
                        contactsSelected.add(contactEmail);
                    }
                }
            }
        }

        Intent intent = new Intent();
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contactsSelected);
        for(int i=0; i<contactsSelected.size();i++){
            log("setResultContacts: "+contactsSelected.get(i));
        }

        intent.putExtra(EXTRA_MEGA_CONTACTS, false);
        setResult(RESULT_OK, intent);
        hideKeyboard();
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        log("onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constants.REQUEST_READ_CONTACTS: {
                log("REQUEST_READ_CONTACTS");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    boolean hasReadContactsPermissions = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
                    if (hasReadContactsPermissions) {
                        filteredContactsPhone.clear();
                        emptyImageView.setVisibility(View.VISIBLE);
                        emptyTextView.setVisibility(View.VISIBLE);

                        progressBar.setVisibility(View.VISIBLE);
                        new GetContactsTask().execute();
                    }
                }
                else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    boolean hasReadContactsPermissions = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED);
                    if (hasReadContactsPermissions) {
                        log("Permission denied");
                        filteredContactsPhone.clear();
                        emptyImageView.setVisibility(View.VISIBLE);
                        emptyTextView.setVisibility(View.VISIBLE);
                        emptyTextView.setText(R.string.no_contacts_permissions);

                        progressBar.setVisibility(View.GONE);
                    }
                }
                break;
            }
        }
    }

    public void setSeparatorVisibility (){
        if (contactType == Constants.CONTACT_TYPE_MEGA) {
            if (addedContactsMEGA.size() > 0){
                separator.setVisibility(View.VISIBLE);
            }
            else {
                separator.setVisibility(View.GONE);
            }
        }
        else if (contactType ==  Constants.CONTACT_TYPE_DEVICE) {
            if (addedContactsPhone.size() > 0){
                separator.setVisibility(View.VISIBLE);
            }
            else {
                separator.setVisibility(View.GONE);
            }
        }
        else {
            if (addedContactsShare.size() > 0){
                separator.setVisibility(View.VISIBLE);
            }
            else {
                separator.setVisibility(View.GONE);
            }
        }
    }

    public void setRecyclersVisibility () {
        if (contactType == Constants.CONTACT_TYPE_MEGA) {
            if (filteredContactMEGA.size() > 0){
                containerContacts.setVisibility(View.VISIBLE);
            }
            else {
                containerContacts.setVisibility(View.GONE);
            }
        }
        else if (contactType ==  Constants.CONTACT_TYPE_DEVICE) {
            if (filteredContactsPhone.size() > 0){
                containerContacts.setVisibility(View.VISIBLE);
            }
            else {
                containerContacts.setVisibility(View.GONE);
            }
        }
        else {
            if (filteredShareContacts.size() >= 2){
                containerContacts.setVisibility(View.VISIBLE);
            }
            else {
                containerContacts.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    public static void log(String message) {
        Util.log("AddContactActivityLollipop", message);
    }
}
