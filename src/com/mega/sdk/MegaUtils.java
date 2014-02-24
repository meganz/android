package com.mega.sdk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.ExifInterface;

public class MegaUtils
{
	private static int THUMBNAIL_SIZE = 120;
	private static int THUMBNAIL_QUALITY = 90;
	private static int PREVIEW_SIZE =  1000;
	private static int PREVIEW_QUALITY = 90;

	public static ArrayList<Long> pendingThumbnails = new ArrayList<Long>();
	public static ArrayList<Long> pendingPreviews = new ArrayList<Long>();

	private static Bitmap decodeFile(File f, int requiredSize) {
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, options);

			int scale = 1;
			while (options.outWidth / scale / 2 >= requiredSize
					&& options.outHeight / scale / 2 >= requiredSize)
				scale *= 2;

		    options.inJustDecodeBounds = false;
			options.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(f), null, options);
		} 
		catch(Exception e) {}
		return null;
	}
	
	/*
	 * Change image orientation based on EXIF image data
	 */
	private static Bitmap fixExifOrientation(File file, Bitmap bitmap) {
		int orientation = ExifInterface.ORIENTATION_UNDEFINED;
		try {
			ExifInterface exif = new ExifInterface(file.getAbsolutePath());
			orientation = exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION, orientation);
		} catch (IOException e) {
			return bitmap;
		}
		
		int rotation = 0;
		switch(orientation)
		{
		case ExifInterface.ORIENTATION_ROTATE_90:
			rotation = 90;
			break;
		case ExifInterface.ORIENTATION_ROTATE_180:
			rotation = 180;
			break;
		case ExifInterface.ORIENTATION_ROTATE_270:
			rotation = 270;
			break;
		default:
			return bitmap;
		}

		Matrix matrix = new Matrix();
		matrix.postRotate(rotation);
		return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
				bitmap.getHeight(), matrix, true);
	}
	
	private static boolean saveThumbnail(Bitmap bitmap, File outputFile)
	{
		int srcWidth = bitmap.getWidth();
		int srcHeight = bitmap.getHeight();

		if ((srcWidth == 0) || (srcHeight == 0))
			return false;

		double scale = (double) THUMBNAIL_SIZE / Math.min(srcWidth, srcHeight);
		int translateX = 0;
		int translateY = 0;
		if (srcHeight > srcWidth)
			translateY = (srcHeight - srcWidth) / -3;
		else
			translateX = (srcWidth - srcHeight) / -2;

		Bitmap.Config conf = Bitmap.Config.ARGB_8888;
		Bitmap scaled = Bitmap.createBitmap(THUMBNAIL_SIZE, THUMBNAIL_SIZE, conf);
		Canvas canvas = new Canvas(scaled);
		canvas.setDensity(bitmap.getDensity());
		Matrix matrix = new Matrix();
		matrix.preTranslate(translateX, translateY);
		matrix.postScale((float) scale, (float) scale);
		canvas.drawBitmap(bitmap, matrix, null);
		FileOutputStream stream;
		try 
		{
			stream = new FileOutputStream(outputFile);
			if(!scaled.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, stream))
			{
				outputFile.delete();
				return false;
			}
			stream.close();
			return true;
		} catch (Exception e) {}
		return false;
	}

	private static boolean savePreview(Bitmap bitmap, File outputFile)
	{
		int srcWidth = bitmap.getWidth();
		int srcHeight = bitmap.getHeight();

		if ((srcWidth == 0) || (srcHeight == 0))
			return false;

		double scale = (double) PREVIEW_SIZE / Math.max(srcWidth, srcHeight);
		Bitmap.Config conf = Bitmap.Config.ARGB_8888;
		Bitmap scaled = Bitmap.createBitmap((int)(srcWidth*scale), 
				(int)(srcHeight*scale), conf);
		Canvas canvas = new Canvas(scaled);
		canvas.setDensity(bitmap.getDensity());
		Matrix matrix = new Matrix();
		matrix.postScale((float) scale, (float) scale);
		canvas.drawBitmap(bitmap, matrix, null);
		FileOutputStream stream;
		try 
		{
			stream = new FileOutputStream(outputFile);
			if(!scaled.compress(Bitmap.CompressFormat.JPEG, PREVIEW_QUALITY, stream))
			{
				outputFile.delete();
				return false;
			}
			stream.close();
			return true;
		} catch (Exception e) {}
		return false;
	}

	
	public static boolean createThumbnail(File image, File thumbnail)
	{
		if(!image.exists())
			return false;
			
		if(thumbnail.exists())
			thumbnail.delete();
				
		Bitmap bitmap = decodeFile(image, THUMBNAIL_SIZE * 2);
		if (bitmap == null)
			return false;
		
		bitmap = fixExifOrientation(image, bitmap);
		if ((bitmap == null) || (bitmap.getWidth() == 0) || (bitmap.getHeight() == 0))
			return false;
		
		return saveThumbnail(bitmap, thumbnail);		
	}
	
	public static boolean createPreview(File image, File preview)
	{
		if(!image.exists())
			return false;
			
		if(preview.exists())
			preview.delete();
				
		Bitmap bitmap = decodeFile(image, PREVIEW_SIZE);
		if (bitmap == null)
			return false;
		
		bitmap = fixExifOrientation(image, bitmap);
		if ((bitmap == null) || (bitmap.getWidth() == 0) || (bitmap.getHeight() == 0))
			return false;
		
		return savePreview(bitmap, preview);		
	}
}
