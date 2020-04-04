package dev.gtcl.reddit.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.databinding.ItemMenuBinding
import dev.gtcl.reddit.listings.Post

class PostOptionsAdapter(val context: Context, var post: Post): BaseAdapter(){

    @SuppressLint("ResourceType")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        if(convertView != null) return convertView
        return ImageView(context).apply { setImageResource(R.drawable.ic_more_vert_24dp) }
    }

    override fun isEnabled(position: Int) = position in 0 until OPTIONS_SIZE - 1

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
            MenuItem.UPVOTE -> viewHolder.bind(item, if(post.likes == true) ContextCompat.getColor(context, android.R.color.holo_orange_dark) else null)
            MenuItem.DOWNVOTE -> viewHolder.bind(item, if(post.likes == false) ContextCompat.getColor(context, android.R.color.holo_blue_dark) else null)
            MenuItem.PROFILE -> viewHolder.bind(item, customLabel = post.author)
            else -> viewHolder.bind(item)
        }
        return resultView
    }

    override fun getItem(position: Int): MenuItem {
        return when(position){
            MenuItem.UPVOTE.position -> MenuItem.UPVOTE
            MenuItem.DOWNVOTE.position -> MenuItem.DOWNVOTE
            MenuItem.SHARE.position -> MenuItem.SHARE
            MenuItem.PROFILE.position -> MenuItem.PROFILE
            MenuItem.AWARD.position -> MenuItem.AWARD
            MenuItem.SAVE.position -> if(post.saved) MenuItem.UNSAVE else MenuItem.SAVE
            MenuItem.HIDE.position -> if(post.hidden) MenuItem.UNHIDE else MenuItem.HIDE
            MenuItem.REPORT.position -> MenuItem.REPORT
            MenuItem.EMPTY.position -> MenuItem.EMPTY
            else -> throw NoSuchElementException()
        }
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount() = OPTIONS_SIZE

}

class OptionViewHolder private constructor(private val binding: ItemMenuBinding): RecyclerView.ViewHolder(binding.root){

    fun bind(menuItem: MenuItem, iconTint: Int? = null, customLabel: String? = null){
        if(menuItem.labelResId == null || menuItem.iconResId == null) {
            itemView.layoutParams = AbsListView.LayoutParams(-1, 1)
            return
        }
        if(customLabel == null)
            binding.text.setText(menuItem.labelResId)
        else
            binding.text.text = customLabel
        binding.icon.setImageResource(menuItem.iconResId)
        if(iconTint != null)
            binding.icon.setColorFilter(iconTint, android.graphics.PorterDuff.Mode.SRC_IN)
        else
            binding.icon.colorFilter = null
        binding.executePendingBindings()
    }

    companion object{
        fun create(context: Context): OptionViewHolder =
            OptionViewHolder(
                ItemMenuBinding.inflate(LayoutInflater.from(context))
            )
    }
}

const val OPTIONS_SIZE = 9
enum class MenuItem(val labelResId: Int?, val iconResId: Int?, val position: Int) {
    UPVOTE(R.string.upvote, R.drawable.ic_upvote_24dp, 0),
    DOWNVOTE(R.string.downvote, R.drawable.ic_downvote_24dp, 1),
    SHARE(R.string.share, R.drawable.ic_share_24dp, 2),
    PROFILE(R.string.profile, R.drawable.ic_profile_24dp, 3),
    AWARD(R.string.award, R.drawable.ic_award_24dp, 4),
    SAVE(R.string.save, R.drawable.ic_bookmark_24dp, 5),
    UNSAVE(R.string.unsave, R.drawable.ic_remove_circle_outline_24dp, 5),
    HIDE(R.string.hide, R.drawable.ic_hide_24dp, 6),
    UNHIDE(R.string.unhide, R.drawable.ic_unhide_24dp, 6),
    REPORT(R.string.report, R.drawable.ic_flag_24dp, 7),
    EMPTY(null, null, 8)
}