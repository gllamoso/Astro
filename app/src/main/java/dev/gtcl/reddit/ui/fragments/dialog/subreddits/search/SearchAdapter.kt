package dev.gtcl.reddit.ui.fragments.dialog.subreddits.search

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.ui.viewholders.SubredditViewHolder

class SearchAdapter(private val subredditActions: SubredditActions) : RecyclerView.Adapter<SubredditViewHolder>(){
    private var subscribedSubs: HashSet<String> = HashSet()
    private var favSubs: HashSet<String> = HashSet()
    private var subs: List<Subreddit> = listOf()

    fun submitList(subs: List<Subreddit>){
        for(sub: Subreddit in subs){
            sub.isAddedToDb = subscribedSubs.contains(sub.displayName)
            sub.isFavorite = favSubs.contains(sub.displayName)
        }
        this.subs = subs
        notifyDataSetChanged()
    }

    fun submitSubscriptions(subs: List<Subreddit>){
        subscribedSubs = subs.map { it.displayName }.toHashSet()
        favSubs = subs.filter { it.isFavorite }.map { it.displayName}.toHashSet()
        for(sub : Subreddit in subs){
            sub.apply {
                isAddedToDb = subscribedSubs.contains(displayName)
                isFavorite = favSubs.contains(displayName)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubredditViewHolder {
        return SubredditViewHolder.create(parent)
    }

    override fun getItemCount() = subs.size

    override fun onBindViewHolder(holder: SubredditViewHolder, position: Int) {
        holder.bind(subs[position], subredditActions, null)
    }

}