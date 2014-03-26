package com.mega.android;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaNode;
import com.mega.sdk.NodeList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class MegaBrowserGridAdapter extends BaseAdapter implements OnClickListener {
	
	Context context;
	ArrayList<Integer> imageIds;
	ArrayList<String> names;
	
	NodeList nodes;
	int positionClicked;
	
	MegaApiAndroid megaApi;
	
	long parentHandle;
	
	ListView listFragment;
	ImageView emptyImageViewFragment;
	TextView emptyTextViewFragment;
	ActionBar aB;
	
	public MegaBrowserGridAdapter(Context _context, NodeList _nodes, long _parentHandle, ListView listView, ImageView emptyImageView, TextView emptyTextView, ActionBar aB) {
		this.context = _context;
		this.nodes = _nodes;
		this.parentHandle = _parentHandle;
		this.listFragment = listView;
		this.emptyImageViewFragment = emptyImageView;
		this.emptyTextViewFragment = emptyTextView;
		this.aB = aB;
		
		this.positionClicked = -1;
		this.imageIds = new ArrayList<Integer>();
		this.names = new ArrayList<String>();
		
		if (megaApi == null){
			megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
		}
	}
	
	public void setNodes(NodeList nodes){
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
    public class ViewHolderBrowserGrid {
        ImageButton imageView1;
        TextView textViewFileName1;
        RelativeLayout itemLayout1;
        ImageButton imageView2;
        TextView textViewFileName2;
        RelativeLayout itemLayout2;
        TextView textViewFileSize1;
        TextView textViewFileSize2;
        ImageButton imageButtonThreeDots1;
        ImageButton imageButtonThreeDots2;
        ImageView arrowSelection1;
        RelativeLayout optionsLayout1;
//        ImageButton optionOpen1;
        ImageView optionProperties1;
        ImageView optionDownload1;
        ImageView optionRename1;
        ImageView optionCopy1;
        ImageView optionMove1;
        ImageView optionPublicLink1;
        ImageView optionDelete1;
        ImageView arrowSelection2;
        RelativeLayout optionsLayout2;
//        ImageButton optionOpen2;
        ImageView optionProperties2;
        ImageView optionDownload2;
        ImageView optionRename2;
        ImageView optionCopy2;
        ImageView optionMove2;
        ImageView optionPublicLink2;
        ImageView optionDelete2;
        int currentPosition;
        long document1;
        long document2;
    }
    
    ViewHolderBrowserGrid holder = null;
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
		if ((position % 2) == 0){
			v = inflater.inflate(R.layout.item_file_grid, parent, false);
			holder = new ViewHolderBrowserGrid();
			holder.itemLayout1 = (RelativeLayout) v.findViewById(R.id.file_grid_item_layout1);
			holder.itemLayout2 = (RelativeLayout) v.findViewById(R.id.file_grid_item_layout2);
			
			//Set width and height itemLayout1
			RelativeLayout.LayoutParams paramsIL1 = new RelativeLayout.LayoutParams(Util.px2dp(172*scaleW, outMetrics),LayoutParams.WRAP_CONTENT);
			paramsIL1.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), Util.px2dp(5*scaleW, outMetrics), 0);
			paramsIL1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			paramsIL1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			holder.itemLayout1.setLayoutParams(paramsIL1);
			
			//Set width and height itemLayout2
			RelativeLayout.LayoutParams paramsIL2 = new RelativeLayout.LayoutParams(Util.px2dp(172*scaleW, outMetrics),LayoutParams.WRAP_CONTENT);
			paramsIL2.setMargins(0, Util.px2dp(5*scaleH, outMetrics), 0, 0);
			paramsIL2.addRule(RelativeLayout.RIGHT_OF, R.id.file_grid_item_layout1);
			paramsIL2.addRule(RelativeLayout.LEFT_OF, R.id.file_grid_separator_final);
			paramsIL2.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			TranslateAnimation anim = new TranslateAnimation(Util.px2dp(-5*scaleW, outMetrics), Util.px2dp(-5*scaleW, outMetrics), 0, 0);
	        anim.setDuration(0);
	        
	        holder.itemLayout2.startAnimation(anim);
			holder.itemLayout2.setLayoutParams(paramsIL2);
			
			holder.imageView1 = (ImageButton) v.findViewById(R.id.file_grid_thumbnail1);
            holder.imageView2 = (ImageButton) v.findViewById(R.id.file_grid_thumbnail2);
            
            
			RelativeLayout.LayoutParams paramsIV1 = new RelativeLayout.LayoutParams(Util.px2dp(157*scaleW, outMetrics),Util.px2dp(157*scaleH, outMetrics));
			paramsIV1.addRule(RelativeLayout.CENTER_HORIZONTAL);
			holder.imageView1.setScaleType(ImageView.ScaleType.FIT_CENTER);
			paramsIV1.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), Util.px2dp(5*scaleW, outMetrics), 0);
			holder.imageView1.setLayoutParams(paramsIV1);
			
			RelativeLayout.LayoutParams paramsIV2 = new RelativeLayout.LayoutParams(Util.px2dp(157*scaleW, outMetrics),Util.px2dp(157*scaleH, outMetrics));
			paramsIV2.addRule(RelativeLayout.CENTER_HORIZONTAL);
			holder.imageView2.setScaleType(ImageView.ScaleType.FIT_CENTER);
			paramsIV2.setMargins(0, Util.px2dp(5*scaleH, outMetrics), 0, 0);
			holder.imageView2.setLayoutParams(paramsIV2);

			holder.textViewFileName1 = (TextView) v.findViewById(R.id.file_grid_filename1);
			holder.textViewFileName1.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			holder.textViewFileName1.getLayoutParams().width = Util.px2dp((125*scaleW), outMetrics);
			holder.textViewFileName1.setEllipsize(TextUtils.TruncateAt.MIDDLE);
			holder.textViewFileName1.setSingleLine(true);
			holder.textViewFileName2 = (TextView) v.findViewById(R.id.file_grid_filename2);
			holder.textViewFileName2.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
			holder.textViewFileName2.getLayoutParams().width = Util.px2dp((125*scaleW), outMetrics);
			holder.textViewFileName2.setEllipsize(TextUtils.TruncateAt.MIDDLE);
			holder.textViewFileName2.setSingleLine(true);
			
			holder.textViewFileSize1 = (TextView) v.findViewById(R.id.file_grid_filesize1);
			holder.textViewFileSize2 = (TextView) v.findViewById(R.id.file_grid_filesize2);
			
			holder.imageButtonThreeDots1 = (ImageButton) v.findViewById(R.id.file_grid_three_dots1);
			holder.imageButtonThreeDots2 = (ImageButton) v.findViewById(R.id.file_grid_three_dots2);
			
			holder.optionsLayout1 = (RelativeLayout) v.findViewById(R.id.file_grid_options1);
//			holder.optionOpen1 = (ImageButton) v.findViewById(R.id.file_grid_option_open1);
//			holder.optionOpen1.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
			holder.optionDownload1 = (ImageView) v.findViewById(R.id.file_grid_option_download1);
			holder.optionDownload1.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionDownload1.getLayoutParams()).setMargins(Util.px2dp((9*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionProperties1 = (ImageView) v.findViewById(R.id.file_grid_option_properties1);
			holder.optionProperties1.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionProperties1.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionRename1 = (ImageView) v.findViewById(R.id.file_grid_option_rename1);
			holder.optionRename1.getLayoutParams().width = Util.px2dp((30*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionRename1.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionCopy1 = (ImageView) v.findViewById(R.id.file_grid_option_copy1);
			holder.optionCopy1.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionCopy1.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionMove1 = (ImageView) v.findViewById(R.id.file_grid_option_move1);
			holder.optionMove1.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionMove1.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionPublicLink1 = (ImageView) v.findViewById(R.id.file_grid_option_public_link1);
			holder.optionPublicLink1.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionPublicLink1.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionDelete1 = (ImageView) v.findViewById(R.id.file_grid_option_delete1);
			holder.optionDelete1.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionDelete1.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.arrowSelection1 = (ImageView) v.findViewById(R.id.file_grid_arrow_selection1);
			holder.arrowSelection1.setVisibility(View.GONE);

			holder.optionsLayout2 = (RelativeLayout) v.findViewById(R.id.file_grid_options2);
//			holder.optionOpen2 = (ImageButton) v.findViewById(R.id.file_grid_option_open2);
//			holder.optionOpen2.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
			holder.optionDownload2 = (ImageView) v.findViewById(R.id.file_grid_option_download2);
			holder.optionDownload2.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionDownload2.getLayoutParams()).setMargins(Util.px2dp((9*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionProperties2 = (ImageView) v.findViewById(R.id.file_grid_option_properties2);
			holder.optionProperties2.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionProperties2.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionRename2 = (ImageView) v.findViewById(R.id.file_grid_option_rename2);
			holder.optionRename2.getLayoutParams().width = Util.px2dp((30*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionRename2.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionCopy2 = (ImageView) v.findViewById(R.id.file_grid_option_copy2);
			holder.optionCopy2.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionCopy2.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionMove2 = (ImageView) v.findViewById(R.id.file_grid_option_move2);
			holder.optionMove2.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionMove2.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionPublicLink2 = (ImageView) v.findViewById(R.id.file_grid_option_public_link2);
			holder.optionPublicLink2.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionPublicLink2.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.optionDelete2 = (ImageView) v.findViewById(R.id.file_grid_option_delete2);
			holder.optionDelete2.getLayoutParams().width = Util.px2dp((35*scaleW), outMetrics);
			((TableRow.LayoutParams) holder.optionDelete2.getLayoutParams()).setMargins(Util.px2dp((17*scaleH), outMetrics), Util.px2dp((4*scaleH), outMetrics), 0, 0);
			holder.arrowSelection2 = (ImageView) v.findViewById(R.id.file_grid_arrow_selection2);
			holder.arrowSelection2.setVisibility(View.GONE);
		
//			if (convertView == null) {
//				convertView = inflater.inflate(R.layout.item_file_grid, parent, false);
//				holder = new ViewHolderBrowserGrid();
//				holder.itemLayout1 = (RelativeLayout) convertView.findViewById(R.id.file_grid_item_layout1);
//				holder.itemLayout2 = (RelativeLayout) convertView.findViewById(R.id.file_grid_item_layout2);
//				
//				//Set width and height itemLayout1
//				RelativeLayout.LayoutParams paramsIL1 = new RelativeLayout.LayoutParams(Util.px2dp(172*scaleW, outMetrics),LayoutParams.WRAP_CONTENT);
//				paramsIL1.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), Util.px2dp(5*scaleW, outMetrics), 0);
//				paramsIL1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
//				paramsIL1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
//				holder.itemLayout1.setLayoutParams(paramsIL1);
//				
//				//Set width and height itemLayout2
//				RelativeLayout.LayoutParams paramsIL2 = new RelativeLayout.LayoutParams(Util.px2dp(172*scaleW, outMetrics),LayoutParams.WRAP_CONTENT);
//				paramsIL2.setMargins(0, Util.px2dp(5*scaleH, outMetrics), 0, 0);
//				paramsIL2.addRule(RelativeLayout.RIGHT_OF, R.id.file_grid_item_layout1);
//				paramsIL2.addRule(RelativeLayout.LEFT_OF, R.id.file_grid_separator_final);
//				paramsIL2.addRule(RelativeLayout.ALIGN_PARENT_TOP);
//				TranslateAnimation anim = new TranslateAnimation(Util.px2dp(-5*scaleW, outMetrics), Util.px2dp(-5*scaleW, outMetrics), 0, 0);
//		        anim.setDuration(0);
//		        
//		        holder.itemLayout2.startAnimation(anim);
//				holder.itemLayout2.setLayoutParams(paramsIL2);
//				
//				holder.imageView1 = (ImageButton) convertView.findViewById(R.id.file_grid_thumbnail1);
//	            holder.imageView2 = (ImageButton) convertView.findViewById(R.id.file_grid_thumbnail2);
//	            
//	            
//				RelativeLayout.LayoutParams paramsIV1 = new RelativeLayout.LayoutParams(Util.px2dp(157*scaleW, outMetrics),Util.px2dp(157*scaleH, outMetrics));
//				paramsIV1.addRule(RelativeLayout.CENTER_HORIZONTAL);
//				holder.imageView1.setScaleType(ImageView.ScaleType.FIT_CENTER);
//				paramsIV1.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), Util.px2dp(5*scaleW, outMetrics), 0);
//				holder.imageView1.setLayoutParams(paramsIV1);
//				
//				RelativeLayout.LayoutParams paramsIV2 = new RelativeLayout.LayoutParams(Util.px2dp(157*scaleW, outMetrics),Util.px2dp(157*scaleH, outMetrics));
//				paramsIV2.addRule(RelativeLayout.CENTER_HORIZONTAL);
//				holder.imageView2.setScaleType(ImageView.ScaleType.FIT_CENTER);
//				paramsIV2.setMargins(0, Util.px2dp(5*scaleH, outMetrics), 0, 0);
//				holder.imageView2.setLayoutParams(paramsIV2);
//
//				holder.textViewFileName1 = (TextView) convertView.findViewById(R.id.file_grid_filename1);
//				holder.textViewFileName1.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
//				holder.textViewFileName1.getLayoutParams().width = Util.px2dp((125*scaleW), outMetrics);
//				holder.textViewFileName1.setEllipsize(TextUtils.TruncateAt.MIDDLE);
//				holder.textViewFileName1.setSingleLine(true);
//				holder.textViewFileName2 = (TextView) convertView.findViewById(R.id.file_grid_filename2);
//				holder.textViewFileName2.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
//				holder.textViewFileName2.getLayoutParams().width = Util.px2dp((125*scaleW), outMetrics);
//				holder.textViewFileName2.setEllipsize(TextUtils.TruncateAt.MIDDLE);
//				holder.textViewFileName2.setSingleLine(true);
//				
//				holder.textViewFileSize1 = (TextView) convertView.findViewById(R.id.file_grid_filesize1);
//				holder.textViewFileSize2 = (TextView) convertView.findViewById(R.id.file_grid_filesize2);
//				
//				holder.imageButtonThreeDots1 = (ImageButton) convertView.findViewById(R.id.file_grid_three_dots1);
//				holder.imageButtonThreeDots2 = (ImageButton) convertView.findViewById(R.id.file_grid_three_dots2);
//				
//				holder.optionsLayout1 = (RelativeLayout) convertView.findViewById(R.id.file_grid_options1);
//				holder.optionOpen1 = (ImageButton) convertView.findViewById(R.id.file_grid_option_open1);
//				holder.optionOpen1.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
//				holder.optionProperties1 = (ImageButton) convertView.findViewById(R.id.file_grid_option_properties1);
//				holder.optionProperties1.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
//				holder.optionDownload1 = (ImageButton) convertView.findViewById(R.id.file_grid_option_download1);
//				holder.optionDownload1.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
//				holder.optionDelete1 = (ImageButton) convertView.findViewById(R.id.file_grid_option_delete1);
//				holder.optionDelete1.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), Util.px2dp((30*scaleW), outMetrics), 0);
//				holder.arrowSelection1 = (ImageView) convertView.findViewById(R.id.file_grid_arrow_selection1);
//				holder.arrowSelection1.setVisibility(View.GONE);
//
//				holder.optionsLayout2 = (RelativeLayout) convertView.findViewById(R.id.file_grid_options2);
//				holder.optionOpen2 = (ImageButton) convertView.findViewById(R.id.file_grid_option_open2);
//				holder.optionOpen2.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
//				holder.optionProperties2 = (ImageButton) convertView.findViewById(R.id.file_grid_option_properties2);
//				holder.optionProperties2.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
//				holder.optionDownload2 = (ImageButton) convertView.findViewById(R.id.file_grid_option_download2);
//				holder.optionDownload2.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
//				holder.optionDelete2 = (ImageButton) convertView.findViewById(R.id.file_grid_option_delete2);
//				holder.optionDelete2.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), Util.px2dp((30*scaleW), outMetrics), 0);
//				holder.arrowSelection2 = (ImageView) convertView.findViewById(R.id.file_grid_arrow_selection2);
//				holder.arrowSelection2.setVisibility(View.GONE);
				
//				convertView.setTag(holder);
//			}
//			else{
//				holder = (ViewHolderBrowserGrid) convertView.getTag();
//				if (holder == null){
//					convertView = inflater.inflate(R.layout.item_file_grid, parent, false);
//					holder = new ViewHolderBrowserGrid();
//					holder.itemLayout1 = (RelativeLayout) convertView.findViewById(R.id.file_grid_item_layout1);
//					holder.itemLayout2 = (RelativeLayout) convertView.findViewById(R.id.file_grid_item_layout2);
//					
//					//Set width and height itemLayout1
//					RelativeLayout.LayoutParams paramsIL1 = new RelativeLayout.LayoutParams(Util.px2dp(172*scaleW, outMetrics),LayoutParams.WRAP_CONTENT);
//					paramsIL1.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), Util.px2dp(5*scaleW, outMetrics), 0);
//					paramsIL1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
//					paramsIL1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
//					holder.itemLayout1.setLayoutParams(paramsIL1);
//					
//					//Set width and height itemLayout2
//					RelativeLayout.LayoutParams paramsIL2 = new RelativeLayout.LayoutParams(Util.px2dp(172*scaleW, outMetrics),LayoutParams.WRAP_CONTENT);
//					paramsIL2.setMargins(0, Util.px2dp(5*scaleH, outMetrics), 0, 0);
//					paramsIL2.addRule(RelativeLayout.RIGHT_OF, R.id.file_grid_item_layout1);
//					paramsIL2.addRule(RelativeLayout.LEFT_OF, R.id.file_grid_separator_final);
//					paramsIL2.addRule(RelativeLayout.ALIGN_PARENT_TOP);
//					TranslateAnimation anim = new TranslateAnimation(Util.px2dp(-5*scaleW, outMetrics), Util.px2dp(-5*scaleW, outMetrics), 0, 0);
//			        anim.setDuration(0);
//			        
//			        holder.itemLayout2.startAnimation(anim);
//					holder.itemLayout2.setLayoutParams(paramsIL2);
//					
//					holder.imageView1 = (ImageButton) convertView.findViewById(R.id.file_grid_thumbnail1);
//		            holder.imageView2 = (ImageButton) convertView.findViewById(R.id.file_grid_thumbnail2);
//		            
//		            
//					RelativeLayout.LayoutParams paramsIV1 = new RelativeLayout.LayoutParams(Util.px2dp(157*scaleW, outMetrics),Util.px2dp(157*scaleH, outMetrics));
//					paramsIV1.addRule(RelativeLayout.CENTER_HORIZONTAL);
//					holder.imageView1.setScaleType(ImageView.ScaleType.FIT_CENTER);
//					paramsIV1.setMargins(Util.px2dp(5*scaleW, outMetrics), Util.px2dp(5*scaleH, outMetrics), Util.px2dp(5*scaleW, outMetrics), 0);
//					holder.imageView1.setLayoutParams(paramsIV1);
//					
//					RelativeLayout.LayoutParams paramsIV2 = new RelativeLayout.LayoutParams(Util.px2dp(157*scaleW, outMetrics),Util.px2dp(157*scaleH, outMetrics));
//					paramsIV2.addRule(RelativeLayout.CENTER_HORIZONTAL);
//					holder.imageView2.setScaleType(ImageView.ScaleType.FIT_CENTER);
//					paramsIV2.setMargins(0, Util.px2dp(5*scaleH, outMetrics), 0, 0);
//					holder.imageView2.setLayoutParams(paramsIV2);
//
//					holder.textViewFileName1 = (TextView) convertView.findViewById(R.id.file_grid_filename1);
//					holder.textViewFileName1.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
//					holder.textViewFileName1.getLayoutParams().width = Util.px2dp((125*scaleW), outMetrics);
//					holder.textViewFileName1.setEllipsize(TextUtils.TruncateAt.MIDDLE);
//					holder.textViewFileName1.setSingleLine(true);
//					holder.textViewFileName2 = (TextView) convertView.findViewById(R.id.file_grid_filename2);
//					holder.textViewFileName2.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
//					holder.textViewFileName2.getLayoutParams().width = Util.px2dp((125*scaleW), outMetrics);
//					holder.textViewFileName2.setEllipsize(TextUtils.TruncateAt.MIDDLE);
//					holder.textViewFileName2.setSingleLine(true);
//					
//					holder.textViewFileSize1 = (TextView) convertView.findViewById(R.id.file_grid_filesize1);
//					holder.textViewFileSize2 = (TextView) convertView.findViewById(R.id.file_grid_filesize2);
//					
//					holder.imageButtonThreeDots1 = (ImageButton) convertView.findViewById(R.id.file_grid_three_dots1);
//					holder.imageButtonThreeDots2 = (ImageButton) convertView.findViewById(R.id.file_grid_three_dots2);
//					
//					holder.optionsLayout1 = (RelativeLayout) convertView.findViewById(R.id.file_grid_options1);
//					holder.optionOpen1 = (ImageButton) convertView.findViewById(R.id.file_grid_option_open1);
//					holder.optionOpen1.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
//					holder.optionProperties1 = (ImageButton) convertView.findViewById(R.id.file_grid_option_properties1);
//					holder.optionProperties1.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
//					holder.optionDownload1 = (ImageButton) convertView.findViewById(R.id.file_grid_option_download1);
//					holder.optionDownload1.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
//					holder.optionDelete1 = (ImageButton) convertView.findViewById(R.id.file_grid_option_delete1);
//					holder.optionDelete1.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), Util.px2dp((30*scaleW), outMetrics), 0);
//					holder.arrowSelection1 = (ImageView) convertView.findViewById(R.id.file_grid_arrow_selection1);
//					holder.arrowSelection1.setVisibility(View.GONE);
//
//					holder.optionsLayout2 = (RelativeLayout) convertView.findViewById(R.id.file_grid_options2);
//					holder.optionOpen2 = (ImageButton) convertView.findViewById(R.id.file_grid_option_open2);
//					holder.optionOpen2.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
//					holder.optionProperties2 = (ImageButton) convertView.findViewById(R.id.file_grid_option_properties2);
//					holder.optionProperties2.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
//					holder.optionDownload2 = (ImageButton) convertView.findViewById(R.id.file_grid_option_download2);
//					holder.optionDownload2.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), 0, 0);
//					holder.optionDelete2 = (ImageButton) convertView.findViewById(R.id.file_grid_option_delete2);
//					holder.optionDelete2.setPadding(Util.px2dp((30*scaleW), outMetrics), Util.px2dp((10*scaleH), outMetrics), Util.px2dp((30*scaleW), outMetrics), 0);
//					holder.arrowSelection2 = (ImageView) convertView.findViewById(R.id.file_grid_arrow_selection2);
//					holder.arrowSelection2.setVisibility(View.GONE);
//					
//					convertView.setTag(holder);
//				}
//			}

			holder.currentPosition = position;

			MegaNode node1 = (MegaNode) getItem(position);
			holder.document1 = node1.getHandle();
			Bitmap thumb1 = null;
			
			holder.textViewFileName1.setText(node1.getName());
			if (node1.isFolder()){
				holder.textViewFileSize1.setText(getInfoFolder(node1));
				holder.imageView1.setImageResource(R.drawable.mime_folder);
			}
			else{
				long node1Size = node1.getSize();
				holder.textViewFileSize1.setText(Util.getSizeString(node1Size));
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
								thumb1 = ThumbnailUtils.getThumbnailFromMegaGrid(node1, context, holder, megaApi, this, 1);
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
				else{
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
								ThumbnailUtils.createThumbnailGrid(context, node1, holder, megaApi, this, 1);
							}
							catch(Exception e){} //Too many AsyncTasks
						}
					}
				}
			}
			
			MegaNode node2;
			if (position < (getCount()-1)){
				node2 = (MegaNode) getItem(position+1);
				holder.document2 = node2.getHandle();
				Bitmap thumb2 = null;
				
				holder.textViewFileName2.setText(node2.getName());
				if (node2.isFolder()){
					holder.textViewFileSize2.setText(getInfoFolder(node2));
					holder.imageView2.setImageResource(R.drawable.mime_folder);
				}
				else{
					long node2Size = node2.getSize();
					holder.textViewFileSize2.setText(Util.getSizeString(node2Size));
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
									thumb2 = ThumbnailUtils.getThumbnailFromMegaGrid(node2, context, holder, megaApi, this, 2);
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
					else{
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
									ThumbnailUtils.createThumbnailGrid(context, node2, holder, megaApi, this, 2);
								}
								catch(Exception e){} //Too many AsyncTasks
							}
						}
					}
				}
				
				holder.itemLayout2.setVisibility(View.VISIBLE);				
			}
			else{
				holder.itemLayout2.setVisibility(View.GONE);
			}
			
			holder.imageView1.setTag(holder);
			holder.imageView1.setOnClickListener(this);
			
			holder.imageView2.setTag(holder);
			holder.imageView2.setOnClickListener(this);
			
			holder.imageButtonThreeDots1.setTag(holder);
			holder.imageButtonThreeDots1.setOnClickListener(this);
			
			holder.imageButtonThreeDots2.setTag(holder);
			holder.imageButtonThreeDots2.setOnClickListener(this);
			
			if (positionClicked != -1){
				if (positionClicked == position){
					holder.arrowSelection1.setVisibility(View.VISIBLE);
					LayoutParams params = holder.optionsLayout1.getLayoutParams();
					params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
					listFragment.smoothScrollToPosition(_position);
					holder.arrowSelection2.setVisibility(View.GONE);
					LayoutParams params2 = holder.optionsLayout2.getLayoutParams();
					params2.height = 0;
				}
				else if (positionClicked == (position+1)){
					holder.arrowSelection2.setVisibility(View.VISIBLE);
					LayoutParams params = holder.optionsLayout2.getLayoutParams();
					params.height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());
					listFragment.smoothScrollToPosition(_position);
					holder.arrowSelection1.setVisibility(View.GONE);
					LayoutParams params1 = holder.optionsLayout1.getLayoutParams();
					params1.height = 0;
				}
				else{
					holder.arrowSelection1.setVisibility(View.GONE);
					LayoutParams params1 = holder.optionsLayout1.getLayoutParams();
					params1.height = 0;
					
					holder.arrowSelection2.setVisibility(View.GONE);
					LayoutParams params2 = holder.optionsLayout2.getLayoutParams();
					params2.height = 0;
				}
			}
			else{
				holder.arrowSelection1.setVisibility(View.GONE);
				LayoutParams params1 = holder.optionsLayout1.getLayoutParams();
				params1.height = 0;
				
				holder.arrowSelection2.setVisibility(View.GONE);
				LayoutParams params2 = holder.optionsLayout2.getLayoutParams();
				params2.height = 0;
			}
			
//			holder.optionOpen1.setTag(holder);
//			holder.optionOpen1.setOnClickListener(this);
			
//			holder.optionOpen2.setTag(holder);
//			holder.optionOpen2.setOnClickListener(this);
			
			holder.optionDownload1.setTag(holder);
			holder.optionDownload1.setOnClickListener(this);
			
			holder.optionProperties1.setTag(holder);
			holder.optionProperties1.setOnClickListener(this);
			
			holder.optionRename1.setTag(holder);
			holder.optionRename1.setOnClickListener(this);
			
			holder.optionCopy1.setTag(holder);
			holder.optionCopy1.setOnClickListener(this);
			
			holder.optionMove1.setTag(holder);
			holder.optionMove1.setOnClickListener(this);
			
			holder.optionDelete1.setTag(holder);
			holder.optionDelete1.setOnClickListener(this);
			
			holder.optionPublicLink2.setTag(holder);
			holder.optionPublicLink2.setOnClickListener(this);
			
			holder.optionDownload2.setTag(holder);
			holder.optionDownload2.setOnClickListener(this);
			
			holder.optionProperties2.setTag(holder);
			holder.optionProperties2.setOnClickListener(this);
			
			holder.optionRename2.setTag(holder);
			holder.optionRename2.setOnClickListener(this);
			
			holder.optionCopy2.setTag(holder);
			holder.optionCopy2.setOnClickListener(this);
			
			holder.optionMove2.setTag(holder);
			holder.optionMove2.setOnClickListener(this);
			
			holder.optionDelete2.setTag(holder);
			holder.optionDelete2.setOnClickListener(this);
			
			holder.optionPublicLink2.setTag(holder);
			holder.optionPublicLink2.setOnClickListener(this);
		}
		else{
			v = inflater.inflate(R.layout.item_file_empty_grid, parent, false);
		}
		
		return v;
//			if (convertView == null) {
//				convertView = inflater.inflate(R.layout.item_file_empty_grid, parent, false);
//			}
//		}
//
//		return convertView;
	}
	
	private String getInfoFolder (MegaNode n){
		NodeList nL = megaApi.getChildren(n);
		
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
        return nodes.size();
    }
 
    @Override
    public Object getItem(int position) {
        return nodes.get(position);
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

		ViewHolderBrowserGrid holder = (ViewHolderBrowserGrid) v.getTag();
		int currentPosition = holder.currentPosition;
		
		switch (v.getId()){
			case R.id.file_grid_thumbnail1:{
				MegaNode n = (MegaNode) getItem(currentPosition);
				if (n.isFolder()){
					aB.setTitle(n.getName());
					((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
					((ManagerActivity)context).supportInvalidateOptionsMenu();

					parentHandle = n.getHandle();
					nodes = megaApi.getChildren(n);
					setNodes(nodes);
					listFragment.setSelection(0);
					
					//If folder has no files
					if (nodes.size() == 0){
						listFragment.setVisibility(View.GONE);
						emptyImageViewFragment.setVisibility(View.VISIBLE);
						emptyTextViewFragment.setVisibility(View.VISIBLE);
						if (megaApi.getRootNode().getHandle()==n.getHandle()) {
							emptyImageViewFragment.setImageResource(R.drawable.ic_empty_cloud_drive);
							emptyTextViewFragment.setText(R.string.file_browser_empty_cloud_drive);
						} else {
							emptyImageViewFragment.setImageResource(R.drawable.ic_empty_folder);
							emptyTextViewFragment.setText(R.string.file_browser_empty_folder);
						}
					}
					else{
						listFragment.setVisibility(View.VISIBLE);
						emptyImageViewFragment.setVisibility(View.GONE);
						emptyTextViewFragment.setVisibility(View.GONE);
					}
				}
				else{
					if (MimeType.typeForName(n.getName()).isImage()){
						Intent intent = new Intent(context, FullScreenImageViewer.class);
						intent.putExtra("position", currentPosition);
						if (megaApi.getParentNode(n).getType() == MegaNode.TYPE_ROOT){
							intent.putExtra("parentNodeHandle", -1L);
						}
						else{
							intent.putExtra("parentNodeHandle", megaApi.getParentNode(n).getHandle());
						}
						context.startActivity(intent);
					}
					else{
						((ManagerActivity) context).onFileClick(n);
					}	
					positionClicked = -1;
					notifyDataSetChanged();
				}
				break;
			}
			case R.id.file_grid_thumbnail2:{
				
				MegaNode n = (MegaNode) getItem(currentPosition+1);
				if (n.isFolder()){
					aB.setTitle(n.getName());
					((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
					((ManagerActivity)context).supportInvalidateOptionsMenu();

					parentHandle = n.getHandle();
					nodes = megaApi.getChildren(n);
					setNodes(nodes);
					listFragment.setSelection(0);
					
					//If folder has no files
					if (nodes.size() == 0){
						listFragment.setVisibility(View.GONE);
						emptyImageViewFragment.setVisibility(View.VISIBLE);
						emptyTextViewFragment.setVisibility(View.VISIBLE);
						if (megaApi.getRootNode().getHandle()==n.getHandle()) {
							emptyImageViewFragment.setImageResource(R.drawable.ic_empty_cloud_drive);
							emptyTextViewFragment.setText(R.string.file_browser_empty_cloud_drive);
						} else {
							emptyImageViewFragment.setImageResource(R.drawable.ic_empty_folder);
							emptyTextViewFragment.setText(R.string.file_browser_empty_folder);
						}
					}
					else{
						listFragment.setVisibility(View.VISIBLE);
						emptyImageViewFragment.setVisibility(View.GONE);
						emptyTextViewFragment.setVisibility(View.GONE);
					}
				}
				else{
					if (MimeType.typeForName(n.getName()).isImage()){
						Intent intent = new Intent(context, FullScreenImageViewer.class);
						intent.putExtra("position", (currentPosition+1));
						if (megaApi.getParentNode(n).getType() == MegaNode.TYPE_ROOT){
							intent.putExtra("parentNodeHandle", -1L);
						}
						else{
							intent.putExtra("parentNodeHandle", megaApi.getParentNode(n).getHandle());
						}
						context.startActivity(intent);
					}
					else{
						((ManagerActivity) context).onFileClick(n);
					}	
					positionClicked = -1;
					notifyDataSetChanged();
				}
				break;
			}
			case R.id.file_grid_three_dots1:{
				if (positionClicked == -1){
					positionClicked = currentPosition;
					notifyDataSetChanged();
				}
				else{
					if (positionClicked == currentPosition){
						positionClicked = -1;
						notifyDataSetChanged();
					}
					else{
						positionClicked = currentPosition;
						notifyDataSetChanged();
					}
				}
				break;
			}
			case R.id.file_grid_three_dots2:{
				if (positionClicked == -1){
					positionClicked = currentPosition+1;
					notifyDataSetChanged();
				}
				else{
					if (positionClicked == (currentPosition+1)){
						positionClicked = -1;
						notifyDataSetChanged();
					}
					else{
						positionClicked = currentPosition+1;
						notifyDataSetChanged();
					}
				}
				break;
			}
//			case R.id.file_grid_option_open1:{
//				MegaNode n = (MegaNode) getItem(currentPosition);
//				if (n.isFolder()){
//					aB.setTitle(n.getName());
//					((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
//					((ManagerActivity)context).supportInvalidateOptionsMenu();
//					
//					parentHandle = n.getHandle();
//					nodes = megaApi.getChildren(n);
//					setNodes(nodes);
//					listFragment.setSelection(0);
//					
//					//If folder has no files
//					if (nodes.size() == 0){
//						listFragment.setVisibility(View.GONE);
//						emptyImageViewFragment.setVisibility(View.VISIBLE);
//						emptyTextViewFragment.setVisibility(View.VISIBLE);
//						if (megaApi.getRootNode().getHandle()==n.getHandle()) {
//							emptyImageViewFragment.setImageResource(R.drawable.ic_empty_cloud_drive);
//							emptyTextViewFragment.setText(R.string.file_browser_empty_cloud_drive);
//						} else {
//							emptyImageViewFragment.setImageResource(R.drawable.ic_empty_folder);
//							emptyTextViewFragment.setText(R.string.file_browser_empty_folder);
//						}
//					}
//					else{
//						listFragment.setVisibility(View.VISIBLE);
//						emptyImageViewFragment.setVisibility(View.GONE);
//						emptyTextViewFragment.setVisibility(View.GONE);
//					}
//				}
//				else{
//					if (MimeType.typeForName(n.getName()).isImage()){
//						Intent intent = new Intent(context, FullScreenImageViewer.class);
//						intent.putExtra("position", currentPosition);
//						if (megaApi.getParentNode(n).getType() == MegaNode.TYPE_ROOT){
//							intent.putExtra("parentNodeHandle", -1L);
//						}
//						else{
//							intent.putExtra("parentNodeHandle", megaApi.getParentNode(n).getHandle());
//						}
//						context.startActivity(intent);
//					}
//					else{
//						Toast.makeText(context, "[IS FILE (not image)]Node handle clicked: " + n.getHandle(), Toast.LENGTH_SHORT).show();
//					}	
//					positionClicked = -1;
//					notifyDataSetChanged();
//				}
//				
//				break;
//			}
//			case R.id.file_grid_option_open2:{
//				MegaNode n = (MegaNode) getItem(currentPosition+1);
//				if (n.isFolder()){
//					aB.setTitle(n.getName());
//					((ManagerActivity)context).getmDrawerToggle().setDrawerIndicatorEnabled(false);
//					((ManagerActivity)context).supportInvalidateOptionsMenu();
//
//					parentHandle = n.getHandle();
//					nodes = megaApi.getChildren(n);
//					setNodes(nodes);
//					listFragment.setSelection(0);
//					
//					//If folder has no files
//					if (nodes.size() == 0){
//						listFragment.setVisibility(View.GONE);
//						emptyImageViewFragment.setVisibility(View.VISIBLE);
//						emptyTextViewFragment.setVisibility(View.VISIBLE);
//						if (megaApi.getRootNode().getHandle()==n.getHandle()) {
//							emptyImageViewFragment.setImageResource(R.drawable.ic_empty_cloud_drive);
//							emptyTextViewFragment.setText(R.string.file_browser_empty_cloud_drive);
//						} else {
//							emptyImageViewFragment.setImageResource(R.drawable.ic_empty_folder);
//							emptyTextViewFragment.setText(R.string.file_browser_empty_folder);
//						}
//					}
//					else{
//						listFragment.setVisibility(View.VISIBLE);
//						emptyImageViewFragment.setVisibility(View.GONE);
//						emptyTextViewFragment.setVisibility(View.GONE);
//					}
//				}
//				else{
//					if (MimeType.typeForName(n.getName()).isImage()){
//						Intent intent = new Intent(context, FullScreenImageViewer.class);
//						intent.putExtra("position", (currentPosition+1));
//						if (megaApi.getParentNode(n).getType() == MegaNode.TYPE_ROOT){
//							intent.putExtra("parentNodeHandle", -1L);
//						}
//						else{
//							intent.putExtra("parentNodeHandle", megaApi.getParentNode(n).getHandle());
//						}
//						context.startActivity(intent);
//					}
//					else{
//						Toast.makeText(context, "[IS FILE (not image)]Node handle clicked: " + n.getHandle(), Toast.LENGTH_SHORT).show();
//					}	
//					positionClicked = -1;
//					notifyDataSetChanged();
//				}
//				
//				break;
//			}
			
			case R.id.file_grid_option_download1:{
				MegaNode n = (MegaNode) getItem(currentPosition);
				positionClicked = -1;
				notifyDataSetChanged();
				((ManagerActivity) context).onFileClick(n);
				break;
			}
			case R.id.file_grid_option_download2:{
				MegaNode n = (MegaNode) getItem(currentPosition+1);
				positionClicked = -1;
				notifyDataSetChanged();
				((ManagerActivity) context).onFileClick(n);
				break;
			}
			case R.id.file_grid_option_properties1:{
				Intent i = new Intent(context, FilePropertiesActivity.class);
				MegaNode n = (MegaNode) getItem(currentPosition);
				if (n.isFolder()){
					i.putExtra("imageId", R.drawable.mime_folder);
				}
				else{
					i.putExtra("imageId", MimeType.typeForName(n.getName()).getIconResourceId());	
				}				
				i.putExtra("name", n.getName());
				context.startActivity(i);							
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.file_grid_option_properties2:{
				Intent i = new Intent(context, FilePropertiesActivity.class);
				MegaNode n = (MegaNode) getItem(currentPosition+1);
				if (n.isFolder()){
					i.putExtra("imageId", R.drawable.mime_folder);
				}
				else{
					i.putExtra("imageId", MimeType.typeForName(n.getName()).getIconResourceId());	
				}				
				i.putExtra("name", n.getName());
				context.startActivity(i);							
				positionClicked = -1;
				notifyDataSetChanged();
				break;
			}
			case R.id.file_grid_option_delete1:{
				MegaNode n = (MegaNode) getItem(currentPosition);
				((ManagerActivity) context).moveToTrash(n);
				break;
			}
			case R.id.file_grid_option_delete2:{
				MegaNode n = (MegaNode) getItem(currentPosition+1);
				((ManagerActivity) context).moveToTrash(n);
				break;
			}
			case R.id.file_grid_option_rename1:{
				MegaNode n = (MegaNode) getItem(currentPosition);
				((ManagerActivity) context).showRenameDialog(n, n.getName());
				break;
			}
			case R.id.file_grid_option_rename2:{
				MegaNode n = (MegaNode) getItem(currentPosition+1);
				((ManagerActivity) context).showRenameDialog(n, n.getName());
				break;
			}
			case R.id.file_grid_option_move1:{
				MegaNode n = (MegaNode) getItem(currentPosition);
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(n.getHandle());
				((ManagerActivity) context).showMove(handleList);
				break;
			}
			case R.id.file_grid_option_move2:{
				MegaNode n = (MegaNode) getItem(currentPosition+1);
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(n.getHandle());
				((ManagerActivity) context).showMove(handleList);
				break;
			}
			case R.id.file_grid_option_copy1:{
				MegaNode n = (MegaNode) getItem(currentPosition);
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(n.getHandle());
				((ManagerActivity) context).showCopy(handleList);
				break;
			}
			case R.id.file_grid_option_copy2:{
				MegaNode n = (MegaNode) getItem(currentPosition+1);
				ArrayList<Long> handleList = new ArrayList<Long>();
				handleList.add(n.getHandle());
				((ManagerActivity) context).showCopy(handleList);
				break;
			}
		}
	}
	
	public long getParentHandle(){
		return parentHandle;
	}
	
	public void setParentHandle(long parentHandle){
		this.parentHandle = parentHandle;
	}
	
	private static void log(String log) {
		Util.log("MegaBrowserGridAdapter", log);
	}
}
