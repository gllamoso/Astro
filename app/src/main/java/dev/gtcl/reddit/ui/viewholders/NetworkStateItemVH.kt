package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemNetworkStateBinding
import dev.gtcl.reddit.network.NetworkState


class NetworkStateItemVH private constructor(private val binding: ItemNetworkStateBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(networkState: NetworkState?, retryCallback: () -> Unit) {
        binding.networkState = networkState
        binding.itemNetworkStateRetryButton.setOnClickListener {
            retryCallback()
        }
        binding.executePendingBindings()
    }

    companion object {
        fun create(parent: ViewGroup): NetworkStateItemVH {
            return NetworkStateItemVH(ItemNetworkStateBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}