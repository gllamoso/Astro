package dev.gtcl.reddit.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ImageView
import android.widget.TextView
import dev.gtcl.reddit.*
import dev.gtcl.reddit.users.User

class CustomExpandableListAdapter(val context: Context, private val adapterOnClickListeners: AdapterOnClickListeners) : BaseExpandableListAdapter(){
    private val addAccountString = context.getString(R.string.add_account)
    private val logoutString = context.getString(R.string.logout)

    private val groups = listOf(context.getString(R.string.accounts), context.getString(R.string.posts), context.getString(R.string.users), context.getString(R.string.settings))
    private lateinit var accountOptions: List<String>
    private lateinit var users: List<User>

    override fun getGroup(groupPosition: Int): Any = groups[groupPosition]

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = groupPosition == 0

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
                convertViewVar.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_account_circle_black_24dp)
                if (isExpanded)
                    convertViewVar.findViewById<ImageView>(R.id.indicator).setImageResource(R.drawable.ic_up_no_stem_24dp)
                else
                    convertViewVar.findViewById<ImageView>(R.id.indicator).setImageResource(R.drawable.ic_down_no_stem_24dp)
            }
            1 -> convertViewVar.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_posts_black_24dp)
            2 -> convertViewVar.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_user_24dp)
            3 -> convertViewVar.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_settings_black_24dp)
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
        val childTitle = getChild(0, childPosition) as String
        if(convertViewVar == null) {
            val layoutInflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertViewVar = layoutInflater.inflate(R.layout.item_child_expandable, null)
        }
        val titleTextView = convertViewVar!!.findViewById<TextView>(R.id.title)
        titleTextView.text = childTitle
        val removeButton = convertViewVar.findViewById<ImageView>(R.id.remove_button)
        val icon = convertViewVar.findViewById<ImageView>(R.id.icon)
        when(childPosition){
            accountOptions.size - 2 -> {
                convertViewVar.setOnClickListener{
                    adapterOnClickListeners.onAddAccountClicked()
                }
                removeButton.visibility = View.GONE
                icon.visibility = View.VISIBLE
                icon.setImageResource(R.drawable.ic_add_black_24dp)
            }
            accountOptions.size - 1 -> {
                convertViewVar.setOnClickListener{
                    adapterOnClickListeners.onLogoutClicked()
                }
                removeButton.visibility = View.GONE
                icon.visibility = View.VISIBLE
                icon.setImageResource(R.drawable.ic_logout_black_24dp)
            }
            else -> {
                convertViewVar.setOnClickListener{
                    adapterOnClickListeners.onAccountClicked(users[childPosition])
                }
                icon.visibility = View.GONE
                removeButton.visibility = View.VISIBLE
                removeButton.setOnClickListener {
                    adapterOnClickListeners.onRemoveAccountClicked(getChild(0, childPosition) as String)
                }
            }
        }

        return convertViewVar
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()

    override fun getGroupCount(): Int = groups.size

    fun setUsers(listOfUsers: List<User>){
        users = listOfUsers
        val options = (listOfUsers.map { it.name } as MutableList)
        options.add(addAccountString)
        options.add(logoutString)
        accountOptions = options
        notifyDataSetChanged()
    }

}

interface AdapterOnClickListeners{

    fun onAddAccountClicked()

    fun onRemoveAccountClicked(username: String)

    fun onAccountClicked(user: User)

    fun onLogoutClicked()
}