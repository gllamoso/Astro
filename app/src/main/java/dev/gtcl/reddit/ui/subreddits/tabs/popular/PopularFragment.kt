package dev.gtcl.reddit.ui.subreddits.tabs.popular

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.gtcl.reddit.databinding.FragmentSubredditsPopularBinding
import dev.gtcl.reddit.ui.subreddits.SubredditSelectorFragment

class PopularFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentSubredditsPopularBinding.inflate(inflater)
        val model = (parentFragment as SubredditSelectorFragment).model
        binding.lifecycleOwner = this
        binding.viewModel = model
        binding.list.adapter = SubredditsPageListAdapter(
            SubredditsPageListAdapter.OnClickListener {model.selectSubreddit(it)})
            {model.retryPopular()}

        return binding.root
    }

}