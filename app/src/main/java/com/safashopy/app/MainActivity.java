package com.safashopy.app;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View.OnClickListener;
import android.view.View;
import android.os.Bundle;
import android.widget.Toast;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    static final String BASE_URL = "https://safashopy.com";
    String productTitle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        findViewById(R.id.toolbarTextView).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                WebView webView = findViewById(R.id.web_view);
                webView.loadUrl(BASE_URL);
            }
        });

        WebView webView = findViewById(R.id.web_view);
        webView.loadUrl(BASE_URL);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setSupportZoom(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                boolean isEnabled = url.startsWith(BASE_URL + "/product/");
                if (isEnabled) {
                    WebView webView = findViewById(R.id.web_view);
                    webView.evaluateJavascript(
                            "(function() { " +
                                    "var title = document.getElementsByClassName('product_title')[0]?.innerHTML || ''; " +
                                    "var elements = Array.from(document.getElementsByClassName('tab-content-scroll')[0]?.children || []);" +
                                    "var contents = elements.map(function(element) { return element.innerHTML; });" +
                                    "return JSON.stringify({title: title, contents: contents}); " +
                                    "}) ();", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String html) {

//                                    new ImageDownloader(MainActivity.this, productTitle, findViewById(R.id.progressbar)).execute(html);
                                    if (html != null && !html.isEmpty()) {
                                        if (html.startsWith("\"") && html.endsWith("\"")) {
                                            html = html.substring(1, html.length() - 1).replace("\\\"", "\"");
                                        }
                                        try {
                                            JSONObject productData = new JSONObject(html);
                                            String title = productData.optString("title", "");

                                            JSONArray contentsArray = productData.optJSONArray("contents");
                                            if (contentsArray != null) {
                                                for (int i = 0; i < contentsArray.length(); i++) {
                                                    String content = contentsArray.optString(i, "").replaceAll("<[^>]+>", "")
                                                            .replaceAll("&;|&nbsp;", "")
                                                            .replaceAll("\\s*\\n\\s*", " ")
                                                            .replaceAll("\\s+", " ")
                                                            .replaceAll("\\n", "")
                                                            .replaceAll("\\\\n", "") // إزالة \\n المتبقي
                                                            .replaceAll("\\s+", " ")
                                                            .trim();
                                                    if (!content.isEmpty()) {
                                                        contentsArray.put(i, content);
                                                    } else {
                                                        contentsArray.remove(i);
                                                        i--;
                                                    }

                                                }
                                            }
                                            MainActivity.this.productTitle = title;
                                            System.out.println("Title: " + title);
                                            System.out.println("Cleaned Contents: " + contentsArray.toString());

                                            MainActivity.this.productTitle = title + contentsArray;


                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });
                }
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {

                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimeType);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Downloading file...");
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                long downloadId = dm.enqueue(request);
                dm.enqueue(request);
                Toast.makeText(getApplicationContext(), R.string.downloading_file, Toast.LENGTH_LONG).show();

                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                        if (downloadId == id) {
                            Uri downloadedFileUri = dm.getUriForDownloadedFile(downloadId);
                            shareInvoice(downloadedFileUri); // Method to share via WhatsApp
                        }
                    }
                };

                registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));


            }
        });
    }

    private void shareInvoice(Uri fileUri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setPackage("com.whatsapp");
        shareIntent.setType("application/pdf"); // Assuming the invoice is a PDF
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Invoice via:"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!(((WebView) findViewById(R.id.web_view)).getUrl().toString().startsWith(BASE_URL + "/product/"))) {
            Toast.makeText(this, R.string.no_product, Toast.LENGTH_SHORT).show();
            return true;
        }
        findViewById(R.id.progressbar).setVisibility(View.VISIBLE);
        WebView webView = findViewById(R.id.web_view);
        switch (item.getItemId()) {
            case R.id.share_title:
                new ImageDownloader(MainActivity.this, productTitle, findViewById(R.id.progressbar)).execute("");
                return true;
            case R.id.share_cover:
                webView.evaluateJavascript(
                        "(function() { " +
                                "  const container = document.getElementsByClassName('swiper-control-top')[0]; " +
                                "  const slide1 = container.getElementsByClassName('swiper-slide-active')[0]; " +
                                "  const slide2 = container.getElementsByClassName('swiper-slide')[0]; " +
                                "  const link = (slide1??slide2)?.firstElementChild?.firstElementChild; " +
                                "  return link?.href || link?.src; " +
                                "}) (); ",
                        new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String html) {
                                if (html.equals("null")) {
                                    findViewById(R.id.progressbar).setVisibility(View.GONE);
                                    Toast.makeText(MainActivity.this, R.string.no_url, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                html = html.substring(1, html.length() - 1);
                                new ImageDownloader(MainActivity.this, productTitle, findViewById(R.id.progressbar)).execute(html);
                            }
                        });
                return true;
            case R.id.share_media:
                webView.evaluateJavascript(
                        "(function() { " +
                                "  urlList = []; " +
                                "  Array.from(" +
                                "    document.getElementsByClassName('swiper-control-top')[0]?.getElementsByClassName('swiper-slide'), " +
                                "    child => { const link = child.firstElementChild?.firstElementChild; urlList.push(link?.href || link?.src); }" +
                                "  ); " +
                                "  return urlList.join(',');" +
                                "}) ();",
                        new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String html) { // quoted comma-separated url list
                                if (html.isEmpty() || html.equals("\"\"") || html.equals("null")) {
                                    findViewById(R.id.progressbar).setVisibility(View.GONE);
                                    Toast.makeText(MainActivity.this, R.string.no_url, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                html = html.substring(1, html.length() - 1);
                                new ImageDownloader(MainActivity.this, productTitle, findViewById(R.id.progressbar)).execute(html.split(","));
                            }
                        });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        WebView webView = findViewById(R.id.web_view);
        webView.goBack();
    }
}
