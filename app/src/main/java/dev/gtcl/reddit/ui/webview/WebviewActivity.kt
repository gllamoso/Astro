package dev.gtcl.reddit.ui.webview

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.R
import dev.gtcl.reddit.databinding.ActivityWebviewBinding
import dev.gtcl.reddit.ui.URL_KEY

class WebviewActivity : AppCompatActivity(){

    val model: WebviewActivityViewModel by lazy {
        val viewModelFactory = WebviewActivityViewModelFactory()
        ViewModelProvider(this, viewModelFactory).get(WebviewActivityViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityWebviewBinding>(this, R.layout.activity_webview)
        val url = intent?.getStringExtra(URL_KEY)

        val webView = binding.webview
        clearCookies()
        webView.webViewClient = NewAccountWebViewClient(getString(R.string.redirect_uri)){model.setRedirectUrl(it)}
        url?.let { webView.loadUrl(url) }

        model.redirectUrl.observe(this, Observer {
            val intent = Intent()
            intent.data = it.toUri()
            setResult(Activity.RESULT_OK, intent)
            finish()
        })
    }

    @Suppress("DEPRECATION")
    private fun clearCookies(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        } else {
            val cookieSyncMngr = CookieSyncManager.createInstance(this)
            cookieSyncMngr.startSync()
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookie()
            cookieManager.removeSessionCookie()
            cookieSyncMngr.stopSync()
            cookieSyncMngr.sync()
        }
    }

    private class NewAccountWebViewClient(val redirectUri: String, private val onRedirectUrlFound: (String) -> Unit) : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            return handleUri(Uri.parse(url))
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            return handleUri(request?.url)
        }

        private fun handleUri(uri: Uri?): Boolean{
            val url = uri.toString()
            if(url.contains(redirectUri))
                onRedirectUrlFound(url)

            return false
        }
    }
}