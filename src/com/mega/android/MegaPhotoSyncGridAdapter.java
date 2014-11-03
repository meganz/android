package com.mega.android;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mega.android.CameraUploadFragment.PhotoSyncGridHolder;
import com.mega.android.utils.ThumbnailUtils;
import com.mega.android.utils.Util;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaNode;

public class MegaPhotoSyncGridAdapter extends BaseAdapter implements OnClickListener {
	
	Context context;
	
	ArrayList<MegaNode> nodes;
	ArrayList<PhotoSyncGridHolder> nodesArrayGrid;
	int positionClicked;
	
	MegaApiAndroid megaApi;
	
	long photoSyncHandle = -1;
	
	ListView listFragment;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	ActionBar aB;
	
	int orderGetChildren = MegaApiJava.ORDER_MODIFICATION_DESC;
	
	public MegaPhotoSyncGridAdapter(Context _context, ArrayList<PhotoSyncGridHolder> _nodesArrayGrid, long _photosyncHandle, ListView listView, ImageView emptyImageView, TextView emptyTextView, ActionBar aB, ArrayList<MegaNode> _nodes) {
		this.context = _context;
		this.nodesArrayGrid = _nodesArrayGrid;
		this.photoSyncHandle = _photosyncHandle;
		this.nodes = _nodes;
		
		this.listFragment = listView;
		this.emptyImageViewFragment = emptyImageView;
		this.emptyTextViewFragment = emptyTextView;
		this.aB = aB;
		
		this.positionClicked = -1;
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
	}
	
	public void setNodes(ArrayList<PhotoSyncGridHolder> nodesArrayGrid, ArrayList<MegaNode> nodes){
		this.nodesArrayGrid = nodesArrayGrid;
		this.nodes = nodes;
		positionClicked = -1;	
		notifyDataSetChanged();
//		listFragment.clearFocus();
//		if (listFragment != null){
//			listFragment.post(new Runnable() {
//                @Override
//                public void run() {                	
//                    listFragment.setSelection(0);
//                }
//            });
//		}
//		list.smoothScrollToPosition(0);
	}
	
	/*private view holder class*/
    public class ViewHolderPhotoSyncGrid {
    	public RelativeLayout itemLayout;
    	public ImageButton imageView1;
    	public ImageButton imageView2;
    	public ImageButton imageView3;
        
    	public RelativeLayout monthLayout;
    	public TextView monthTextView;

    	public int currentPosition;
    	public long document1;
    	public long document2;
    	public long document3;
    }
    
    ViewHolderPhotoSyncGrid holder = null;
	int positionG;

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View v;
		
		listFragment = (ListView) parent;
		final int _position = position;
		positionG = position;
		
		Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		DisplayMetrics outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    float density  = ((Activity)context).getResources().getDisplayMetrics().density;
		
	    float scaleW = Util.getScaleW(outMetrics, density);
	    float scaleH = Util.getScaleH(outMetrics, density);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		v = inflater.inflate(R.layout.item_photo_sync_grid, parent, false);
		holder = new ViewHolderPhotoSyncGrid();
		holder.itemLayout = (RelativeLayout) v.findViewById(R.id.photo_sync_grid_item_layout);
		holder.imageView1 = (ImageButton) v.findViewById(R.id.photo_sync_grid_thumbnail1);
		holder.imageView2 = (ImageButton) v.findViewById(R.id.photo_sync_grid_thumbnail2);
		holder.imageView3 = (ImageButton) v.findViewById(R.id.photo_sync_grid_thumbnail3);
		
		RelativeLayout.LayoutParams paramsIL = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		paramsIL.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		paramsIL.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		paramsIL.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), Util.px2dp(5*scaleW, outMetrics), 0);
		holder.itemLayout.setLayoutParams(paramsIL);
		
		RelativeLayout.LayoutParams paramsIV1 = new RelativeLayout.LayoutParams(Util.px2dp(110*scaleW, outMetrics),Util.px2dp(110*scaleH, outMetrics));
		holder.imageView1.setScaleType(ImageView.ScaleType.FIT_CENTER);
		paramsIV1.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), 0, 0);
		paramsIV1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		holder.imageView1.setLayoutParams(paramsIV1);
		
		RelativeLayout.LayoutParams paramsIV2 = new RelativeLayout.LayoutParams(Util.px2dp(110*scaleW, outMetrics),Util.px2dp(110*scaleH, outMetrics));
		holder.imageView2.setScaleType(ImageView.ScaleType.FIT_CENTER);
		paramsIV2.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), 0, 0);
		paramsIV2.addRule(RelativeLayout.CENTER_HORIZONTAL);
		holder.imageView2.setLayoutParams(paramsIV2);
		
		RelativeLayout.LayoutParams paramsIV3 = new RelativeLayout.LayoutParams(Util.px2dp(110*scaleW, outMetrics),Util.px2dp(110*scaleH, outMetrics));
		holder.imageView3.setScaleType(ImageView.ScaleType.FIT_CENTER);
		paramsIV3.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), 0, 0);
		paramsIV3.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		holder.imageView3.setLayoutParams(paramsIV3);
		
		holder.imageView1.setTag(holder);
		holder.imageView1.setOnClickListener(this);
		
		holder.imageView2.setTag(holder);
		holder.imageView2.setOnClickListener(this);
		
		holder.imageView3.setTag(holder);
		holder.imageView3.setOnClickListener(this);
		
		
		
		holder.monthLayout = (RelativeLayout) v.findViewById(R.id.photo_sync_grid_month_layout);
		holder.monthTextView = (TextView) v.findViewById(R.id.photo_sync_grid_month_name);
			
			//Set width and height itemLayout1
//			RelativeLayout.LayoutParams paramsIL1 = new RelativeLayout.LayoutParams(Util.px2dp(172*scaleW, outMetrics),LayoutParams.WRAP_CONTENT);
//			paramsIL1.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), Util.px2dp(5*scaleW, outMetrics), 0);
//			paramsIL1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
//			paramsIL1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
//			holder.itemLayout1.setLayoutParams(paramsIL1);
			
//			RelativeLayout.LayoutParams paramsIV1 = new RelativeLayout.LayoutParams(Util.px2dp(157*scaleW, outMetrics),Util.px2dp(157*scaleH, outMetrics));
//			paramsIV1.addRule(RelativeLayout.CENTER_HORIZONTAL);
//			holder.imageView1.setScaleType(ImageView.ScaleType.FIT_CENTER);
//			paramsIV1.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), Util.px2dp(5*scaleW, outMetrics), 0);
//			holder.imageView1.setLayoutParams(paramsIV1);
//			
//			RelativeLayout.LayoutParams paramsIV2 = new RelativeLayout.LayoutParams(Util.px2dp(157*scaleW, outMetrics),Util.px2dp(157*scaleH, outMetrics));
//			paramsIV2.addRule(RelativeLayout.CENTER_HORIZONTAL);
//			holder.imageView2.setScaleType(ImageView.ScaleType.FIT_CENTER);
//			paramsIV2.setMargins(0, Util.px2dp(5*scaleH, outMetrics), 0, 0);
//			holder.imageView2.setLayoutParams(paramsIV2);

		holder.currentPosition = position;
		
		PhotoSyncGridHolder psGH = (PhotoSyncGridHolder) getItem(position);
		
		if (!psGH.isNode){
			holder.itemLayout.setVisibility(View.GONE);
			holder.monthLayout.setVisibility(View.VISIBLE);
			holder.monthTextView.setText(psGH.monthYear);
		}
		else{
			holder.itemLayout.setVisibility(View.VISIBLE);
			holder.monthLayout.setVisibility(View.GONE);
			
			holder.document1 = psGH.handle1;
			holder.document2 = psGH.handle2;
			holder.document3 = psGH.handle3;
			
			MegaNode node1 = megaApi.getNodeByHandle(psGH.handle1);
			if (node1 != null){					
				Bitmap thumb1 = null;
				if (!node1.isFolder()){
					holder.imageView1.setImageResource(MimeType.typeForName(node1.getName()).getIconResourceId());
					if (node1.hasThumbnail()){
						thumb1 = ThumbnailUtils.getThumbnailFromCache(node1);
						if (thumb1 != null){
							holder.imageView1.setImageBitmap(thumb1);
						}
						else{
							thumb1 = ThumbnailUtils.getThumbnailFromFolder(node1, context);
							if (thumb1 != null){
								holder.imageView1.setImageBitmap(thumb1);
							}
							else{ 
								try{
									thumb1 = ThumbnailUtils.getThumbnailFromMegaPhotoSyncGrid(node1, context, holder, megaApi, this, 1);
								}
								catch(Exception e){} //Too many AsyncTasks
								
								if (thumb1 != null){
									holder.imageView1.setImageBitmap(thumb1);
								}
								else{
									holder.imageView1.setImageResource(MimeType.typeForName(node1.getName()).getIconResourceId());
								}
							}
						}
					}
				}
			}
			else{
				holder.imageView1.setImageResource(android.R.color.transparent);
			}
			
			MegaNode node2 = megaApi.getNodeByHandle(psGH.handle2);
			if (node2 != null){
				Bitmap thumb2 = null;
				if (!node2.isFolder()){
					holder.imageView2.setImageResource(MimeType.typeForName(node2.getName()).getIconResourceId());
					if (node2.hasThumbnail()){
						thumb2 = ThumbnailUtils.getThumbnailFromCache(node2);
						if (thumb2 != null){
							holder.imageView2.setImageBitmap(thumb2);
						}
						else{
							thumb2 = ThumbnailUtils.getThumbnailFromFolder(node2, context);
							if (thumb2 != null){
								holder.imageView2.setImageBitmap(thumb2);
							}
							else{ 
								try{
									thumb2 = ThumbnailUtils.getThumbnailFromMegaPhotoSyncGrid(node2, context, holder, megaApi, this, 2);
								}
								catch(Exception e){} //Too many AsyncTasks
								
								if (thumb2 != null){
									holder.imageView2.setImageBitmap(thumb2);
								}
								else{
									holder.imageView2.setImageResource(MimeType.typeForName(node2.getName()).getIconResourceId());
								}
							}
						}
					}
				}
			}
			else{
				holder.imageView2.setImageResource(android.R.color.transparent);
			}
			
			MegaNode node3 = megaApi.getNodeByHandle(psGH.handle3);
			if (node3 != null){
				Bitmap thumb3 = null;
				if (!node3.isFolder()){
					holder.imageView3.setImageResource(MimeType.typeForName(node3.getName()).getIconResourceId());
					if (node3.hasThumbnail()){
						thumb3 = ThumbnailUtils.getThumbnailFromCache(node3);
						if (thumb3 != null){
							holder.imageView3.setImageBitmap(thumb3);
						}
						else{
							thumb3 = ThumbnailUtils.getThumbnailFromFolder(node3, context);
							if (thumb3 != null){
								holder.imageView3.setImageBitmap(thumb3);
							}
							else{ 
								try{
									thumb3 = ThumbnailUtils.getThumbnailFromMegaPhotoSyncGrid(node3, context, holder, megaApi, this, 3);
								}
								catch(Exception e){} //Too many AsyncTasks
								
								if (thumb3 != null){
									holder.imageView3.setImageBitmap(thumb3);
								}
								else{
									holder.imageView3.setImageResource(MimeType.typeForName(node3.getName()).getIconResourceId());
								}
							}
						}
					}
				}
			}
			else{
				holder.imageView3.setImageResource(android.R.color.transparent);
			}
		}
		
		return v;

	}
	
	private String getInfoFolder (MegaNode n){
		ArrayList<MegaNode> nL = megaApi.getChildren(n);
		
		int numFolders = 0;
		int numFiles = 0;
		
		for (int i=0;i<nL.size();i++){
			MegaNode c = nL.get(i);
			if (c.isFolder()){
				numFolders++;
			}
			else{
				numFiles++;
			}
		}
		
		String info = "";
		if (numFolders > 0){
			info = numFolders +  " " + context.getResources().getQuantityString(R.plurals.general_num_folders, numFolders);
			if (numFiles > 0){
				info = info + ", " + numFiles + " " + context.getResources().getQuantityString(R.plurals.general_num_files, numFiles);
			}
		}
		else {
			info = numFiles +  " " + context.getResources().getQuantityString(R.plurals.general_num_files, numFiles);
		}
		
		return info;
	}

	@Override
    public int getCount() {
        return nodesArrayGrid.size();
    }
 
    @Override
    public Object getItem(int position) {
        return nodesArrayGrid.get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }    
    
    public int getPositionClicked (){
    	return positionClicked;
    }
    
    public void setPositionClicked(int p){
    	positionClicked = p;
    }

	@Override
	public void onClick(View v) {

		ViewHolderPhotoSyncGrid holder = (ViewHolderPhotoSyncGrid) v.getTag();
		int currentPosition = holder.currentPosition;
		
		switch (v.getId()){
			case R.id.photo_sync_grid_thumbnail1:{
				
				MegaNode n = megaApi.getNodeByHandle(holder.document1);
				if (n != null){
					if (!n.isFolder()){
						if (MimeType.typeForName(n.getName()).isImage()){
							int positionInNodes = 0;
							for (int i=0;i<nodes.size();i++){
								if(nodes.get(i).getHandle() == n.getHandle()){
									positionInNodes = i;
								}
							}
							
							Intent intent = new Intent(context, FullScreenImageViewer.class);
							intent.putExtra("position", positionInNodes);
							intent.putExtra("parentNodeHandle", megaApi.getParentNode(n).getHandle());
							intent.putExtra("adapterType", ManagerActivity.PHOTO_SYNC_ADAPTER);
							intent.putExtra("orderGetChildren", orderGetChildren);
							context.startActivity(intent);
						}
						else if (MimeType.typeForName(n.getName()).isVideo() || MimeType.typeForName(n.getName()).isAudio() ){
							MegaNode file = n;
							Intent service = new Intent(context, MegaStreamingService.class);
					  		context.startService(service);
					  		String fileName = file.getName();
							try {
								fileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
							} 
							catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
							
					  		String url = "http://127.0.0.1:4443/" + file.getBase64Handle() + "/" + fileName;
					  		String mimeType = MimeType.typeForName(file.getName()).getType();
					  		System.out.println("FILENAME: " + fileName);
					  		
					  		Intent mediaIntent = new Intent(Intent.ACTION_VIEW);
					  		mediaIntent.setDataAndType(Uri.parse(url), mimeType);
					  		try
					  		{
					  			context.startActivity(mediaIntent);
					  		}
					  		catch(Exception e)
					  		{
					  			Toast.makeText(context, "NOOOOOOOO", Toast.LENGTH_LONG).show();
					  		}						
						}
						else{
							ArrayList<Long> handleList = new ArrayList<Long>();
							handleList.add(n.getHandle());
							((ManagerActivity) context).onFileClick(handleList);
						}	
						positionClicked = -1;
						notifyDataSetChanged();
					}
				}
				break;
			}
			case R.id.photo_sync_grid_thumbnail2:{
				MegaNode n = megaApi.getNodeByHandle(holder.document2);
				if (n != null){
					if (!n.isFolder()){
						if (MimeType.typeForName(n.getName()).isImage()){
							int positionInNodes = 0;
							for (int i=0;i<nodes.size();i++){
								if(nodes.get(i).getHandle() == n.getHandle()){
									positionInNodes = i;
								}
							}
							
							Intent intent = new Intent(context, FullScreenImageViewer.class);
							intent.putExtra("position", positionInNodes);
							intent.putExtra("parentNodeHandle", megaApi.getParentNode(n).getHandle());
							intent.putExtra("adapterType", ManagerActivity.PHOTO_SYNC_ADAPTER);
							intent.putExtra("orderGetChildren", orderGetChildren);
							context.startActivity(intent);
						}
						else if (MimeType.typeForName(n.getName()).isVideo() || MimeType.typeForName(n.getName()).isAudio() ){
							MegaNode file = n;
							Intent service = new Intent(context, MegaStreamingService.class);
					  		context.startService(service);
					  		String fileName = file.getName();
							try {
								fileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
							} 
							catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
							
					  		String url = "http://127.0.0.1:4443/" + file.getBase64Handle() + "/" + fileName;
					  		String mimeType = MimeType.typeForName(file.getName()).getType();
					  		System.out.println("FILENAME: " + fileName);
					  		
					  		Intent mediaIntent = new Intent(Intent.ACTION_VIEW);
					  		mediaIntent.setDataAndType(Uri.parse(url), mimeType);
					  		try
					  		{
					  			context.startActivity(mediaIntent);
					  		}
					  		catch(Exception e)
					  		{
					  			Toast.makeText(context, "NOOOOOOOO", Toast.LENGTH_LONG).show();
					  		}						
						}
						else{
							ArrayList<Long> handleList = new ArrayList<Long>();
							handleList.add(n.getHandle());
							((ManagerActivity) context).onFileClick(handleList);
						}	
						positionClicked = -1;
						notifyDataSetChanged();
					}
				}
				break;
			}
			case R.id.photo_sync_grid_thumbnail3:{
				MegaNode n = megaApi.getNodeByHandle(holder.document3);
				if (n != null){
					if (!n.isFolder()){
						if (MimeType.typeForName(n.getName()).isImage()){
							int positionInNodes = 0;
							for (int i=0;i<nodes.size();i++){
								if(nodes.get(i).getHandle() == n.getHandle()){
									positionInNodes = i;
								}
							}
							
							Intent intent = new Intent(context, FullScreenImageViewer.class);
							intent.putExtra("position", positionInNodes);
							intent.putExtra("parentNodeHandle", megaApi.getParentNode(n).getHandle());
							intent.putExtra("adapterType", ManagerActivity.PHOTO_SYNC_ADAPTER);
							intent.putExtra("orderGetChildren", orderGetChildren);
							context.startActivity(intent);
						}
						else if (MimeType.typeForName(n.getName()).isVideo() || MimeType.typeForName(n.getName()).isAudio() ){
							MegaNode file = n;
							Intent service = new Intent(context, MegaStreamingService.class);
					  		context.startService(service);
					  		String fileName = file.getName();
							try {
								fileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
							} 
							catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
							
					  		String url = "http://127.0.0.1:4443/" + file.getBase64Handle() + "/" + fileName;
					  		String mimeType = MimeType.typeForName(file.getName()).getType();
					  		System.out.println("FILENAME: " + fileName);
					  		
					  		Intent mediaIntent = new Intent(Intent.ACTION_VIEW);
					  		mediaIntent.setDataAndType(Uri.parse(url), mimeType);
					  		try
					  		{
					  			context.startActivity(mediaIntent);
					  		}
					  		catch(Exception e)
					  		{
					  			Toast.makeText(context, "NOOOOOOOO", Toast.LENGTH_LONG).show();
					  		}						
						}
						else{
							ArrayList<Long> handleList = new ArrayList<Long>();
							handleList.add(n.getHandle());
							((ManagerActivity) context).onFileClick(handleList);
						}	
						positionClicked = -1;
						notifyDataSetChanged();
					}
				}
				break;
			}
		}
	}
	
	public long getPhotoSyncHandle(){
		return photoSyncHandle;
	}
	
	public void setOrder(int orderGetChildren){
		this.orderGetChildren = orderGetChildren;
	}
	
	private static void log(String log) {
		Util.log("MegaBrowserGridAdapter", log);
	}
}
