package dev.gtcl.astro.ui.fragments.inbox

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.astro.*
import dev.gtcl.astro.databinding.FragmentDialogComposeMessageBinding

class ComposeDialogFragment : DialogFragment(){

    private lateinit var binding: FragmentDialogComposeMessageBinding

    private val model: ComposeVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
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
        binding = FragmentDialogComposeMessageBinding.inflate(inflater)
        binding.model = model
        binding.lifecycleOwner = viewLifecycleOwner

        if(!model.initialized){
            val user = arguments?.getString(USER_KEY)
            if(user != null){
                binding.fragmentDialogComposeMessageToText.setText(user)
            } else {
                val sharedPrefs = requireContext().getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
                val to = sharedPrefs.getString(TO_KEY, "")
                val subject = sharedPrefs.getString(SUBJECT_KEY, "")
                val message = sharedPrefs.getString(MESSAGE_KEY, "")
                binding.fragmentDialogComposeMessageToText.setText(to)
                binding.fragmentDialogComposeMessageSubjectText.setText(subject)
                binding.fragmentDialogComposeMessageMessageText.setText(message)
            }
        }

        binding.fragmentDialogComposeMessageToolbar.setNavigationOnClickListener {
            if(model.isLoading.value != true) {
                dismiss()
            }
        }

        binding.fragmentDialogComposeMessageToolbar.setOnMenuItemClickListener {
            if(model.isLoading.value == true){
                return@setOnMenuItemClickListener false
            }
            val to = binding.fragmentDialogComposeMessageToText.text.toString()
            val subject = binding.fragmentDialogComposeMessageSubjectText.text.toString()
            val message = binding.fragmentDialogComposeMessageMessageText.text.toString()
            var valid = true

            if(to.isBlank()){
                binding.fragmentDialogComposeMessageToInputLayout.error = getString(R.string.required)
                valid = false
            }

            if(subject.isBlank()){
                binding.fragmentDialogComposeMessageSubjectInputLayout.error = getString(R.string.required)
                valid = false
            }

            if(subject.length > 100){
                binding.fragmentDialogComposeMessageSubjectInputLayout.error = getString(R.string.invalid)
                valid = false
            }

            if(message.isBlank()){
                binding.fragmentDialogComposeMessageMessageInputLayout.error = getString(R.string.required)
                valid = false
            }

            if(valid){
                model.sendMessage(to, subject, message)
                return@setOnMenuItemClickListener true
            }

            return@setOnMenuItemClickListener false
        }

        model.errorMessage.observe(viewLifecycleOwner, {
            if(it != null){
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                model.errorMessageObserved()
            }
        })

        model.isLoading.observe(viewLifecycleOwner, {
            dialog?.apply {
                setCancelable(!it)
                setCanceledOnTouchOutside(!it)
            }
        })

        model.userDoesNotExist.observe(viewLifecycleOwner, {
            if(it != null){
                binding.fragmentDialogComposeMessageToInputLayout.error = getString(R.string.user_does_not_exist)
                model.userDoesNotExistObserved()
            }
        })

        model.messageSent.observe(viewLifecycleOwner, {
            if(it){
                dismiss()
            }
        })

        setTextChangeListeners()

        model.initialized = true

        return binding.root
    }

    private fun setTextChangeListeners(){
        binding.fragmentDialogComposeMessageToText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.fragmentDialogComposeMessageToInputLayout.error = null
            }
        })

        binding.fragmentDialogComposeMessageSubjectText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.fragmentDialogComposeMessageSubjectInputLayout.error = null
            }
        })

        binding.fragmentDialogComposeMessageMessageText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.fragmentDialogComposeMessageMessageInputLayout.error = null
            }
        })
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if(model.messageSent.value == true){
            clearSharedPreferenceDraft()
        } else {
            val to = binding.fragmentDialogComposeMessageToText.text.toString()
            val subject = binding.fragmentDialogComposeMessageSubjectText.text.toString()
            val message = binding.fragmentDialogComposeMessageMessageText.text.toString()
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