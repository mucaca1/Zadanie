package com.example.madam.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import com.example.madam.R
import com.example.madam.databinding.FragmentHomeBinding
import com.example.madam.ui.activities.LoginActivity
import com.example.madam.ui.activities.MainActivity
import com.example.madam.ui.adapters.RecyclerAdapter
import com.example.madam.ui.adapters.VideoPlayerBindingAdapter
import com.example.madam.ui.viewModels.VideoViewModel
import com.opinyour.android.app.data.utils.Injection
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private lateinit var videoViewModel: VideoViewModel
    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_home, container, false
        )
        binding.lifecycleOwner = this
        videoViewModel =
            ViewModelProvider(this, Injection.provideViewModelFactory(requireContext()))
                .get(VideoViewModel::class.java)

        binding.model = videoViewModel
        binding.recyclerVideoList.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        val adapter = RecyclerAdapter(videoViewModel.userManager.getLoggedUser()!!) { item ->
            videoViewModel.deleteVideo(item)
        }
        binding.recyclerVideoList.adapter = adapter

        videoViewModel.videos.observe(viewLifecycleOwner) { videos ->
            adapter.items = videos.sortedByDescending { it.created_at }
        }

        videoViewModel.success.observe(viewLifecycleOwner) {
            Toasty.success(requireContext(), it, Toast.LENGTH_LONG).show()
        }

        videoViewModel.userManager.refreshTokenSuccess.observe(viewLifecycleOwner) {
            if (it) {
                if (videoViewModel.lastItem != null) {
                    videoViewModel.deleteVideo(videoViewModel.lastItem!!)
                }
            } else {
                videoViewModel.userManager.logoutUser()
            }
        }

        binding.recyclerVideoList.addOnScrollListener(createListener())

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        videoViewModel.loadVideos()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            (activity as MainActivity).isLogged.value = videoViewModel.userManager.isLogged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                videoViewModel.userManager.logoutUser()
                (activity as MainActivity).goToActivity(LoginActivity::class.java)
            }
        })
    }

    private fun createListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val llm = binding.recyclerVideoList.layoutManager as LinearLayoutManager

                val smoothScroller: SmoothScroller = object : LinearSmoothScroller(context) {
                    override fun getVerticalSnapPreference(): Int {
                        return SNAP_TO_START
                    }

                    override fun onStop() {
                        VideoPlayerBindingAdapter.playIndexThenPausePreviousPlayer(llm.findFirstVisibleItemPosition())
                    }

                    override fun calculateTimeForScrolling(dx: Int): Int {
                        val proportion: Float = dx.toFloat()
                        return (0.35 * proportion).toInt()
                    }
                }

                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                   VideoPlayerBindingAdapter.pauseCurrentPlayingVideo()
                }
                if (newState == RecyclerView.SCROLL_STATE_IDLE || newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    var positionToScroll = 0
                    if (SCROLLED_UP != null && SCROLLED_UP == true) {
                        positionToScroll = llm.findFirstVisibleItemPosition()
                    } else if (SCROLLED_UP != null && SCROLLED_UP == false) {
                        positionToScroll = llm.findLastVisibleItemPosition()
                    }
                    smoothScroller.targetPosition = positionToScroll
                    binding.recyclerVideoList.layoutManager?.startSmoothScroll(smoothScroller)
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    println("Scrolled Downwards")
                    SCROLLED_UP = false
                } else if (dy < 0) {
                    println("Scrolled Upwards")
                    SCROLLED_UP = true
                }
            }
        }
    }

    companion object {
        private var SCROLLED_UP: Boolean? = null
    }
}