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
import com.example.madam.databinding.FragmentRegistrationBinding
import com.example.madam.ui.activities.LoginActivity
import com.example.madam.ui.viewModels.RegistrationViewModel
import com.opinyour.android.app.data.utils.Injection
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_login.*


class RegistrationFragment : Fragment() {
    private lateinit var registrationViewModel: RegistrationViewModel
    private lateinit var binding: FragmentRegistrationBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_registration, container, false
        )
        binding.lifecycleOwner = this
        registrationViewModel =
            ViewModelProvider(this, Injection.provideViewModelFactory(requireContext()))
                .get(RegistrationViewModel::class.java)
        binding.model = registrationViewModel

        Log.i("Registration", "Init constructor")

        binding.goToLoginFragmentButton.setOnClickListener {
            (activity as LoginActivity).view_login_pager.currentItem = 0
        }

        registrationViewModel.message.observe(viewLifecycleOwner, Observer {
            if (it == "Registrácia prebehla úspešne") {
                Toasty.success(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
            else if (it != "") {
                Toasty.error(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        })

        return binding.root
    }

}
