package dev.gtcl.reddit.ui.viewholders

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.Vote
import dev.gtcl.reddit.actions.UserActions
import dev.gtcl.reddit.databinding.ItemUserBinding
import dev.gtcl.reddit.databinding.PopupPostOptionsBinding
import dev.gtcl.reddit.databinding.PopupUserActionsBinding
import dev.gtcl.reddit.models.reddit.User
import dev.gtcl.reddit.models.reddit.UserType

class UserVH private constructor(private val binding: ItemUserBinding): RecyclerView.ViewHolder(binding.root){

    fun bind(user: User, userType: UserType, userActions: UserActions){
        binding.user = user

        binding.background.setOnClickListener {
            userActions.viewProfile(user)
        }

        binding.moreOptions.setOnClickListener {
            showPopupWindow(user, userType, userActions, it)
        }

        binding.executePendingBindings()
    }

    private fun showPopupWindow(user: User, userType: UserType, userActions: UserActions, anchorView: View){
        val inflater = anchorView.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupUserActionsBinding.inflate(inflater)
        val popupWindow = PopupWindow(popupBinding.root, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT, true)
        popupBinding.apply {
            this.userType = userType
            executePendingBindings()
            viewProfile.root.setOnClickListener {
                userActions.viewProfile(user)
                popupWindow.dismiss()
            }
            if(userType == UserType.FRIEND){
                message.root.setOnClickListener {
                    userActions.message(user)
                    popupWindow.dismiss()
                }
            }
            remove.root.setOnClickListener{
                userActions.remove(adapterPosition)
                popupWindow.dismiss()
            }
        }
        popupBinding.root.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        popupWindow.width = ViewGroup.LayoutParams.WRAP_CONTENT
        popupWindow.height = popupBinding.root.measuredHeight
        popupWindow.elevation = 20F
        popupWindow.showAsDropDown(anchorView)
        popupBinding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup) = UserVH(ItemUserBinding.inflate(LayoutInflater.from(parent.context)))
    }
}