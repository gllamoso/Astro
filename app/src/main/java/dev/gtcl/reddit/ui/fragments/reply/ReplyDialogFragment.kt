package dev.gtcl.reddit.ui.fragments.reply

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentDialogReplyBinding
import dev.gtcl.reddit.models.reddit.listing.Comment
import dev.gtcl.reddit.models.reddit.listing.Item
import dev.gtcl.reddit.models.reddit.listing.Message
import dev.gtcl.reddit.models.reddit.listing.Post

class ReplyDialogFragment: DialogFragment() {

    private lateinit var binding: FragmentDialogReplyBinding

    private val model: ReplyVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(ReplyVM::class.java)
    }

    override fun onStart() {
        super.onStart()

        dialog?.let {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            it.window?.setLayout(width, height)
        }

//        dialog?.window?.setBackgroundDrawableResource(android.R.color.black) // This makes the dialog full screen
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDialogReplyBinding.inflate(inflater)
        binding.model = model
        binding.lifecycleOwner = viewLifecycleOwner
        binding.parentMessage.movementMethod = ScrollingMovementMethod()

        val parent = requireArguments().get(ITEM_KEY) as Item
        val position = requireArguments().get(POSITION_KEY) as Int

        initParent(parent)
        setListeners(parent, position)

        binding.executePendingBindings()
        return binding.root
    }

    private fun initParent(parent: Item){
        when(parent){
            is Post -> {
                binding.replyToUser = parent.author
                binding.replyToBody = parent.title
            }
            is Comment -> {
                binding.replyToUser = parent.author
                binding.replyToBody = parent.body
            }
            is Message -> {
                binding.replyToUser = parent.author
                binding.replyToBody = parent.body
            }
            else -> {
                throw IllegalArgumentException("Unable to reply to the following item type: ${parent.kind}")
            }
        }
    }

    private fun setListeners(parent: Item, position: Int){
        binding.toolbar.setNavigationOnClickListener {
            if(model.isLoading.value != true) {
                dismiss()
            }
        }

        binding.toolbar.setOnMenuItemClickListener {
            if(model.isLoading.value == true){
                return@setOnMenuItemClickListener false
            }
            val comment = binding.responseText.text.toString()
            if(comment.isBlank()){
                binding.responseInputLayout.error = getString(R.string.required)
                return@setOnMenuItemClickListener false
            }
            model.reply(parent, comment, position)
            true
        }

        model.isLoading.observe(viewLifecycleOwner, Observer {
            dialog?.apply {
                setCancelable(!it)
                setCanceledOnTouchOutside(!it)
            }
        })

        model.newReply.observe(viewLifecycleOwner, Observer {
            if(it != null){
                parentFragmentManager.setFragmentResult(NEW_REPLY_KEY, bundleOf(NEW_REPLY_KEY to it))
                model.newReplyObserved()
                dismiss()
            }
        })

        binding.responseText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.responseInputLayout.error = null
            }
        })
    }

    companion object{
        fun newInstance(parent: Item, position: Int): ReplyDialogFragment{
            return ReplyDialogFragment().apply {
                arguments = bundleOf(ITEM_KEY to parent, POSITION_KEY to position)
            }
        }
    }



}