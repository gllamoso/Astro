package dev.gtcl.reddit.ui.fragments.media

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentDialogMediaBinding
import dev.gtcl.reddit.models.reddit.listing.Post
import dev.gtcl.reddit.models.reddit.listing.UrlType
import java.lang.ref.WeakReference


class MediaDialogFragment: DialogFragment() {

    private lateinit var binding: FragmentDialogMediaBinding
    private var onPostClicked: ((Post) -> Unit)? = null

    val model: MediaVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(MediaVM::class.java)
    }

    fun setActions(onPostClicked: (Post) -> Unit){
        this.onPostClicked = onPostClicked
    }

    override fun onStart() {
        super.onStart()

        dialog?.let {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            it.window?.setLayout(width, height)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDialogMediaBinding.inflate(inflater)
        binding.model = model
        binding.lifecycleOwner = this

        if(!model.initialized){
            val args = requireArguments()
            val url = args.getString(URL_KEY)!!
            val urlType = args.get(URL_TYPE_KEY) as UrlType
            val post = args.get(POST_KEY) as Post?
            model.initialize(url, urlType, post)
        }

        model.passPlayerControlView(WeakReference(binding.bottomBarControls.playerController))

        setViewPager()
        setOnClickListeners()
        setObservers()

        binding.executePendingBindings()

        return binding.root
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
    }

    private fun setViewPager(){
        binding.viewPager.apply {
            adapter = MediaAdapter(this@MediaDialogFragment)
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
                onPostClicked?.invoke(post)
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

    companion object{
        fun newInstance(url: String, urlType: UrlType, post: Post?): MediaDialogFragment{
            val fragment = MediaDialogFragment()
            val args = bundleOf(URL_KEY to url, URL_TYPE_KEY to urlType, POST_KEY to post)
            fragment.arguments = args
            return fragment
        }
    }
}