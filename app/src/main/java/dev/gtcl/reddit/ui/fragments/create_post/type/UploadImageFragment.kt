package dev.gtcl.reddit.ui.fragments.create_post.type

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.reddit.ImagePost
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.databinding.FragmentCreatePostImageBinding
import dev.gtcl.reddit.ui.fragments.create_post.CreatePostVM
import java.io.File

class UploadImageFragment: Fragment() {

    private lateinit var binding: FragmentCreatePostImageBinding

    private val model: UploadImageVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(UploadImageVM::class.java)
    }

    private val parentModel: CreatePostVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(CreatePostVM::class.java)
    }

    private val photoUri: Uri by lazy {
        val storageDir = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath}/${requireContext().getText(R.string.app_name)}/temp").apply {
            deleteRecursively()
            mkdirs()
        }
        val tempFile = File.createTempFile("preview", ".jpg", storageDir)
        FileProvider.getUriForFile(requireContext(), "dev.gtcl.reddit.provider", tempFile)
    }

    private val getFromGallery = registerForActivityResult(ActivityResultContracts.GetContent()) {
        model.setUri(it)
    }

    private val getFromCamera = registerForActivityResult(ActivityResultContracts.TakePicture()){
        if(!it){
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
        binding.lifecycleOwner = viewLifecycleOwner
        binding.model = model
        initClickListeners()
        return binding.root
    }

    private fun initClickListeners(){
        binding.cameraButton.setOnClickListener {
            getFromCamera.launch(photoUri)
        }

        binding.galleryButton.setOnClickListener {
            getFromGallery.launch("image/*")
        }

        binding.closeButton.setOnClickListener {
            model.setUri(null)
        }
    }

    private fun initObservers(){
        parentModel.fetchData.observe(viewLifecycleOwner, Observer {
            if(it == true){
                if(model.uri.value != null){
                    parentModel.setPostContent(
                        ImagePost(model.uri.value!!)
                    )
                } else {
                    Snackbar.make(binding.root, getString(R.string.select_photo), Snackbar.LENGTH_LONG).show()
                }
                parentModel.dataFetched()
            }
        })
    }

    private fun removeObservers(){
        parentModel.fetchData.removeObservers(viewLifecycleOwner)
    }
}