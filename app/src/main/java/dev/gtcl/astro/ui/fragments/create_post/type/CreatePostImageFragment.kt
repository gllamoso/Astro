package dev.gtcl.astro.ui.fragments.create_post.type

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.astro.ImagePost
import dev.gtcl.astro.R
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.ViewModelFactory
import dev.gtcl.astro.databinding.FragmentCreatePostImageBinding
import dev.gtcl.astro.ui.fragments.create_post.CreatePostVM

class CreatePostImageFragment : Fragment() {

    private var binding: FragmentCreatePostImageBinding? = null

    private val model: UploadImageVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(UploadImageVM::class.java)
    }

    private val parentModel: CreatePostVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(CreatePostVM::class.java)
    }

    private val photoUri: Uri by lazy {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )!!
    }

    private val getFromGallery = registerForActivityResult(ActivityResultContracts.GetContent()) {
        model.setUri(it)
    }

    private val getFromCamera = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        if (!it) {
            return@registerForActivityResult
        }
        model.setUri(photoUri)
    }

    override fun onResume() {
        super.onResume()
        initObservers()
    }

    override fun onPause() {
        super.onPause()
        removeObservers()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCreatePostImageBinding.inflate(inflater)
        binding?.lifecycleOwner = viewLifecycleOwner
        binding?.model = model
        initClickListeners()
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!requireActivity().isChangingConfigurations) {
            requireContext().contentResolver.delete(photoUri, null, null)
        }
        binding = null
    }

    private fun initClickListeners() {
        val requestCameraAccess = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                getFromCamera.launch(photoUri)
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.please_grant_necessary_permissions),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding?.fragmentCreatePostImageCameraButton?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                getFromCamera.launch(photoUri)
            } else {
                requestCameraAccess.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        val requestGalleryAccess = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                getFromGallery.launch("image/*")
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.please_grant_necessary_permissions),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding?.fragmentCreatePostImageGalleryButton?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                getFromGallery.launch("image/*")
            } else {
                requestGalleryAccess.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        binding?.fragmentCreatePostImageClose?.setOnClickListener {
            model.setUri(null)
        }
    }

    private fun initObservers() {
        parentModel.fetchInput.observe(viewLifecycleOwner, { fetchInput ->
            if (fetchInput == true) {
                if (model.uri.value != null) {
                    parentModel.setPostContent(
                        ImagePost(model.uri.value!!)
                    )
                } else {
                    binding?.root?.let {
                        Snackbar.make(it, getString(R.string.select_photo), Snackbar.LENGTH_LONG)
                            .show()
                    }
                }
                parentModel.dataFetched()
            }
        })
    }

    private fun removeObservers() {
        parentModel.fetchInput.removeObservers(viewLifecycleOwner)
    }
}