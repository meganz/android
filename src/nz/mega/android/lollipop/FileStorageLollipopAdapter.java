package nz.mega.android.lollipop;

import java.util.ArrayList;
import java.util.List;

import nz.mega.android.MimeTypeList;
import nz.mega.android.R;
import nz.mega.android.lollipop.FileStorageActivityLollipop.FileDocument;
import nz.mega.android.lollipop.FileStorageActivityLollipop.Mode;
import nz.mega.android.lollipop.MegaBrowserLollipopAdapter.ViewHolderBrowser;
import nz.mega.android.lollipop.MegaSharedFolderLollipopAdapter.OnItemClickListener;
import nz.mega.android.utils.Util;
import nz.mega.sdk.MegaNode;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/*
 * Adapter for FilestorageActivity list
 */
public class FileStorageLollipopAdapter extends RecyclerView.Adapter<FileStorageLollipopAdapter.ViewHolderFileStorage> implements OnClickListener {
		
	private Context context;
	private List<FileDocument> currentFiles;
	private Mode mode;
	OnItemClickListener mItemClickListener;
	RecyclerView listFragment;
	private SparseBooleanArray selectedItems;
	int positionClicked;
	boolean multipleSelect;
	
	public FileStorageLollipopAdapter(Context context, RecyclerView listView, Mode mode2) {
		this.mode = mode2;
		this.listFragment = listView;
		this.context = context;
	}	
	
	/*private view holder class*/
    class ViewHolderFileStorage extends RecyclerView.ViewHolder implements View.OnClickListener{
    	
    	public ImageView imageView;
    	public TextView textViewFileName;
    	public TextView textViewFileSize;
    	public TextView textViewUpdated;
    	public RelativeLayout itemLayout;
    	public int currentPosition;
    	public FileDocument document;    	
    	
    	public ViewHolderFileStorage(View itemView) {
			super(itemView);
            itemView.setOnClickListener(this);
		}
    	
    	@Override
		public void onClick(View v) {
			if(mItemClickListener != null){
				mItemClickListener.onItemClick(v, getPosition());
			}			
		}
    }
	
	@Override
	public ViewHolderFileStorage onCreateViewHolder(ViewGroup parent, int viewType) {
		
		listFragment = (RecyclerView) parent;
		//		final int _position = position;
				
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;		
	    float scaleW = Util.getScaleW(outMetrics, density);
		
	    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_explorer, parent, false);
	    ViewHolderFileStorage holder = new ViewHolderFileStorage(v);

		holder.itemLayout = (RelativeLayout) v.findViewById(R.id.file_explorer_item_layout);
		holder.itemLayout.setOnClickListener(this);
		holder.imageView = (ImageView) v.findViewById(R.id.file_explorer_thumbnail);
		holder.textViewFileName = (TextView) v.findViewById(R.id.file_explorer_filename);
		holder.textViewFileName.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
		holder.textViewFileName.getLayoutParams().width = Util.px2dp((225*scaleW), outMetrics);
		holder.textViewFileSize = (TextView) v.findViewById(R.id.file_explorer_filesize);
		holder.textViewUpdated = (TextView) v.findViewById(R.id.file_explorer_updated);
			
		v.setTag(holder);
		return holder;
	}	
	
	@Override
	public void onBindViewHolder(ViewHolderFileStorage holder, int position){
	
		//NUEVOOO
		
		holder.currentPosition = position;
		FileDocument document = currentFiles.get(position);

		holder.textViewFileName.setText(document.getName());
	
		long documentSize = document.getSize();
		holder.textViewFileSize.setText(Util.getSizeString(documentSize));
		
		if(mode == Mode.PICK_FILE)
		{
			if(document.getFile().canRead() == false){
				Util.setViewAlpha(holder.imageView, .4f);
				holder.textViewFileName.setTextColor(context.getResources().getColor(R.color.text_secondary));
			}	
			else{
				Util.setViewAlpha(holder.imageView, 1);
				holder.textViewFileName.setTextColor(context.getResources().getColor(android.R.color.black));
				
				if (multipleSelect) {
					if(this.isItemChecked(position)){
						holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.file_list_selected_row));
					}
					else{
						holder.itemLayout.setBackgroundColor(Color.WHITE);
					}		
				}
				else{
					holder.itemLayout.setBackgroundColor(Color.WHITE);
				}
			}
		}
		else 
		{
//			if(isEnabled(position)){
//				holder.itemLayout.setEnabled(false)
//			}				
				
			Util.setViewAlpha(holder.imageView, .4f);
			holder.textViewFileName.setTextColor(context.getResources().getColor(R.color.text_secondary));
		}	
			
		if (document.isFolder()){	
			holder.imageView.setImageResource(R.drawable.ic_folder_list);
		}
		else{
			//Document is FILE
			holder.imageView.setImageResource(MimeTypeList.typeForName(document.getName()).getIconResourceId());			
		}
		
		try {
			holder.textViewUpdated.setText(DateUtils.getRelativeTimeSpanString(document.getTimestampInMillis()));
			} 
		catch(Exception ex)	{
			holder.textViewUpdated.setText("");
		}
	}
	
	
	// Set new files on folder change
	public void setFiles(List<FileDocument> newFiles) {
		currentFiles = newFiles;
		notifyDataSetChanged();
	}
	
	public FileDocument getDocumentAt(int position) {
		if(currentFiles == null || position >= currentFiles.size())
		{
			return null;
		}
		
		return currentFiles.get(position);
	}
	
	@Override
	public int getItemCount() {
		if (currentFiles == null) {
			return 0;
		}
		int size = currentFiles.size();
		return size == 0 ? 1 : size;
	}
	
	public int getPositionClicked() {
		return positionClicked;
	}

	public void setPositionClicked(int p) {
		positionClicked = p;
		notifyDataSetChanged();
	}

	public boolean isEnabled(int position) {
		if (currentFiles.size() == 0) {
			return false;
		}
		FileDocument document = currentFiles.get(position);
		if (mode == Mode.PICK_FOLDER && !document.isFolder()) {
			return false;
		}
		if (document.getFile().canRead() == false) {
			return false;
		}

		return true;
	}
	
	public void toggleSelection(int pos) {
		log("toggleSelection");
		if (selectedItems.get(pos, false)) {
			log("delete pos: "+pos);
			selectedItems.delete(pos);
		}
		else {
			log("PUT pos: "+pos);
			selectedItems.put(pos, true);
		}
		notifyItemChanged(pos);
	}
	
	public void selectAll(){
		for (int i= 0; i<this.getItemCount();i++){
			if(!isItemChecked(i)){
				toggleSelection(i);
			}
		}
	}

	public void clearSelections() {
		if(selectedItems!=null){
			selectedItems.clear();
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
	
	/*
	 * Get list of all selected nodes
	 */
	public List<FileDocument> getSelectedDocuments() {
		ArrayList<FileDocument> nodes = new ArrayList<FileDocument>();
		
		for (int i = 0; i < selectedItems.size(); i++) {
			if (selectedItems.valueAt(i) == true) {
				FileDocument document = getDocumentAt(selectedItems.keyAt(i));
				if (document != null){
					nodes.add(document);
				}
			}
		}
		return nodes;
	}
	
	/*
	 * Get list of all selected nodes
	 */
	public int getSelectedCount() {
		
		if (selectedItems!=null){
			return selectedItems.size();
		}

		return -1;
	}
	
	public boolean isMultipleSelect() {
		return multipleSelect;
	}

	public void setMultipleSelect(boolean multipleSelect) {
		if (this.multipleSelect != multipleSelect) {
			this.multipleSelect = multipleSelect;
			notifyDataSetChanged();
		}
		if(this.multipleSelect)
		{
			selectedItems = new SparseBooleanArray();
		}
	}
	
	public Object getItem(int position) {
		return currentFiles.get(position);
	}
	
	@Override
	public void onClick(View v) {
		log("click!");
		
		ViewHolderFileStorage holder = (ViewHolderFileStorage) v.getTag();

		int currentPosition = holder.currentPosition;
		final FileDocument doc = (FileDocument) getItem(currentPosition);
		log(" in position: "+holder.currentPosition+" document: "+doc.getName());

		switch (v.getId()) {		
			case R.id.file_explorer_item_layout:{
				((FileStorageActivityLollipop) context).itemClick(currentPosition);
				break;
			}
		}
	}	

	private static void log(String message) {
		Util.log("FileStorageLollipopAdapter", message);
	}
}
