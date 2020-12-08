package com.example.madam.ui.fragments


import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.madam.R
import com.example.madam.data.db.repositories.model.UserItem
import com.example.madam.databinding.FragmentShowPhotoDetailBinding
import com.example.madam.ui.activities.ShowPhotoDetailActivity
import com.example.madam.ui.viewModels.ProfileViewModel
import com.opinyour.android.app.data.utils.Injection
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso


class ShowPhotoDetailFragment : Fragment() {
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var binding: FragmentShowPhotoDetailBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_show_photo_detail, container, false
        )
        profileViewModel =
            ViewModelProvider(this, Injection.provideViewModelFactory(requireContext()))
                .get(ProfileViewModel::class.java)
        binding.lifecycleOwner = this
        Log.i("Photo detail", "Init constructor")

        profileViewModel.reloadedUser.observe(viewLifecycleOwner, Observer {
            setUserProfile(it)
        })

        binding.back.setOnClickListener {
            (activity as ShowPhotoDetailActivity).onBackPressed()
        }

        profileViewModel.reloadUser()
        return binding.root
    }


    private fun setUserProfile(userI: UserItem?) {
        Log.i("profileP", "Setting profile photo")
        val user: UserItem? = userI ?: profileViewModel.userManager.getLoggedUser()
        if (user != null) {
            if (user.profile != "") {
                binding.loadingPanel.visibility = View.VISIBLE
                Glide.with(this)
                    .load("http://api.mcomputing.eu/mobv/uploads/" + user.profile)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            p0: GlideException?,
                            p1: Any?,
                            target: Target<Drawable>?,
                            p3: Boolean
                        ): Boolean {
                            return false
                        }
                        override fun onResourceReady(
                            p0: Drawable?,
                            p1: Any?,
                            target: Target<Drawable>?,
                            p3: DataSource?,
                            p4: Boolean
                        ): Boolean {
                            Log.d("Succ", "OnResourceReady")
                            //do something when picture already loaded
                            binding.loadingPanel.visibility = View.GONE
                            return false
                        }
                    })
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(binding.profileImage)
            } else {
                Glide.with(this)
                    .load(R.drawable.user)
                    .into(binding.profileImage)
            }
        }
    }


    companion object {
        fun newInstance() = ShowPhotoDetailFragment()
    }
}

