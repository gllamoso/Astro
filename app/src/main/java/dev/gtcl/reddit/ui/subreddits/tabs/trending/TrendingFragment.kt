package dev.gtcl.reddit.ui.subreddits.tabs.trending

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.gtcl.reddit.databinding.FragmentSubredditsTrendingBinding
import dev.gtcl.reddit.ui.subreddits.SubredditSelectorFragment

class TrendingFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentSubredditsTrendingBinding.inflate(inflater)

        val model = (parentFragment as SubredditSelectorFragment).model
        binding.viewModel = model
        binding.lifecycleOwner = this

        val adapter = TrendingAdapter(TrendingAdapter.OnClickListener {
            model.selectSubreddit(it)
        })

        binding.list.adapter = adapter

        return binding.root
    }

//    override fun onStart() {
//        super.onStart()
//        Log.d("TAE", "TrendingFragment started")
//    }
//
//    override fun onResume() {
//        super.onResume()
//        Log.d("TAE", "TrendingFragment resumed")
//    }
//
//    override fun onPause() {
//        super.onPause()
//        Log.d("TAE", "TrendingFragment paused")
//    }
//
//    override fun onStop() {
//        super.onStop()
//        Log.d("TAE", "TrendingFragment stopped")
//    }
}