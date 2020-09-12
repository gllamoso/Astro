package dev.gtcl.astro.ui.fragments.media.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import dev.gtcl.astro.MEDIA_KEY
import dev.gtcl.astro.databinding.FragmentViewpagerBinding
import dev.gtcl.astro.models.reddit.MediaURL
import dev.gtcl.astro.ui.fragments.media.MediaDialogVM


class MediaListFragment : Fragment() {

    private var binding: FragmentViewpagerBinding? = null

    val model: MediaDialogVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(MediaDialogVM::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentViewpagerBinding.inflate(inflater)

        val items = requireArguments().get(MEDIA_KEY) as List<MediaURL>
        val mediaAdapter =
            MediaListFragmentAdapter(
                childFragmentManager,
                viewLifecycleOwner.lifecycle,
                items,
                true
            )
        binding?.fragmentViewPagerViewPager?.apply {
            this.adapter = mediaAdapter
            model.setItemPosition(currentItem)
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    model.setItemPosition(position)
                }
            })
            (getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }

        model.errorMessage.observe(viewLifecycleOwner, {
            if (it != null) {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                model.errorMessageObserved()
            }
        })

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.fragmentViewPagerViewPager?.adapter = null
        binding = null
    }

    companion object {
        fun newInstance(mediaItems: List<MediaURL>): MediaListFragment {
            val fragment = MediaListFragment()
            val args = bundleOf(MEDIA_KEY to mediaItems)
            fragment.arguments = args
            return fragment
        }
    }
}