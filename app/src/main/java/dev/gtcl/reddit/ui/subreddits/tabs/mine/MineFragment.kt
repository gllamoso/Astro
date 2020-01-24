package dev.gtcl.reddit.ui.subreddits.tabs.mine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.gtcl.reddit.databinding.FragmentSubredditsMineBinding
import dev.gtcl.reddit.ui.subreddits.SubredditSelectorFragment

class MineFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentSubredditsMineBinding.inflate(inflater)

        val model = (parentFragment as SubredditSelectorFragment).model
        binding.viewModel = model
        binding.lifecycleOwner = this

        // Set adapter
        val adapter = SubredditsListAdapter(SubredditsListAdapter.OnClickListener {
            model.selectSubreddit(it)
        })
        
        binding.list.adapter = adapter
        return binding.root
    }

}
