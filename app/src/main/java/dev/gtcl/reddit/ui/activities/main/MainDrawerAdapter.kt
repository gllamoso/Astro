package dev.gtcl.reddit.ui.activities.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ImageView
import android.widget.TextView
import dev.gtcl.reddit.*
import dev.gtcl.reddit.models.reddit.Account

class MainDrawerAdapter(val context: Context, private val drawerOnClickListeners: DrawerOnClickListeners) : BaseExpandableListAdapter(){
    private val addAccountString = context.getString(R.string.add_account)
    private val logoutString = context.getString(R.string.logout)

    private val groups = listOf(context.getString(R.string.accounts), context.getString(R.string.home), context.getString(R.string.my_account), context.getString(R.string.settings))
    private lateinit var accountOptions: List<String>
    private lateinit var accounts: List<Account>

    override fun getGroup(groupPosition: Int): Any = groups[groupPosition]

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true

    override fun hasStableIds(): Boolean = false

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
        var convertViewVar = convertView
        val groupTitle = getGroup(groupPosition) as String
        if(convertViewVar == null) {
            val layoutInflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertViewVar = layoutInflater.inflate(R.layout.item_group_expandable, null)
        }
        val titleTextView = convertViewVar!!.findViewById<TextView>(R.id.title)
//        titleTextView.setTypeface(null, Typeface.BOLD)
        titleTextView.text = groupTitle

        when(groupPosition){
            0 -> {
                convertViewVar.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_profiles_24dp)
                if (isExpanded)
                    convertViewVar.findViewById<ImageView>(R.id.indicator).setImageResource(R.drawable.ic_up_no_stem_24dp)
                else
                    convertViewVar.findViewById<ImageView>(R.id.indicator).setImageResource(R.drawable.ic_down_no_stem_24dp)
            }
            1 -> {
                convertViewVar.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_home_24dp)
                convertViewVar.setOnClickListener {
                    drawerOnClickListeners.onHomeClicked()
                }
            }
            2 -> {
                convertViewVar.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_profile_24dp)
                convertViewVar.setOnClickListener {
                    drawerOnClickListeners.onMyAccountClicked()
                }
            }
            3 -> {
                convertViewVar.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_settings_24dp)
                convertViewVar.setOnClickListener {
                    drawerOnClickListeners.onSettingsClicked()
                }
            }
        }
        return convertViewVar
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return if(groupPosition == 0) accountOptions.size
            else 0
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return if (groupPosition == 0) accountOptions[childPosition]
            else Any()
    }

    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View {
        var convertViewVar = convertView
        val child = getChild(0, childPosition)
        if(convertViewVar == null) {
            val layoutInflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertViewVar = layoutInflater.inflate(R.layout.item_child_expandable, null)
        }
        val titleTextView = convertViewVar!!.findViewById<TextView>(R.id.title)
        val icon = convertViewVar.findViewById<ImageView>(R.id.right_icon)
        when(childPosition){
            accountOptions.size - 2 -> {
                convertViewVar.setOnClickListener{
                    drawerOnClickListeners.onAddAccountClicked()
                }
                icon.setImageResource(R.drawable.ic_add_24dp)
                titleTextView.text = child as String
            }
            accountOptions.size - 1 -> {
                convertViewVar.setOnClickListener{
                    drawerOnClickListeners.onLogoutClicked()
                }
                icon.setImageResource(R.drawable.ic_logout_24dp)
                titleTextView.text = child as String
            }
            else -> {
                val user = accounts[childPosition]
                titleTextView.text = user.name
                convertViewVar.setOnClickListener{
                    drawerOnClickListeners.onAccountClicked(user)
                }
                icon.setOnClickListener {
                    drawerOnClickListeners.onRemoveAccountClicked(getChild(0, childPosition) as String)
                }
                icon.setImageResource(R.drawable.ic_remove_circle_outline_24dp)
            }
        }

        return convertViewVar
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()

    override fun getGroupCount(): Int = groups.size

    fun setUsers(listOfAccounts: List<Account>){
        accounts = listOfAccounts
        val options = (listOfAccounts.map { it.name } as MutableList)
        options.add(addAccountString)
        options.add(logoutString)
        accountOptions = options
        notifyDataSetChanged()
    }

}

interface DrawerOnClickListeners{
    // Accounts
    fun onAddAccountClicked()
    fun onRemoveAccountClicked(username: String)
    fun onAccountClicked(account: Account)
    fun onLogoutClicked()

    // Posts
    fun onHomeClicked()

    // My Account
    fun onMyAccountClicked()

    // Settings
    fun onSettingsClicked()
}