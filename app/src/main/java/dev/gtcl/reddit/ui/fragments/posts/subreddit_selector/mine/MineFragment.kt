package dev.gtcl.reddit.ui.fragments.posts.subreddit_selector.mine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.gtcl.reddit.databinding.FragmentSubredditsMineBinding
import dev.gtcl.reddit.ui.fragments.MainFragment

class MineFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentSubredditsMineBinding.inflate(inflater)

        val model = (parentFragment!!.parentFragment as MainFragment).model
        binding.viewModel = model
        binding.lifecycleOwner = this

        // Set adapter
        val adapter = SubredditsListAdapter(SubredditsListAdapter.OnClickListener {
            model.getPosts(it)
        })

        binding.list.adapter = adapter
        return binding.root
    }

}
