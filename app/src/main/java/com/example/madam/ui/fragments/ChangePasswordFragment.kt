package com.example.madam.ui.fragments


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.madam.R
import com.example.madam.databinding.FragmentChangePasswordBinding
import com.example.madam.ui.activities.ChangePasswordActivity
import com.example.madam.ui.activities.LoginActivity
import com.example.madam.ui.activities.MainActivity
import com.example.madam.ui.viewModels.ChangePasswordViewModel
import com.opinyour.android.app.data.utils.Injection
import es.dmoral.toasty.Toasty


class ChangePasswordFragment : Fragment() {
    private lateinit var changePasswordViewModel: ChangePasswordViewModel
    private lateinit var binding: FragmentChangePasswordBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_change_password, container, false
        )
        binding.lifecycleOwner = this
        changePasswordViewModel =
            ViewModelProvider(this, Injection.provideViewModelFactory(requireContext()))
                .get(ChangePasswordViewModel::class.java)

        binding.model = changePasswordViewModel
        Log.i("ChangePassword", "Init constructor")

        changePasswordViewModel.message.observe(viewLifecycleOwner, Observer {
            if (it == "Heslo bolo úspešne zmenené") {
                Toasty.success(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
            else if (it != "") {
                Toasty.error(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        })

        changePasswordViewModel.goBack.observe(viewLifecycleOwner, Observer {
            if (it) {
                (activity as ChangePasswordActivity).onBackPressed()
            }
        })

        binding.back.setOnClickListener {
            (activity as ChangePasswordActivity).onBackPressed()
        }

        changePasswordViewModel.userManager.refreshTokenSuccess.observe(viewLifecycleOwner, Observer {
            if (it)
                changePasswordViewModel.changePassword()
            else {
                changePasswordViewModel.userManager.logoutUser()
                (activity as MainActivity).goToActivity(LoginActivity::class.java)
            }
        })

        return binding.root
    }


    companion object {
        fun newInstance() = ChangePasswordFragment()
    }
}
