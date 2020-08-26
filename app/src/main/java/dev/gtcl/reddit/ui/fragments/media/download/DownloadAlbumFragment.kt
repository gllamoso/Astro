package dev.gtcl.reddit.ui.fragments.media.download

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import dev.gtcl.reddit.R
import dev.gtcl.reddit.databinding.FragmentDialogAlbumNameBinding
import dev.gtcl.reddit.ui.fragments.media.MediaDialogVM

class DownloadAlbumFragment: DialogFragment(){

    private lateinit var binding: FragmentDialogAlbumNameBinding

    val model: MediaDialogVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(MediaDialogVM::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDialogAlbumNameBinding.inflate(inflater)

        binding.toolbar.setOnMenuItemClickListener {
            dismiss()
            true
        }

        binding.text.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.textInputLayout.error = null
            }
        })

//        binding.downloadButton.setOnClickListener {
//            val text = binding.text.text.toString()
//            if(text.isEmpty()){
//                binding.textInputLayout.error = getString(R.string.invalid)
//            } else {
//                model.downloadAlbum(binding.text.text.toString())
//                dismiss()
//            }
//        }

        return binding.root
    }

    companion object {
        fun newInstance() = DownloadAlbumFragment()
    }
}