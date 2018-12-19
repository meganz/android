package mega.privacy.android.app.lollipop.megachat.chatAdapters;

import android.app.Activity;
import android.content.Context;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.ChatFileStorageFragment;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;


public class MegaChatFileStorageAdapter extends RecyclerView.Adapter<MegaChatFileStorageAdapter.ViewHolderBrowser> implements OnClickListener, View.OnLongClickListener{

    Context context;
    MegaApiAndroid megaApi;
    ActionBar aB;
    ArrayList<String> uriImages;
    Object fragment;
    DisplayMetrics outMetrics;
    private SparseBooleanArray selectedItems;
    boolean multipleSelect;

    int padding = 6;
    private int dimPhotos;

    DatabaseHandler dbH;
    private int count;
    MegaPreferences prefs;
    private SparseBooleanArray checkedItems = new SparseBooleanArray();

    /* public static view holder class */
    public static class ViewHolderBrowser extends ViewHolder {

        public ViewHolderBrowser(View v) {
            super(v);
        }
        public RelativeLayout itemLayout;
    }

    public static class ViewHolderBrowserGrid extends ViewHolderBrowser {

        public ViewHolderBrowserGrid(View v){
            super(v);
        }
        public ImageView photo;
        public RelativeLayout thumbLayout;
        public ImageView photoSelected;
//        public RelativeLayout photoUnselected;
    }

    public void toggleSelection(int pos) {
        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
            ((ChatFileStorageFragment) fragment).removePosition(pos);
        }else {
            selectedItems.put(pos, true);
            ((ChatFileStorageFragment) fragment).addPosition(pos);
        }
        notifyItemChanged(pos);

        if (selectedItems.size() <= 0){
            ((ChatFileStorageFragment) fragment).hideMultipleSelect();
        }
        notifyDataSetChanged();

    }

    public void toggleAllSelection(int pos) {
        final int positionToflip = pos;

        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
            ((ChatFileStorageFragment) fragment).removePosition(pos);

        }else {
            selectedItems.put(pos, true);
            ((ChatFileStorageFragment) fragment).addPosition(pos);

        }
        log("adapter type is GRID");
        if (selectedItems.size() <= 0){
            ((ChatFileStorageFragment) fragment).hideMultipleSelect();
        }
        notifyItemChanged(positionToflip);
    }

    public void clearSelections() {
        for (int i= 0; i<this.getItemCount();i++){
            if(isItemChecked(i)){
                toggleAllSelection(i);
            }
        }
        notifyDataSetChanged();

    }

    private boolean isItemChecked(int position) {
        return selectedItems.get(position);
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<Integer>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    public MegaChatFileStorageAdapter(Context _context, Object fragment, RecyclerView recyclerView, ActionBar aB, ArrayList<String> _uriImages, int dimPhotos) {
        this.context = _context;
        this.fragment = fragment;
        this.uriImages = _uriImages;
        this.dimPhotos = dimPhotos;
        dbH = DatabaseHandler.getDbHandler(context);

        this.aB = aB;

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication())
                    .getMegaApi();
        }
    }

    public void setNodes(ArrayList<String> uriImages) {
        this.uriImages = uriImages;
        notifyDataSetChanged();
    }

    public void setDimensionPhotos(int dimPhotos){
        this.dimPhotos = dimPhotos;
        notifyDataSetChanged();
    }


    public int getSpanSizeOfPosition(int position){
        return 1;
    }

    @Override
    public ViewHolderBrowser onCreateViewHolder(ViewGroup parent, int viewType) {

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_storage_grid, parent, false);
        ViewHolderBrowserGrid holderGrid = new ViewHolderBrowserGrid(v);

        holderGrid.itemLayout = (RelativeLayout) v.findViewById(R.id.file_storage_grid_item_layout);

        holderGrid.thumbLayout = (RelativeLayout) v.findViewById(R.id.file_storage_grid_thumbnail_layout);
//        ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) holderGrid.thumbLayout.getLayoutParams();
//        marginParams.setMargins(padding, padding, 0, 0);
//        holderGrid.thumbLayout.setLayoutParams(marginParams);

        holderGrid.thumbLayout.setPadding(padding,padding,padding,padding);

        ViewGroup.LayoutParams params = holderGrid.thumbLayout.getLayoutParams();
        params.height = dimPhotos;
        params.width = dimPhotos;
        holderGrid.thumbLayout.setLayoutParams(params);

        holderGrid.thumbLayout.setVisibility(View.GONE);

        holderGrid.photo = (ImageView) v.findViewById(R.id.file_storage_grid_thumbnail);
        holderGrid.photoSelected = (ImageView) v.findViewById(R.id.thumbnail_selected);
        holderGrid.photoSelected.setVisibility(View.GONE);
//        holderGrid.photoUnselected = (RelativeLayout) v.findViewById(R.id.thumbnail_unselected);
//        holderGrid.photoUnselected.setVisibility(View.GONE);

        holderGrid.photoSelected.setMaxHeight(dimPhotos);
        holderGrid.photoSelected.setMaxWidth(dimPhotos);

        holderGrid.itemLayout.setTag(holderGrid);
        holderGrid.itemLayout.setOnClickListener(this);
        holderGrid.itemLayout.setOnLongClickListener(this);

        v.setTag(holderGrid);

        return holderGrid;
    }

    @Override
    public void onBindViewHolder(ViewHolderBrowser holder, int position) {
        ViewHolderBrowserGrid holderGrid = (ViewHolderBrowserGrid) holder;
        onBindViewHolderGrid(holderGrid, position);
    }

    public void onBindViewHolderGrid(ViewHolderBrowserGrid holder, int position){
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
//        Bitmap image = (Bitmap) getItem(position);
//        if(image == null){
//            return;
//        }
        holder.thumbLayout.setVisibility(View.VISIBLE);

        holder.photo.setVisibility(View.VISIBLE);

        //holder.photo.setImageBitmap(image);
        Picasso.with(holder.photo.getContext())
                .load(uriImages.get(position))
                .fit()
                .centerCrop()
                .into(holder.photo);

        if (!multipleSelect) {
            holder.photoSelected.setVisibility(View.GONE);
//            holder.photoUnselected.setVisibility(View.GONE);

        }else {

            if(this.isItemChecked(position)){
                holder.photoSelected.setVisibility(View.VISIBLE);
//                holder.photoUnselected.setVisibility(View.GONE);

            }else{
                holder.photoSelected.setVisibility(View.GONE);
//                holder.photoUnselected.setVisibility(View.VISIBLE);
            }
        }

    }

    @Override
    public int getItemCount() {
        if (uriImages != null){
            return uriImages.size();
        }else{
            return 0;
        }
    }

    public Object getItem(int position) {
        if (uriImages != null){
            return uriImages.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public String getItemAt(int position) {

        try {
            if (uriImages != null) {
                return uriImages.get(position);
            }
        } catch (IndexOutOfBoundsException e) {
        }
        return null;
    }

    private static void log(String log) {
        Util.log("MegaChatFileStorageAdapter", log);
    }

    @Override
    public void onClick(View v) {

        ViewHolderBrowser holder = (ViewHolderBrowser) v.getTag();
        int currentPosition = holder.getAdapterPosition();
        log("onClick -> Current position: "+currentPosition);
        if(currentPosition<0){
            log("Current position error - not valid value");
            return;
        }
        if (!isMultipleSelect()){
            setMultipleSelect(true);
            ((ChatFileStorageFragment) fragment).itemClick(currentPosition);

        }else{
            ((ChatFileStorageFragment) fragment).itemClick(currentPosition);

        }
        //((ChatFileStorageFragment) fragment).itemClick(currentPosition);

    }

    @Override
    public boolean onLongClick(View view) {

        ViewHolderBrowser holder = (ViewHolderBrowser) view.getTag();
        int currentPosition = holder.getAdapterPosition();

        if (!isMultipleSelect()){
            setMultipleSelect(true);
            ((ChatFileStorageFragment) fragment).itemClick(currentPosition);

        }else{
            ((ChatFileStorageFragment) fragment).itemClick(currentPosition);

        }

        return true;
    }

    public boolean isMultipleSelect() {
        return multipleSelect;
    }

    public void setMultipleSelect(boolean multipleSelect) {
        if (this.multipleSelect != multipleSelect) {
            this.multipleSelect = multipleSelect;
        }
        if(this.multipleSelect){
            selectedItems = new SparseBooleanArray();
        }
        ((ChatFileStorageFragment) fragment).activatedMultiselect(this.multipleSelect);
    }

}
