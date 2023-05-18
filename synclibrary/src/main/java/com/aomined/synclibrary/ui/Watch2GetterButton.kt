package com.aomined.synclibrary.ui

import android.app.Activity
import android.content.Context
import android.graphics.drawable.TransitionDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.widget.ContentFrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.*
import com.aomined.synclibrary.R
import com.aomined.synclibrary.SyncUsers
import com.aomined.synclibrary.TAG
import com.aomined.synclibrary.Watch2Application
import com.aomined.synclibrary.databinding.CustOMW2gettbtnBinding
import com.aomined.synclibrary.listeners.UserListener
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import es.dmoral.toasty.Toasty

class Watch2GetterButton(context: Context, attrs: AttributeSet):RelativeLayout(context, attrs) {

    private val binding: CustOMW2gettbtnBinding
    private var isLoading = false
    init {
        val inflater = LayoutInflater.from(context)
        binding = CustOMW2gettbtnBinding.inflate(inflater, this, true)
        setIconState()
        click{

        }
    }

    fun setVideo(videoId:String, videoOption:String = ""){
        if(wSdBar == null){
            Log.e(TAG, "Wsdbar: failed to config video" )
            return
        }

        wSdBar!!.setVideoConfig(videoId, videoOption)
    }

    fun click(onClickListener: OnClickListener){
        setOnClickListener {
            if(isLoading) return@setOnClickListener

            Watch2Application.instance.setActivityWatch2Getter()

            if(SyncUsers.getInstance().isConnected()){
                openSideBar()
            }else{
                if(SyncUsers.getInstance().isInitialized()){
                    setIconState()
                    //abrir ui para ver party o crear
                    openSideBar()
                }else{
                    setLoading(true)
                    SyncUsers.getInstance().initSystem(context, object:UserListener{
                        override fun onSuccessLogin() {
                            setLoading(false)
                            setIconState()
                            //abrir ui para ver party o crear
                            openSideBar()
                        }

                        override fun onError(e: Exception) {
                            setLoading(false)
                            Toasty.error(context, context.getString(R.string.something_went_wrong)).show()
                        }

                    })
                }
            }
            onClickListener.onClick(it)
        }
    }

    fun setIconState(){
        if(SyncUsers.getInstance().isInitialized()){
            binding.iconView.animateDrawableTransition(R.drawable.li_ve_cht_on)
        }else{
            binding.iconView.animateDrawableTransition(R.drawable.li_ve_cht)
        }
    }

    private fun setLoading(isLoading:Boolean){
        this.isLoading = isLoading
        binding.loadingAnim.isVisible = isLoading
        binding.iconView.isVisible = !isLoading
    }


    private fun ImageView.animateDrawableTransition(drawableToResId: Int, duration: Int = 400) {
        val drawableFrom = drawable
        val drawableTo = ContextCompat.getDrawable(context, drawableToResId)
        if (drawableTo == drawableFrom) return // Ya tiene el mismo Drawable
        val transitionDrawable = TransitionDrawable(arrayOf(drawableFrom, drawableTo))
        setImageDrawable(transitionDrawable)

        transitionDrawable.startTransition(duration)
    }

    private var wSdBar:Watch2SDBar? = null
    private fun openSideBar(){
        if(wSdBar != null){
            showSdBarAnim()
            return
        }
        // Crea una instancia del CustomRelativeLayout
        wSdBar = Watch2SDBar(context)
        wSdBar!!.setWatchButton(this)
        // Crea un LayoutParams para el CustomRelativeLayout
        /*val layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        layoutParams.addRule(ALIGN_PARENT_END)
*/
        // Obtiene los márgenes de la barra de estado
        /*val insets = ViewCompat.getRootWindowInsets(this)
        val statusBarInsets = insets?.getInsets(WindowInsetsCompat.Type.systemBars())
        val statusBarHeight = statusBarInsets?.top ?: 0

        // Ajusta los márgenes del CustomRelativeLayout
        layoutParams.setMargins(0, statusBarHeight, 0, 0)
*/

        // Agrega el CustomRelativeLayout a la raíz del ViewGroup principal
        //val primaryRoot = (rootView as ViewGroup)

        val primaryRoot = getParentRoot()
       // Log.e(TAG, "openSideBar: ${primaryRoot?.childCount}" )
        primaryRoot?.let {
           // Log.e(TAG, "openSideBar: ${primaryRoot.getChildAt(0).javaClass.simpleName}" )
            primaryRoot.addView(wSdBar)
            showSdBarAnim()
        }



    }

    private fun getParentRoot(): ViewGroup? {
        var parent: ViewGroup? = null
        var currentView = this as View?

        // Busca el padre más alto en la jerarquía de vistas
        while (currentView?.parent != null && currentView.parent !is ContentFrameLayout) {
            currentView = currentView.parent as? View
        }

        // Obtiene el padre anterior al ContentFrameLayout
        if (currentView?.parent is ViewGroup) {
            parent = currentView.parent as ViewGroup
        }
        return parent
    }

    private fun showSdBarAnim(){
        wSdBar?.let { wBar ->
            YoYo.with(Techniques.SlideInRight)
                .onStart {
                    wBar.isVisible = true
                }
                .duration(350)
                .playOn(wBar)
        }
    }

    fun hideSdBarAnim(){
        wSdBar?.let { wBar ->
            YoYo.with(Techniques.SlideOutRight)
                .onEnd {
                    wBar.isVisible = false
                }
                .duration(350)
                .playOn(wBar)
        }
    }



}