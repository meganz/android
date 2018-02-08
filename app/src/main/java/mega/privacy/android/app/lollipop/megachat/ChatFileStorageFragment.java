package mega.privacy.android.app.lollipop.megachat;


import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridLayoutManager;
import mega.privacy.android.app.components.CustomizedGridRecyclerView;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaBrowserLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.MegaChatFileStorageAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.managerSections.FileBrowserFragmentLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaUser;

public class ChatFileStorageFragment extends Fragment {

    private static final String BUNDLE_RECYCLER_LAYOUT = "classname.recycler.layout";

    RecyclerView recyclerView;
    MegaChatFileStorageAdapter adapter;
    ChatFileStorageFragment fileStorageFragment = this;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    Context context;
    ActionBar aB;

    float density;
    DisplayMetrics outMetrics;
    Display display;

    DatabaseHandler dbH;
    MegaPreferences prefs;

    public ActionMode actionMode;

    LinearLayoutManager mLayoutManager;
    CustomizedGridLayoutManager gridLayoutManager;
    MegaNode selectedNode = null;

    ArrayList<Bitmap> thumBitmap = new ArrayList<>();
    ArrayList<String> imagesPath = new ArrayList<>();
    String downloadLocationDefaultPath = Util.downloadDIR;

    int count = 0;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        log("*******FRAGMENT: onSaveInstanceState");
        super.onSaveInstanceState(outState);
        if(recyclerView.getLayoutManager()!=null){
            outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, recyclerView.getLayoutManager().onSaveInstanceState());
        }
    }

    public static ChatFileStorageFragment newInstance() {
        log("*******FRAGMENT: newInstance");
        ChatFileStorageFragment fragment = new ChatFileStorageFragment();
        return fragment;
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("*******FRAGMENT: onCreate");
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        dbH = DatabaseHandler.getDbHandler(getActivity());

        prefs = dbH.getPreferences();
        if (prefs != null){
            log("prefs != null");
            if (prefs.getStorageAskAlways() != null){
                if (!Boolean.parseBoolean(prefs.getStorageAskAlways())){
                    log("askMe==false");
                    if (prefs.getStorageDownloadLocation() != null){
                        if (prefs.getStorageDownloadLocation().compareTo("") != 0){
                            downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
                        }
                    }
                }
            }
        }

        super.onCreate(savedInstanceState);
        log("after onCreate called super");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("*******FRAGMENT: onCreateView");

        if(!isAdded()){
            return null;
        }

        log("fragment ADDED");

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (aB == null){
            aB = ((AppCompatActivity)context).getSupportActionBar();
        }

        if (megaApi.getRootNode() == null){
            return null;
        }

        display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        density  = getResources().getDisplayMetrics().density;

        //GET THUMBNAIL PATH:
        String[] projection = new String[]{
            MediaStore.Images.Media.DATA,
        };

        Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cur = getActivity().managedQuery(images, projection, "", null, "");

        if (cur.moveToFirst()) {

            int dataColumn = cur.getColumnIndex(MediaStore.Images.Media.DATA);
            do {
                Cursor ca = getActivity().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[] { MediaStore.MediaColumns._ID }, MediaStore.MediaColumns.DATA + "=?", new String[] {cur.getString(dataColumn)}, null);

                if (ca != null && ca.moveToFirst()) {
                    int id = ca.getInt(ca.getColumnIndex(MediaStore.MediaColumns._ID));
                    thumBitmap.add(MediaStore.Images.Thumbnails.getThumbnail(getActivity().getContentResolver(), id, MediaStore.Images.Thumbnails.MINI_KIND, null ));
                }
            } while (cur.moveToNext());
        }
        cur.close();

        log("********thumBitmap.size(): "+thumBitmap.size());


        //GET IMAGES PATH:
        String[] projection1 = new String[]{
            MediaStore.Images.Media.DATA,
        };

        Uri images1 = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cur1 = getActivity().managedQuery(images1, projection1, "", null, "");

        if (cur1.moveToFirst()) {
            int dataColumn = cur1.getColumnIndex(MediaStore.Images.Media.DATA);
            do {
                imagesPath.add(cur1.getString(dataColumn));
            } while (cur1.moveToNext());
        }
        cur1.close();
        count = imagesPath.size();
        log("imagesPath.size(): "+imagesPath.size());

        ((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

        log("Grid View");
        View v = inflater.inflate(R.layout.fragment_filestorage, container, false);

        recyclerView = (CustomizedGridRecyclerView) v.findViewById(R.id.file_storage_grid_view_browser);
        recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(80, outMetrics));
        recyclerView.setClipToPadding(false);

        recyclerView.setHasFixedSize(true);
        gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();

        recyclerView.setItemAnimator(new DefaultItemAnimator());

        if (adapter == null){
            adapter = new MegaChatFileStorageAdapter(context, this, recyclerView, aB, thumBitmap);

        }else{
            adapter.setNodes(thumBitmap);
        }

        adapter.setMultipleSelect(false);

        recyclerView.setAdapter(adapter);

        setNodes(thumBitmap);

        if (adapter.getItemCount() == 0){
            recyclerView.setVisibility(View.VISIBLE);
        }else{
            recyclerView.setVisibility(View.VISIBLE);
        }
            return v;
    }


    @Override
    public void onAttach(Activity activity) {
        log("*******FRAGMENT: onAttach");
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }

    @Override
    public void onAttach(Context context) {
        log("*******FRAGMENT: onAttach");

        super.onAttach(context);
        this.context = context;
        aB = ((AppCompatActivity)context).getSupportActionBar();
    }

    public void itemClick(int position) {
        log("*******FRAGMENT: itemClick-> "+position);

        ((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();
        if (adapter.isMultipleSelect()){
            log("*****multiselect ON");
            adapter.toggleSelection(position);

//            List<Bitmap> selectedNodes = adapter.getSelectedNodes();
//            if (selectedNodes.size() > 0){
//
////                ((ManagerActivityLollipop)context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_RED);
//            }
////			else{
////				hideMultipleSelect();
////			}
        }
        else{
            log("********************* SEND IMAGEEEEEE");

            String filePath = imagesPath.get(position);
            log("****** Position: "+position+", filePath"+filePath);

            ((ChatActivityLollipop) getActivity()).uploadTakePicture(filePath);

        }
    }


    private static void log(String log) {
        Util.log("ChatFileStorageFragment", log);
    }


    public int onBackPressed(){
        log("*******FRAGMENT: onBackPressed");
        ((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

        if (adapter != null){

           recyclerView.setVisibility(View.GONE);
//
//            int lastVisiblePosition = 0;
//            if(!lastPositionStack.empty()){
//                lastVisiblePosition = lastPositionStack.pop();
//                log("Pop of the stack "+lastVisiblePosition+" position");
//            }
//            log("Scroll to "+lastVisiblePosition+" position");
//
//            if(lastVisiblePosition>=0){
//
//                if(((ManagerActivityLollipop)context).isList){
//                    mLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
//                }
//                else{
//                    gridLayoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
//                }
//            }

        }

        return 0;
    }

    public RecyclerView getRecyclerView(){
        return recyclerView;
    }

    public void setNodes(ArrayList<Bitmap> thumImages){
        log("*******FRAGMENT: setNodes");

        this.thumBitmap = thumImages;
            if (adapter != null){
                adapter.setNodes(thumImages);

                if (adapter.getItemCount() == 0){
                    recyclerView.setVisibility(View.VISIBLE);
                }else{
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
            else{
                log("grid adapter is NULL----------------");
            }
    }

    public int getItemCount(){
        log("*******FRAGMENT: getItemCount");

        if(adapter!=null){
            return adapter.getItemCount();
        }
        return 0;
    }


    public boolean showSelectMenuItem(){
        log("*******FRAGMENT: showSelectMenuItem");
        if (adapter != null){
            return adapter.isMultipleSelect();
        }

        return false;
    }

    public void selectAll(){
        log("*******FRAGMENT: selectAll");
        if (adapter != null){
            if(adapter.isMultipleSelect()){
                adapter.selectAll();
            }
            else{
                adapter.setMultipleSelect(true);
                adapter.selectAll();

            }

        }
    }

    private void clearSelections() {
        log("*******FRAGMENT: clearSelections");

        if(adapter.isMultipleSelect()){
            adapter.clearSelections();
        }
    }

    public void hideMultipleSelect() {
        log("*******FRAGMENT: hideMultipleSelect");

        log("hideMultipleSelect");
        adapter.setMultipleSelect(false);
    }

    public boolean isMultipleselect(){
        log("*******FRAGMENT: isMultipleselect");

        if(adapter!=null){
            return adapter.isMultipleSelect();
        }
        return false;
    }


}
