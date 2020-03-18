package dev.gtcl.reddit.ui.fragments.posts.listing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemNetworkStateBinding
import dev.gtcl.reddit.network.NetworkState

//class NetworkStateItemViewHolder(view: View, private val retryCallback: () -> Unit) : RecyclerView.ViewHolder(view) {
//    private val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)
//    private val retry = view.findViewById<Button>(R.id.retry_button)
//    private val errorMsg = view.findViewById<TextView>(R.id.error_msg)
//    init {
//        retry.setOnClickListener {
//            retryCallback()
//        }
//    }
//    fun bindTo(networkState: NetworkState?) {
//        progressBar.visibility = toVisibility(networkState?.status == Status.RUNNING)
//        retry.visibility = toVisibility(networkState?.status == Status.FAILED)
//        errorMsg.visibility = toVisibility(networkState?.msg != null)
//        errorMsg.text = networkState?.msg
//    }
//
//    companion object {
//        fun create(parent: ViewGroup, retryCallback: () -> Unit): NetworkStateItemViewHolder {
//            val view = LayoutInflater.from(parent.context)
//                .inflate(R.layout.item_network_state, parent, false)
//            return NetworkStateItemViewHolder(view, retryCallback)
//        }
//
//        fun toVisibility(constraint : Boolean): Int {
//            return if (constraint) {
//                View.VISIBLE
//            } else {
//                View.GONE
//            }
//        }
//    }
//}

class NetworkStateItemViewHolder private constructor(private val binding: ItemNetworkStateBinding, private val retryCallback: () -> Unit) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.retryButton.setOnClickListener {
            retryCallback()
        }
    }
    fun bindTo(networkState: NetworkState?) {
        binding.networkState = networkState
        binding.executePendingBindings()
    }

    companion object {
        fun create(parent: ViewGroup, retryCallback: () -> Unit): NetworkStateItemViewHolder {
            return NetworkStateItemViewHolder(ItemNetworkStateBinding.inflate(LayoutInflater.from(parent.context)), retryCallback)
        }
    }
}