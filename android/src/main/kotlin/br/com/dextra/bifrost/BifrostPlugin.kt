package br.com.dextra.bifrost

import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import java.lang.ref.WeakReference

class BifrostPlugin : FlutterPlugin {

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    BifrostCoordinatorChannel.setMethodCallHandler(null)
  }

  override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    Bifrost.pluginRef = WeakReference(this)
  }
}
