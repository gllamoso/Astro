package dev.gtcl.reddit.ui.fragments.comments

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.Util
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentCommentsBinding
import dev.gtcl.reddit.actions.ViewPagerActions
import dev.gtcl.reddit.models.reddit.listing.More
import dev.gtcl.reddit.models.reddit.listing.Post

class CommentsFragment : Fragment() { // TODO: Create static newInstance method

    private val model: CommentsVM by lazy {
        val viewModelFactory = ViewModelFactory(requireContext().applicationContext as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(CommentsVM::class.java)
    }

    private val mediaController: MediaController by lazy { MediaController(requireContext()) }

    private var mPlayer: SimpleExoPlayer? = null
    private var mPlayWhenReady = true
    private var mCurrentWindow = 0
    private var mPlaybackPosition = 0.toLong()

    private lateinit var binding: FragmentCommentsBinding

    private lateinit var viewPagerActions: ViewPagerActions
    fun setActions(viewPagerActions: ViewPagerActions){
        this.viewPagerActions = viewPagerActions
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCommentsBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = model

        val bundle = arguments
        val post = bundle?.get(POST_KEY)
        post?.let { model.setPost(it as Post) }

        val adapter = CommentsAdapter(object : CommentsAdapter.CommentItemClickListener{
            override fun onMoreCommentsClicked(position: Int, more: More) {
                model.fetchMoreComments(position, more)
                model.clearMoreComments()
            }

            override fun onContinueThreadClicked(more: More) {
                val url = "${model.post.value?.permalink}${more.parentId.replace("t1_","")}"
//                findNavController().navigate(MainFragmentDirections.actionMainFragmentToCommentsFragment(CommentUrl(url)))
            }
        })
        binding.bottomSheet.commentList.adapter = adapter
        model.comments.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.submitList(it)
                binding.nestedScrollView.scrollTo(0,0)
                model.clearComments()
            }

        })

        model.moreComments.observe(viewLifecycleOwner, Observer {
            if(it != null) {
                adapter.addItems(it.position, it.comments)
                model.clearMoreComments()
            }
        })

        binding.toolbar.setNavigationOnClickListener {
            viewPagerActions.navigatePreviousPage()
            mediaController.hide()
        }

        binding.bottomSheet.upvoteButton.setOnClickListener{
            Toast.makeText(context, "Upvoted!", Toast.LENGTH_LONG).show()
        }

        BottomSheetBehavior.from(binding.bottomSheet.bottomSheet).addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback(){
            override fun onSlide(p0: View, p1: Float) {
                mediaController.hide()
                if(!::viewPagerActions.isInitialized) return
                viewPagerActions.enablePagerSwiping(false)
            }

            override fun onStateChanged(p0: View, newState: Int) {
                mediaController.hide()
                if(!::viewPagerActions.isInitialized) return
                when(newState){
                    BottomSheetBehavior.STATE_HIDDEN, BottomSheetBehavior.STATE_COLLAPSED ->  viewPagerActions.enablePagerSwiping(true)
                    else ->  viewPagerActions.enablePagerSwiping(false)
                }
            }
        })

        model.post.observe(viewLifecycleOwner, Observer{
            if(it != null && !model.commentsFetched) {
                when {
                    it.isSelf -> setTextView(it.selftext)
                    it.preview?.redditVideo != null -> setPlayerView(it.preview.redditVideo.hlsUrl)
                    it.secureMedia?.redditVideo != null -> setPlayerView(it.secureMedia.redditVideo.hlsUrl)
                    it.media?.redditVideo != null -> setPlayerView(it.media.redditVideo.hlsUrl)
                    it.isImage -> setImageView(it.url!!)
                    else -> setTextView(it.url)
                }
                model.fetchPostAndComments(it.permalink)
                model.commentsFetched = true
            }
        })

        binding.bottomSheet.bottomBar.setOnClickListener {
            val currentState = BottomSheetBehavior.from(binding.bottomSheet.bottomSheet).state
            BottomSheetBehavior.from(binding.bottomSheet.bottomSheet).state =
                if(currentState == BottomSheetBehavior.STATE_EXPANDED) BottomSheetBehavior.STATE_COLLAPSED
                else BottomSheetBehavior.STATE_EXPANDED
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.nestedScrollView.scrollTo(0,0)
        binding.bottomSheet.commentList.scrollToPosition(0)
        if((Util.SDK_INT < 24 || mPlayer == null)){
//            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    private fun setTextView(text: String?){
        releasePlayer()
        binding.contentText.text = text
        binding.contentText.visibility = View.VISIBLE
        binding.playerView.visibility = View.GONE
        binding.urlImageView.visibility = View.GONE
        binding.contentBackground.setBackgroundColor(ContextCompat.getColor(binding.root.context, android.R.color.transparent))
    }

    private fun setPlayerView(videoUrl: String){
        Log.d("TAE","VideoURL: $videoUrl")
        initializePlayer(videoUrl)
        binding.playerView.visibility = View.VISIBLE
        binding.contentText.visibility = View.GONE
        binding.urlImageView.visibility = View.GONE
        binding.contentBackground.setBackgroundColor(ContextCompat.getColor(binding.root.context, android.R.color.black))
    }

    private fun setImageView(url: String){
        releasePlayer()
        Log.d("TAE","ImageView URL: $url")
        binding.playerView.visibility = View.GONE
        binding.contentText.visibility = View.GONE
        binding.urlImageView.visibility = View.VISIBLE
        binding.contentBackground.setBackgroundColor(ContextCompat.getColor(binding.root.context, android.R.color.black))

        val imgUri = url.toUri().buildUpon().scheme("https").build()
        Glide.with(requireContext())
            .load(imgUri)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.anim_loading)
                    .error(R.drawable.ic_broken_image))
            .into(binding.urlImageView)
    }

    private fun releasePlayer() {
        if(mPlayer != null){
            mPlayWhenReady = mPlayer!!.playWhenReady
            mPlaybackPosition = mPlayer!!.currentPosition
            mCurrentWindow = mPlayer!!.currentWindowIndex
            mPlayer!!.release()
            mPlayer = null
        }
    }

    private fun initializePlayer(url: String){
        if(mPlayer == null){
            val trackSelector = DefaultTrackSelector()
            trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSizeSd())
            mPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector)
            mPlayer!!.repeatMode = Player.REPEAT_MODE_ONE
        }

        binding.playerView.player = mPlayer
        val uri = Uri.parse(url)
        val mediaSource = buildMediaSource(requireContext(), uri)
        mPlayer!!.apply {
            playWhenReady = mPlayWhenReady
            seekTo(mCurrentWindow, mPlaybackPosition)
            prepare(mediaSource, false, false)
        }
    }

    companion object{
        fun newInstance(post: Post): CommentsFragment{
            val fragment = CommentsFragment()
            val args = bundleOf(POST_KEY to post)
            fragment.arguments = args
            return fragment
        }
    }
}
