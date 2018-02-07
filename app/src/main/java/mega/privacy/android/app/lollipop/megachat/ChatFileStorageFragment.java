package mega.privacy.android.app.lollipop.megachat;


import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
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

import java.io.File;
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
    FastScroller fastScroller;

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

    ArrayList<String> imagesPath = new ArrayList<>();
    ArrayList<String> thumbPath = new ArrayList<>();
    String downloadLocationDefaultPath = Util.downloadDIR;

    int count = 0;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        log("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        if(recyclerView.getLayoutManager()!=null){
            outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, recyclerView.getLayoutManager().onSaveInstanceState());
        }
    }

    public static ChatFileStorageFragment newInstance() {
        log("newInstance");
        ChatFileStorageFragment fragment = new ChatFileStorageFragment();
        return fragment;
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate");
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
        log("onCreateView");

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

        //GET IMAGES PATH:
        String[] projection = new String[]{
            MediaStore.Images.Media.DATA,
        };

        Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cur = getActivity().managedQuery(images, projection, "", null, "");

        if (cur.moveToFirst()) {

            int dataColumn = cur.getColumnIndex(MediaStore.Images.Media.DATA);
            do {
                imagesPath.add(cur.getString(dataColumn));
            } while (cur.moveToNext());
        }
        cur.close();
        count = imagesPath.size();
        log("imagesPath.size(): "+imagesPath.size());
        String re = getThumbnailPath(context, imagesPath.get(1));
        log("re "+re);




//        //GET IMAGES PATH:
//       String[] projection = new String[]{
//                MediaStore.Images.Media.DATA,
//        };
//
//        Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
//        Cursor cur = getActivity().managedQuery(images, projection, "", null, "");
//
//        if (cur.moveToFirst()) {
//
//            int dataColumn = cur.getColumnIndex(MediaStore.Images.Media.DATA);
//            do {
//                imagesPath.add(cur.getString(dataColumn));
//            } while (cur.moveToNext());
//        }
//        cur.close();
//        count = imagesPath.size();
//        log("imagesPath.size(): "+imagesPath.size());


        //GET THUMBNAILS PATH
//        Cursor cursor = MediaStore.Images.Thumbnails.queryMiniThumbnails(getActivity().getContentResolver(), getActivity().selectedImageUri, MediaStore.Images.Thumbnails.MINI_KIND, null );
//        if( cursor != null && cursor.getCount() > 0 ) {
//            cursor.moveToFirst();
//            String uri = cursor.getString( cursor.getColumnIndex( MediaStore.Images.Thumbnails.DATA ) );
//        }

        ((MegaApplication) ((Activity)context).getApplication()).sendSignalPresenceActivity();

        log("Grid View");
        View v = inflater.inflate(R.layout.fragment_filestorage, container, false);

        recyclerView = (CustomizedGridRecyclerView) v.findViewById(R.id.file_storage_grid_view_browser);
        fastScroller = (FastScroller) v.findViewById(R.id.fastscroll);

        recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(80, outMetrics));
        recyclerView.setClipToPadding(false);

        recyclerView.setHasFixedSize(true);
        gridLayoutManager = (CustomizedGridLayoutManager) recyclerView.getLayoutManager();

        recyclerView.setItemAnimator(new DefaultItemAnimator());

        if (adapter == null){
            adapter = new MegaChatFileStorageAdapter(context, this, imagesPath, recyclerView, aB);

        }else{
            adapter.setNodes(imagesPath);
        }

//        adapter.setMultipleSelect(false);

        recyclerView.setAdapter(adapter);
        fastScroller.setRecyclerView(recyclerView);

        setNodes(imagesPath);

        if (adapter.getItemCount() == 0){
            recyclerView.setVisibility(View.VISIBLE);
        }else{
            recyclerView.setVisibility(View.VISIBLE);
        }
            return v;
    }


    @Override
    public void onAttach(Activity activity) {
        log("onAttach");
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        aB = ((AppCompatActivity)context).getSupportActionBar();
    }

    private static void log(String log) {
        Util.log("ChatFileStorageFragment", log);
    }


    public int onBackPressed(){
        log("onBackPressed");
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

    public void setNodes(ArrayList<String> imagesPath){
        log("setNodes: "+imagesPath.size());

        visibilityFastScroller();

        this.imagesPath = imagesPath;
            if (adapter != null){
                adapter.setNodes(imagesPath);

                if (adapter.getItemCount() == 0){
                    log("********* adapter = 0");
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
        if(adapter!=null){
            return adapter.getItemCount();
        }
        return 0;
    }

    public void visibilityFastScroller(){
        if(adapter == null){
            fastScroller.setVisibility(View.GONE);
        }else{
            if(adapter.getItemCount() < Constants.MIN_ITEMS_SCROLLBAR){
                fastScroller.setVisibility(View.GONE);
            }else{
                fastScroller.setVisibility(View.VISIBLE);
            }
        }

    }

    public static String getThumbnailPath(Context context, String path)
    {
        long imageId = -1;

        String[] projection = new String[] { MediaStore.MediaColumns._ID };
        String selection = MediaStore.MediaColumns.DATA + "=?";
        String[] selectionArgs = new String[] { path };
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
        if (cursor != null && cursor.moveToFirst())
        {
            imageId = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            cursor.close();
        }

        String result = null;
        cursor = MediaStore.Images.Thumbnails.queryMiniThumbnail(context.getContentResolver(), imageId, MediaStore.Images.Thumbnails.MINI_KIND, null);
        if (cursor != null && cursor.getCount() > 0)
        {
            cursor.moveToFirst();
            result = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails.DATA));
            cursor.close();
        }

        return result;
    }

}
