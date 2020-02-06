package dev.gtcl.reddit.ui.fragments.comments

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
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

    private var mPlayer: SimpleExoPlayer? = null
    private var mPlayWhenReady = true
    private var mCurrentWindow = 0
    private var mPlaybackPosition = 0.toLong()

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
            if(parentViewModel.postContentCreated.value != true){
                if(it.isSelf)
                    setTextView(it.selftext)
                else if(it.preview?.redditVideoPreview != null)
                    setPlayerView(it.preview.redditVideoPreview.hlsUrl)
                parentViewModel.postGenerated(true)
            }
        })

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.nestedScrollView.scrollTo(0,0)
        binding.commentList.scrollToPosition(0)
        if((Util.SDK_INT < 24 || mPlayer == null)){
//            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    private fun setTextView(text: String){
        releasePlayer()
        binding.contentText.text = text
        binding.contentText.visibility = View.VISIBLE
        binding.playerView.visibility = View.GONE
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            binding.contentLayout.setBackgroundColor(ContextCompat.getColor(context!!, R.color.white))
        else
            binding.contentLayout.setBackgroundColor(resources.getColor(R.color.white))
    }

    private fun setPlayerView(videoPath: String){
        initializePlayer(videoPath)
        binding.playerView.visibility = View.VISIBLE
        binding.contentText.visibility = View.GONE
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            binding.contentLayout.setBackgroundColor(ContextCompat.getColor(context!!, R.color.black))
        else
            binding.contentLayout.setBackgroundColor(resources.getColor(R.color.black))
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


    private fun buildMediaSource(uri: Uri): MediaSource {
        val userAgent = "exoplayer-codelab"

        return if (uri.lastPathSegment!!.contains("mp3") || uri.lastPathSegment!!.contains("mp4")) {
            ProgressiveMediaSource.Factory(DefaultHttpDataSourceFactory(userAgent))
                .createMediaSource(uri)
        } else if (uri.lastPathSegment!!.contains("m3u8")) {
            HlsMediaSource.Factory(DefaultHttpDataSourceFactory(userAgent))
                .createMediaSource(uri)
        } else {
            val dataSourceFactory = DefaultDataSourceFactory(context, "exoplayer-codelab")
            val mediaSourceFactor = DashMediaSource.Factory(dataSourceFactory)
            mediaSourceFactor.createMediaSource(uri)
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
        val mediaSource = buildMediaSource(uri)
        mPlayer!!.apply {
            playWhenReady = mPlayWhenReady
            seekTo(mCurrentWindow, mPlaybackPosition)
            prepare(mediaSource, false, false)
        }
    }
}
