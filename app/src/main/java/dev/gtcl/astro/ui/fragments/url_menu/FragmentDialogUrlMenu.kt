package dev.gtcl.astro.ui.fragments.url_menu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import dev.gtcl.astro.R
import dev.gtcl.astro.URL_KEY
import dev.gtcl.astro.databinding.FragmentDialogUrlBinding
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.url.PREFIXED_REDDIT_ITEM

class FragmentDialogUrlMenu: DialogFragment() {

    private var binding: FragmentDialogUrlBinding? = null

    private val activityModel: MainActivityVM by activityViewModels()

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
//        dialog?.window?.setBackgroundDrawableResource(android.R.color.black) // This makes the dialog full screen
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDialogUrlBinding.inflate(LayoutInflater.from(requireContext()))
        val url = requireArguments().getString(URL_KEY, "")

        binding?.apply {
            this.url = url
            fragmentDialogUrlOpenInBrowser.root.setOnClickListener {
                activityModel.openChromeTab(url)
                dismiss()
            }
            fragmentDialogUrlShareLink.root.setOnClickListener {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, getText(R.string.share_subject_message))
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                startActivity(Intent.createChooser(shareIntent, null))
                dismiss()
            }
            fragmentDialogUrlCopyLink.root.setOnClickListener {
                val context = requireContext()
                val clip = ClipData.newPlainText(null, url)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, context.getString(R.string.link_copied), Toast.LENGTH_SHORT).show()
                dismiss()
            }
            executePendingBindings()
        }

        return binding!!.root
    }

    companion object{
        fun newInstance(url: String) = FragmentDialogUrlMenu().apply {
            val isRedditPrefixedItem = PREFIXED_REDDIT_ITEM.matches(url)
            val fullUrl = if(isRedditPrefixedItem){
                val sb = StringBuilder("https://www.reddit.com")
                if(!url.startsWith("/")){
                    sb.append("/")
                }
                sb.append(url)
                sb.toString()
            } else {
                url
            }
            arguments = bundleOf(URL_KEY to fullUrl)
        }
    }
}