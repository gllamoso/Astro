package dev.gtcl.astro.ui.fragments.multireddits

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.astro.*
import dev.gtcl.astro.databinding.FragmentMultiredditSubredditsBinding
import dev.gtcl.astro.models.reddit.listing.MultiRedditUpdate
import dev.gtcl.astro.models.reddit.listing.Subreddit

class MultiRedditFragment : Fragment(),
    MultiRedditSubredditsAdapter.OnSubredditRemovedListener {

    private var binding: FragmentMultiredditSubredditsBinding? = null
    private lateinit var navController: NavController
    private lateinit var adapter: MultiRedditSubredditsAdapter

    private val model: MultiRedditVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(MultiRedditVM::class.java)
    }

    private val args: MultiRedditFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMultiredditSubredditsBinding.inflate(inflater)
        binding?.model = model
        binding?.lifecycleOwner = viewLifecycleOwner
        navController = findNavController()
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Glide.get(requireContext()).clearMemory()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        if (!model.initialized) {
            val multiReddit = args.multiReddit
            model.fetchMultiReddit(multiReddit)
        }

        adapter = MultiRedditSubredditsAdapter(this)
        binding?.fragmentMultiRedditSubredditsList?.adapter = adapter

        binding?.fragmentMultiRedditSubredditsToolbar?.setNavigationOnClickListener {
            navController.popBackStack()
        }

        binding?.fragmentMultiRedditSubredditsFab?.setOnClickListener {
            navController.navigate(
                MultiRedditFragmentDirections.actionMultiRedditFragmentToSearchFragment(true)
            )
        }

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<List<String>>(
            SELECTED_SUBREDDITS_KEY
        )?.observe(
            viewLifecycleOwner, {
                model.addSubredditsToMultiReddit(it)
            }
        )

        model.errorMessage.observe(viewLifecycleOwner, { errorMessage ->
            if (errorMessage != null) {
                binding?.root?.let {
                    Snackbar.make(it, errorMessage, Snackbar.LENGTH_LONG).show()
                }
                model.errorMessageObserved()
            }

        })

        binding?.fragmentMultiRedditSubredditsToolbar?.setOnMenuItemClickListener {
            if (it.itemId == R.id.edit) {
                if (model.multi.value != null) {
                    MultiRedditDetailsDialogFragment.newInstance(model.multi.value!!)
                        .show(childFragmentManager, null)
                }
            }
            true
        }

        childFragmentManager.setFragmentResultListener(MULTI_KEY, viewLifecycleOwner, { _, bundle ->
            val multiUpdate = bundle.get(MULTI_KEY) as MultiRedditUpdate
            model.updateMultiReddit(multiUpdate)
        })
    }

    override fun onRemove(subreddit: Subreddit, position: Int) {
        model.remove(subreddit, position)
        adapter.notifyItemRemoved(position)
    }
}