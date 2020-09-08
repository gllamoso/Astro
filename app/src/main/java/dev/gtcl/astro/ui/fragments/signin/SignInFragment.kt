package dev.gtcl.astro.ui.fragments.signin

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.astro.*
import dev.gtcl.astro.databinding.FragmentSignInBinding
import dev.gtcl.astro.models.reddit.listing.FrontPage
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.ListingPage

class SignInFragment : Fragment() {

    private var binding: FragmentSignInBinding? = null

    private val model: SignInVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(SignInVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentSignInBinding.inflate(inflater)
        binding?.model = model
        binding?.lifecycleOwner = this
        clearCookies()
        setWebview()
        binding?.executePendingBindings()
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    private fun setWebview() {
        val url = String.format(REDDIT_AUTH_URL, REDDIT_CLIENT_ID, STATE, REDDIT_REDIRECT_URL)

        val backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                binding?.fragmentSignInWebView?.goBack()
                model.decrementStackCount()
            }
        }

        model.pageStackCount.observe(viewLifecycleOwner, {
            if (it != null) {
                backPressedCallback.isEnabled = (it > 1)
            }
        })

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressedCallback
        )

        val onNavigateToNewPage: () -> Unit = {
            model.incrementStackCount()
        }

        val onRedirectUrlFound: (String) -> Unit = {
            model.setNewUser(it)
        }

        binding?.fragmentSignInWebView?.apply {
            webViewClient =
                NewAccountWebViewClient(
                    REDDIT_REDIRECT_URL,
                    onNavigateToNewPage,
                    onRedirectUrlFound
                )
            loadUrl(url)
        }

        binding?.fragmentSignInToolbar?.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        model.loading.observe(viewLifecycleOwner, {
            if (it != null) {
                backPressedCallback.isEnabled = !it
            }
        })

        model.errorMessage.observe(viewLifecycleOwner, { errorMessage ->
            if (errorMessage != null) {
                binding?.root?.let {
                    Snackbar.make(it, errorMessage, Snackbar.LENGTH_LONG).show()
                }
                model.errorMessageObserved()
            }
        })

        model.successfullyAddedAccount.observe(viewLifecycleOwner, {
            if (it == true) {
                findNavController().navigate(
                    SignInFragmentDirections.signInWithNewAccount(
                        ListingPage(
                            FrontPage
                        )
                    )
                )
                activityModel.syncSubscriptionsWithReddit()
            }
        })
    }

    companion object
    class NewAccountWebViewClient(
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
            if (url.contains(redirectUrl)) {
                onRedirectUrlFound(url)
                return true
            }

            return false
        }
    }
}