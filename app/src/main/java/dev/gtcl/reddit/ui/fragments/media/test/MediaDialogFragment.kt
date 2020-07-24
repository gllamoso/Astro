package dev.gtcl.reddit.ui.fragments.media.test

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentDialogMediaListBinding
import dev.gtcl.reddit.models.reddit.MediaURL
import dev.gtcl.reddit.ui.activities.MainActivityVM
import dev.gtcl.reddit.ui.fragments.PostPage
import dev.gtcl.reddit.ui.fragments.media.test.list.MediaListRecyclerViewAdapter

class MediaDialogFragment : DialogFragment(){

    private lateinit var binding: FragmentDialogMediaListBinding

    private val model: MediaListVM by lazy{
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(MediaListVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    override fun onStart() {
        super.onStart()

        dialog?.let {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            it.window?.setLayout(width, height)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDialogMediaListBinding.inflate(inflater)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.model = model
        model.setMedia(requireArguments().get(MEDIA_KEY) as MediaURL)

        initAdapters()
        initWindowBackground()
        initTopbar()
        initBottomBar()

        return binding.root
    }

    private fun initAdapters(){
        val mediaListAdapter = MediaListRecyclerViewAdapter{ position ->
            model.setItemPosition(position)
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        }

        model.mediaItems.observe(viewLifecycleOwner, Observer {
            if(binding.viewpager.adapter == null){
                val swipeToDismissAdapter = MediaSwipeToDismissAdapter(this, it)
                binding.viewpager.apply {
                    this.adapter = swipeToDismissAdapter
                    currentItem = 1
                    registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
                        override fun onPageScrollStateChanged(state: Int) {
                            if(state == ViewPager2.SCROLL_STATE_IDLE && currentItem != 1){
                                dismiss()
                            }
                        }

                        override fun onPageScrolled(
                            position: Int,
                            positionOffset: Float,
                            positionOffsetPixels: Int
                        ) {
                            super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                            val offset = if(position == 0) {
                                1 - positionOffset
                            } else {
                                positionOffset
                            }
                            val multiplier = 1000
                            binding.topToolbar.translationY = -multiplier * offset
                            binding.bottomBarControls.root.translationY = multiplier * offset
                        }
                    })
                }
            }
            mediaListAdapter.submitList(it)

            binding.drawerLayout.setDrawerLockMode(
                if(it.size > 1) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                GravityCompat.END
            )
        })



        binding.albumThumbnails.adapter = mediaListAdapter
    }

    private fun initTopbar(){
        binding.albumIcon.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        binding.topToolbar.setNavigationOnClickListener {
            dismiss()
        }
    }

    private fun initWindowBackground(){
//        dialog?.window?.setBackgroundDrawableResource(
//            if(mediaURL.mediaType == MediaType.PICTURE || mediaURL.mediaType == MediaType.GIF){
//                R.color.darkTransparent
//            } else {
//                android.R.color.black
//            }
//        )

        dialog?.window?.setBackgroundDrawableResource(android.R.color.black)
    }

    private fun initBottomBar(){
        val mediaUrl = requireArguments().get(MEDIA_KEY) as MediaURL
        val postPage = requireArguments().get(POST_PAGE_KEY) as PostPage?
        model.setPost(postPage?.post)

        binding.bottomBarControls.apply {
            commentButton.setOnClickListener {
                if (postPage != null) {
                    activityModel.newPage(postPage)
                }
                dismiss()
            }

            shareButton.setOnClickListener {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.share_subject_message))
                shareIntent.putExtra(Intent.EXTRA_TEXT, mediaUrl.url)
                startActivity(Intent.createChooser(shareIntent, null))
            }

            downloadButton.setOnClickListener {
                model.downloadCurrentItem()
            }
        }
    }

    companion object{
        fun newInstance(mediaURL: MediaURL, postPage: PostPage? = null): MediaDialogFragment{
            val fragment = MediaDialogFragment()
            val args = bundleOf(MEDIA_KEY to mediaURL, POST_PAGE_KEY to postPage)
            fragment.arguments = args
            return fragment
        }
    }
}