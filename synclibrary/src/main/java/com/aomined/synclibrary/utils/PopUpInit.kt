package com.aomined.synclibrary.utils

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.isVisible
import com.aomined.synclibrary.R
import com.aomined.synclibrary.databinding.PopUpInitBinding
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo


class PopUpInit(context:Context):AlertDialog(context) {

    private var callBack: ((String) -> Unit)? = null

    fun showInit(callBack: (String) -> Unit){
        this.callBack = callBack
        show()
    }

    private lateinit var binding: PopUpInitBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PopUpInitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        window!!.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        window!!.setBackgroundDrawable(ActivityCompat.getDrawable(context, android.R.color.transparent))

        binding.titleText.text = String.format(context.getString(R.string.welcome_watch2), context.getString(R.string.app_name))

        binding.btnSave.setOnClickListener {
            val str = binding.usernameInput.text
            str?.let {
                if(str.isEmpty()){
                    showError(R.string.empty_username)
                    return@let
                }
                callBack!!(it.toString())
                showLoading()
            } ?: run {
                //manage error
                showError(R.string.something_went_wrong)
            }

        }
    }

    private fun showLoading(){
        binding.normalLayout.isVisible = false
        binding.loadingLay.isVisible = true
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.usernameInput.windowToken, 0)

        inputMethodManager.hideSoftInputFromWindow(binding.usernameInput.windowToken, 0)
    }

    fun hideLoading(){
        binding.normalLayout.isVisible = true
        binding.loadingLay.isVisible = false
    }

    fun showError(res:Int) {
        binding.errorText.isVisible = true
        binding.errorText.setText(res)
        YoYo.with(Techniques.FadeIn)
            .duration(450)
            .onEnd {
                hideError()
            }
            .playOn(binding.errorText)
    }

    private fun hideError() {
        YoYo.with(Techniques.FadeOut)
            .duration(750)
            .delay(3000)
            .onEnd {
                binding.errorText.isVisible = false
            }
            .playOn(binding.errorText)
    }



}