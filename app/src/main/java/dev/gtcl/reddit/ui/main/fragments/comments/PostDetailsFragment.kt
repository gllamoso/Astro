package dev.gtcl.reddit.ui.main.fragments.comments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dev.gtcl.reddit.comments.More
import dev.gtcl.reddit.databinding.FragmentPostDetailsBinding
import dev.gtcl.reddit.ui.main.fragments.MainFragment
import dev.gtcl.reddit.ui.main.fragments.MainFragmentViewModel

class PostDetailsFragment : Fragment() {

    private val parentViewModel: MainFragmentViewModel by lazy {
        (parentFragment as MainFragment).model
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentPostDetailsBinding.inflate(inflater)
        binding.lifecycleOwner = this

//        val post = PostDetailsFragmentArgs.fromBundle(arguments!!).post
//        binding.model = model
//        model.setPost(post)
//        model.getPostAndComments()
//
        val adapter = CommentsAdapter(object : CommentsAdapter.CommentItemClickListener{
            override fun onMoreCommentsClicked(position: Int, more: More) {
                parentViewModel.getMoreComments(position, more)
                parentViewModel.clearMoreComments()
            }

            override fun onContinueThreadClicked(more: More) {
                parentViewModel.getPostAndComments("${parentViewModel.selectedPost.value?.permalink}${more.parentId.replace("t1_","")}")
            }
        })
        binding.commentList.adapter = adapter
//        model.comments.observe(this, Observer {
//            if(it != null){
//                adapter.submitList(it)
//                model.clearComments()
//            }
//        })
//
//        model.moreComments.observe(this, Observer {
//            if(it != null)
//                adapter.addItems(it.position, it.comments)
//        })
//
//        binding.toolbar.setNavigationOnClickListener {
//            if(activity is MainActivity)
//                (activity as MainActivity).navigateUp()
//        }
//
//        binding.upvoteButton.setOnClickListener{
//            Toast.makeText(context, "Upvoted!", Toast.LENGTH_LONG).show()
//        }
//
//        model.redditPost.observe(this, Observer{
//            binding.contentPlaceholder.removeAllViews()
//            val displayMetrics = resources.displayMetrics
//            val padding = (16 * displayMetrics.density).toInt()
//            if(it.isSelf){
//                val nestedScrollView = NestedScrollView(context!!)
//                val textView = TextView(context)
//                textView.setPaddingRelative(padding, padding, padding, padding)
//                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
//                    textView.setTextColor(ContextCompat.getColor(context!!, R.color.black))
//                else
//                    textView.setTextColor(resources.getColor(R.color.black))
////                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
////                    textView.text = Html.fromHtml(it.selftext, Html.FROM_HTML_MODE_COMPACT)
////                else
////                    textView.text = Html.fromHtml(it.selftext)
//                textView.text = it.selftext
//                nestedScrollView.addView(textView)
//                binding.contentPlaceholder.addView(nestedScrollView)
//            } else {
//                binding.contentPlaceholder.alpha = 0.toFloat()
//                val videoView = VideoView(context).apply {
//                    alpha = 0.toFloat()
////                    setVideoPath("https://v.redd.it/tw3w92jk7kb41/HLSPlaylist.m3u8")
//                    setVideoPath("https://archive.org/download/Popeye_forPresident/Popeye_forPresident_512kb.mp4")
//                    val params = LinearLayout.LayoutParams(
//                        (displayMetrics.widthPixels * 1.00).toInt(),
//                        (displayMetrics.heightPixels * 1.00).toInt()
//                    ).apply {
//                        gravity = Gravity.CENTER
//                    }
//                    layoutParams = params
//                    val mediaController = MediaController(context)
//                    mediaController.setAnchorView(this)
//                    setMediaController(mediaController)
//                    setPadding(padding)
//                    setOnPreparedListener {
//                        mp -> mp.isLooping = true
//                        binding.contentPlaceholder.animate().alpha(1.toFloat())
//                        animate().alpha(1.toFloat())
//                        seekTo(0)
//                        start()
//                    }
//                }
//                binding.contentPlaceholder.addView(videoView)
//            }
//        })

        return binding.root
    }
}
