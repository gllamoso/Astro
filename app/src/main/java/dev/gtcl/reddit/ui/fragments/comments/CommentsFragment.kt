package dev.gtcl.reddit.ui.fragments.comments

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dev.gtcl.reddit.R
import dev.gtcl.reddit.comments.More
import dev.gtcl.reddit.databinding.FragmentCommentsBinding
import dev.gtcl.reddit.ui.fragments.MainFragment
import dev.gtcl.reddit.ui.fragments.MainFragmentViewModel

class CommentsFragment : Fragment() {

    private val parentViewModel: MainFragmentViewModel by lazy {
        (parentFragment as MainFragment).model
    }

    private val mediaController: MediaController by lazy {
        (parentFragment as MainFragment).mediaController
    }

    private lateinit var binding: FragmentCommentsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCommentsBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = parentViewModel
        val adapter = CommentsAdapter(object : CommentsAdapter.CommentItemClickListener{
            override fun onMoreCommentsClicked(position: Int, more: More) {
                parentViewModel.fetchMoreComments(position, more)
                parentViewModel.clearMoreComments()
            }

            override fun onContinueThreadClicked(more: More) {
                parentViewModel.fetchPostAndComments("${parentViewModel.post.value?.permalink}${more.parentId.replace("t1_","")}")
            }
        })
        binding.commentList.adapter = adapter
        parentViewModel.comments.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.submitList(it)
                parentViewModel.clearComments()
            }
        })

        parentViewModel.moreComments.observe(viewLifecycleOwner, Observer {
            if(it != null)
                adapter.addItems(it.position, it.comments)
        })

        binding.toolbar.setNavigationOnClickListener {
            parentViewModel.scrollToPage(0)
            mediaController.hide()
        }

        binding.upvoteButton.setOnClickListener{
            Toast.makeText(context, "Upvoted!", Toast.LENGTH_LONG).show()
        }

        val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback(){
            override fun onSlide(p0: View, p1: Float) {
                parentViewModel.setScrollable(false)
                mediaController.hide()
            }

            override fun onStateChanged(p0: View, newState: Int) {
                mediaController.hide()
                when(newState){
                    BottomSheetBehavior.STATE_HIDDEN, BottomSheetBehavior.STATE_COLLAPSED ->  parentViewModel.setScrollable(true)
                    else ->  parentViewModel.setScrollable(false)
                }
            }

        })

        parentViewModel.postContentCreated.observe(this, Observer {
            if(it)
                binding.nestedScrollView.scrollTo(0,0)
        })

        parentViewModel.post.observe(this, Observer{
            if(!parentViewModel.postContentCreated.value!!){
                binding.contentPlaceholder.removeAllViews()
                if(it.isSelf)
                    createTextView(it.selftext)
                else
                    createVideoView("https://v.redd.it/tw3w92jk7kb41/HLSPlaylist.m3u8")
                //"https://archive.org/download/Popeye_forPresident/Popeye_forPresident_512kb.mp4"
                parentViewModel.postGenerated(true)
            }
        })

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.nestedScrollView.scrollTo(0,0)
        binding.commentList.scrollToPosition(0)
    }

    private fun createTextView(text: String){
        val textView = TextView(context)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            textView.setTextColor(ContextCompat.getColor(context!!, R.color.black))
        else
            textView.setTextColor(resources.getColor(R.color.black))
        textView.text = text
        binding.contentPlaceholder.addView(textView)
    }

    private fun createVideoView(videoPath: String){
        val displayMetrics = resources.displayMetrics

        val videoView = VideoView(context).apply {
            alpha = 0.toFloat()
            setVideoPath(videoPath)
            val params = FrameLayout.LayoutParams(
                displayMetrics.widthPixels,
                (displayMetrics.heightPixels * 0.75).toInt()
            )
            layoutParams = params
//            mediaController.setAnchorView(bin)
            setMediaController(mediaController)
            setOnPreparedListener { mp ->
                mp.isLooping = true
                animate().alpha(1.toFloat())
                seekTo(0)
                start()
                /*
                mp.setOnVideoSizeChangedListener{ mediaplayer, width, height ->

                }*/
            }
        }
        binding.contentPlaceholder.addView(videoView)
    }
}
