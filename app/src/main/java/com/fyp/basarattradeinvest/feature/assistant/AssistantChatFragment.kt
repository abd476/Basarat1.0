package com.fyp.basarattradeinvest.feature.assistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.databinding.FragmentAssistantChatBinding
import com.fyp.basarattradeinvest.feature.assistant.adapter.ChatMessageAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Chat interface screen for the BASARAT AI Investment Assistant.
 * Supports streaming responses, conversation history, regeneration, and portfolio-aware queries.
 */
class AssistantChatFragment : Fragment() {

    private var _binding: FragmentAssistantChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AssistantViewModel by viewModels { AssistantViewModel.Factory() }
    private lateinit var chatAdapter: ChatMessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssistantChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val conversationId = arguments?.getString("conversation_id")

        setupToolbar()
        setupRecyclerView()
        setupInput()
        observeState()

        if (!conversationId.isNullOrEmpty()) {
            viewModel.loadConversation(conversationId)
        } else {
            viewModel.startNewChat()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatMessageAdapter()
        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }

        binding.rvMessages.apply {
            this.layoutManager = layoutManager
            adapter = chatAdapter
            // Auto-scroll when new items are inserted
            chatAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    binding.rvMessages.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }
            })
        }
    }

    private fun setupInput() {
        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        binding.btnRegenerate.setOnClickListener {
            viewModel.regenerateLastResponse()
        }

        binding.btnRetry.setOnClickListener {
            val conversationId = arguments?.getString("conversation_id")
            if (!conversationId.isNullOrEmpty()) {
                viewModel.loadConversation(conversationId)
            } else {
                viewModel.startNewChat()
            }
        }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text?.toString()?.trim().orEmpty()
        if (text.isNotEmpty()) {
            binding.etMessage.text?.clear()
            viewModel.sendMessage(text)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.chatState.collect { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun renderUiState(state: ChatUiState) {
        when (state) {
            is ChatUiState.Loading -> {
                binding.progressLoading.visibility = View.VISIBLE
                binding.layoutError.visibility = View.GONE
                binding.layoutWelcome.visibility = View.GONE
                binding.rvMessages.visibility = View.GONE
                binding.layoutStreaming.visibility = View.GONE
                binding.btnRegenerate.visibility = View.GONE
            }

            is ChatUiState.Error -> {
                binding.progressLoading.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.layoutWelcome.visibility = View.GONE
                binding.rvMessages.visibility = View.GONE
                binding.layoutStreaming.visibility = View.GONE
                binding.btnRegenerate.visibility = View.GONE
                binding.tvErrorMessage.text = state.message
            }

            is ChatUiState.Active -> {
                binding.progressLoading.visibility = View.GONE
                binding.layoutError.visibility = View.GONE

                // Update toolbar title if conversation has a name
                state.conversation?.title?.let { title ->
                    if (title.isNotBlank()) {
                        binding.toolbar.title = title
                    }
                }

                // Show welcome banner if no messages and not streaming
                val hasMessages = state.messages.isNotEmpty()
                val isStreaming = !state.streamingContent.isNullOrEmpty()

                if (!hasMessages && !isStreaming) {
                    binding.layoutWelcome.visibility = View.VISIBLE
                    binding.rvMessages.visibility = View.GONE
                } else {
                    binding.layoutWelcome.visibility = View.GONE
                    binding.rvMessages.visibility = View.VISIBLE
                    chatAdapter.submitList(state.messages) {
                        if (state.messages.isNotEmpty()) {
                            binding.rvMessages.scrollToPosition(state.messages.size - 1)
                        }
                    }
                }

                // Streaming overlay indicator
                if (isStreaming) {
                    binding.layoutStreaming.visibility = View.VISIBLE
                    binding.tvStreamingContent.text = state.streamingContent
                    if (chatAdapter.itemCount > 0) {
                        binding.rvMessages.scrollToPosition(chatAdapter.itemCount - 1)
                    }
                } else {
                    binding.layoutStreaming.visibility = View.GONE
                }

                // Composer & Regenerate controls state
                val isBusy = state.isSending || state.isRegenerating
                binding.btnSend.isEnabled = !isBusy
                binding.btnSend.alpha = if (isBusy) 0.5f else 1.0f

                val canRegenerate = !isBusy && state.messages.any { !it.isUser }
                binding.btnRegenerate.visibility = if (canRegenerate) View.VISIBLE else View.GONE

                // Transient error notification
                state.errorMessage?.let { error ->
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                        .setAction("Dismiss") { viewModel.dismissError() }
                        .show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
