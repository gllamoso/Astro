package dev.gtcl.reddit.ui.fragments.multireddits

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentMultiredditSubredditsBinding
import dev.gtcl.reddit.models.reddit.listing.MultiRedditUpdate
import dev.gtcl.reddit.models.reddit.listing.Subreddit

class MultiRedditFragment: Fragment(),
    MultiRedditSubredditsAdapter.OnSubredditRemovedListener {

    private lateinit var binding: FragmentMultiredditSubredditsBinding
    private lateinit var navController: NavController
    private lateinit var adapter: MultiRedditSubredditsAdapter

    private val model: MultiRedditVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(MultiRedditVM::class.java)
    }

    private val args: MultiRedditFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMultiredditSubredditsBinding.inflate(inflater)
        binding.model = model
        binding.lifecycleOwner = viewLifecycleOwner
        navController = findNavController()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        if(!model.initialized){
            val multiReddit = args.multiReddit
            model.fetchMultiReddit(multiReddit)
        }

        adapter = MultiRedditSubredditsAdapter(this)
        binding.fragmentMultiRedditSubredditsList.adapter = adapter


        binding.fragmentMultiRedditSubredditsToolbar.setNavigationOnClickListener {
            navController.popBackStack()
        }

        binding.fragmentMultiRedditSubredditsFab.setOnClickListener {
            navController.navigate(
                MultiRedditFragmentDirections.actionMultiRedditFragmentToSearchFragment(true)
            )
        }

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<List<String>>(SELECTED_SUBREDDITS_KEY)?.observe(
            viewLifecycleOwner, {
                model.addSubredditsToMultiReddit(it)
            }
        )

        model.errorMessage.observe(viewLifecycleOwner, {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        })

        binding.fragmentMultiRedditSubredditsToolbar.setOnMenuItemClickListener {
            if(it.itemId == R.id.edit){
                if(model.multi.value != null){
                    MultiRedditDetailsDialogFragment.newInstance(model.multi.value!!).show(childFragmentManager, null)
                }
            }
            true
        }

        childFragmentManager.setFragmentResultListener(MULTI_KEY, viewLifecycleOwner, FragmentResultListener{ _, bundle ->
            val multiUpdate = bundle.get(MULTI_KEY) as MultiRedditUpdate
            model.updateMultiReddit(multiUpdate)
        })
    }

    override fun onRemove(subreddit: Subreddit, position: Int) {
        model.remove(subreddit, position)
        adapter.notifyItemRemoved(position)
    }
}