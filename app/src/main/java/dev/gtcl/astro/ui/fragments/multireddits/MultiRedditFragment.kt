package dev.gtcl.astro.ui.fragments.multireddits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.R
import dev.gtcl.astro.SELECTED_SUBREDDITS_KEY
import dev.gtcl.astro.ViewModelFactory
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.databinding.FragmentMultiredditSubredditsBinding
import dev.gtcl.astro.models.reddit.listing.Item
import dev.gtcl.astro.models.reddit.listing.Subreddit
import dev.gtcl.astro.models.reddit.listing.SubredditListing
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.view_pager.ListingPage
import dev.gtcl.astro.ui.viewholders.OnSubredditRemovedListener

class MultiRedditFragment : Fragment(),
    ItemClickListener,
    OnSubredditRemovedListener {

    private var binding: FragmentMultiredditSubredditsBinding? = null
    private lateinit var navController: NavController
    private lateinit var adapter: MultiRedditSubredditsAdapter

    private val activityModel: MainActivityVM by activityViewModels()

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
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        if (!model.initialized) {
            model.fetchMultiReddit(args.path)
        }

        adapter = MultiRedditSubredditsAdapter(this, this)
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
                model.addSubredditsToMultiReddit(args.path, it)
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
                    MultiRedditCreationDialogFragment.newInstance(model.multi.value!!, false)
                        .show(childFragmentManager, null)
                }
            }
            true
        }

        activityModel.newMulti.observe(viewLifecycleOwner, {
            if (it != null) {
                model.updateMulti(it)
                activityModel.newMultiObserved()
            }
        })
    }

    override fun onRemove(subreddit: Subreddit, position: Int) {
        model.remove(args.path, subreddit, position)
        adapter.notifyItemRemoved(position)
    }

    override fun itemClicked(item: Item, position: Int) {
        if (item is Subreddit) {
            findNavController().navigate(
                MultiRedditFragmentDirections.actionMultiRedditFragmentToViewPagerFragment(
                    ListingPage(SubredditListing(item.displayName))
                )
            )
        }
    }

    override fun itemLongClicked(item: Item, position: Int) {} // Unused


}