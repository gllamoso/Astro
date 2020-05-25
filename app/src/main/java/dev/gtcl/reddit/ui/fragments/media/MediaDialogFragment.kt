package dev.gtcl.reddit.ui.fragments.media

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentDialogMediaBinding
import dev.gtcl.reddit.models.reddit.Post
import dev.gtcl.reddit.models.reddit.UrlType


class MediaDialogFragment: DialogFragment() {

    private lateinit var binding: FragmentDialogMediaBinding
    lateinit var postUrlCallback: (Post) -> Unit
    private var playerControlViewPassed = false // PlayerControlView is sometimes passed onAttachFragment or onCreateView

    val model: MediaDialogVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(MediaDialogVM::class.java)
    }

    override fun onStart() {
        super.onStart()

        dialog?.let {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            it.window?.setLayout(width, height)
        }
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        if(childFragment is MediaFragment) {
            childFragment.showUiCallback = this::showOrHideUi
            childFragment.postUrlCallback = {
                postUrlCallback(it)
                dismiss()
            }
            if(::binding.isInitialized){
                childFragment.controllerView = binding.bottomBarControls.playerController
                playerControlViewPassed = true
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDialogMediaBinding.inflate(inflater)
        binding.model = model
        if(!model.initialized){
            model.setUrlType(requireArguments().get(URL_TYPE_KEY) as UrlType)
            model.setPost(requireArguments().get(POST_KEY) as Post?)
            model.setShowUi(true)
            model.url = requireArguments().get(URL_KEY) as String
            model.initialized = true
        }

        setViewPager()
        setOnClickListeners()
        setObservers()

        binding.executePendingBindings()
        if(!playerControlViewPassed){
            setControllerView()
        }

        return binding.root
    }

    private fun setControllerView(){
        for(fragment: Fragment in childFragmentManager.fragments){ // On orientation change, child fragment might be created before this fragment
            if(fragment is MediaFragment) {
                fragment.controllerView = binding.bottomBarControls.playerController
                playerControlViewPassed = true
                break
            }
        }
    }

    private fun setObservers(){
        model.urlType.observe(viewLifecycleOwner, Observer {
            dialog?.window?.setBackgroundDrawableResource(
                if(it == UrlType.IMAGE || it == UrlType.GIF){
                    R.color.darkTransparent
                } else {
                    android.R.color.black
                }
            )
        })

        model.showUi.observe(viewLifecycleOwner, Observer {
            if(model.urlType.value == UrlType.GFYCAT || model.urlType.value == UrlType.M3U8){
                if(it){
                    binding.bottomBarControls.playerController.show()
                } else {
                    binding.bottomBarControls.playerController.hide()
                }
            }
            binding.invalidateAll()
        })
    }

    private fun setViewPager(){
        binding.viewPager.apply {
            adapter = MediaAdapter(this@MediaDialogFragment, model.url, model.urlType.value!!, model.post.value)
            currentItem = 1
            orientation = ViewPager2.ORIENTATION_VERTICAL
            registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    if(state == ViewPager2.SCROLL_STATE_IDLE){
                        if(currentItem != 1)
                            dismiss()
                    }
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                    super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                    val offset = if(position == 0) {
                        1 - positionOffset
                    } else {
                        positionOffset
                    }
                    val multiplier = 500
                    binding.topToolbar.translationY = -multiplier * offset
                    binding.bottomBarControls.root.translationY = multiplier * offset
                }
            })
        }
    }

    private fun setOnClickListeners(){

        binding.topToolbar.setNavigationOnClickListener {
            dismiss()
        }

        binding.bottomBarControls.apply {
            commentButton.setOnClickListener {
                val post = requireArguments().get(POST_KEY) as Post
                postUrlCallback(post)
                dismiss()
            }
            shareButton.setOnClickListener {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.share_subject_message))
                shareIntent.putExtra(Intent.EXTRA_TEXT, requireArguments().get(URL_KEY) as String)
                startActivity(Intent.createChooser(shareIntent, null))
            }
            downloadButton.setOnClickListener {
                model.download()
            }
        }
    }

    private fun showOrHideUi(){
        model.alternateShowUi()
    }

    companion object{
        fun newInstance(url: String, urlType: UrlType, post: Post?): MediaDialogFragment{
            val fragment = MediaDialogFragment()
            val args = bundleOf(URL_KEY to url, URL_TYPE_KEY to urlType, POST_KEY to post)
            fragment.arguments = args
            return fragment
        }
    }
}