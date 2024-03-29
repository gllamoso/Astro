package dev.gtcl.astro.ui.fragments.view_pager

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
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.ViewModelFactory
import dev.gtcl.astro.actions.NavigationActions
import dev.gtcl.astro.databinding.FragmentViewpagerBinding
import dev.gtcl.astro.models.reddit.listing.PostListing
import dev.gtcl.astro.models.reddit.listing.ProfileListing
import dev.gtcl.astro.ui.activities.MainActivityVM

class ViewPagerFragment : Fragment(), NavigationActions {

    private var binding: FragmentViewpagerBinding? = null

    private val args: ViewPagerFragmentArgs by navArgs()

    private val model: ViewPagerVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(ViewPagerVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    private lateinit var backPressedCallback: OnBackPressedCallback

    private lateinit var pageAdapter: PageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentViewpagerBinding.inflate(inflater)
        initViewPagerAdapter()
        initBackPressedCallback()
        initOtherObservers()

        return binding?.root
    }

    override fun onResume() {
        super.onResume()
        backPressedCallback.isEnabled = binding?.fragmentViewPagerViewPager?.currentItem != 0
        binding?.fragmentViewPagerViewPager?.currentItem = pageAdapter.itemCount - 1
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.fragmentViewPagerViewPager?.adapter = null
        binding = null
    }

    private fun initViewPagerAdapter() {
        pageAdapter = PageAdapter(childFragmentManager, requireActivity().lifecycle)

        if (model.pages.isEmpty()) {
            val startingPage = args.startingPage
            pageAdapter.addPage(startingPage)
            model.pages.add(startingPage)
        } else {
            pageAdapter.addPages(model.pages)
        }

        activityModel.newViewPagerPage.observe(viewLifecycleOwner, {
            if (it != null) {
                newPage(it)
                activityModel.newViewPagerPageObserved()
            }
        })

        binding?.fragmentViewPagerViewPager?.apply {
            adapter = pageAdapter
            isUserInputEnabled = model.isViewPagerSwipeEnabled
            offscreenPageLimit = 4
            setPageTransformer(SlidePageTransformer())
            (getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }

        model.syncViewPager.observe(viewLifecycleOwner, {
            if (it != null) {
                val viewPager = binding?.fragmentViewPagerViewPager
                val currentPage = binding?.fragmentViewPagerViewPager?.currentItem
                val isNotFirstPage = currentPage != 0
                pageAdapter.popFragmentsGreaterThanPosition(currentPage ?: 0)
                model.pages.subList((currentPage ?: -1) + 1, model.pages.size).clear()
                viewPager?.isUserInputEnabled = isNotFirstPage
                backPressedCallback.isEnabled = isNotFirstPage
                model.isViewPagerSwipeEnabled = isNotFirstPage
                model.notifyViewPagerObserved()
            }
        })
    }

    private fun initBackPressedCallback() {
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentPage = binding?.fragmentViewPagerViewPager?.currentItem ?: 1
                binding?.fragmentViewPagerViewPager?.setCurrentItem(currentPage - 1, true)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressedCallback
        )
    }

    private fun initOtherObservers() {
        model.swipeEnabled.observe(viewLifecycleOwner, {
            binding?.fragmentViewPagerViewPager?.isUserInputEnabled = it
        })

        model.navigateToPreviousPage.observe(viewLifecycleOwner, {
            if (it != null) {
                val currentPage = binding?.fragmentViewPagerViewPager?.currentItem ?: 1
                binding?.fragmentViewPagerViewPager?.setCurrentItem(currentPage - 1, true)
                model.navigateToPreviousPageObserved()
            }
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

    override fun listingSelected(postListing: PostListing) {
        if (postListing is ProfileListing) {
            accountSelected(postListing.user)
        } else {
            findNavController().navigate(
                ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                    ListingPage(postListing)
                )
            )
        }
    }

    override fun accountSelected(user: String?) {
        findNavController().navigate(
            ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                AccountPage(user)
            )
        )
    }

    override fun messagesSelected() {
        findNavController().navigate(
            ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                InboxPage
            )
        )
    }

    override fun signInNewAccount() {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSignInFragment())
    }

    override fun launchWebview(url: String) {
        activityModel.openChromeTab(url)
    }

    private fun newPage(page: ViewPagerPage) {
        pageAdapter.addPage(page)
        model.pages.add(page)
        val currentPage = binding?.fragmentViewPagerViewPager?.currentItem ?: 0
        binding?.fragmentViewPagerViewPager?.setCurrentItem(currentPage + 1, true)
    }

}