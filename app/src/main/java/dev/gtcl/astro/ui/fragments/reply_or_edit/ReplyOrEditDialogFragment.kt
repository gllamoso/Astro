package dev.gtcl.astro.ui.fragments.reply_or_edit

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.astro.*
import dev.gtcl.astro.databinding.FragmentDialogReplyOrEditBinding
import dev.gtcl.astro.models.reddit.listing.Comment
import dev.gtcl.astro.models.reddit.listing.Item
import dev.gtcl.astro.models.reddit.listing.Message
import dev.gtcl.astro.models.reddit.listing.Post

class ReplyOrEditDialogFragment : DialogFragment() {

    private var binding: FragmentDialogReplyOrEditBinding? = null

    private val model: ReplyOrEditVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(ReplyOrEditVM::class.java)
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
        binding = FragmentDialogReplyOrEditBinding.inflate(inflater)
        binding?.model = model
        binding?.lifecycleOwner = viewLifecycleOwner
        binding?.fragmentDialogReplyParentMessage?.movementMethod = ScrollingMovementMethod()

        val parent = requireArguments().get(ITEM_KEY) as Item
        val position = requireArguments().getInt(POSITION_KEY)
        val reply = requireArguments().getBoolean(NEW_REPLY_KEY)

        initParent(parent, reply)
        setListeners(parent, position, reply)

        binding?.executePendingBindings()
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun initParent(parent: Item, reply: Boolean) {
        when (parent) {
            is Post -> {
                binding?.replyToUser = parent.author
                if (reply) {
                    binding?.replyToBody = if(parent.selfTextFormatted.isBlank()){
                        parent.titleFormatted
                    } else {
                        parent.selfTextFormatted
                    }
                } else {
                    binding?.fragmentDialogReplyResponseText?.setText(parent.selfTextFormatted)
                }
            }
            is Comment -> {
                binding?.replyToUser = parent.author
                if (reply) {
                    binding?.replyToBody = parent.bodyFormatted
                } else {
                    binding?.fragmentDialogReplyResponseText?.setText(parent.bodyFormatted)
                }
            }
            is Message -> {
                binding?.replyToUser = parent.author
                binding?.replyToBody = parent.bodyFormatted
            }
            else -> {
                throw IllegalArgumentException("Unable to reply to the following item type: ${parent.kind}")
            }
        }
    }

    private fun setListeners(parent: Item, position: Int, reply: Boolean) {
        binding?.fragmentDialogReplyToolbar?.setNavigationOnClickListener {
            if (model.isLoading.value != true) {
                dismiss()
            }
        }

        binding?.fragmentDialogReplyToolbar?.setOnMenuItemClickListener {
            if (model.isLoading.value == true) {
                return@setOnMenuItemClickListener false
            }
            val text = binding?.fragmentDialogReplyResponseText?.text.toString()
            if (text.isBlank()) {
                binding?.fragmentDialogReplyResponseInputLayout?.error =
                    getString(R.string.required)
                return@setOnMenuItemClickListener false
            }
            if (reply) {
                model.reply(parent, text)
            } else {
                model.edit(parent, text)
            }

            true
        }

        model.isLoading.observe(viewLifecycleOwner, {
            dialog?.apply {
                setCancelable(!it)
                setCanceledOnTouchOutside(!it)
            }
        })

        model.newItem.observe(viewLifecycleOwner, {
            if (it != null) {
                val bundle =
                    bundleOf(ITEM_KEY to it, POSITION_KEY to position, NEW_REPLY_KEY to reply)
                parentFragmentManager.setFragmentResult(NEW_REPLY_KEY, bundle)
                model.newReplyObserved()
                dismiss()
            }
        })

        model.errorMessage.observe(viewLifecycleOwner, {
            if (it != null) {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                model.errorMessageObserved()
            }
        })

        binding?.fragmentDialogReplyResponseText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding?.fragmentDialogReplyResponseInputLayout?.error = null
            }
        })
    }

    companion object {
        fun newInstance(parent: Item, position: Int, reply: Boolean): ReplyOrEditDialogFragment {
            return ReplyOrEditDialogFragment().apply {
                arguments =
                    bundleOf(ITEM_KEY to parent, POSITION_KEY to position, NEW_REPLY_KEY to reply)
            }
        }
    }


}