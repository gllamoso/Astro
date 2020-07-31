package dev.gtcl.reddit.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import dev.gtcl.reddit.*
import dev.gtcl.reddit.actions.*
import dev.gtcl.reddit.databinding.FragmentViewpagerBinding
import dev.gtcl.reddit.models.reddit.listing.ListingType
import dev.gtcl.reddit.models.reddit.listing.Post
import dev.gtcl.reddit.models.reddit.listing.SubscriptionListing
import dev.gtcl.reddit.ui.activities.MainActivityVM

class ViewPagerFragment : Fragment(), ViewPagerActions, NavigationActions {

    private lateinit var binding: FragmentViewpagerBinding

    private val args: ViewPagerFragmentArgs by navArgs()

    private val model: ViewPagerVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
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
        backPressedCallback.isEnabled = binding.viewpager.currentItem != 0
    }

    private fun initViewPagerAdapter(){
        pageAdapter = PageAdapter(this)
        if(model.pages.isNotEmpty()){
            pageAdapter.setPageStack(model.pages)
        } else {
            pageAdapter.addPage(args.startingPage)
        }

        model.newPage.observe(viewLifecycleOwner, Observer {
            if(it != null){
                pageAdapter.addPage(it)
                navigateNext()
                model.newPageObserved()
            }
        })

        activityModel.newPage.observe(viewLifecycleOwner, Observer {
            if(it != null){
                model.newPage(it)
                activityModel.newPageObserved()
            }
        })

        binding.viewpager.apply {
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
                navigatePreviousPage()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    private fun initObservers() {
        model.swipeEnabled.observe(viewLifecycleOwner, Observer {
            binding.viewpager.isUserInputEnabled = it
        })

        model.navigateToPreviousPage.observe(viewLifecycleOwner, Observer {
            navigatePreviousPage()
            model.navigateToPreviousPageObserved()
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        model.pages = pageAdapter.getPageStack()
    }

//    __      ___               _____                                     _   _
//    \ \    / (_)             |  __ \                          /\       | | (_)
//     \ \  / / _  _____      _| |__) |_ _  __ _  ___ _ __     /  \   ___| |_ _  ___  _ __  ___
//      \ \/ / | |/ _ \ \ /\ / /  ___/ _` |/ _` |/ _ \ '__|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//       \  /  | |  __/\ V  V /| |  | (_| | (_| |  __/ |     / ____ \ (__| |_| | (_) | | | \__ \
//        \/   |_|\___| \_/\_/ |_|   \__,_|\__, |\___|_|    /_/    \_\___|\__|_|\___/|_| |_|___/
//                                          __/ |
//                                         |___/

    override fun enablePagerSwiping(enable: Boolean) {
        binding.viewpager.isUserInputEnabled = enable
    }

    override fun navigatePreviousPage() {
        val currentPage = binding.viewpager.currentItem
        binding.viewpager.setCurrentItem(currentPage - 1, true)
    }

    override fun navigateToComments(post: Post, position: Int) {
        pageAdapter.addPage(PostPage(post, position))
        navigateNext()
    }

    private fun navigateNext() {
        val currentPage = binding.viewpager.currentItem
        binding.viewpager.setCurrentItem(currentPage + 1, true)
    }

//     _   _             _             _   _                            _   _
//    | \ | |           (_)           | | (_)                 /\       | | (_)
//    |  \| | __ ___   ___  __ _  __ _| |_ _  ___  _ __      /  \   ___| |_ _  ___  _ __  ___
//    | . ` |/ _` \ \ / / |/ _` |/ _` | __| |/ _ \| '_ \    / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |\  | (_| |\ V /| | (_| | (_| | |_| | (_) | | | |  / ____ \ (__| |_| | (_) | | | \__ \
//    |_| \_|\__,_| \_/ |_|\__, |\__,_|\__|_|\___/|_| |_| /_/    \_\___|\__|_|\___/|_| |_|___/
//                          __/ |
//                         |___/

    override fun listingSelected(listing: ListingType) {
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
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToInboxFragment())
    }

    override fun signInNewAccount() {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSignInFragment())
    }

    override fun launchWebview(url: String) {
        activityModel.openChromeTab(url)
    }

}