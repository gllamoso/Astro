package dev.gtcl.reddit.ui.fragments.account.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.databinding.FragmentRecyclerViewBinding
import dev.gtcl.reddit.models.reddit.ProfileListing
import dev.gtcl.reddit.ui.LoadMoreScrollListener
import dev.gtcl.reddit.ui.OnLoadMoreListener
import dev.gtcl.reddit.actions.PostActions
import dev.gtcl.reddit.ui.fragments.LoadMoreScrollViewModel

class UserDownvotedFragment : SimpleListingScrollerFragment() {
    override fun setListingInfo() {
        model.setListingInfo(ProfileListing(ProfileInfo.DOWNVOTED), PostSort.BEST, null, 40)
    }
}