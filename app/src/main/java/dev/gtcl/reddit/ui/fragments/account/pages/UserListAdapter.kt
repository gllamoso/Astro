package dev.gtcl.reddit.ui.fragments.account.pages

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.actions.UserActions
import dev.gtcl.reddit.models.reddit.User
import dev.gtcl.reddit.models.reddit.UserType
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.viewholders.NetworkStateItemVH
import dev.gtcl.reddit.ui.viewholders.NoItemFoundVH
import dev.gtcl.reddit.ui.viewholders.UserVH

class UserListAdapter(
    private val userType: UserType,
    private val retry: () -> Unit,
    private val userActions: UserActions
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var users: MutableList<User>? = null

    var networkState: NetworkState = NetworkState.LOADING

    fun submitList(users: List<User>?){
        notifyItemRangeRemoved(0, itemCount)
        this.users = users?.toMutableList()
        if(users.isNullOrEmpty()){
            notifyItemChanged(0)
        } else {
            notifyItemRangeInserted(0, users.size)
        }
    }

    fun removeAt(position: Int){
        users?.removeAt(position)
        if(users.isNullOrEmpty()){
            notifyItemChanged(0)
        } else {
            notifyItemRemoved(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            R.layout.item_network_state -> NetworkStateItemVH.create(parent)
            R.layout.item_no_items_found -> NoItemFoundVH.create(parent)
            R.layout.item_user -> UserVH.create(parent)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun getItemCount(): Int {
        return if(users.isNullOrEmpty()){
            1
        } else {
            users!!.size
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(val viewType = getItemViewType(position)){
            R.layout.item_network_state -> (holder as NetworkStateItemVH).bind(networkState, retry)
            R.layout.item_no_items_found -> (holder as NoItemFoundVH).bind(null)
            R.layout.item_user -> (holder as UserVH).bind(users!![position], userType, userActions)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when{
            users == null -> R.layout.item_network_state
            users!!.isEmpty() -> R.layout.item_no_items_found
            else -> R.layout.item_user
        }
    }
}