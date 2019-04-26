package mega.privacy.android.app;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.TextView;

import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;

public class SorterContentActivity extends BaseActivity {

    public static void showShortOptions(final Context context, DisplayMetrics outMetrics) {

        ManagerActivityLollipop.DrawerItem drawerItem = null;

        float scaleText;
        float scaleW = Util.getScaleW(outMetrics, context.getResources().getDisplayMetrics().density);
        float scaleH = Util.getScaleH(outMetrics, context.getResources().getDisplayMetrics().density);
        if (scaleH < scaleW){
            scaleText = scaleH;
        }
        else{
            scaleText = scaleW;
        }

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

        TextView sortByNameTV = (TextView) dialoglayout.findViewById(R.id.sortby_dialog_name_text);
        sortByNameTV.setText(context.getString(R.string.sortby_name));
        ViewGroup.MarginLayoutParams nameMLP = (ViewGroup.MarginLayoutParams) sortByNameTV.getLayoutParams();
        sortByNameTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        nameMLP.setMargins(Util.scaleWidthPx(25, outMetrics), Util.scaleHeightPx(15, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

        TextView sortByDateTV = (TextView) dialoglayout.findViewById(R.id.sortby_dialog_date_text);
        sortByDateTV.setText(context.getString(R.string.sortby_modification_date));
        ViewGroup.MarginLayoutParams dateMLP = (ViewGroup.MarginLayoutParams) sortByDateTV.getLayoutParams();
        sortByDateTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        dateMLP.setMargins(Util.scaleWidthPx(25, outMetrics), Util.scaleHeightPx(15, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

        TextView sortBySizeTV = (TextView) dialoglayout.findViewById(R.id.sortby_dialog_size_text);
        sortBySizeTV.setText(context.getString(R.string.sortby_size));
        ViewGroup.MarginLayoutParams sizeMLP = (ViewGroup.MarginLayoutParams) sortBySizeTV.getLayoutParams();
        sortBySizeTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        sizeMLP.setMargins(Util.scaleWidthPx(25, outMetrics), Util.scaleHeightPx(15, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

        final CheckedTextView ascendingCheck = (CheckedTextView) dialoglayout.findViewById(R.id.sortby_dialog_ascending_check);
        ascendingCheck.setText(context.getString(R.string.sortby_name_ascending));
        ascendingCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        ascendingCheck.setCompoundDrawablePadding(Util.scaleWidthPx(34, outMetrics));
        ViewGroup.MarginLayoutParams ascendingMLP = (ViewGroup.MarginLayoutParams) ascendingCheck.getLayoutParams();
        ascendingMLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

        final CheckedTextView descendingCheck = (CheckedTextView) dialoglayout.findViewById(R.id.sortby_dialog_descending_check);
        descendingCheck.setText(context.getString(R.string.sortby_name_descending));
        descendingCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        descendingCheck.setCompoundDrawablePadding(Util.scaleWidthPx(34, outMetrics));
        ViewGroup.MarginLayoutParams descendingMLP = (ViewGroup.MarginLayoutParams) descendingCheck.getLayoutParams();
        descendingMLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

        final CheckedTextView newestCheck = (CheckedTextView) dialoglayout.findViewById(R.id.sortby_dialog_newest_check);
        newestCheck.setText(context.getString(R.string.sortby_date_newest));
        newestCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        newestCheck.setCompoundDrawablePadding(Util.scaleWidthPx(34, outMetrics));
        ViewGroup.MarginLayoutParams newestMLP = (ViewGroup.MarginLayoutParams) newestCheck.getLayoutParams();
        newestMLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

        final CheckedTextView oldestCheck = (CheckedTextView) dialoglayout.findViewById(R.id.sortby_dialog_oldest_check);
        oldestCheck.setText(context.getString(R.string.sortby_date_oldest));
        oldestCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        oldestCheck.setCompoundDrawablePadding(Util.scaleWidthPx(34, outMetrics));
        ViewGroup.MarginLayoutParams oldestMLP = (ViewGroup.MarginLayoutParams) oldestCheck.getLayoutParams();
        oldestMLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

        final CheckedTextView largestCheck = (CheckedTextView) dialoglayout.findViewById(R.id.sortby_dialog_largest_first_check);
        largestCheck.setText(context.getString(R.string.sortby_size_largest_first));
        largestCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        largestCheck.setCompoundDrawablePadding(Util.scaleWidthPx(34, outMetrics));
        ViewGroup.MarginLayoutParams largestMLP = (ViewGroup.MarginLayoutParams) largestCheck.getLayoutParams();
        largestMLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

        final CheckedTextView smallestCheck = (CheckedTextView) dialoglayout.findViewById(R.id.sortby_dialog_smallest_first_check);
        smallestCheck.setText(context.getString(R.string.sortby_size_smallest_first));
        smallestCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16*scaleText));
        smallestCheck.setCompoundDrawablePadding(Util.scaleWidthPx(34, outMetrics));
        ViewGroup.MarginLayoutParams smallestMLP = (ViewGroup.MarginLayoutParams) smallestCheck.getLayoutParams();
        smallestMLP.setMargins(Util.scaleWidthPx(15, outMetrics), Util.scaleHeightPx(10, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
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

        if (context instanceof ManagerActivityLollipop) {
            switch (drawerItem) {
                case CONTACTS: {
                    switch(((ManagerActivityLollipop) context).getOrderContacts()){
                        case MegaApiJava.ORDER_DEFAULT_ASC:{
                            ascendingCheck.setChecked(true);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            break;
                        }
                        case MegaApiJava.ORDER_DEFAULT_DESC:{
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(true);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            break;
                        }
                        case MegaApiJava.ORDER_CREATION_ASC:{
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(true);
                            oldestCheck.setChecked(false);
                            break;
                        }
                        case MegaApiJava.ORDER_CREATION_DESC:{
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(true);
                            break;
                        }
                    }

                    sortByDateTV.setText(context.getString(R.string.sortby_date));
                    sortByDateTV.setVisibility(View.VISIBLE);
                    newestCheck.setVisibility(View.VISIBLE);
                    oldestCheck.setVisibility(View.VISIBLE);
                    sortBySizeTV.setVisibility(View.GONE);
                    largestCheck.setVisibility(View.GONE);
                    smallestCheck.setVisibility(View.GONE);

                    ascendingCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(true);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            if(((ManagerActivityLollipop) context).getOrderContacts()!=MegaApiJava.ORDER_DEFAULT_ASC){
                                ((ManagerActivityLollipop) context).selectSortByContacts(MegaApiJava.ORDER_DEFAULT_ASC);
                            }
                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });

                    descendingCheck.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(true);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            if(((ManagerActivityLollipop) context).getOrderContacts()!=MegaApiJava.ORDER_DEFAULT_DESC) {
                                ((ManagerActivityLollipop) context).selectSortByContacts(MegaApiJava.ORDER_DEFAULT_DESC);
                            }
                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });

                    newestCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(true);
                            oldestCheck.setChecked(false);
                            if(((ManagerActivityLollipop) context).getOrderContacts()!=MegaApiJava.ORDER_CREATION_ASC){
                                ((ManagerActivityLollipop) context).selectSortByContacts(MegaApiJava.ORDER_CREATION_ASC);
                            }
                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });

                    oldestCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(true);
                            if(((ManagerActivityLollipop) context).getOrderContacts()!=MegaApiJava.ORDER_CREATION_DESC) {
                                ((ManagerActivityLollipop) context).selectSortByContacts(MegaApiJava.ORDER_CREATION_DESC);
                            }
                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });

                    break;
                }
                case SAVED_FOR_OFFLINE: {
                    switch(((ManagerActivityLollipop) context).getOrderOthers()){
                        case MegaApiJava.ORDER_DEFAULT_ASC:{
                            log("ASCE");
                            ascendingCheck.setChecked(true);
                            descendingCheck.setChecked(false);
                            break;
                        }
                        case MegaApiJava.ORDER_DEFAULT_DESC:{
                            log("DESC");
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(true);
                            break;
                        }
                    }

                    sortByDateTV.setVisibility(View.GONE);
                    newestCheck.setVisibility(View.GONE);
                    oldestCheck.setVisibility(View.GONE);
                    sortBySizeTV.setVisibility(View.GONE);
                    largestCheck.setVisibility(View.GONE);
                    smallestCheck.setVisibility(View.GONE);

                    ascendingCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(true);
                            descendingCheck.setChecked(false);
                            if(((ManagerActivityLollipop) context).getOrderOthers()!=MegaApiJava.ORDER_DEFAULT_ASC) {
                                ((ManagerActivityLollipop) context).selectSortByOffline(MegaApiJava.ORDER_DEFAULT_ASC);
                            }
                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });

                    descendingCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(true);
                            if(((ManagerActivityLollipop) context).getOrderOthers()!=MegaApiJava.ORDER_DEFAULT_DESC) {
                                ((ManagerActivityLollipop) context).selectSortByOffline(MegaApiJava.ORDER_DEFAULT_DESC);
                            }
                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });

                    break;
                }
                case SHARED_ITEMS: {
                    int index = ((ManagerActivityLollipop) context).getTabItemShares();
                    if(index==1){
                        if (((ManagerActivityLollipop) context).parentHandleOutgoing == -1){
                            switch(((ManagerActivityLollipop) context).getOrderOthers()){
                                case MegaApiJava.ORDER_DEFAULT_ASC:{
                                    log("ASCE");
                                    ascendingCheck.setChecked(true);
                                    descendingCheck.setChecked(false);
                                    break;
                                }
                                case MegaApiJava.ORDER_DEFAULT_DESC:{
                                    log("DESC");
                                    ascendingCheck.setChecked(false);
                                    descendingCheck.setChecked(true);
                                    break;
                                }
                            }
                        }
                        else{
                            switch(((ManagerActivityLollipop) context).orderCloud){
                                case MegaApiJava.ORDER_DEFAULT_ASC:{
                                    ascendingCheck.setChecked(true);
                                    descendingCheck.setChecked(false);
                                    newestCheck.setChecked(false);
                                    oldestCheck.setChecked(false);
                                    largestCheck.setChecked(false);
                                    smallestCheck.setChecked(false);
                                    break;
                                }
                                case MegaApiJava.ORDER_DEFAULT_DESC:{
                                    ascendingCheck.setChecked(false);
                                    descendingCheck.setChecked(true);
                                    newestCheck.setChecked(false);
                                    oldestCheck.setChecked(false);
                                    largestCheck.setChecked(false);
                                    smallestCheck.setChecked(false);
                                    break;
                                }
                                case MegaApiJava.ORDER_MODIFICATION_ASC:{
                                    ascendingCheck.setChecked(false);
                                    descendingCheck.setChecked(false);
                                    newestCheck.setChecked(false);
                                    oldestCheck.setChecked(true);
                                    largestCheck.setChecked(false);
                                    smallestCheck.setChecked(false);
                                    break;
                                }
                                case MegaApiJava.ORDER_MODIFICATION_DESC:{
                                    ascendingCheck.setChecked(false);
                                    descendingCheck.setChecked(false);
                                    newestCheck.setChecked(true);
                                    oldestCheck.setChecked(false);
                                    largestCheck.setChecked(false);
                                    smallestCheck.setChecked(false);
                                    break;
                                }
                                case MegaApiJava.ORDER_SIZE_ASC:{
                                    ascendingCheck.setChecked(false);
                                    descendingCheck.setChecked(false);
                                    newestCheck.setChecked(false);
                                    oldestCheck.setChecked(false);
                                    largestCheck.setChecked(false);
                                    smallestCheck.setChecked(true);
                                    break;
                                }
                                case MegaApiJava.ORDER_SIZE_DESC:{
                                    ascendingCheck.setChecked(false);
                                    descendingCheck.setChecked(false);
                                    newestCheck.setChecked(false);
                                    oldestCheck.setChecked(false);
                                    largestCheck.setChecked(true);
                                    smallestCheck.setChecked(false);
                                    break;
                                }
                            }
                        }
                    }
                    else{
                        if (((ManagerActivityLollipop) context).parentHandleIncoming == -1){
                            switch(((ManagerActivityLollipop) context).getOrderOthers()){
                                case MegaApiJava.ORDER_DEFAULT_ASC:{
                                    log("ASCE");
                                    ascendingCheck.setChecked(true);
                                    descendingCheck.setChecked(false);
                                    break;
                                }
                                case MegaApiJava.ORDER_DEFAULT_DESC:{
                                    log("DESC");
                                    ascendingCheck.setChecked(false);
                                    descendingCheck.setChecked(true);
                                    break;
                                }
                            }
                        }
                        else{
                            switch(((ManagerActivityLollipop) context).orderCloud){
                                case MegaApiJava.ORDER_DEFAULT_ASC:{
                                    ascendingCheck.setChecked(true);
                                    descendingCheck.setChecked(false);
                                    newestCheck.setChecked(false);
                                    oldestCheck.setChecked(false);
                                    largestCheck.setChecked(false);
                                    smallestCheck.setChecked(false);
                                    break;
                                }
                                case MegaApiJava.ORDER_DEFAULT_DESC:{
                                    ascendingCheck.setChecked(false);
                                    descendingCheck.setChecked(true);
                                    newestCheck.setChecked(false);
                                    oldestCheck.setChecked(false);
                                    largestCheck.setChecked(false);
                                    smallestCheck.setChecked(false);
                                    break;
                                }
                                case MegaApiJava.ORDER_MODIFICATION_ASC:{
                                    ascendingCheck.setChecked(false);
                                    descendingCheck.setChecked(false);
                                    newestCheck.setChecked(false);
                                    oldestCheck.setChecked(true);
                                    largestCheck.setChecked(false);
                                    smallestCheck.setChecked(false);
                                    break;
                                }
                                case MegaApiJava.ORDER_MODIFICATION_DESC:{
                                    ascendingCheck.setChecked(false);
                                    descendingCheck.setChecked(false);
                                    newestCheck.setChecked(true);
                                    oldestCheck.setChecked(false);
                                    largestCheck.setChecked(false);
                                    smallestCheck.setChecked(false);
                                    break;
                                }
                                case MegaApiJava.ORDER_SIZE_ASC:{
                                    ascendingCheck.setChecked(false);
                                    descendingCheck.setChecked(false);
                                    newestCheck.setChecked(false);
                                    oldestCheck.setChecked(false);
                                    largestCheck.setChecked(false);
                                    smallestCheck.setChecked(true);
                                    break;
                                }
                                case MegaApiJava.ORDER_SIZE_DESC:{
                                    ascendingCheck.setChecked(false);
                                    descendingCheck.setChecked(false);
                                    newestCheck.setChecked(false);
                                    oldestCheck.setChecked(false);
                                    largestCheck.setChecked(true);
                                    smallestCheck.setChecked(false);
                                    break;
                                }
                            }
                        }
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

                        ascendingCheck.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                ascendingCheck.setChecked(true);
                                descendingCheck.setChecked(false);
                                if(((ManagerActivityLollipop) context).getOrderOthers()!=MegaApiJava.ORDER_DEFAULT_ASC){
                                    ((ManagerActivityLollipop) context).refreshOthersOrder(MegaApiJava.ORDER_DEFAULT_ASC);
                                }

                                if (dialog != null){
                                    dialog.dismiss();
                                }
                            }
                        });

                        descendingCheck.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                ascendingCheck.setChecked(false);
                                descendingCheck.setChecked(true);
                                if(((ManagerActivityLollipop) context).getOrderOthers()!=MegaApiJava.ORDER_DEFAULT_DESC){
                                    ((ManagerActivityLollipop) context).refreshOthersOrder(MegaApiJava.ORDER_DEFAULT_DESC);
                                }

                                if (dialog != null){
                                    dialog.dismiss();
                                }
                            }
                        });
                    }
                    else{
                        log("No first level navigation on Incoming Shares");
                        sortByNameTV.setText(context.getString(R.string.sortby_name));

                        ascendingCheck.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                ascendingCheck.setChecked(true);
                                descendingCheck.setChecked(false);
                                newestCheck.setChecked(false);
                                oldestCheck.setChecked(false);
                                largestCheck.setChecked(false);
                                smallestCheck.setChecked(false);

                                ((ManagerActivityLollipop) context).refreshCloudOrder(MegaApiJava.ORDER_DEFAULT_ASC);

                                if (dialog != null){
                                    dialog.dismiss();
                                }
                            }
                        });

                        descendingCheck.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                ascendingCheck.setChecked(false);
                                descendingCheck.setChecked(true);
                                newestCheck.setChecked(false);
                                oldestCheck.setChecked(false);
                                largestCheck.setChecked(false);
                                smallestCheck.setChecked(false);

                                ((ManagerActivityLollipop) context).refreshCloudOrder(MegaApiJava.ORDER_DEFAULT_DESC);

                                if (dialog != null){
                                    dialog.dismiss();
                                }
                            }
                        });

                        newestCheck.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                ascendingCheck.setChecked(false);
                                descendingCheck.setChecked(false);
                                newestCheck.setChecked(true);
                                oldestCheck.setChecked(false);
                                largestCheck.setChecked(false);
                                smallestCheck.setChecked(false);

                                ((ManagerActivityLollipop) context).refreshCloudOrder(MegaApiJava.ORDER_MODIFICATION_DESC);

                                if (dialog != null){
                                    dialog.dismiss();
                                }
                            }
                        });

                        oldestCheck.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                ascendingCheck.setChecked(false);
                                descendingCheck.setChecked(false);;
                                newestCheck.setChecked(false);
                                oldestCheck.setChecked(true);
                                largestCheck.setChecked(false);
                                smallestCheck.setChecked(false);

                                ((ManagerActivityLollipop) context).refreshCloudOrder(MegaApiJava.ORDER_MODIFICATION_ASC);

                                if (dialog != null){
                                    dialog.dismiss();
                                }
                            }
                        });


                        largestCheck.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                ascendingCheck.setChecked(false);
                                descendingCheck.setChecked(false);
                                newestCheck.setChecked(false);
                                oldestCheck.setChecked(false);
                                largestCheck.setChecked(true);
                                smallestCheck.setChecked(false);

                                ((ManagerActivityLollipop) context).refreshCloudOrder(MegaApiJava.ORDER_SIZE_DESC);

                                if (dialog != null){
                                    dialog.dismiss();
                                }
                            }
                        });

                        smallestCheck.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                ascendingCheck.setChecked(false);
                                descendingCheck.setChecked(false);
                                newestCheck.setChecked(false);
                                oldestCheck.setChecked(false);
                                largestCheck.setChecked(false);
                                smallestCheck.setChecked(true);

                                ((ManagerActivityLollipop) context).refreshCloudOrder(MegaApiJava.ORDER_SIZE_ASC);

                                if (dialog != null){
                                    dialog.dismiss();
                                }
                            }
                        });
                    }

                    break;
                }
                case CAMERA_UPLOADS:
                case MEDIA_UPLOADS: {
                    switch(((ManagerActivityLollipop) context).orderCamera){
                        case MegaApiJava.ORDER_MODIFICATION_ASC:{
                            log("ASCE");
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(true);
                            break;
                        }
                        case MegaApiJava.ORDER_MODIFICATION_DESC:{
                            log("DESC");
                            newestCheck.setChecked(true);
                            oldestCheck.setChecked(false);
                            break;
                        }
                    }

                    sortByNameTV.setVisibility(View.GONE);
                    ascendingCheck.setVisibility(View.GONE);
                    descendingCheck.setVisibility(View.GONE);
                    sortBySizeTV.setVisibility(View.GONE);
                    largestCheck.setVisibility(View.GONE);
                    smallestCheck.setVisibility(View.GONE);

                    oldestCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(true);
                            descendingCheck.setChecked(false);
                            if(((ManagerActivityLollipop) context).orderCamera!=MegaApiJava.ORDER_MODIFICATION_ASC){
                                ((ManagerActivityLollipop) context).selectSortUploads(MegaApiJava.ORDER_MODIFICATION_ASC);
                            }

                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });

                    newestCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(true);
                            if(((ManagerActivityLollipop) context).orderCamera!=MegaApiJava.ORDER_MODIFICATION_DESC){
                                ((ManagerActivityLollipop) context).selectSortUploads(MegaApiJava.ORDER_MODIFICATION_DESC);
                            }

                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });

                    break;
                }
                default: {
                    switch(((ManagerActivityLollipop) context).orderCloud){
                        case MegaApiJava.ORDER_DEFAULT_ASC:{
                            ascendingCheck.setChecked(true);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(false);
                            break;
                        }
                        case MegaApiJava.ORDER_DEFAULT_DESC:{
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(true);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(false);
                            break;
                        }
                        case MegaApiJava.ORDER_MODIFICATION_ASC:{
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(true);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(false);
                            break;
                        }
                        case MegaApiJava.ORDER_MODIFICATION_DESC:{
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(true);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(false);
                            break;
                        }
                        case MegaApiJava.ORDER_SIZE_ASC:{
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(true);
                            break;
                        }
                        case MegaApiJava.ORDER_SIZE_DESC:{
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(true);
                            smallestCheck.setChecked(false);
                            break;
                        }
                    }

                    ascendingCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(true);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(false);

                            ((ManagerActivityLollipop) context).refreshCloudOrder(MegaApiJava.ORDER_DEFAULT_ASC);

                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });

                    descendingCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(true);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(false);

                            ((ManagerActivityLollipop) context).refreshCloudOrder(MegaApiJava.ORDER_DEFAULT_DESC);

                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });


                    newestCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(true);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(false);

                            ((ManagerActivityLollipop) context).refreshCloudOrder(MegaApiJava.ORDER_MODIFICATION_DESC);

                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });

                    oldestCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);;
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(true);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(false);

                            ((ManagerActivityLollipop) context).refreshCloudOrder(MegaApiJava.ORDER_MODIFICATION_ASC);

                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });


                    largestCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(true);
                            smallestCheck.setChecked(false);

                            ((ManagerActivityLollipop) context).refreshCloudOrder(MegaApiJava.ORDER_SIZE_DESC);

                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });

                    smallestCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(true);

                            ((ManagerActivityLollipop) context).refreshCloudOrder(MegaApiJava.ORDER_SIZE_ASC);

                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });

                    break;
                }
            }
        }
        else if (context instanceof FileExplorerActivityLollipop) {

            MegaPreferences prefs = Util.getPreferences(context);
            int order = MegaApiJava.ORDER_DEFAULT_ASC;
            drawerItem = ((FileExplorerActivityLollipop) context).getCurrentItem();

            switch (drawerItem) {
                case CLOUD_DRIVE: {
                    if (prefs != null){
                        order = Integer.parseInt(prefs.getPreferredSortCloud());
                    }

                    switch(order){
                        case MegaApiJava.ORDER_DEFAULT_ASC:{
                            ascendingCheck.setChecked(true);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(false);
                            break;
                        }
                        case MegaApiJava.ORDER_DEFAULT_DESC:{
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(true);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(false);
                            break;
                        }
                        case MegaApiJava.ORDER_MODIFICATION_ASC:{
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(true);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(false);
                            break;
                        }
                        case MegaApiJava.ORDER_MODIFICATION_DESC:{
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(true);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(false);
                            break;
                        }
                        case MegaApiJava.ORDER_SIZE_ASC:{
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(true);
                            break;
                        }
                        case MegaApiJava.ORDER_SIZE_DESC:{
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(true);
                            smallestCheck.setChecked(false);
                            break;
                        }
                    }

                    ascendingCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(true);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(false);

                            ((FileExplorerActivityLollipop) context).refreshOrderNodes(MegaApiJava.ORDER_DEFAULT_ASC);

                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });

                    descendingCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(true);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(false);

                            ((FileExplorerActivityLollipop) context).refreshOrderNodes(MegaApiJava.ORDER_DEFAULT_DESC);

                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });


                    newestCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(true);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(false);

                            ((FileExplorerActivityLollipop) context).refreshOrderNodes(MegaApiJava.ORDER_MODIFICATION_DESC);

                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });

                    oldestCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);;
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(true);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(false);

                            ((FileExplorerActivityLollipop) context).refreshOrderNodes(MegaApiJava.ORDER_MODIFICATION_ASC);

                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });


                    largestCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(true);
                            smallestCheck.setChecked(false);

                            ((FileExplorerActivityLollipop) context).refreshOrderNodes(MegaApiJava.ORDER_SIZE_DESC);

                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });

                    smallestCheck.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ascendingCheck.setChecked(false);
                            descendingCheck.setChecked(false);
                            newestCheck.setChecked(false);
                            oldestCheck.setChecked(false);
                            largestCheck.setChecked(false);
                            smallestCheck.setChecked(true);

                            ((FileExplorerActivityLollipop) context).refreshOrderNodes(MegaApiJava.ORDER_SIZE_ASC);

                            if (dialog != null){
                                dialog.dismiss();
                            }
                        }
                    });
                    break;
                }
                case SHARED_ITEMS: {
                    if (((FileExplorerActivityLollipop) context).getParentHandleIncoming() == -1){
                        if (prefs != null){
                            order = Integer.parseInt(prefs.getPreferredSortOthers());
                        }
                        switch(order){
                            case MegaApiJava.ORDER_DEFAULT_ASC:{
                                log("ASCE");
                                ascendingCheck.setChecked(true);
                                descendingCheck.setChecked(false);
                                break;
                            }
                            case MegaApiJava.ORDER_DEFAULT_DESC:{
                                log("DESC");
                                ascendingCheck.setChecked(false);
                                descendingCheck.setChecked(true);
                                break;
                            }
                        }
                    }
                    else{
                        if (prefs != null){
                            order = Integer.parseInt(prefs.getPreferredSortCloud());
                        }
                        switch(order){
                            case MegaApiJava.ORDER_DEFAULT_ASC:{
                                ascendingCheck.setChecked(true);
                                descendingCheck.setChecked(false);
                                newestCheck.setChecked(false);
                                oldestCheck.setChecked(false);
                                largestCheck.setChecked(false);
                                smallestCheck.setChecked(false);
                                break;
                            }
                            case MegaApiJava.ORDER_DEFAULT_DESC:{
                                ascendingCheck.setChecked(false);
                                descendingCheck.setChecked(true);
                                newestCheck.setChecked(false);
                                oldestCheck.setChecked(false);
                                largestCheck.setChecked(false);
                                smallestCheck.setChecked(false);
                                break;
                            }
                            case MegaApiJava.ORDER_MODIFICATION_ASC:{
                                ascendingCheck.setChecked(false);
                                descendingCheck.setChecked(false);
                                newestCheck.setChecked(false);
                                oldestCheck.setChecked(true);
                                largestCheck.setChecked(false);
                                smallestCheck.setChecked(false);
                                break;
                            }
                            case MegaApiJava.ORDER_MODIFICATION_DESC:{
                                ascendingCheck.setChecked(false);
                                descendingCheck.setChecked(false);
                                newestCheck.setChecked(true);
                                oldestCheck.setChecked(false);
                                largestCheck.setChecked(false);
                                smallestCheck.setChecked(false);
                                break;
                            }
                            case MegaApiJava.ORDER_SIZE_ASC:{
                                ascendingCheck.setChecked(false);
                                descendingCheck.setChecked(false);
                                newestCheck.setChecked(false);
                                oldestCheck.setChecked(false);
                                largestCheck.setChecked(false);
                                smallestCheck.setChecked(true);
                                break;
                            }
                            case MegaApiJava.ORDER_SIZE_DESC:{
                                ascendingCheck.setChecked(false);
                                descendingCheck.setChecked(false);
                                newestCheck.setChecked(false);
                                oldestCheck.setChecked(false);
                                largestCheck.setChecked(true);
                                smallestCheck.setChecked(false);
                                break;
                            }
                        }
                    }

                    if(((FileExplorerActivityLollipop) context).getParentHandleIncoming() == -1){
                        sortByNameTV.setText(context.getString(R.string.sortby_owner_mail));

                        sortByDateTV.setVisibility(View.GONE);
                        newestCheck.setVisibility(View.GONE);
                        oldestCheck.setVisibility(View.GONE);
                        sortBySizeTV.setVisibility(View.GONE);
                        largestCheck.setVisibility(View.GONE);
                        smallestCheck.setVisibility(View.GONE);
                        final int finalOrder = order;
                        ascendingCheck.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                ascendingCheck.setChecked(true);
                                descendingCheck.setChecked(false);
                                if(finalOrder!=MegaApiJava.ORDER_DEFAULT_ASC){
                                    ((FileExplorerActivityLollipop) context).refreshOrderNodes(MegaApiJava.ORDER_DEFAULT_ASC);
                                }

                                if (dialog != null){
                                    dialog.dismiss();
                                }
                            }
                        });

                        descendingCheck.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                ascendingCheck.setChecked(false);
                                descendingCheck.setChecked(true);
                                if(finalOrder!=MegaApiJava.ORDER_DEFAULT_DESC){
                                    ((FileExplorerActivityLollipop) context).refreshOrderNodes(MegaApiJava.ORDER_DEFAULT_DESC);
                                }

                                if (dialog != null){
                                    dialog.dismiss();
                                }
                            }
                        });
                    }
                    else{
                        log("No first level navigation on Incoming Shares");
                        sortByNameTV.setText(context.getString(R.string.sortby_name));

                        ascendingCheck.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                ascendingCheck.setChecked(true);
                                descendingCheck.setChecked(false);
                                newestCheck.setChecked(false);
                                oldestCheck.setChecked(false);
                                largestCheck.setChecked(false);
                                smallestCheck.setChecked(false);

                                ((FileExplorerActivityLollipop) context).refreshOrderNodes(MegaApiJava.ORDER_DEFAULT_ASC);

                                if (dialog != null){
                                    dialog.dismiss();
                                }
                            }
                        });

                        descendingCheck.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                ascendingCheck.setChecked(false);
                                descendingCheck.setChecked(true);
                                newestCheck.setChecked(false);
                                oldestCheck.setChecked(false);
                                largestCheck.setChecked(false);
                                smallestCheck.setChecked(false);

                                ((FileExplorerActivityLollipop) context).refreshOrderNodes(MegaApiJava.ORDER_DEFAULT_DESC);

                                if (dialog != null){
                                    dialog.dismiss();
                                }
                            }
                        });

                        newestCheck.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                ascendingCheck.setChecked(false);
                                descendingCheck.setChecked(false);
                                newestCheck.setChecked(true);
                                oldestCheck.setChecked(false);
                                largestCheck.setChecked(false);
                                smallestCheck.setChecked(false);

                                ((FileExplorerActivityLollipop) context).refreshOrderNodes(MegaApiJava.ORDER_MODIFICATION_DESC);

                                if (dialog != null){
                                    dialog.dismiss();
                                }
                            }
                        });

                        oldestCheck.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                ascendingCheck.setChecked(false);
                                descendingCheck.setChecked(false);;
                                newestCheck.setChecked(false);
                                oldestCheck.setChecked(true);
                                largestCheck.setChecked(false);
                                smallestCheck.setChecked(false);

                                ((FileExplorerActivityLollipop) context).refreshOrderNodes(MegaApiJava.ORDER_MODIFICATION_ASC);

                                if (dialog != null){
                                    dialog.dismiss();
                                }
                            }
                        });


                        largestCheck.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                ascendingCheck.setChecked(false);
                                descendingCheck.setChecked(false);
                                newestCheck.setChecked(false);
                                oldestCheck.setChecked(false);
                                largestCheck.setChecked(true);
                                smallestCheck.setChecked(false);

                                ((FileExplorerActivityLollipop) context).refreshOrderNodes(MegaApiJava.ORDER_SIZE_DESC);

                                if (dialog != null){
                                    dialog.dismiss();
                                }
                            }
                        });

                        smallestCheck.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                ascendingCheck.setChecked(false);
                                descendingCheck.setChecked(false);
                                newestCheck.setChecked(false);
                                oldestCheck.setChecked(false);
                                largestCheck.setChecked(false);
                                smallestCheck.setChecked(true);

                                ((FileExplorerActivityLollipop) context).refreshOrderNodes(MegaApiJava.ORDER_SIZE_ASC);

                                if (dialog != null){
                                    dialog.dismiss();
                                }
                            }
                        });
                    }

                    break;
                }
            }
        }
    }

    public static void log(String message) {
        Util.log("SorterContentActivity", message);
    }
}
