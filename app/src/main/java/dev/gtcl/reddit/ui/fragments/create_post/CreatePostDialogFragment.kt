package dev.gtcl.reddit.ui.fragments.create_post

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentDialogCreatePostBinding
import dev.gtcl.reddit.models.reddit.listing.Flair
import dev.gtcl.reddit.ui.fragments.ContinueThreadPage
import dev.gtcl.reddit.ui.fragments.ViewPagerVM
import dev.gtcl.reddit.ui.fragments.create_post.flair.FlairSelectionDialogFragment
import java.util.*
import kotlin.NoSuchElementException

class CreatePostDialogFragment : DialogFragment(){

    private lateinit var binding: FragmentDialogCreatePostBinding

    private val model: CreatePostVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(CreatePostVM::class.java)
    }

    private val viewPagerModel: ViewPagerVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(ViewPagerVM::class.java)
    }

    override fun onStart() {
        super.onStart()

        dialog?.let {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            it.window?.setLayout(width, height)
        }

//        dialog?.window?.setBackgroundDrawableResource(android.R.color.black) // This makes the dialog full screen
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDialogCreatePostBinding.inflate(inflater)
        binding.model = model
        binding.lifecycleOwner = viewLifecycleOwner
        initSubredditText()
        initViewPagerAdapter()
        initToolbar()
        initObservers()

        return binding.root
    }

    private fun initSubredditText(){
        val subredditName = requireArguments().getString(SUBREDDIT_KEY)
        binding.rulesButton.isEnabled = subredditName != null
        binding.subredditText.setText(subredditName)
        model.searchSubreddits(subredditName ?: "")

        binding.subredditText.addTextChangedListener(object: TextWatcher{

            private var timer = Timer()
            private val DELAY = 500L

            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                model.searchSubreddits(s.toString())
                timer.cancel()
                binding.flairChip.text = getString(R.string.flair)
                binding.flairChip.isChecked = false
                timer = Timer().apply {
                    schedule(object: TimerTask(){
                        override fun run() {
                            model.validateSubreddit(s.toString())
                        }
                    }, DELAY)
                }
            }
        })

        binding.rulesButton.setOnClickListener {
            model.fetchRules(binding.subredditText.text.toString())
        }
    }

    private fun initViewPagerAdapter(){
        val viewPager = binding.viewPager
        val tabLayout = binding.tabLayout
        val adapter = CreatePostStateAdapter(this)
        viewPager.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager){ tab, position ->
            tab.text = getText(when(position){
                0 -> R.string.text
                1 -> R.string.image
                2 -> R.string.link
                else -> throw NoSuchElementException("No such tab in the following position: $position")
            })
        }.attach()
    }

    private fun initToolbar(){
        val toolbar = binding.toolbar

        toolbar.setNavigationOnClickListener {
            dismiss()
        }

        toolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.send -> {
                    var fetchData = true

                    if(binding.subredditText.text.isNullOrEmpty()){
                        binding.subredditTextInputLayout.error = getText(R.string.required)
                        fetchData = false
                    } else if(model.subredditValid.value != true){
                        binding.subredditTextInputLayout.error = getText(R.string.invalid)
                        fetchData = false
                    }

                    if(binding.titleText.text.isNullOrEmpty()){
                        binding.titleTextInputLayout.error = getText(R.string.required)
                        fetchData = false
                    } else if(binding.titleText.text.toString().length > 300){
                        binding.titleTextInputLayout.error = getText(R.string.invalid)
                        fetchData = false
                    }

                    if(fetchData){
                        model.fetchData()
                    }
                }
            }
            true
        }
    }

    private fun initObservers(){
        model.subredditValid.observe(viewLifecycleOwner, Observer {
            binding.rulesButton.isEnabled = it
        })

        model.subredditSuggestions.observe(viewLifecycleOwner, Observer {
            val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line, it)
            binding.subredditText.setAdapter(adapter)
        })

        model.errorMessage.observe(viewLifecycleOwner, Observer {
            Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
        })

        model.rules.observe(viewLifecycleOwner, Observer {
            if(it != null){
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle(R.string.rules)
                builder.setMessage(it)
                builder.setPositiveButton(R.string.done) { dialog, _ -> dialog?.dismiss() }
                builder.create().show()
                model.rulesObserved()
            }
        })

        binding.subredditText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.subredditTextInputLayout.error = null
                binding.flairChip.apply {
                    text = getString(R.string.flair)
                    isChecked = false
                }
                model.selectFlair(null)
            }
        })

        binding.titleText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.titleTextInputLayout.error = null
            }
        })

        binding.flairChip.setOnClickListener {
            binding.flairChip.isChecked = false
            if(model.subredditValid.value == true){
                model.fetchFlairs(binding.subredditText.text.toString())

                childFragmentManager.setFragmentResultListener(FLAIR_SELECTED_KEY, viewLifecycleOwner, FragmentResultListener { _, result ->
                    model.selectFlair(result.get(FLAIRS_KEY) as Flair?)
                })

                childFragmentManager.setFragmentResultListener(FLAIR_EDIT_KEY, viewLifecycleOwner, FragmentResultListener { _, result ->
                    model.selectFlair(result.get(FLAIRS_KEY) as Flair)
                })
            }
        }

        model.flairs.observe(viewLifecycleOwner, Observer {
            if(it != null && model.subredditValid.value == true){
                if(it.isNotEmpty()){
                    FlairSelectionDialogFragment.newInstance(it, binding.subredditText.text.toString()).show(childFragmentManager, null)
                } else {
                    Snackbar.make(binding.root, getString(R.string.no_flair_found), Snackbar.LENGTH_LONG).show()
                }
                model.flairsObserved()
                binding.flairChip.isChecked = false
            }
        })

        model.postContent.observe(viewLifecycleOwner, Observer { postContent ->
            postContent?.let {
                val sub = binding.subredditText.text.toString()
                val title = binding.titleText.text.toString()
                when(it){
                    is TextPost -> {
                        model.submitTextPost(
                            sub,
                            title,
                            it.body,
                            binding.getNotificationsChip.isChecked,
                            binding.nsfwChip.isChecked,
                            binding.spoilerChip.isChecked)
                    }
                    is ImagePost ->{
                        model.submitPhotoPost(
                            sub,
                            title,
                            it.uri,
                            binding.getNotificationsChip.isChecked,
                            binding.nsfwChip.isChecked,
                            binding.spoilerChip.isChecked)
                    }
                    is LinkPost -> {
                        model.submitUrlPost(
                            sub,
                            title,
                            it.url,
                            binding.getNotificationsChip.isChecked,
                            binding.nsfwChip.isChecked,
                            binding.spoilerChip.isChecked)
                    }
                }
                model.postContentObserved()
            }
        })

        model.urlResubmit.observe(viewLifecycleOwner, Observer {
            if(it != null){
                val builder = AlertDialog.Builder(requireContext())
                builder.setMessage(R.string.url_already_posted)
                builder.setPositiveButton(R.string.done) { dialog, _ ->
                    val sub = binding.subredditText.text.toString()
                    val title = binding.titleText.text.toString()
                    model.submitUrlPost(
                        sub,
                        title,
                        it,
                        binding.getNotificationsChip.isChecked,
                        binding.nsfwChip.isChecked,
                        binding.spoilerChip.isChecked,
                    true)
                    dialog?.dismiss()
                }
                builder.create().show()
                model.urlResubmitObserved()
            }
        })

        model.newPostData.observe(viewLifecycleOwner, Observer {
            if(it != null){
                viewPagerModel.newPage(ContinueThreadPage(it.url))
                model.newPostObserved()
                dismiss()
            }
        })
    }

    companion object{
        fun newInstance(subredditName: String?): CreatePostDialogFragment {
            return CreatePostDialogFragment().apply {
                arguments = bundleOf(SUBREDDIT_KEY to subredditName)
            }
        }
    }
}