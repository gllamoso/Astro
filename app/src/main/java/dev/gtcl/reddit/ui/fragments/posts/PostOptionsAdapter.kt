package dev.gtcl.reddit.ui.fragments.posts

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.databinding.ItemMenuBinding

class PostOptionsAdapter(val context: Context): BaseAdapter(){

    @SuppressLint("ResourceType")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        if(convertView != null) return convertView
        val imageView = ImageView(context)
        imageView.setImageResource(R.drawable.ic_more_vert_24dp)
        return imageView
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: OptionViewHolder
        val resultView: View
        if(convertView == null) {
            viewHolder = OptionViewHolder.create(context)
            val view = viewHolder.getView()
            view.tag = viewHolder
            resultView = view
        }
        else {
            viewHolder = convertView.tag as OptionViewHolder
            resultView = viewHolder.getView()
        }
        viewHolder.bind(getItem(position))
        return resultView
    }

    override fun getItem(position: Int): MenuItem {
        return when(position){
            0 -> MenuItem.UPVOTE
            1 -> MenuItem.DOWNVOTE
            2 -> MenuItem.COMMENTS
            3 -> MenuItem.SHARE
            4 -> MenuItem.AWARD
            5 -> MenuItem.SAVE
            6 -> MenuItem.HIDE
            7 -> MenuItem.REPORT
            else -> throw NoSuchElementException()
        }
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount() = OPTIONS_SIZE

}

class OptionViewHolder private constructor(private val binding: ItemMenuBinding){

    fun bind(menuItem: MenuItem){
        binding.text.setText(menuItem.labelResId)
        binding.icon.setImageResource(menuItem.iconResId)
        binding.executePendingBindings()
    }

    fun getView() = binding.root

    companion object{
        fun create(context: Context): OptionViewHolder = OptionViewHolder(ItemMenuBinding.inflate(LayoutInflater.from(context)))
    }
}

private const val OPTIONS_SIZE = 8
enum class MenuItem(val labelResId: Int, val iconResId: Int) {
    UPVOTE(R.string.upvote, R.drawable.ic_upvote_24dp),
    DOWNVOTE(R.string.downvote, R.drawable.ic_downvote_24dp),
    COMMENTS(R.string.comments, R.drawable.ic_comment_24dp),
    SHARE(R.string.share, R.drawable.ic_share_24dp),
    AWARD(R.string.award, R.drawable.ic_award_24dp),
    SAVE(R.string.save, R.drawable.ic_bookmark_24dp),
    HIDE(R.string.hide, R.drawable.ic_x_24dp),
    REPORT(R.string.report, R.drawable.ic_flag_24dp)
}