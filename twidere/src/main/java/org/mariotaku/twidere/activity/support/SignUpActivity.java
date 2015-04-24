package org.mariotaku.twidere.activity.support;

import org.mariotaku.twidere.R;
import org.mariotaku.twidere.proxy.ProxySettings;

import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.view.WindowManager;
import android.content.Context;

/**
 * Created by atavism on 4/23/15.
 */
public class SignUpActivity extends BaseActionBarActivity {

    private static WebView   webView                = null;

    private static final String TWITTER_SIGNUP_URL = "https://twitter.com/signup";

    private static final String PROXY_HOST = "127.0.0.1";
    private static final int PROXY_PORT = 9192;
    private static final String APP_NAME = "org.getlantern.FireTweet";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser_sign_in);

        final Context context = this;

        WebView webView = (WebView) findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        
        ProxySettings.setProxy(context, webView, PROXY_HOST, PROXY_PORT, APP_NAME);

        webView.loadUrl(TWITTER_SIGNUP_URL);

    }

}
