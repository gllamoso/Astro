package dev.gtcl.reddit.ui.fragments.media.test.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import dev.gtcl.reddit.MEDIA_KEY
import dev.gtcl.reddit.databinding.FragmentViewpagerBinding
import dev.gtcl.reddit.models.reddit.MediaURL
import dev.gtcl.reddit.ui.fragments.media.test.MediaListVM


class MediaListFragment : Fragment(){

    private lateinit var binding: FragmentViewpagerBinding

    val model: MediaListVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(MediaListVM::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentViewpagerBinding.inflate(inflater)
        val items = requireArguments().get(MEDIA_KEY) as List<MediaURL>
        val adapter = MediaListFragmentAdapter(this, items)
        binding.viewpager.apply {
            this.adapter = adapter
            model.setItemPosition(0)
            registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    if(state == ViewPager2.SCROLL_STATE_IDLE){
                        model.setItemPosition(currentItem)
                    }
                }
            })
            (getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }
        model.itemPosition.observe(viewLifecycleOwner, Observer {
            binding.viewpager.currentItem = it
        })
        return binding.root
    }

    companion object{
        fun newInstance(mediaItems: List<MediaURL>): MediaListFragment{
            val fragment = MediaListFragment()
            val args = bundleOf(MEDIA_KEY to mediaItems)
            fragment.arguments = args
            return fragment
        }
    }
}