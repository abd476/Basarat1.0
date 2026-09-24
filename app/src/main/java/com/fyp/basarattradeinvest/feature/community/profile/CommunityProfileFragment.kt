package com.fyp.basarattradeinvest.feature.community.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.databinding.FragmentCommunityProfileBinding
import com.fyp.basarattradeinvest.domain.model.community.CommunityProfile
import com.fyp.basarattradeinvest.feature.community.adapter.CommunityPostAdapter
import kotlinx.coroutines.launch

class CommunityProfileFragment : Fragment() {

    private var _binding: FragmentCommunityProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CommunityProfileViewModel by viewModels {
        CommunityProfileViewModel.Factory()
    }

    private lateinit var postAdapter: CommunityPostAdapter
    private var targetUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetUserId = arguments?.getString("user_id")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunityProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeUiState()

        viewModel.loadProfile(userId = targetUserId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.communityNotificationsFragment)
        }
    }

    private fun setupRecyclerView() {
        postAdapter = CommunityPostAdapter(
            onPostClick = { post ->
                if (!post.stockSymbol.isNullOrBlank()) {
                    val bundle = Bundle().apply { putString("symbol", post.stockSymbol) }
                    findNavController().navigate(R.id.stockDetailFragment, bundle)
                } else {
                    Toast.makeText(requireContext(), "Community Post #${post.id.take(6)}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.rvPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadProfile(userId = targetUserId, isRefresh = true)
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadProfile(userId = targetUserId)
        }

        binding.btnFollowersStat.setOnClickListener {
            val uid = targetUserId ?: "me"
            val bundle = Bundle().apply {
                putString("user_id", uid)
                putBoolean("is_following", false)
            }
            findNavController().navigate(R.id.communityFollowersFragment, bundle)
        }

        binding.btnFollowingStat.setOnClickListener {
            val uid = targetUserId ?: "me"
            val bundle = Bundle().apply {
                putString("user_id", uid)
                putBoolean("is_following", true)
            }
            findNavController().navigate(R.id.communityFollowersFragment, bundle)
        }

        binding.fabCreatePost.setOnClickListener {
            showCreatePostDialog()
        }

        binding.btnCreateFirstPost.setOnClickListener {
            showCreatePostDialog()
        }
    }

    private fun showCreatePostDialog() {
        val dialog = com.fyp.basarattradeinvest.feature.community.create.CreatePostBottomSheetDialogFragment.newInstance()
        dialog.onPostCreated = {
            viewModel.loadProfile(userId = targetUserId, isRefresh = true)
        }
        dialog.show(childFragmentManager, com.fyp.basarattradeinvest.feature.community.create.CreatePostBottomSheetDialogFragment.TAG)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is CommunityProfileUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.layoutError.visibility = View.GONE
                        }
                        is CommunityProfileUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.layoutError.visibility = View.GONE
                            binding.swipeRefresh.isRefreshing = false
                            bindProfile(state.profile)
                            postAdapter.submitList(state.posts)

                            if (state.posts.isEmpty()) {
                                binding.layoutEmptyPosts.visibility = View.VISIBLE
                            } else {
                                binding.layoutEmptyPosts.visibility = View.GONE
                            }
                        }
                        is CommunityProfileUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.swipeRefresh.isRefreshing = false
                            binding.layoutError.visibility = View.VISIBLE
                            binding.tvErrorMessage.text = state.message
                        }
                    }
                }
            }
        }
    }

    private fun bindProfile(profile: CommunityProfile) {
        binding.tvToolbarTitle.text = if (profile.isOwnProfile) "My Profile" else "@${profile.username}"
        binding.tvProfileFullName.text = profile.displayName
        binding.tvProfileUsername.text = "@${profile.username}"

        binding.tvStatsPostsCount.text = profile.publishedPostCount.toString()
        binding.tvStatsFollowersCount.text = profile.followersCount.toString()
        binding.tvStatsFollowingCount.text = profile.followingCount.toString()

        if (!profile.isOwnProfile && profile.isFollowing) {
            binding.tvFollowStatusBadge.visibility = View.VISIBLE
            binding.tvFollowStatusBadge.text = "Following"
        } else {
            binding.tvFollowStatusBadge.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
