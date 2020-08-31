package dev.gtcl.astro.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import dev.gtcl.astro.*
import dev.gtcl.astro.actions.*
import dev.gtcl.astro.databinding.FragmentViewpagerBinding
import dev.gtcl.astro.models.reddit.MediaURL
import dev.gtcl.astro.models.reddit.listing.Listing
import dev.gtcl.astro.models.reddit.listing.SubscriptionListing
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.media.MediaDialogFragment

class ViewPagerFragment : Fragment(), NavigationActions, LinkHandler {

    private lateinit var binding: FragmentViewpagerBinding

    private val args: ViewPagerFragmentArgs by navArgs()

    private val model: ViewPagerVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(ViewPagerVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    private lateinit var pageAdapter: PageAdapter
    private lateinit var backPressedCallback: OnBackPressedCallback

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentViewpagerBinding.inflate(inflater)
        initViewPagerAdapter()
        initBackPressedCallback()
        initObservers()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        backPressedCallback.isEnabled = binding.fragmentViewPagerViewPager.currentItem != 0
    }

    private fun initViewPagerAdapter(){
        pageAdapter = PageAdapter(this)
        if(model.pages.isNotEmpty()){
            pageAdapter.setPageStack(model.pages)
        } else {
            pageAdapter.addPage(args.startingPage)
        }

        activityModel.newPage.observe(viewLifecycleOwner, {
            if(it != null){
                newPage(it)
                activityModel.newPageObserved()
            }
        })

        binding.fragmentViewPagerViewPager.apply {
            adapter = pageAdapter
            isUserInputEnabled = model.isViewPagerSwipeEnabled
            offscreenPageLimit = 3
            registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    if(state == ViewPager2.SCROLL_STATE_IDLE){
                        pageAdapter.popFragmentsGreaterThanPosition(currentItem)
                        isUserInputEnabled = currentItem != 0
                        backPressedCallback.isEnabled = currentItem != 0
                        model.isViewPagerSwipeEnabled = isUserInputEnabled
                    }
                }
            })
            setPageTransformer(SlidePageTransformer())
            (getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }
    }

    private fun initBackPressedCallback(){
        backPressedCallback = object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                val currentPage = binding.fragmentViewPagerViewPager.currentItem
                binding.fragmentViewPagerViewPager.setCurrentItem(currentPage - 1, true)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    private fun initObservers() {
        model.swipeEnabled.observe(viewLifecycleOwner, {
            binding.fragmentViewPagerViewPager.isUserInputEnabled = it
        })

        model.navigateToPreviousPage.observe(viewLifecycleOwner, {
            if(it != null){
                val currentPage = binding.fragmentViewPagerViewPager.currentItem
                binding.fragmentViewPagerViewPager.setCurrentItem(currentPage - 1, true)
                model.navigateToPreviousPageObserved()
            }
        })

        model.linkClicked.observe(viewLifecycleOwner, {
            if(it != null){
                handleLink(it)
                model.linkObserved()
            }
        })

        childFragmentManager.setFragmentResultListener(URL_KEY, viewLifecycleOwner, { _, bundle ->
            val url = bundle.getString(URL_KEY)!!
            newPage(ContinueThreadPage(url, null, false))
        })
    }

//     _   _             _             _   _                            _   _
//    | \ | |           (_)           | | (_)                 /\       | | (_)
//    |  \| | __ ___   ___  __ _  __ _| |_ _  ___  _ __      /  \   ___| |_ _  ___  _ __  ___
//    | . ` |/ _` \ \ / / |/ _` |/ _` | __| |/ _ \| '_ \    / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |\  | (_| |\ V /| | (_| | (_| | |_| | (_) | | | |  / ____ \ (__| |_| | (_) | | | \__ \
//    |_| \_|\__,_| \_/ |_|\__, |\__,_|\__|_|\___/|_| |_| /_/    \_\___|\__|_|\___/|_| |_|___/
//                          __/ |
//                         |___/

    override fun listingSelected(listing: Listing) {
        if(listing is SubscriptionListing && listing.subscription.type == SubscriptionType.USER){
            accountSelected(listing.subscription.displayName)
        } else {
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(ListingPage(listing)))
        }
    }

    override fun accountSelected(user: String?) {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(AccountPage(user)))
    }

    override fun messagesSelected() {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(InboxPage))
    }

    override fun signInNewAccount() {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSignInFragment())
    }

    override fun launchWebview(url: String) {
        activityModel.openChromeTab(url)
    }

    private fun newPage(page: ViewPagerPage){
        pageAdapter.addPage(page)
        model.pages.add(page)
        val currentPage = binding.fragmentViewPagerViewPager.currentItem
        binding.fragmentViewPagerViewPager.setCurrentItem(currentPage + 1, true)
    }

    override fun handleLink(link: String) {
        when(link.getUrlType()){
            UrlType.IMAGE -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.PICTURE)).show(childFragmentManager, null)
            UrlType.GIF -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.GIF)).show(childFragmentManager, null)
            UrlType.GIFV, UrlType.HLS, UrlType.STANDARD_VIDEO -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.VIDEO)).show(childFragmentManager, null)
            UrlType.GFYCAT -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.GFYCAT)).show(childFragmentManager, null)
            UrlType.IMGUR_ALBUM -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.IMGUR_ALBUM)).show(childFragmentManager, null)
            UrlType.REDDIT_COMMENTS -> newPage(ContinueThreadPage(link, null, true))
            UrlType.OTHER, UrlType.REDDIT_VIDEO -> activityModel.openChromeTab(link)
            UrlType.IMGUR_IMAGE -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.IMGUR_PICTURE)).show(childFragmentManager, null)
            UrlType.REDGIFS -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.REDGIFS)).show(childFragmentManager, null)
            null -> throw IllegalArgumentException("Unable to determin link type: link")
        }
    }

}