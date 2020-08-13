package dev.gtcl.reddit.ui.fragments.inbox

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dev.gtcl.reddit.*
import dev.gtcl.reddit.database.SavedAccount
import dev.gtcl.reddit.databinding.FragmentDialogComposeBinding
import dev.gtcl.reddit.ui.fragments.reply.ReplyVM

class ComposeDialogFragment : DialogFragment(){

    private lateinit var binding: FragmentDialogComposeBinding

    private val model: ComposeVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(ComposeVM::class.java)
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
        binding = FragmentDialogComposeBinding.inflate(inflater)
        binding.model = model
        binding.lifecycleOwner = viewLifecycleOwner

        if(!model.initialized){
            val user = arguments?.getString(USER_KEY)
            if(user != null){
                binding.toText.setText(user)
            } else {
                val sharedPrefs = requireContext().getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
                val to = sharedPrefs.getString(TO_KEY, "")
                val subject = sharedPrefs.getString(SUBJECT_KEY, "")
                val message = sharedPrefs.getString(MESSAGE_KEY, "")
                binding.toText.setText(to)
                binding.subjectText.setText(subject)
                binding.messageText.setText(message)
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            if(model.isLoading.value != true) {
                dismiss()
            }
        }

        binding.toolbar.setOnMenuItemClickListener {
            if(model.isLoading.value == true){
                return@setOnMenuItemClickListener false
            }
            val to = binding.toText.text.toString()
            val subject = binding.subjectText.text.toString()
            val message = binding.messageText.text.toString()
            var valid = true

            if(to.isBlank()){
                binding.toInputLayout.error = getString(R.string.required)
                valid = false
            }

            if(subject.isBlank()){
                binding.subjectInputLayout.error = getString(R.string.required)
                valid = false
            }

            if(subject.length > 100){
                binding.subjectInputLayout.error = getString(R.string.invalid)
                valid = false
            }

            if(message.isBlank()){
                binding.messageInputLayout.error = getString(R.string.required)
                valid = false
            }

            if(valid){
                model.sendMessage(to, subject, message)
                return@setOnMenuItemClickListener true
            }

            return@setOnMenuItemClickListener false
        }

        model.errorMessage.observe(viewLifecycleOwner, Observer {
            if(it != null){
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                model.errorMessageObserved()
            }
        })

        model.isLoading.observe(viewLifecycleOwner, Observer {
            dialog?.apply {
                setCancelable(!it)
                setCanceledOnTouchOutside(!it)
            }
        })

        model.userDoesNotExist.observe(viewLifecycleOwner, Observer {
            if(it != null){
                binding.toInputLayout.error = getString(R.string.user_does_not_exist)
                model.userDoesNotExistObserved()
            }
        })

        model.messageSent.observe(viewLifecycleOwner, Observer {
            if(it){
                dismiss()
            }
        })

        setTextChangeListeners()

        model.initialized = true

        return binding.root
    }

    private fun setTextChangeListeners(){
        binding.toText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.toInputLayout.error = null
            }
        })

        binding.subjectText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.subjectInputLayout.error = null
            }
        })

        binding.messageText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.messageInputLayout.error = null
            }
        })
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if(model.messageSent.value == true){
            clearSharedPreferenceDraft()
        } else {
            val to = binding.toText.text.toString()
            val subject = binding.subjectText.text.toString()
            val message = binding.messageText.text.toString()
            if(to.isNotBlank() || subject.isNotBlank() || message.isNotBlank()){
                val bundle = bundleOf(TO_KEY to to, SUBJECT_KEY to subject, MESSAGE_KEY to message)
                setFragmentResult(DRAFT_KEY, bundle)
            }
        }
    }

    private fun clearSharedPreferenceDraft(){
        val sharedPrefs = requireContext().getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            remove(TO_KEY)
            remove(SUBJECT_KEY)
            remove(MESSAGE_KEY)
            commit()
        }
    }

    companion object{
        fun newInstance(user: String? = null): ComposeDialogFragment{
            return ComposeDialogFragment().apply {
                arguments = bundleOf(USER_KEY to user)
            }
        }
    }
}