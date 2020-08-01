package dev.gtcl.reddit.ui

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.LeftDrawerHeader
import dev.gtcl.reddit.R
import dev.gtcl.reddit.actions.LeftDrawerActions
import dev.gtcl.reddit.database.SavedAccount
import dev.gtcl.reddit.ui.viewholders.ExpandableItem
import dev.gtcl.reddit.ui.viewholders.ExpandableHeaderVH
import dev.gtcl.reddit.ui.viewholders.LeftDrawerItemVH

class LeftDrawerAdapter(
    private val context: Context,
    private val leftDrawerActions: LeftDrawerActions,
    private val currentHeader: LeftDrawerHeader? = null
) : RecyclerView.Adapter<LeftDrawerItemVH>(){

    var isExpanded = false
        set(value){
            val notify = isExpanded != value
            if(notify){
                notifyItemRangeRemoved(0, itemCount)
                field = value
                notifyItemRangeInserted(0, itemCount)
            }
        }
    private var users: List<SavedAccount> = listOf()

    fun submitUsers(users: List<SavedAccount>){
        this.users = users
        if(isExpanded){
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = LeftDrawerItemVH.create(parent)

    override fun getItemCount(): Int{
        return if(!isExpanded){
            4
        } else {
            2 + users.size
        }
    }

    override fun onBindViewHolder(holder: LeftDrawerItemVH, position: Int) {
        if(!isExpanded){
            when(position){
                0 -> holder.bind(
                    currentHeader == LeftDrawerHeader.HOME,
                    context.getString(R.string.home),
                    context.getDrawable(R.drawable.ic_home_24dp),
                    null,
                        onClick = { leftDrawerActions.onHomeClicked() })
                1 -> holder.bind(
                    currentHeader == LeftDrawerHeader.MY_ACCOUNT,
                    context.getString(R.string.my_account),
                    context.getDrawable(R.drawable.ic_profile_24dp),
                    null,
                    onClick = {leftDrawerActions.onMyAccountClicked()})
                2 -> holder.bind(
                    currentHeader == LeftDrawerHeader.INBOX,
                    context.getString(R.string.inbox),
                    context.getDrawable(R.drawable.ic_inbox_24dp),
                    null,
                    onClick = {leftDrawerActions.onInboxClicked()})
                3 -> holder.bind(
                    currentHeader == LeftDrawerHeader.SETTINGS,
                    context.getString(R.string.settings),
                    context.getDrawable(R.drawable.ic_settings_24dp),
                    null,
                    onClick = {leftDrawerActions.onSettingsClicked()})
            }
        } else {
            when (position) {
                users.size -> holder.bind(
                    false,
                    context.getString(R.string.add_account),
                     null,
                    context.getDrawable(R.drawable.ic_add_24dp),
                    onClick = { leftDrawerActions.onAddAccountClicked() })
                users.size + 1 -> holder.bind(
                    false,
                    context.getString(R.string.logout),
                    null,
                    context.getDrawable(R.drawable.ic_logout_24dp),
                    onClick = { leftDrawerActions.onLogoutClicked() }){
                }
                else -> holder.bind(
                    false,
                    users[position].name,
                    context.getDrawable(R.drawable.ic_profile_24dp),
                    context.getDrawable(R.drawable.ic_remove_circle_outline_24dp),
                    onClick = { leftDrawerActions.onAccountClicked(users[position])},
                    onRightIconClicked = {leftDrawerActions.onRemoveAccountClicked(users[position])})
            }
        }
    }

}