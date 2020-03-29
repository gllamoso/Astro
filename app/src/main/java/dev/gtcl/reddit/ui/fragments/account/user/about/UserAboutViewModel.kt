package dev.gtcl.reddit.ui.fragments.account.user.about

import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.RedditApplication

class UserAboutViewModel(application: RedditApplication) : ViewModel(){
    // Repos
    val userRepository = application.userRepository

}