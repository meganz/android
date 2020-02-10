package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.VersionsFileActivity;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class VersionsFileAdapter extends RecyclerView.Adapter<VersionsFileAdapter.ViewHolderVersion> implements OnClickListener, View.OnLongClickListener {

	public static final int ITEM_VIEW_TYPE_LIST = 0;
	public static final int ITEM_VIEW_TYPE_GRID = 1;

	Context context;
	MegaApiAndroid megaApi;

//	int positionClicked;
	ArrayList<MegaNode> nodes;

	long parentHandle = -1;
	DisplayMetrics outMetrics;

	private SparseBooleanArray selectedItems;

	RecyclerView listFragment;

	boolean multipleSelect;

	/* public static view holder class */
	public static class ViewHolderVersion extends ViewHolder {

		public ViewHolderVersion(View v) {
			super(v);
		}

		public TextView textViewFileName;
		public TextView textViewFileSize;
		public long document;
		public ImageView imageView;
		public RelativeLayout itemLayout;
		public RelativeLayout threeDotsLayout;
		public RelativeLayout headerLayout;
		public TextView titleHeader;
		public TextView sizeHeader;
	}

	public void toggleAllSelection(int pos) {
		logDebug("Position: " + pos);
		final int positionToflip = pos;

		if (selectedItems.get(pos, false)) {
			logDebug("Delete pos: " + pos);
			selectedItems.delete(pos);
		}
		else {
			logDebug("PUT pos: " + pos);
			selectedItems.put(pos, true);
		}

		VersionsFileAdapter.ViewHolderVersion view = (VersionsFileAdapter.ViewHolderVersion) listFragment.findViewHolderForLayoutPosition(pos);
		if(view!=null){
			logDebug("Start animation: " + pos + " multiselection state: " + isMultipleSelect());
			Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
			flipAnimation.setAnimationListener(new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					logDebug("onAnimationEnd");
					if (selectedItems.size() <= 0){
						logDebug("hideMultipleSelect");
						((VersionsFileActivity) context).hideMultipleSelect();
					}
					logDebug("notified item changed");
					notifyItemChanged(positionToflip);
				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}
			});
			view.imageView.startAnimation(flipAnimation);
		}
		else{
			logWarning("NULL view pos: " + positionToflip);
			notifyItemChanged(pos);
		}

	}

	public void toggleSelection(int pos) {
		logDebug("Position: " + pos);

		if (selectedItems.get(pos, false)) {
			logDebug("Delete pos: " + pos);
			selectedItems.delete(pos);
		}
		else {
			logDebug("PUT pos: " + pos);
			selectedItems.put(pos, true);
		}
		notifyItemChanged(pos);

		VersionsFileAdapter.ViewHolderVersion view = (VersionsFileAdapter.ViewHolderVersion) listFragment.findViewHolderForLayoutPosition(pos);
		if(view!=null){
			logDebug("Start animation: " + pos);
			Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
			flipAnimation.setAnimationListener(new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					if (selectedItems.size() <= 0){
						((VersionsFileActivity) context).hideMultipleSelect();
					}
				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}
			});

			view.imageView.startAnimation(flipAnimation);

		}
		else{
			logWarning("View is null - not animation");
		}
	}

	public void selectAll(){
		for (int i= 0; i<this.getItemCount();i++){
			if(!isItemChecked(i)){
				toggleAllSelection(i);
			}
		}
	}

	public void clearSelections() {
		logDebug("clearSelections");
		for (int i= 0; i<this.getItemCount();i++){
			if(isItemChecked(i)){
				toggleAllSelection(i);
			}
		}
	}

//	public void clearSelections() {
//		if(selectedItems!=null){
//			selectedItems.clear();
//			for (int i= 0; i<this.getItemCount();i++) {
//				if (isItemChecked(i)) {
//					toggleAllSelection(i);
//				}
//			}
//		}
//		notifyDataSetChanged();
//	}
//
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
	public List<MegaNode> getSelectedNodes() {
		ArrayList<MegaNode> nodes = new ArrayList<MegaNode>();

		for (int i = 0; i < selectedItems.size(); i++) {
			if (selectedItems.valueAt(i) == true) {
				MegaNode document = getNodeAt(selectedItems.keyAt(i));
				if (document != null){
					nodes.add(document);
				}
			}
		}
		return nodes;
	}

	public List<MegaNode> getNodes () {
		return nodes;
	}

	public VersionsFileAdapter(Context _context, ArrayList<MegaNode> _nodes, RecyclerView recyclerView) {
		this.context = _context;
		this.nodes = _nodes;

		this.listFragment = recyclerView;

		if (megaApi == null) {
			megaApi = ((MegaApplication) ((Activity) context).getApplication())
					.getMegaApi();
		}
	}

	public void setNodes(ArrayList<MegaNode> nodes) {
		logDebug("setNodes");
		this.nodes = nodes;
//		contentTextFragment.setText(getInfoFolder(node));
		notifyDataSetChanged();
	}

	@Override
	public ViewHolderVersion onCreateViewHolder(ViewGroup parent, int viewType) {
		logDebug("onCreateViewHolder");
		Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics();
		display.getMetrics(outMetrics);
		
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_version_file, parent, false);

		ViewHolderVersion holderList = new ViewHolderVersion(v);
		holderList.itemLayout = (RelativeLayout) v.findViewById(R.id.version_file_item_layout);
		holderList.imageView = (ImageView) v.findViewById(R.id.version_file_thumbnail);

		holderList.textViewFileName = (TextView) v.findViewById(R.id.version_file_filename);

		holderList.textViewFileSize = (TextView) v.findViewById(R.id.version_file_filesize);

		holderList.threeDotsLayout = (RelativeLayout) v.findViewById(R.id.version_file_three_dots_layout);

		holderList.headerLayout = (RelativeLayout) v.findViewById(R.id.version_file_header_layout);
		holderList.titleHeader = (TextView) v.findViewById(R.id.version_file_header_title);
		holderList.sizeHeader = (TextView) v.findViewById(R.id.version_file_header_size);

		holderList.itemLayout.setTag(holderList);
		holderList.itemLayout.setOnClickListener(this);

		switch (((VersionsFileActivity) context).getAccessLevel()) {
			case MegaShare.ACCESS_FULL:
			case MegaShare.ACCESS_OWNER:
				holderList.itemLayout.setOnLongClickListener(this);
				break;

			default:
				holderList.itemLayout.setOnLongClickListener(null);
		}

		holderList.threeDotsLayout.setTag(holderList);
		holderList.threeDotsLayout.setOnClickListener(this);

		v.setTag(holderList);

		return holderList;
	}
	
	@Override
	public void onBindViewHolder(ViewHolderVersion holder, int position) {
		logDebug("Position: " + position);

		MegaNode node = (MegaNode) getItem(position);
		holder.document = node.getHandle();
		Bitmap thumb = null;

		if(position==0){
			holder.titleHeader.setText(context.getString(R.string.header_current_section_item));
			holder.sizeHeader.setVisibility(View.GONE);
			holder.headerLayout.setVisibility(View.VISIBLE);
		}
		else if(position==1){
			holder.titleHeader.setText(context.getResources().getQuantityString(R.plurals.header_previous_section_item, megaApi.getNumVersions(node)));

			if(((VersionsFileActivity)context).versionsSize!=null){
				holder.sizeHeader.setText(((VersionsFileActivity)context).versionsSize);
				holder.sizeHeader.setVisibility(View.VISIBLE);
			}
			else{
				holder.sizeHeader.setVisibility(View.GONE);
			}

			holder.headerLayout.setVisibility(View.VISIBLE);
		}
		else{
			holder.headerLayout.setVisibility(View.GONE);
		}
		
		holder.textViewFileName.setText(node.getName());
		holder.textViewFileSize.setText("");

		long nodeSize = node.getSize();
		String fileInfo = getSizeString(nodeSize) + " . " + getNodeDate(node);
		holder.textViewFileSize.setText(fileInfo);

		RelativeLayout.LayoutParams paramsLarge = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
		paramsLarge.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
		paramsLarge.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
		int leftLarge = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, context.getResources().getDisplayMetrics());
		paramsLarge.setMargins(leftLarge, 0, 0, 0);

		if (!multipleSelect) {
			logDebug("Not multiselect");
			holder.itemLayout.setBackgroundColor(Color.WHITE);
			holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
			holder.imageView.setLayoutParams(paramsLarge);

			logDebug("Check the thumb");

			if (node.hasThumbnail()) {
				logDebug("Node has thumbnail");

				thumb = getThumbnailFromCache(node);
				if (thumb != null) {
					RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
					params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
					params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
					int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, context.getResources().getDisplayMetrics());
					int right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
					params1.setMargins(left, 0, right, 0);

					holder.imageView.setLayoutParams(params1);
					holder.imageView.setImageBitmap(thumb);

				} else {
					thumb = getThumbnailFromFolder(node, context);
					if (thumb != null) {
						RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
						params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
						params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
						int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, context.getResources().getDisplayMetrics());
						int right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
						params1.setMargins(left, 0, right, 0);

						holder.imageView.setLayoutParams(params1);
						holder.imageView.setImageBitmap(thumb);

					} else {
						try {
							thumb = ThumbnailUtilsLollipop.getThumbnailFromMegaList(node, context, holder, megaApi, this);
						} catch (Exception e) {
						} // Too many AsyncTasks

						if (thumb != null) {
							RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
							params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
							params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
							int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, context.getResources().getDisplayMetrics());
							int right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
							params1.setMargins(left, 0, right, 0);

							holder.imageView.setLayoutParams(params1);
							holder.imageView.setImageBitmap(thumb);
						}
					}
				}
			} else {
				logDebug("Node NOT thumbnail");
				thumb = getThumbnailFromCache(node);
				if (thumb != null) {
					RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
					params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
					params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
					int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, context.getResources().getDisplayMetrics());
					int right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
					params1.setMargins(left, 0, right, 0);

					holder.imageView.setLayoutParams(params1);
					holder.imageView.setImageBitmap(thumb);


				} else {
					thumb = getThumbnailFromFolder(node, context);
					if (thumb != null) {
						RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
						params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
						params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
						int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, context.getResources().getDisplayMetrics());
						int right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
						params1.setMargins(left, 0, right, 0);

						holder.imageView.setLayoutParams(params1);
						holder.imageView.setImageBitmap(thumb);

					} else {
						holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
						holder.imageView.setLayoutParams(paramsLarge);

						try {
							ThumbnailUtilsLollipop.createThumbnailList(context, node,holder, megaApi, this);
						} catch (Exception e) {
						} // Too many AsyncTasks
					}
				}
			}
		}
		else {
			logDebug("Multiselection ON");
			if(this.isItemChecked(position)){
				holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.new_multiselect_color));

				holder.imageView.setLayoutParams(paramsLarge);
				holder.imageView.setImageResource(R.drawable.ic_select_folder);
			}
			else{
				holder.itemLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.white));

				logDebug("Check the thumb");
				holder.imageView.setLayoutParams(paramsLarge);

				if (node.hasThumbnail()) {
					logDebug("Node has thumbnail");

					thumb = getThumbnailFromCache(node);
					if (thumb != null) {
						RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
						params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
						params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
						int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, context.getResources().getDisplayMetrics());
						int right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
						params1.setMargins(left, 0, right, 0);

						holder.imageView.setLayoutParams(params1);
						holder.imageView.setImageBitmap(thumb);

					} else {
						thumb = getThumbnailFromFolder(node, context);
						if (thumb != null) {
							RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
							params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
							params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
							int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, context.getResources().getDisplayMetrics());
							int right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
							params1.setMargins(left, 0, right, 0);

							holder.imageView.setLayoutParams(params1);
							holder.imageView.setImageBitmap(thumb);

						} else {
							try {
								thumb = ThumbnailUtilsLollipop.getThumbnailFromMegaList(node, context, holder, megaApi, this);
							} catch (Exception e) {
							} // Too many AsyncTasks

							if (thumb != null) {
								RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
								params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
								params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
								int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, context.getResources().getDisplayMetrics());
								int right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
								params1.setMargins(left, 0, right, 0);

								holder.imageView.setLayoutParams(params1);
								holder.imageView.setImageBitmap(thumb);
							}
						}
					}
				} else {
					logDebug("Node NOT thumbnail");

					thumb = getThumbnailFromCache(node);
					if (thumb != null) {
						RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
						params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
						params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
						int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, context.getResources().getDisplayMetrics());
						int right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
						params1.setMargins(left, 0, right, 0);

						holder.imageView.setLayoutParams(params1);
						holder.imageView.setImageBitmap(thumb);

					} else {
						thumb = getThumbnailFromFolder(node, context);
						if (thumb != null) {
							RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
							params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
							params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
							int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, context.getResources().getDisplayMetrics());
							int right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
							params1.setMargins(left, 0, right, 0);

							holder.imageView.setLayoutParams(params1);
							holder.imageView.setImageBitmap(thumb);

						} else {
							logDebug("NOT thumbnail");
							holder.imageView.setLayoutParams(paramsLarge);
							holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

							if (MimeTypeList.typeForName(node.getName()).isImage()) {
								try {
									ThumbnailUtilsLollipop.createThumbnailList(context, node, holder, megaApi, this);
								} catch (Exception e) {
								}
							}
						}
					}
				}
			}
		}
	}

	private String getItemNode(int position) {
		return nodes.get(position).getName();
	}

	@Override
	public int getItemCount() {
		if (nodes != null){
			return nodes.size();
		}else{
			return 0;
		}
	}

	public Object getItem(int position) {
		if (nodes != null){
			return nodes.get(position);
		}
		
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public void onClick(View v) {
		logDebug("onClick");

		ViewHolderVersion holder = (ViewHolderVersion) v.getTag();
		int currentPosition = holder.getAdapterPosition();
		logDebug("Current position: " + currentPosition);

		if(currentPosition<0){
			logError("Current position error - not valid value");
			return;
		}

		final MegaNode n = (MegaNode) getItem(currentPosition);

		switch (v.getId()) {
			case R.id.version_file_three_dots_layout:{

				logDebug("version_file_three_dots: " + currentPosition);
				if(!isOnline(context)){
					((VersionsFileActivity) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
					return;
				}

				if(multipleSelect){
					((VersionsFileActivity) context).itemClick(currentPosition);
				}
				else{
					((VersionsFileActivity) context).showOptionsPanel(n, currentPosition);

				}

				break;
			}
			case R.id.version_file_item_layout:{

				((VersionsFileActivity) context).itemClick(currentPosition);

				break;
			}
		}
	}

	@Override
	public boolean onLongClick(View view) {
		ViewHolderVersion holder = (ViewHolderVersion) view.getTag();
		int currentPosition = holder.getAdapterPosition();

		if (!isMultipleSelect()) {
			if (currentPosition < 0) {
				logWarning("Position not valid: " + currentPosition);
			} else {
				setMultipleSelect(true);
				((VersionsFileActivity) context).startActionMode(currentPosition);
			}
		}

		return true;
	}

	/*
	 * Get document at specified position
	 */
	public MegaNode getNodeAt(int position) {
		try {
			if (nodes != null) {
				return nodes.get(position);
			}
		} catch (IndexOutOfBoundsException e) {
		}
		return null;
	}

	public String getNodeDate(MegaNode node){

		Calendar calendar = calculateDateFromTimestamp(node.getModificationTime());
		String format3 = new SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault()).format(calendar.getTime());
		return format3;
	}

	public long getParentHandle() {
		return parentHandle;
	}

	public void setParentHandle(long parentHandle) {
		this.parentHandle = parentHandle;
	}

	public boolean isMultipleSelect() {
		return multipleSelect;
	}

	public void setMultipleSelect(boolean multipleSelect) {
		logDebug("multipleSelect: " + multipleSelect);
		if (this.multipleSelect != multipleSelect) {
			this.multipleSelect = multipleSelect;
		}
		if(this.multipleSelect){
			selectedItems = new SparseBooleanArray();
		}
	}
}