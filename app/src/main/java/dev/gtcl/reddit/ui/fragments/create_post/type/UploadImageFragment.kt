package dev.gtcl.reddit.ui.fragments.create_post.type

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import dev.gtcl.reddit.R
import dev.gtcl.reddit.databinding.FragmentCreatePostImageBinding
import dev.gtcl.reddit.ui.fragments.create_post.CreatePostVM
import java.io.File

class UploadImageFragment: Fragment() {

    private lateinit var binding: FragmentCreatePostImageBinding

    private lateinit var uri: Uri

    private val getFromGallery = registerForActivityResult(ActivityResultContracts.GetContent()) {
        binding.imagePreviewLayout.visibility = View.VISIBLE
        Glide.with(requireContext())
            .load(it)
            .centerInside()
            .into(binding.imagePreview)
    }

    private val getFromCamera = registerForActivityResult(ActivityResultContracts.TakePicture()){
        if(!it){
            return@registerForActivityResult
        }

        binding.imagePreviewLayout.visibility = View.VISIBLE
        Glide.with(requireContext())
            .load(uri)
            .centerInside()
            .into(binding.imagePreview)
    }


    val model: CreatePostVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(CreatePostVM::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val storageDir = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath}/${requireContext().getText(R.string.app_name)}/")
        val tempImage = File.createTempFile("preview", ".jpg", storageDir)
        uri = FileProvider.getUriForFile(requireContext(), "dev.gtcl.reddit.provider", tempImage)

        binding = FragmentCreatePostImageBinding.inflate(inflater)
        setOnClickListeners()
        return binding.root
    }

    private fun setOnClickListeners(){
        binding.cameraButton.setOnClickListener {
            getFromCamera.launch(uri)
        }

        binding.galleryButton.setOnClickListener {
            getFromGallery.launch("image/*")
        }

        binding.closeButton.setOnClickListener {
            binding.imagePreviewLayout.visibility = View.GONE
            binding.imagePreview.setImageResource(android.R.color.transparent)
        }
    }
}