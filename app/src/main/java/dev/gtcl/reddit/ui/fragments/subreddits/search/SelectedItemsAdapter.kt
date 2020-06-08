package dev.gtcl.reddit.ui.fragments.subreddits.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemSelectedBinding

class SelectedItemsAdapter(private val onClickListener: (String) -> Unit): ListAdapter<String, SelectedItemsAdapter.SelectedItemVH>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SelectedItemVH.create(parent)

    override fun onBindViewHolder(holder: SelectedItemVH, position: Int) {
        val name = getItem(position)
        holder.bind(name, onClickListener)
    }

    class SelectedItemVH private constructor(private val binding: ItemSelectedBinding): RecyclerView.ViewHolder(binding.root){

        fun bind(name: String, onClickListener: (String) -> Unit){
            val displayName = if(name.startsWith("u_")){
                name.replaceFirst("u_", "u/")
            } else {
                name
            }
            binding.name = displayName
            binding.root.setOnClickListener {
                onClickListener.invoke(name)
            }
            binding.executePendingBindings()
        }

        companion object{
            fun create(parent: ViewGroup) =  SelectedItemVH(ItemSelectedBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }

    companion object{
        private val DIFF_CALLBACK = object: DiffUtil.ItemCallback<String>(){
            override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
            override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        }
    }

}