package mega.privacy.android.app;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.TextView;

import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;

public class SorterContentActivity extends BaseActivity {

    public void showShortOptions(final Context context, DisplayMetrics outMetrics) {

        ManagerActivityLollipop.DrawerItem drawerItem = null;

        AlertDialog sortByDialog;
        LayoutInflater inflater = null;
        if (context instanceof ManagerActivityLollipop) {
            inflater = ((ManagerActivityLollipop) context).getLayoutInflater();
            drawerItem = ((ManagerActivityLollipop) context).getDrawerItem();
        }
        else if (context instanceof FileExplorerActivityLollipop) {
            inflater = ((FileExplorerActivityLollipop) context).getLayoutInflater();
        }

        View dialoglayout = inflater.inflate(R.layout.sortby_dialog, null);

        TextView sortByNameTV = dialoglayout.findViewById(R.id.sortby_dialog_name_text);
        sortByNameTV.setText(context.getString(R.string.sortby_name));

        TextView sortByDateTV = dialoglayout.findViewById(R.id.sortby_dialog_date_text);
        sortByDateTV.setText(context.getString(R.string.sortby_modification_date));

        TextView sortBySizeTV = dialoglayout.findViewById(R.id.sortby_dialog_size_text);
        sortBySizeTV.setText(context.getString(R.string.sortby_size));

        final CheckedTextView ascendingCheck = dialoglayout.findViewById(R.id.sortby_dialog_ascending_check);
        ascendingCheck.setText(context.getString(R.string.sortby_name_ascending));

        final CheckedTextView descendingCheck = dialoglayout.findViewById(R.id.sortby_dialog_descending_check);
        descendingCheck.setText(context.getString(R.string.sortby_name_descending));

        final CheckedTextView newestCheck = dialoglayout.findViewById(R.id.sortby_dialog_newest_check);
        newestCheck.setText(context.getString(R.string.sortby_date_newest));

        final CheckedTextView oldestCheck = dialoglayout.findViewById(R.id.sortby_dialog_oldest_check);
        oldestCheck.setText(context.getString(R.string.sortby_date_oldest));

        final CheckedTextView largestCheck = dialoglayout.findViewById(R.id.sortby_dialog_largest_first_check);
        largestCheck.setText(context.getString(R.string.sortby_size_largest_first));

        final CheckedTextView smallestCheck = dialoglayout.findViewById(R.id.sortby_dialog_smallest_first_check);
        smallestCheck.setText(context.getString(R.string.sortby_size_smallest_first));

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialoglayout);
        TextView textViewTitle = new TextView(context);
        textViewTitle.setText(context.getString(R.string.action_sort_by));
        textViewTitle.setTextSize(20);
        textViewTitle.setTextColor(0xde000000);
        textViewTitle.setPadding(Util.scaleWidthPx(23, outMetrics), Util.scaleHeightPx(20, outMetrics), 0, 0);
        builder.setCustomTitle(textViewTitle);

        sortByDialog = builder.create();
        sortByDialog.show();
        final AlertDialog dialog = sortByDialog;

        boolean ascending = false;
        boolean descending = false;
        boolean newest = false;
        boolean oldest = false;
        boolean largest = false;
        boolean smallest = false;
        int order = MegaApiJava.ORDER_DEFAULT_ASC;

        if (context instanceof ManagerActivityLollipop) {
            switch (drawerItem) {
                case CONTACTS: {
                    order = ((ManagerActivityLollipop) context).getOrderContacts();

                    sortByDateTV.setText(context.getString(R.string.sortby_date));
                    sortBySizeTV.setVisibility(View.GONE);
                    largestCheck.setVisibility(View.GONE);
                    smallestCheck.setVisibility(View.GONE);

                    break;
                }
                case SAVED_FOR_OFFLINE: {
                    order = ((ManagerActivityLollipop) context).getOrderOthers();

                    sortByDateTV.setVisibility(View.GONE);
                    newestCheck.setVisibility(View.GONE);
                    oldestCheck.setVisibility(View.GONE);
                    sortBySizeTV.setVisibility(View.GONE);
                    largestCheck.setVisibility(View.GONE);
                    smallestCheck.setVisibility(View.GONE);

                    break;
                }
                case SHARED_ITEMS: {
                    int index = ((ManagerActivityLollipop) context).getTabItemShares();
                    if((index==1 && ((ManagerActivityLollipop) context).parentHandleOutgoing == -1)
                            || (index != 1 && ((ManagerActivityLollipop) context).parentHandleIncoming == -1)){
                        order = ((ManagerActivityLollipop) context).getOrderOthers();
                    }
                    else{
                        order = ((ManagerActivityLollipop) context).orderCloud;
                    }

                    if(((ManagerActivityLollipop) context).isFirstNavigationLevel()){
                        if (((ManagerActivityLollipop) context).getTabItemShares()==0){
                            //Incoming Shares
                            sortByNameTV.setText(context.getString(R.string.sortby_owner_mail));
                        }
                        else{
                            sortByNameTV.setText(context.getString(R.string.sortby_name));
                        }

                        sortByDateTV.setVisibility(View.GONE);
                        newestCheck.setVisibility(View.GONE);
                        oldestCheck.setVisibility(View.GONE);
                        sortBySizeTV.setVisibility(View.GONE);
                        largestCheck.setVisibility(View.GONE);
                        smallestCheck.setVisibility(View.GONE);
                    }
                    else{
                        log("No first level navigation on Incoming Shares");
                        sortByNameTV.setText(context.getString(R.string.sortby_name));
                    }

                    break;
                }
                case CAMERA_UPLOADS:
                case MEDIA_UPLOADS: {
                    order = ((ManagerActivityLollipop) context).orderCamera;

                    sortByNameTV.setVisibility(View.GONE);
                    ascendingCheck.setVisibility(View.GONE);
                    descendingCheck.setVisibility(View.GONE);
                    sortBySizeTV.setVisibility(View.GONE);
                    largestCheck.setVisibility(View.GONE);
                    smallestCheck.setVisibility(View.GONE);

                    break;
                }
                default: {
                    order = ((ManagerActivityLollipop) context).orderCloud;

                    break;
                }
            }
        }
        else if (context instanceof FileExplorerActivityLollipop) {

            MegaPreferences prefs = Util.getPreferences(context);
            drawerItem = ((FileExplorerActivityLollipop) context).getCurrentItem();

            if (drawerItem == null) {
                return;
            }

            switch (drawerItem) {
                case CLOUD_DRIVE: {
                    if (prefs != null && prefs.getPreferredSortCloud() !=  null){
                        order = Integer.parseInt(prefs.getPreferredSortCloud());
                    }

                    break;
                }
                case SHARED_ITEMS: {
                    if (((FileExplorerActivityLollipop) context).getParentHandleIncoming() == -1 && prefs != null && prefs.getPreferredSortOthers() != null){
                        order = Integer.parseInt(prefs.getPreferredSortOthers());
                    }
                    else if (prefs != null && prefs.getPreferredSortCloud() != null){
                        order = Integer.parseInt(prefs.getPreferredSortCloud());
                    }

                    if(((FileExplorerActivityLollipop) context).getParentHandleIncoming() == -1){
                        sortByNameTV.setText(context.getString(R.string.sortby_owner_mail));

                        sortByDateTV.setVisibility(View.GONE);
                        newestCheck.setVisibility(View.GONE);
                        oldestCheck.setVisibility(View.GONE);
                        sortBySizeTV.setVisibility(View.GONE);
                        largestCheck.setVisibility(View.GONE);
                        smallestCheck.setVisibility(View.GONE);
                    }
                    else{
                        log("No first level navigation on Incoming Shares");
                        sortByNameTV.setText(context.getString(R.string.sortby_name));
                    }

                    break;
                }
            }
        }

        switch(order){
            case MegaApiJava.ORDER_DEFAULT_ASC:{
                ascending = true;
                break;
            }
            case MegaApiJava.ORDER_DEFAULT_DESC:{
                descending = true;
                break;
            }
            case MegaApiJava.ORDER_MODIFICATION_ASC:
            case MegaApiJava.ORDER_CREATION_DESC:{
                oldest = true;
                break;
            }
            case MegaApiJava.ORDER_MODIFICATION_DESC:
            case MegaApiJava.ORDER_CREATION_ASC:{
                newest = true;
                break;
            }
            case MegaApiJava.ORDER_SIZE_ASC:{
                smallest = true;
                break;
            }
            case MegaApiJava.ORDER_SIZE_DESC:{
                largest = true;
                break;
            }
        }

        ascendingCheck.setChecked(ascending);
        descendingCheck.setChecked(descending);
        newestCheck.setChecked(newest);
        oldestCheck.setChecked(oldest);
        largestCheck.setChecked(largest);
        smallestCheck.setChecked(smallest);

        final ManagerActivityLollipop.DrawerItem finalDrawerItem = drawerItem;
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean ascending = false;
                boolean descending = false;
                boolean newest = false;
                boolean oldest = false;
                boolean largest = false;
                boolean smallest = false;
                int order = MegaApiJava.ORDER_DEFAULT_ASC;

                switch (v.getId()) {
                    case R.id.sortby_dialog_ascending_check: {
                        ascending = true;
                        order = MegaApiJava.ORDER_DEFAULT_ASC;
                        break;
                    }
                    case R.id.sortby_dialog_descending_check: {
                        descending = true;
                        order = MegaApiJava.ORDER_DEFAULT_DESC;
                        break;
                    }
                    case R.id.sortby_dialog_newest_check: {
                        newest = true;

                        if (finalDrawerItem == ManagerActivityLollipop.DrawerItem.CONTACTS) {
                            order = MegaApiJava.ORDER_CREATION_ASC;
                        }
                        else {
                            order = MegaApiJava.ORDER_MODIFICATION_DESC;
                        }
                        break;
                    }
                    case R.id.sortby_dialog_oldest_check: {
                        oldest = true;
                        if (finalDrawerItem == ManagerActivityLollipop.DrawerItem.CONTACTS) {
                            order = MegaApiJava.ORDER_CREATION_DESC;
                        }
                        else {
                            order = MegaApiJava.ORDER_MODIFICATION_ASC;
                        }
                        break;
                    }
                    case R.id.sortby_dialog_largest_first_check: {
                        largest = true;
                        order = MegaApiJava.ORDER_SIZE_DESC;
                        break;
                    }
                    case R.id.sortby_dialog_smallest_first_check: {
                        smallest = true;
                        order = MegaApiJava.ORDER_SIZE_ASC;
                        break;
                    }
                }

                ascendingCheck.setChecked(ascending);
                descendingCheck.setChecked(descending);
                newestCheck.setChecked(newest);
                oldestCheck.setChecked(oldest);
                largestCheck.setChecked(largest);
                smallestCheck.setChecked(smallest);

                switch (finalDrawerItem) {
                    case CONTACTS: {
                        ((ManagerActivityLollipop) context).selectSortByContacts(order);
                        break;
                    }
                    case SAVED_FOR_OFFLINE: {
                        ((ManagerActivityLollipop) context).selectSortByOffline(order);
                        break;
                    }
                    case SHARED_ITEMS: {
                        if (context instanceof ManagerActivityLollipop) {
                            if(((ManagerActivityLollipop) context).isFirstNavigationLevel()){
                                ((ManagerActivityLollipop) context).refreshOthersOrder(order);
                            }
                            else {
                                ((ManagerActivityLollipop) context).refreshCloudOrder(order);
                            }
                        }
                        else if (context instanceof FileExplorerActivityLollipop) {
                            setFileExplorerOrder(finalDrawerItem, context, order);
                            if (((FileExplorerActivityLollipop) context).getParentHandleIncoming() == -1) {
                                updateManagerOrder(false, order);
                            }
                            else {
                                updateManagerOrder(true, order);
                            }
                        }
                        break;
                    }
                    case CAMERA_UPLOADS:
                    case MEDIA_UPLOADS: {
                        ((ManagerActivityLollipop) context).selectSortUploads(order);
                        break;
                    }
                    default: {
                        if (context instanceof ManagerActivityLollipop) {
                            ((ManagerActivityLollipop) context).refreshCloudOrder(order);
                        }
                        else if (context instanceof FileExplorerActivityLollipop) {
                            setFileExplorerOrder(finalDrawerItem, context, order);
                            updateManagerOrder(true, order);
                        }
                        break;
                    }
                }

                if (dialog != null){
                    try {
                        dialog.dismiss();
                    } catch (Exception e){}
                }
            }
        };

        ascendingCheck.setOnClickListener(clickListener);
        descendingCheck.setOnClickListener(clickListener);
        newestCheck.setOnClickListener(clickListener);
        oldestCheck.setOnClickListener(clickListener);
        largestCheck.setOnClickListener(clickListener);
        smallestCheck.setOnClickListener(clickListener);
    }

    void updateManagerOrder (boolean cloudOrder, int order) {
        Intent intent = new Intent(Constants.BROADCAST_ACTION_INTENT_UPDATE_ORDER);
        intent.putExtra("cloudOrder", cloudOrder);
        intent.putExtra("order", order);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void setFileExplorerOrder (ManagerActivityLollipop.DrawerItem drawerItem, Context context, int order) {
        ((FileExplorerActivityLollipop) context).refreshOrderNodes(order);
        MegaPreferences prefs = Util.getPreferences(context);
        DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
        if (drawerItem == ManagerActivityLollipop.DrawerItem.SHARED_ITEMS && ((FileExplorerActivityLollipop) context).getParentHandleIncoming() == -1) {
            if (prefs != null) {
                prefs.setPreferredSortOthers(String.valueOf(order));
            }
            dbH.setPreferredSortOthers(String.valueOf(order));
        }
        else {
            if (prefs != null) {
                prefs.setPreferredSortCloud(String.valueOf(order));
            }
            dbH.setPreferredSortCloud(String.valueOf(order));
        }
    }

    public static void log(String message) {
        Util.log("SorterContentActivity", message);
    }
}
