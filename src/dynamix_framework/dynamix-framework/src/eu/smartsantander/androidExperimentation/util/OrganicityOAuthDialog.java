package eu.smartsantander.androidExperimentation.util;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.ambientdynamix.core.BaseActivity;
import org.ambientdynamix.core.R;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;


public class OrganicityOAuthDialog extends Dialog {

    private static final String TAG = "OrganicityOAuthDialog";

    /* Strings used in the OAuth flow */
    public static final String OAUTHCALLBACK_URI = Constants.ORGANICITY_APP_CALLBACK_OAUTHCALLBACK;
    //public static final String TOKEN = "access_token";// FIXME: adapt this
    //public static final String EXPIRES = "expires_in";


    static final int BG_COLOR = Color.argb(1, 239, 64, 112);
    static final FrameLayout.LayoutParams FILL =
            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
    static final int MARGIN = 4;
    static final int PADDING = 2;
    //static final String FB_ICON = "icon.png";

    private String mUrl;
    private GenericDialogListener mListener;
    private ProgressDialog mSpinner;
    private WebView mWebView;
    private LinearLayout mContent;
    private TextView mTitle;

    public OrganicityOAuthDialog(Context context, String url, GenericDialogListener listener) {
        super(context);
        mUrl = url;
        mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSpinner = new ProgressDialog(getContext());
        mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSpinner.setMessage("Loading...");

        mContent = new LinearLayout(getContext());
        mContent.setOrientation(LinearLayout.VERTICAL);
        setUpTitle();
        setUpWebView();

        final float scale = getContext().getResources().getDisplayMetrics().density;
        float[] dimensions = {480, 480};
        addContentView(mContent, new FrameLayout.LayoutParams(
                (int) (dimensions[0] * scale + 0.8f),
                (int) (dimensions[1] * scale + 0.8f)));
    }

    private void setUpTitle() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Drawable icon = getContext().getResources().getDrawable(
                R.drawable.about_icon);
        mTitle = new TextView(getContext());
        mTitle.setText("Organicity OAuth");
        mTitle.setTextColor(Color.WHITE);
        mTitle.setTypeface(Typeface.DEFAULT_BOLD);
        mTitle.setBackgroundColor(BG_COLOR);
        mTitle.setPadding(MARGIN + PADDING, MARGIN, MARGIN, MARGIN);
        mTitle.setCompoundDrawablePadding(MARGIN + PADDING);
        mTitle.setCompoundDrawablesWithIntrinsicBounds(
                icon, null, null, null);
        mContent.addView(mTitle);
    }

    private void setUpWebView() {
        mWebView = new WebView(getContext());
        mWebView.setVerticalScrollBarEnabled(false);
        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setWebViewClient(new OrganicityOAuthDialog.OAuthWebViewClient());
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(mUrl);
        mWebView.setLayoutParams(FILL);
        mContent.addView(mWebView);
    }

    private class OAuthWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith(OAUTHCALLBACK_URI)) {
                Uri uri = Uri.parse(url.split("#")[0] + "?" + url.split("#")[1]);
                BaseActivity.access_token = uri.getQueryParameter("access_token");
                view.stopLoading();
                return false;
            }
            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            /*// TODO: pass error back to listener!
             * mListener.onError(
                    new DialogError(description, errorCode, failingUrl));*/
            OrganicityOAuthDialog.this.dismiss();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            mSpinner.show();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            String title = mWebView.getTitle();
            if (title != null && title.length() > 0) {
                mTitle.setText(title);
            }

            try {// to avoid crashing the app add try-catch block, avoid this stupid crash!
                if (mSpinner != null && mSpinner.isShowing())// by YG
                    mSpinner.dismiss();
            } catch (Exception ex) {
                Log.w(TAG, "wtf exception onPageFinished! " + ex.toString());
            }

            if (url.startsWith(OAUTHCALLBACK_URI)) {
                Bundle values = parseUrl(url);


                String error = values.containsKey("error") ? values.getString("error") : null;
                if (error == null) {
                    error = values.containsKey("error_type") ? values.getString("error_type") : null;
                }

                if (error == null) {
                    mListener.onComplete(values);
                } else if (error.equals("access_denied") ||
                        error.equals("OAuthAccessDeniedException")) {
                    mListener.onCancel();
                }

                OrganicityOAuthDialog.this.dismiss();
            }
        }

    }

    /**
     * Parse a URL query and fragment parameters into a key-value bundle.
     *
     * @param url the URL to parse
     * @return a dictionary bundle of keys and values
     */
    public static Bundle parseUrl(String url) {
        // hack to prevent MalformedURLException
        try {
            URL u = new URL(url);
            Bundle b = decodeUrl(u.getQuery());
            b.putAll(decodeUrl(u.getRef()));
            return b;
        } catch (MalformedURLException e) {
            return new Bundle();
        }
    }

    public static Bundle decodeUrl(String s) {
        Bundle params = new Bundle();
        if (s != null) {
            String array[] = s.split("&");
            for (String parameter : array) {
                String v[] = parameter.split("=");
                // YG: in case param has no value
                if (v.length == 2) {
                    params.putString(URLDecoder.decode(v[0]),
                            URLDecoder.decode(v[1]));
                } else {
                    params.putString(URLDecoder.decode(v[0]), " ");
                }
            }
        }
        return params;
    }

}


