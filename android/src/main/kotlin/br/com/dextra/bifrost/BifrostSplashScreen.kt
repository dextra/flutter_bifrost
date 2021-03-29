package br.com.dextra.bifrost

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.content.Context
import android.os.Bundle
import android.view.View
import io.flutter.embedding.android.SplashScreen

class BifrostSplashScreen(private val color: Int) : SplashScreen {

  private var splashView: View? = null

  override fun createSplashView(context: Context, savedInstanceState: Bundle?): View? {
    if (splashView != null) {
      return splashView
    }
    splashView = View(context)
    splashView?.setBackgroundColor(color)
    return splashView
  }

  override fun transitionToFlutter(onTransitionComplete: Runnable) {
    if (splashView == null) {
      onTransitionComplete.run()
      return
    }
    splashView!!
        .animate()
        .alpha(0.0f)
        .setDuration(300)
        .setListener(
            object : AnimatorListener {
              override fun onAnimationStart(animation: Animator) {}
              override fun onAnimationEnd(animation: Animator) {
                onTransitionComplete.run()
              }

              override fun onAnimationCancel(animation: Animator) {
                onTransitionComplete.run()
              }

              override fun onAnimationRepeat(animation: Animator) {}
            })
  }
}
