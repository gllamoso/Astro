package dev.gtcl.reddit.ui.subreddits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.databinding.FragmentSubredditsBinding
import dev.gtcl.reddit.ui.main.MainActivity
import dev.gtcl.reddit.ui.main.MainActivityViewModel

class SubredditSelectorFragment: Fragment() {

    val model: SubredditSelectorViewModel by lazy {
        val viewModelFactory = SubredditSelectorViewModelFactory(activity!!.application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(SubredditSelectorViewModel::class.java)
    }

    private val parentViewModel: MainActivityViewModel by lazy {
        (activity as MainActivity).model
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentSubredditsBinding.inflate(inflater)
        binding.lifecycleOwner = this

        model.setAccessToken(parentViewModel.accessToken)
        model.getTrendingPosts()
        model.getDefaultSubreddits()
        model.getPopularPosts()

        binding.viewPager.adapter = PageAdapter(childFragmentManager)
        binding.viewPager.offscreenPageLimit = 4
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        binding.tabLayout.getTabAt(0)!!.text = getText(R.string.mine_tab_label)
        binding.tabLayout.getTabAt(1)!!.text = getText(R.string.trending_tab_label)
        binding.tabLayout.getTabAt(2)!!.text = getText(R.string.popular_tab_label)
        binding.tabLayout.getTabAt(3)!!.text = getText(R.string.search_tab_label)

//        model.subredditSelection.observe(this, Observer {
//            if(it != null){
//                (activity as MainActivity).selectSubreddit(it)
//                findNavController().navigateUp()
//                model.subredditSelected()
//            }
//        })

        return binding.root
    }

}
