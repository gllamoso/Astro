package dev.gtcl.reddit.ui.fragments.signin

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.databinding.FragmentSignInBinding
import dev.gtcl.reddit.models.reddit.FrontPage
import dev.gtcl.reddit.ui.fragments.ListingPage
import dev.gtcl.reddit.ui.fragments.ViewPagerFragmentDirections
import java.util.UUID

class SignInFragment : Fragment() {

    private lateinit var binding: FragmentSignInBinding

    private val model: SignInVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(SignInVM::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentSignInBinding.inflate(inflater)
        binding.model = model
        binding.lifecycleOwner = this
        clearCookies()
        setWebview()
        binding.executePendingBindings()
        return binding.root
    }

    private fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    private fun setWebview(){
        val url = String.format(getString(R.string.auth_url), getString(R.string.client_id), STATE, getString(R.string.redirect_uri))

        val backPressedCallback  = object: OnBackPressedCallback(false){
            override fun handleOnBackPressed() {
                binding.webView.goBack()
                model.decrementStackCount()
            }
        }

        model.pageStackCount.observe(viewLifecycleOwner, Observer {
            if(it != null){
                backPressedCallback.isEnabled = (it > 1)
            }
        })

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)

        val onNavigateToNewPage: () -> Unit = {
            model.incrementStackCount()
        }

        val onRedirectUrlFound: (String) -> Unit = {
            model.setNewUser(it)
        }

        binding.webView.apply {
            webViewClient =
                NewAccountWebViewClient(
                    getString(R.string.redirect_uri),
                    onNavigateToNewPage,
                    onRedirectUrlFound
                )
            loadUrl(url)
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        model.loading.observe(viewLifecycleOwner, Observer{
            if(it != null){
                backPressedCallback.isEnabled = !it
            }
        })

        model.errorMessage.observe(viewLifecycleOwner, Observer {
            if(it != null){
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        })

        model.successfullyAddedAccount.observe(viewLifecycleOwner, Observer {
            if(it == true){
                findNavController().navigate(SignInFragmentDirections.signInWithNewAccount(ListingPage(FrontPage)))
            }
        })
    }

    companion object class NewAccountWebViewClient(
        private val redirectUrl: String,
        private val onNavigateToNewPage: () -> Unit,
        private val onRedirectUrlFound: (String) -> Unit
    ) : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            return handleUri(Uri.parse(url))
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            return handleUri(request?.url)
        }

        private fun handleUri(uri: Uri?): Boolean {
            onNavigateToNewPage()
            val url = uri.toString()
            if(url.contains(redirectUrl)){
                onRedirectUrlFound(url)
                return true
            }

            return false
        }
    }
}