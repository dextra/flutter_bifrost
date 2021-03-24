package br.com.dextra.bifrost_example

import android.app.Application
import br.com.dextra.bifrost.Bifrost
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class App : Application() {

  override fun onCreate() {
    super.onCreate()
    // start flutter engine
    Bifrost.startFlutterEngine(this, CommonHandler())
  }

  // application common handler
  private class CommonHandler : MethodChannel.MethodCallHandler {
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
      when (call.method) {
        "getAppVersion" -> result.success(BuildConfig.VERSION_NAME)
      }
    }
  }
}
