package dev.gtcl.astro.ui

import android.content.Context
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.LeftDrawerHeader
import dev.gtcl.astro.R
import dev.gtcl.astro.actions.LeftDrawerActions
import dev.gtcl.astro.database.SavedAccount
import dev.gtcl.astro.ui.viewholders.LeftDrawerItemVH

class LeftDrawerAdapter(
    private val context: Context,
    private val leftDrawerActions: LeftDrawerActions,
    private val currentHeader: LeftDrawerHeader? = null
) : RecyclerView.Adapter<LeftDrawerItemVH>() {

    var isExpanded = false
        set(value) {
            val notify = isExpanded != value
            if (notify) {
                notifyItemRangeRemoved(0, itemCount)
                field = value
                notifyItemRangeInserted(0, itemCount)
            }
        }
    private var users: List<SavedAccount> = listOf()

    fun toggleExpanded() {
        isExpanded = !isExpanded
    }

    fun submitUsers(users: List<SavedAccount>) {
        this.users = users
        if (isExpanded) {
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        LeftDrawerItemVH.create(parent)

    override fun getItemCount(): Int {
        return if (!isExpanded) {
            4
        } else {
            2 + users.size
        }
    }

    override fun onBindViewHolder(holder: LeftDrawerItemVH, position: Int) {
        if (!isExpanded) {
            when (position) {
                0 -> holder.bind(
                    currentHeader == LeftDrawerHeader.HOME,
                    context.getString(R.string.home),
                    ContextCompat.getDrawable(context, R.drawable.ic_home_24),
                    null,
                    onClick = { leftDrawerActions.onHomeClicked() })
                1 -> holder.bind(
                    currentHeader == LeftDrawerHeader.MY_ACCOUNT,
                    context.getString(R.string.my_account),
                    ContextCompat.getDrawable(context, R.drawable.ic_profile_24),
                    null,
                    onClick = { leftDrawerActions.onMyAccountClicked() })
                2 -> holder.bind(
                    currentHeader == LeftDrawerHeader.INBOX,
                    context.getString(R.string.inbox),
                    ContextCompat.getDrawable(context, R.drawable.ic_mail_closed_24),
                    null,
                    onClick = { leftDrawerActions.onInboxClicked() })
                3 -> holder.bind(
                    currentHeader == LeftDrawerHeader.SETTINGS,
                    context.getString(R.string.settings),
                    ContextCompat.getDrawable(context, R.drawable.ic_settings_24),
                    null,
                    onClick = { leftDrawerActions.onSettingsClicked() })
            }
        } else {
            when (position) {
                users.size -> holder.bind(
                    false,
                    context.getString(R.string.add_account),
                    null,
                    ContextCompat.getDrawable(context, R.drawable.ic_add_24),
                    onClick = { leftDrawerActions.onAddAccountClicked() })
                users.size + 1 -> holder.bind(
                    false,
                    context.getString(R.string.logout),
                    null,
                    ContextCompat.getDrawable(context, R.drawable.ic_logout_24),
                    onClick = { leftDrawerActions.onLogoutClicked() }) {
                }
                else -> holder.bind(
                    false,
                    users[position].name,
                    ContextCompat.getDrawable(context, R.drawable.ic_profile_24),
                    ContextCompat.getDrawable(context, R.drawable.ic_remove_circle_outline_24),
                    onClick = { leftDrawerActions.onAccountClicked(users[position]) },
                    onRightIconClicked = { leftDrawerActions.onRemoveAccountClicked(users[position]) })
            }
        }
    }

}