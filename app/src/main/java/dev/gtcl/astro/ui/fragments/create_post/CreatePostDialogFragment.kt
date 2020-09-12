package dev.gtcl.astro.ui.fragments.create_post

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.tabs.TabLayoutMediator
import dev.gtcl.astro.*
import dev.gtcl.astro.databinding.FragmentDialogCreatePostBinding
import dev.gtcl.astro.models.reddit.listing.Flair
import dev.gtcl.astro.models.reddit.listing.Post
import dev.gtcl.astro.ui.fragments.view_pager.ViewPagerVM
import dev.gtcl.astro.ui.fragments.create_post.resubmit.ResubmitDialogFragment
import dev.gtcl.astro.ui.fragments.flair.FlairListDialogFragment
import dev.gtcl.astro.ui.fragments.rules.RulesDialogFragment
import java.util.*
import kotlin.NoSuchElementException

class CreatePostDialogFragment : DialogFragment() {

    private var binding: FragmentDialogCreatePostBinding? = null

    private val model: CreatePostVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(CreatePostVM::class.java)
    }

    private val viewPagerModel: ViewPagerVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(ViewPagerVM::class.java)
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
        binding = FragmentDialogCreatePostBinding.inflate(inflater)
        binding?.model = model
        binding?.lifecycleOwner = viewLifecycleOwner
        initSubredditText()
        val crossPost = arguments?.get(POST_KEY) as Post?
        if (crossPost != null) {
            initCrosspost(crossPost)
        } else {
            initViewPagerAdapter()
        }
        initToolbar()
        initObservers()

        binding?.invalidateAll()
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.fragmentDialogCreatePostViewPager?.adapter = null
        binding = null
    }

    private fun initSubredditText() {
        val subredditName = requireArguments().getString(SUBREDDIT_KEY)
        binding?.fragmentDialogCreatePostRulesButton?.isEnabled = subredditName != null
        binding?.fragmentDialogCreatePostSubredditText?.setText(subredditName)
        model.searchSubreddits(subredditName ?: "")
        model.setSubredditIsValid(subredditName != null)

        binding?.fragmentDialogCreatePostSubredditText?.addTextChangedListener(object :
            TextWatcher {

            private var timer = Timer()
            private val DELAY = 500L

            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                model.searchSubreddits(s.toString())
                timer.cancel()
                model.selectFlair(null)
                timer = Timer().apply {
                    schedule(object : TimerTask() {
                        override fun run() {
                            model.validateSubreddit(s.toString())
                        }
                    }, DELAY)
                }
            }
        })

        binding?.fragmentDialogCreatePostRulesButton?.setOnClickListener {
            RulesDialogFragment.newInstance(binding?.fragmentDialogCreatePostSubredditText?.text.toString())
                .show(childFragmentManager, null)
        }
    }

    private fun initViewPagerAdapter() {
        val adapter = CreatePostStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        val viewPager = binding?.fragmentDialogCreatePostViewPager
        val tabLayout = binding?.fragmentDialogCreatePostTabLayout
        viewPager?.adapter = adapter
        if (viewPager != null && tabLayout != null) {
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = getText(
                    when (position) {
                        0 -> R.string.text
                        1 -> R.string.image
                        2 -> R.string.link
                        else -> throw NoSuchElementException("No such tab in the following position: $position")
                    }
                )
            }.attach()
        }
    }

    private fun initCrosspost(post: Post) {
        binding?.crossPost = post
        binding?.fragmentDialogCreatePostNsfwChip?.isChecked = post.nsfw
        binding?.fragmentDialogCreatePostSpoilerChip?.isChecked = post.spoiler
    }

    private fun initToolbar() {
        val toolbar = binding?.fragmentDialogCreatePostToolbar

        toolbar?.setNavigationOnClickListener {
            dismiss()
        }

        toolbar?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.send -> {
                    var valid = true

                    if (binding?.fragmentDialogCreatePostSubredditText?.text.isNullOrEmpty()) {
                        binding?.fragmentDialogCreatePostSubredditTextInputLayout?.error =
                            getText(R.string.required)
                        valid = false
                    } else if (model.subredditValid.value != true) {
                        binding?.fragmentDialogCreatePostSubredditTextInputLayout?.error =
                            getText(R.string.invalid)
                        valid = false
                    }

                    if (binding?.fragmentDialogCreatePostTitleText?.text.isNullOrEmpty()) {
                        binding?.fragmentDialogCreatePostTitleTextInputLayout?.error =
                            getText(R.string.required)
                        valid = false
                    } else if (binding?.fragmentDialogCreatePostTitleText?.text.toString().length > 300) {
                        binding?.fragmentDialogCreatePostTitleTextInputLayout?.error =
                            getText(R.string.invalid)
                        valid = false
                    }

                    if (valid) {
                        val crossPost = arguments?.get(POST_KEY) as Post?
                        if (crossPost != null) {
                            val sub =
                                binding?.fragmentDialogCreatePostSubredditText?.text.toString()
                                    .trim()
                            val title = binding?.fragmentDialogCreatePostTitleText?.text.toString()
                            val notifications =
                                binding?.fragmentDialogCreatePostGetNotificationsChip?.isChecked
                                    ?: false
                            val nsfw = binding?.fragmentDialogCreatePostNsfwChip?.isChecked ?: false
                            val spoiler =
                                binding?.fragmentDialogCreatePostSpoilerChip?.isChecked ?: false
                            model.submitCrosspost(
                                sub,
                                title,
                                notifications,
                                nsfw,
                                spoiler,
                                crossPost
                            )
                        } else {
                            model.fetchData()
                        }

                    }
                }
            }
            true
        }
    }

    private fun initObservers() {
        model.subredditValid.observe(viewLifecycleOwner, {
            binding?.fragmentDialogCreatePostRulesButton?.isEnabled = it
        })

        model.subredditSuggestions.observe(viewLifecycleOwner, {
            val adapter =
                ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, it)
            binding?.fragmentDialogCreatePostSubredditText?.setAdapter(adapter)
        })

        model.errorMessage.observe(viewLifecycleOwner, {
            if (it != null) {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                model.errorMessageObserved()
            }
        })

        binding?.fragmentDialogCreatePostSubredditText?.addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding?.fragmentDialogCreatePostSubredditTextInputLayout?.error = null
                binding?.fragmentDialogCreatePostFlairChip?.apply {
                    text = getString(R.string.flair)
                    isChecked = false
                }
                model.selectFlair(null)
            }
        })

        binding?.fragmentDialogCreatePostTitleText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding?.fragmentDialogCreatePostTitleTextInputLayout?.error = null
            }
        })

        childFragmentManager.setFragmentResultListener(
            FLAIR_SELECTED_KEY,
            viewLifecycleOwner,
            { _, result ->
                model.selectFlair(result.get(FLAIRS_KEY) as Flair?)
            })

        binding?.fragmentDialogCreatePostFlairChip?.setOnClickListener {
            binding?.fragmentDialogCreatePostFlairChip?.isChecked = model.flair.value != null
            if (model.subredditValid.value == true) {
                FlairListDialogFragment.newInstance(binding?.fragmentDialogCreatePostSubredditText?.text.toString())
                    .show(childFragmentManager, null)
            }
        }

        model.postContent.observe(viewLifecycleOwner, { postContent ->
            postContent?.let {
                val sub = binding?.fragmentDialogCreatePostSubredditText?.text.toString().trim()
                val title = binding?.fragmentDialogCreatePostTitleText?.text.toString()
                val notifications =
                    binding?.fragmentDialogCreatePostGetNotificationsChip?.isChecked ?: false
                val nsfw = binding?.fragmentDialogCreatePostNsfwChip?.isChecked ?: false
                val spoiler = binding?.fragmentDialogCreatePostSpoilerChip?.isChecked ?: false
                when (it) {
                    is TextPost -> {
                        model.submitTextPost(
                            sub,
                            title,
                            it.body,
                            notifications,
                            nsfw,
                            spoiler
                        )
                    }
                    is ImagePost -> {
                        model.submitPhotoPost(
                            sub,
                            title,
                            it.uri,
                            notifications,
                            nsfw,
                            spoiler
                        )
                    }
                    is LinkPost -> {
                        model.submitUrlPost(
                            sub,
                            title,
                            it.url,
                            notifications,
                            nsfw,
                            spoiler
                        )
                    }
                }
                model.postContentObserved()
            }
        })

        model.urlResubmit.observe(viewLifecycleOwner, {
            if (it != null) {
                ResubmitDialogFragment.newInstance(it).show(childFragmentManager, null)
                model.urlResubmitObserved()
            }
        })

        childFragmentManager.setFragmentResultListener(URL_KEY, this) { _, bundle ->
            val resubmitUrl = bundle.getString(URL_KEY)
            if (resubmitUrl != null) {
                val sub = binding?.fragmentDialogCreatePostSubredditText?.text.toString()
                val title = binding?.fragmentDialogCreatePostTitleText?.text.toString()
                model.submitUrlPost(
                    sub,
                    title,
                    resubmitUrl,
                    binding?.fragmentDialogCreatePostGetNotificationsChip?.isChecked ?: false,
                    binding?.fragmentDialogCreatePostNsfwChip?.isChecked ?: false,
                    binding?.fragmentDialogCreatePostSpoilerChip?.isChecked ?: false,
                    true
                )
            }
        }

        model.newPostData.observe(viewLifecycleOwner, { newPostData ->
            if (newPostData != null) {
                VALID_REDDIT_COMMENTS_URL_REGEX.find(newPostData.url)?.value?.let {
                    viewPagerModel.newPost(it)
                }
                model.newPostObserved()
                dismiss()
            }
        })

        model.loading.observe(viewLifecycleOwner, {
            dialog?.setCancelable(!it)
        })
    }

    companion object {
        fun newInstance(subredditName: String?): CreatePostDialogFragment {
            return CreatePostDialogFragment().apply {
                arguments = bundleOf(SUBREDDIT_KEY to subredditName)
            }
        }

        fun newInstance(crossPost: Post): CreatePostDialogFragment {
            return CreatePostDialogFragment().apply {
                arguments = bundleOf(POST_KEY to crossPost)
            }
        }
    }
}