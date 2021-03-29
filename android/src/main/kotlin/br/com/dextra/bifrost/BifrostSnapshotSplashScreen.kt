package br.com.dextra.bifrost

import android.animation.Animator
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import io.flutter.embedding.android.SplashScreen
import io.flutter.embedding.engine.FlutterEngine

class BifrostSnapshotSplashScreen(flutterEngine: FlutterEngine) : SplashScreen {

  private var flutterViewSnapshot: Bitmap? = flutterEngine.renderer.bitmap
  private var splashView: View? = null

  override fun createSplashView(context: Context, savedInstanceState: Bundle?): View? {
    val splash = ImageView(context)
    splash.setImageBitmap(flutterViewSnapshot)
    splashView = splash
    return splash
  }

  override fun transitionToFlutter(onTransitionComplete: Runnable) {
    if (splashView == null) {
      onTransitionComplete.run()
      return
    }
    splashView!!
        .animate()
        .alpha(0.0f)
        .setDuration(500)
        .setListener(
            object : Animator.AnimatorListener {
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
