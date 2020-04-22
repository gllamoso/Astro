package dev.gtcl.reddit.ui.fragments.dialog.imageviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.widget.ViewPager2
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentDialogMediaBinding
import dev.gtcl.reddit.models.reddit.UrlType

class MediaDialogFragment: DialogFragment(){

    lateinit var binding: FragmentDialogMediaBinding

    override fun onStart() {
        super.onStart()

        dialog?.let {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            it.window?.setLayout(width, height)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val url = requireArguments().get(URL_KEY) as String
        val urlType = requireArguments().get(URL_TYPE_KEY) as UrlType
        val backupVideoUrl = requireArguments().get(BACKUP_VIDEO_URL) as String?
        binding = FragmentDialogMediaBinding.inflate(inflater)
        binding.viewPager.apply {
            adapter = MediaAdapter(this@MediaDialogFragment, url, urlType, backupVideoUrl)
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
//                    Log.d("TAE", "onPageScrolled: $position, $positionOffset, $positionOffsetPixels")
                }
            })
        }

        dialog?.window?.setBackgroundDrawableResource(
            if(urlType == UrlType.IMAGE || urlType == UrlType.GIF) android.R.color.transparent
            else android.R.color.black
        )

//        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        binding.topToolbar.setNavigationOnClickListener {
            dismiss()
        }

        return binding.root
    }

    companion object{
        fun newInstance(url: String, urlType: UrlType, commentUrl: String?, backupVideoUrl: String? = null): MediaDialogFragment{
            val fragment = MediaDialogFragment()
            val args = bundleOf(URL_KEY to url, URL_TYPE_KEY to urlType, COMMENT_URL_KEY to commentUrl, BACKUP_VIDEO_URL to backupVideoUrl)
            fragment.arguments = args
            return fragment
        }
    }
}