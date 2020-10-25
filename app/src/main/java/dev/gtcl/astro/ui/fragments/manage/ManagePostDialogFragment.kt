package dev.gtcl.astro.ui.fragments.manage

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

class ManagePostDialogFragment : DialogFragment() {

    private var binding: FragmentDialogManagePostBinding? = null

    private val model: ManagePostVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(ManagePostVM::class.java)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
//        dialog?.window?.setBackgroundDrawableResource(android.R.color.black) // This makes the dialog full screen
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDialogManagePostBinding.inflate(inflater)

        val post = requireArguments().get(POST_KEY) as Post
        binding?.post = post
        binding?.model = model
        binding?.lifecycleOwner = this
        if (post.flairText != null && post.linkFlairTemplateId != null) {
            val flair = Flair(
                post.flairText ?: return null, false,
                post.linkFlairTemplateId ?: return null
            )
            model.selectFlair(flair)
        }
        binding?.executePendingBindings()

        binding?.fragmentDialogManagePostFlairChip?.setOnClickListener {
            binding?.fragmentDialogManagePostFlairChip?.isChecked = model.flair.value != null
            FlairListDialogFragment.newInstance(post.subreddit).show(childFragmentManager, null)
        }

        childFragmentManager.setFragmentResultListener(FLAIR_SELECTED_KEY, this, { _, result ->
            val flair = result.get(FLAIRS_KEY) as Flair?
            model.selectFlair(flair)
        })

        binding?.fragmentDialogManagePostDialogButtons?.dialogPositiveButton?.setOnClickListener {
            val position = requireArguments().getInt(POSITION_KEY)
            val nsfw = binding?.fragmentDialogManagePostNsfwCheckbox?.isChecked ?: false
            val spoiler = binding?.fragmentDialogManagePostSpoilerCheckbox?.isChecked ?: false
            val getNotifications =
                binding?.fragmentDialogManagePostNotificationsCheckbox?.isChecked ?: false
            val flair = model.flair.value
            val bundle = bundleOf(
                POSITION_KEY to position,
                NSFW_KEY to nsfw,
                SPOILER_KEY to spoiler,
                GET_NOTIFICATIONS_KEY to getNotifications,
                FLAIRS_KEY to flair
            )
            parentFragmentManager.setFragmentResult(MANAGE_POST_KEY, bundle)
            dismiss()
        }

        binding?.fragmentDialogManagePostDialogButtons?.dialogNegativeButton?.setOnClickListener {
            dismiss()
        }

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        fun newInstance(post: Post, position: Int = -1): ManagePostDialogFragment {
            return ManagePostDialogFragment().apply {
                arguments = bundleOf(POST_KEY to post, POSITION_KEY to position)
            }
        }
    }
}