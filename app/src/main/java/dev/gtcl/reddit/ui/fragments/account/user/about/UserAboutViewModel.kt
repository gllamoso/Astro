package dev.gtcl.reddit.ui.fragments.account.user.about

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.listings.users.UserRepository

class UserAboutViewModel(application: RedditApplication) : AndroidViewModel(application){
    // Repos
    val userRepository = UserRepository.getInstance(application)

}