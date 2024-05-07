package com.safashopy.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Parcelable;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

class ImageDownloader extends AsyncTask<String, Integer, ArrayList<Uri>> {
    Context context;
    String productTitle;
    ProgressBar progressBar;

    public ImageDownloader(Context context, String productTitle, ProgressBar progressBar) {
        this.context = context;
        this.productTitle = productTitle;
        this.progressBar = progressBar;
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected ArrayList<Uri> doInBackground(String... imageUrlList) {
        final String prefix = "file_";
        for (File f : context.getCacheDir().listFiles()) {
            if (f.getName().startsWith(prefix)) {
                f.delete();
            }
        }

        ArrayList<Uri> uriList = new ArrayList<>();
        for (String imageUrl : imageUrlList) {
            if (imageUrl.isEmpty()) {
                continue;
            }
            try {
                String extension = imageUrl.substring(imageUrl.lastIndexOf("."), imageUrl.length());
                String imageFilePath = context.getCacheDir() + "/" + prefix + System.currentTimeMillis() + extension;
                URL url = new URL(imageUrl);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setDoOutput(true);
                httpURLConnection.connect();
                if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream in = httpURLConnection.getInputStream();
                    FileOutputStream out = new FileOutputStream(imageFilePath);
                    out.write(in.readAllBytes());
                    out.close();
                    in.close();
                    File f = new File(imageFilePath);
                    Uri fileUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName(), f);
                    uriList.add(fileUri);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return uriList;
    }

    @Override
    protected void onPostExecute(ArrayList<Uri> uriList) {

        System.out.println(uriList);
        Intent share;
        if (uriList.isEmpty()) {
            share = new Intent(Intent.ACTION_SEND);
            share.setPackage("com.whatsapp");
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, productTitle);
        } else {
            share = new Intent(Intent.ACTION_SEND_MULTIPLE);
            share.setPackage("com.whatsapp");
            share.setType("*/*");
            share.putExtra(Intent.EXTRA_STREAM, (ArrayList<? extends Parcelable>) uriList);
        }
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(share);
        progressBar.setVisibility(View.GONE);
    }
}
