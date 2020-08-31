package dev.gtcl.astro.ui.fragments.manage

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.astro.*
import dev.gtcl.astro.databinding.FragmentDialogManagePostBinding
import dev.gtcl.astro.models.reddit.listing.Flair
import dev.gtcl.astro.models.reddit.listing.Post
import dev.gtcl.astro.ui.fragments.flair.FlairListDialogFragment

class ManagePostDialogFragment: DialogFragment() {

    private lateinit var binding: FragmentDialogManagePostBinding

    private val model: ManagePostVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(ManagePostVM::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
            .setTitle(R.string.manage)
            .setPositiveButton(R.string.done){ _, _ ->
                val position = requireArguments().getInt(POSITION_KEY)
                val nsfw = binding.fragmentDialogManagePostNsfwCheckbox.isChecked
                val spoiler = binding.fragmentDialogManagePostSpoilerCheckbox.isChecked
                val getNotifications = binding.fragmentDialogManagePostNotificationsCheckbox.isChecked
                val flair = model.flair.value
                val bundle = bundleOf(
                    POSITION_KEY to position,
                    NSFW_KEY to nsfw,
                    SPOILER_KEY to spoiler,
                    GET_NOTIFICATIONS_KEY to getNotifications,
                    FLAIRS_KEY to flair
                )
                parentFragmentManager.setFragmentResult(MANAGE_POST_KEY, bundle)
            }
            .setNegativeButton(R.string.cancel){_,_ ->}

        binding = FragmentDialogManagePostBinding.inflate(LayoutInflater.from(requireContext()))
        builder.setView(binding.root)

        return builder.create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val post = requireArguments().get(POST_KEY) as Post
        binding.post = post
        binding.model = model
        binding.lifecycleOwner = this
        if(post.flairText != null && post.linkFlairTemplateId != null){
            val flair = Flair(post.flairText!!, false, post.linkFlairTemplateId!!)
            model.selectFlair(flair)
        }
        binding.executePendingBindings()

        binding.fragmentDialogManagePostFlairChip.setOnClickListener {
            binding.fragmentDialogManagePostFlairChip.isChecked = model.flair.value != null
            FlairListDialogFragment.newInstance(post.subreddit).show(childFragmentManager, null)
        }

        childFragmentManager.setFragmentResultListener(FLAIR_SELECTED_KEY, this, { _, result ->
            val flair = result.get(FLAIRS_KEY) as Flair?
            model.selectFlair(flair)
        })

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    companion object{
        fun newInstance(post: Post, position: Int = -1): ManagePostDialogFragment{
            return ManagePostDialogFragment().apply {
                arguments = bundleOf(POST_KEY to post, POSITION_KEY to position)
            }
        }
    }
}