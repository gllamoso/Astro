package dev.gtcl.reddit.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.databinding.ItemMenuBinding
import dev.gtcl.reddit.models.reddit.Post

class PostOptionsAdapter(val context: Context, var post: Post): BaseAdapter(){

    @SuppressLint("ResourceType")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        if(convertView != null) return convertView
        return ImageView(context).apply { setImageResource(R.drawable.ic_more_vert_24dp) }
    }

    override fun isEnabled(position: Int) = position in 0 until POST_OPTIONS_SIZE - 1

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: OptionViewHolder
        val resultView: View
        if(convertView == null) {
            viewHolder =
                OptionViewHolder.create(context)
            val view = viewHolder.itemView
            view.tag = viewHolder
            resultView = view
        }
        else {
            viewHolder = convertView.tag as OptionViewHolder
            resultView = viewHolder.itemView
        }
        when(val item = getItem(position)){
            PostMenuItem.UPVOTE -> viewHolder.bind(item, if(post.likes == true) ContextCompat.getColor(context, android.R.color.holo_orange_dark) else null)
            PostMenuItem.DOWNVOTE -> viewHolder.bind(item, if(post.likes == false) ContextCompat.getColor(context, android.R.color.holo_blue_dark) else null)
            PostMenuItem.PROFILE -> viewHolder.bind(item, customLabel = post.author)
            else -> viewHolder.bind(item)
        }
        return resultView
    }

    override fun getItem(position: Int): PostMenuItem {
        return when(position){
            PostMenuItem.UPVOTE.position -> PostMenuItem.UPVOTE
            PostMenuItem.DOWNVOTE.position -> PostMenuItem.DOWNVOTE
            PostMenuItem.SHARE.position -> PostMenuItem.SHARE
            PostMenuItem.PROFILE.position -> PostMenuItem.PROFILE
            PostMenuItem.SAVE.position -> if(post.saved) PostMenuItem.UNSAVE else PostMenuItem.SAVE
            PostMenuItem.HIDE.position -> if(post.hidden) PostMenuItem.UNHIDE else PostMenuItem.HIDE
            PostMenuItem.REPORT.position -> PostMenuItem.REPORT
            PostMenuItem.EMPTY.position -> PostMenuItem.EMPTY
            else -> throw NoSuchElementException()
        }
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount() = POST_OPTIONS_SIZE

}

class OptionViewHolder private constructor(private val binding: ItemMenuBinding): RecyclerView.ViewHolder(binding.root){

    fun bind(menuItem: PostMenuItem, iconTint: Int? = null, customLabel: String? = null){
        if(menuItem == PostMenuItem.EMPTY) {
            itemView.layoutParams = AbsListView.LayoutParams(-1, 1)
            return
        }
        binding.icon.setImageResource(menuItem.iconResId!!)
        if(customLabel == null) {
            binding.text.setText(menuItem.labelResId!!)
        }
        else {
            binding.text.text = customLabel
        }
        if(iconTint != null) {
            binding.icon.setColorFilter(iconTint, android.graphics.PorterDuff.Mode.SRC_IN)
        }
        else {
            binding.icon.colorFilter = null
        }
        binding.executePendingBindings()
    }

    companion object{
        fun create(context: Context): OptionViewHolder =
            OptionViewHolder(
                ItemMenuBinding.inflate(LayoutInflater.from(context))
            )
    }
}

const val POST_OPTIONS_SIZE = 8
enum class PostMenuItem(val labelResId: Int?, val iconResId: Int?, val position: Int) {
    UPVOTE(R.string.upvote, R.drawable.ic_upvote_24dp, 0),
    DOWNVOTE(R.string.downvote, R.drawable.ic_downvote_24dp, 1),
    SHARE(R.string.share, R.drawable.ic_share_24dp, 2),
    PROFILE(R.string.profile, R.drawable.ic_profile_24dp, 3),
    SAVE(R.string.save, R.drawable.ic_bookmark_24dp, 4),
    UNSAVE(R.string.unsave, R.drawable.ic_remove_circle_outline_24dp, 4),
    HIDE(R.string.hide, R.drawable.ic_hide_24dp, 5),
    UNHIDE(R.string.unhide, R.drawable.ic_unhide_24dp, 5),
    REPORT(R.string.report, R.drawable.ic_flag_24dp, 6),
    EMPTY(null, null, 7)
}