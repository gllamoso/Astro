package dev.gtcl.astro.ui.fragments.media

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import dev.gtcl.astro.*
import dev.gtcl.astro.databinding.FragmentDialogMediaBinding
import dev.gtcl.astro.databinding.PopupDownloadActionsBinding
import dev.gtcl.astro.models.reddit.MediaURL
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.view_pager.PostPage
import dev.gtcl.astro.ui.fragments.media.list.MediaThumbnailsAdapter
import timber.log.Timber

class MediaDialogFragment : DialogFragment() {

    private var binding: FragmentDialogMediaBinding? = null

    private val model: MediaDialogVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(MediaDialogVM::class.java)
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
        binding = FragmentDialogMediaBinding.inflate(inflater)
        binding?.lifecycleOwner = viewLifecycleOwner
        binding?.model = model
        binding?.activityModel = activityModel
        activityModel.showMediaControls(true)
        activityModel.mediaDialogOpened(true)
        if (!model.mediaInitialized && model.isLoading.value != true) {
            val url = requireArguments().get(MEDIA_KEY) as MediaURL?
            if (url != null) {
                model.setMedia(url)
            } else {
                val mediaItems = requireArguments().get(MEDIA_ITEMS_KEY) as List<MediaURL>
                model.setItems(mediaItems)
            }

        }

        initAdapters()
        initWindowBackground()
        initTopBar()
        initBottomBar()

        model.errorMessage.observe(viewLifecycleOwner, { errorMessage ->
            if (errorMessage != null) {
                Timber.tag(this::javaClass.name).d(errorMessage)
                model.errorMessageObserved()
            }
        })

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.fragmentMediaDialogViewpager?.adapter = null
        binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        activityModel.mediaDialogOpened(false)
    }

    private fun initAdapters() {
        val currentPosition = model.itemPosition.value ?: 0
        val mediaListAdapter =
            MediaThumbnailsAdapter(currentPosition) { position ->
                model.setItemPosition(position)
                binding?.fragmentMediaDialogDrawer?.closeDrawer(GravityCompat.END)
            }

        model.mediaItems.observe(viewLifecycleOwner, {
            if (binding?.fragmentMediaDialogViewpager?.adapter == null) {
                val swipeToDismissAdapter =
                    MediaSwipeToDismissAdapter(
                        childFragmentManager,
                        viewLifecycleOwner.lifecycle,
                        it
                    )
                binding?.fragmentMediaDialogViewpager?.apply {
                    this.adapter = swipeToDismissAdapter
                    currentItem = 1
                    registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageScrollStateChanged(state: Int) {
                            if (state == ViewPager2.SCROLL_STATE_IDLE && currentItem != 1) {
                                dismiss()
                            }
                        }

                        override fun onPageScrolled(
                            position: Int,
                            positionOffset: Float,
                            positionOffsetPixels: Int
                        ) {
                            super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                            val offset = if (position == 0) {
                                1 - positionOffset
                            } else {
                                positionOffset
                            }
                            val multiplier = 1000
                            binding?.fragmentMediaDialogToolbar?.translationY = -multiplier * offset
                            binding?.fragmentMediaDialogBottomBar?.translationY =
                                multiplier * offset
                        }
                    })
                }
            }
            mediaListAdapter.submitList(it)

            binding?.fragmentMediaDialogDrawer?.setDrawerLockMode(
                if (it.size > 1) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                GravityCompat.END
            )
        })

        model.itemPosition.observe(viewLifecycleOwner, {
            mediaListAdapter.setCurrentPosition(it)
        })

        binding?.fragmentMediaDialogAlbumThumbnails?.adapter = mediaListAdapter
    }

    private fun initTopBar() {
        binding?.fragmentMediaDialogAlbumButton?.setOnClickListener {
            binding?.fragmentMediaDialogDrawer?.openDrawer(GravityCompat.END)
        }

        binding?.fragmentMediaDialogNavigationButton?.setOnClickListener {
            dismiss()
        }

        binding?.fragmentMediaDialogToolbar?.setNavigationOnClickListener {
            dismiss()
        }
    }

    private fun initWindowBackground() {
//        dialog?.window?.setBackgroundDrawableResource(
//            if(mediaURL.mediaType == MediaType.PICTURE || mediaURL.mediaType == MediaType.GIF){
//                R.color.darkTransparent
//            } else {
//                android.R.color.black
//            }
//        )

        dialog?.window?.setBackgroundDrawableResource(android.R.color.black)
    }

    private fun initBottomBar() {
        val mediaUrl = requireArguments().get(MEDIA_KEY) as MediaURL?
        val albumUrl = requireArguments().getString(URL_KEY)
        val postPage = requireArguments().get(POST_PAGE_KEY) as PostPage?
        model.setPost(postPage?.post)

        binding?.fragmentMediaDialogComments?.setOnClickListener {
            if (postPage != null) {
                activityModel.newViewPagerPage(postPage)
            }
            dismiss()
        }

        binding?.fragmentMediaDialogShare?.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.share_subject_message))
            shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                mediaUrl?.url ?: albumUrl ?: throw IllegalArgumentException("Unable to get url")
            )
            startActivity(Intent.createChooser(shareIntent, null))
        }

        val requestPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                if (model.mediaItems.value != null && (model.mediaItems.value
                        ?: return@registerForActivityResult).size > 1
                ) {
                    binding?.fragmentMediaDialogDownload?.let {
                        showDownloadOptionsPopup(it)
                    }
                } else {
                    model.downloadCurrentItem()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.please_grant_necessary_permissions),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding?.fragmentMediaDialogDownload?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                if (model.mediaItems.value != null && (model.mediaItems.value
                        ?: return@setOnClickListener).size > 1
                ) {
                    showDownloadOptionsPopup(it)
                } else {
                    model.downloadCurrentItem()
                }
            } else {
                requestPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        binding?.fragmentMediaDialogLink?.setOnClickListener {
            activityModel.openChromeTab(mediaUrl?.url ?: (albumUrl ?: return@setOnClickListener))
        }
    }

    private fun showDownloadOptionsPopup(anchor: View) {
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupDownloadActionsBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        val currentItem = model.getCurrentMediaItem()
        val mediaType = when (currentItem?.mediaType) {
            MediaType.PICTURE, MediaType.GIF -> SimpleMediaType.PICTURE
            MediaType.VIDEO -> SimpleMediaType.VIDEO
            else -> throw IllegalArgumentException("Invalid media type from item: $currentItem")
        }
        popupBinding.apply {
            this.mediaType = mediaType
            popupDownloadActionsSingle.root.setOnClickListener {
                model.downloadCurrentItem()
                popupWindow.dismiss()
            }
            popupDownloadActionsAll.root.setOnClickListener {
                model.downloadAlbum()
                popupWindow.dismiss()
            }
            executePendingBindings()
            root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }

        popupWindow.showAsDropdown(
            anchor,
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            popupBinding.root.measuredHeight
        )
    }

    companion object {
        fun newInstance(mediaURL: MediaURL, postPage: PostPage? = null): MediaDialogFragment {
            return MediaDialogFragment().apply {
                arguments = bundleOf(MEDIA_KEY to mediaURL, POST_PAGE_KEY to postPage)
            }
        }

        fun newInstance(
            albumUrl: String,
            mediaItems: List<MediaURL>,
            postPage: PostPage? = null
        ): MediaDialogFragment {
            return MediaDialogFragment().apply {
                arguments = bundleOf(
                    URL_KEY to albumUrl,
                    MEDIA_ITEMS_KEY to mediaItems,
                    POST_PAGE_KEY to postPage
                )
            }
        }
    }
}