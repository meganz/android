package mega.privacy.android.app.lollipop;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.shockwave.pdfium.PdfDocument;

import java.io.IOException;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;

public class PdfViewerActivityLollipop extends PinActivityLollipop implements OnPageChangeListener, OnLoadCompleteListener, OnPageErrorListener, View.OnClickListener{

    PDFView pdfView;
    Toolbar tB;
    public ActionBar aB;
    Uri uri;
    String pdfFileName;
    int pageNumber = 0;

    private MenuItem shareMenuItem;

    public static final String SAMPLE_FILE = "sample.pdf";

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate");

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null){
            log("intent null");
            finish();
            return;
        }

        uri = intent.getData();
        if (uri == null){
            log("uri null");
            finish();
            return;
        }

        setContentView(R.layout.activity_pdfviewer);

        tB = (Toolbar) findViewById(R.id.toolbar_pdf_viewer);
        if(tB==null){
            log("Tb is Null");
            return;
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_black);
        upArrow.setColorFilter(getResources().getColor(R.color.lollipop_primary_color), PorterDuff.Mode.SRC_ATOP);
        aB.setHomeAsUpIndicator(upArrow);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);

        pdfView = (PDFView) findViewById(R.id.pdfView);

        pdfView.setBackgroundColor(Color.LTGRAY);
        pdfFileName = getFileName(uri);

        try {
            pdfView.fromUri(uri)
                    .defaultPage(pageNumber)
                    .onPageChange(this)
                    .enableAnnotationRendering(true)
                    .onLoad(this)
                    .scrollHandle(new DefaultScrollHandle(this))
                    .spacing(10) // in dp
                    .onPageError(this)
                    .load();
        } catch (Exception e) {

        }

        pdfView.setOnClickListener(this);

        setTitle(pdfFileName);
        aB.setTitle(pdfFileName);
    }

    public void setToolbarVisibilityHide () {
        log("setToolbarVisibilityHide");

        tB.animate().translationY(-tB.getBottom()).setDuration(200L).withEndAction(new Runnable() {
            @Override
            public void run() {
                aB.hide();
            }
        }).start();
    }

    public void setToolbarVisibility (){
        if (aB.isShowing()) {
            //aB.hide();
            tB.animate().translationY(-tB.getBottom()).setDuration(200L).withEndAction(new Runnable() {
                @Override
                public void run() {
                    aB.hide();
                }
            }).start();
        } else {
            aB.show();
            tB.animate().translationY(0).setDuration(200L).start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_pdfviewer, menu);

        shareMenuItem = menu.findItem(R.id.pdfviewer_share);

        Drawable share = getResources().getDrawable(R.drawable.ic_social_share_white);
        share.setColorFilter(getResources().getColor(R.color.lollipop_primary_color), PorterDuff.Mode.SRC_ATOP);

        shareMenuItem.setIcon(share);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");
        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

        int id = item.getItemId();
        switch(id) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
            case R.id.pdfviewer_share: {
                intentToSendFile(uri);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void intentToSendFile(Uri uri){
        log("intentToSendFile");

        if(uri!=null){
            Intent share = new Intent(android.content.Intent.ACTION_SEND);
            share.setType("application/pdf");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                log("Use provider to share");
                share.putExtra(Intent.EXTRA_STREAM, Uri.parse(uri.toString()));
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            else{
                share.putExtra(Intent.EXTRA_STREAM, uri);
            }
            startActivity(Intent.createChooser(share, getString(R.string.context_share_image)));
        }
        else{
            Snackbar.make(this.getCurrentFocus(), getString(R.string.full_image_viewer_not_preview), Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));
    }

    @Override
    public void onPageError(int page, Throwable t) {
        log("Cannot load page " + page);
    }

    @Override
    public void loadComplete(int nbPages) {
        PdfDocument.Meta meta = pdfView.getDocumentMeta();
        log("title = " + meta.getTitle());
        log("author = " + meta.getAuthor());
        log("subject = " + meta.getSubject());
        log("keywords = " + meta.getKeywords());
        log("creator = " + meta.getCreator());
        log("producer = " + meta.getProducer());
        log("creationDate = " + meta.getCreationDate());
        log("modDate = " + meta.getModDate());

        printBookmarksTree(pdfView.getTableOfContents(), "-");
    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            log(String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    public static void log(String log) {
        Util.log("PdfViewerActivityLollipop", log);
    }

    @Override
    public void onClick(View v) {
        log("onClick");

        setToolbarVisibility();
    }
}
