package dev.gtcl.reddit.ui.fragments.account.pages

import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.models.reddit.ProfileListing

class UserSavedFragment : SimpleListingScrollerFragment() {

    override fun setListingInfo() {
        model.setListingInfo(ProfileListing(ProfileInfo.SAVED), PostSort.BEST, null, 15)
    }
}