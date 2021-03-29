package br.com.dextra.bifrost

import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

internal object BifrostCoordinatorChannel {

  private val channel = MethodChannel(Bifrost.engine.dartExecutor, "bifrost/coordinator")

  init {
    channel.resizeChannelBuffer(2)
  }

  fun onCreatePage(pageData: HashMap<String, Any?>) {
    channel.invokeMethod("onCreatePage", pageData)
  }

  fun onShowPage(pageData: HashMap<String, Any?>) {
    channel.invokeMethod("onShowPage", pageData)
  }

  fun onDeallocPage(pageData: HashMap<String, Any?>) {
    channel.invokeMethod("onDeallocPage", pageData)
  }

  fun onBackPressed(pageData: HashMap<String, Any?>) {
    channel.invokeMethod("onBackPressed", pageData)
  }

  fun canPop(pageData: HashMap<String, Any?>, callback: (Boolean) -> Unit) {
    channel.invokeMethod("canPop", pageData, object : MethodChannel.Result {
      override fun success(result: Any?) = callback((result as? Boolean) ?: false)
      override fun error(code: String?, message: String?, details: Any?) = callback(false)
      override fun notImplemented() = callback(false)
    })
  }

  fun setMethodCallHandler(handler: MethodCallHandler?) {
    channel.setMethodCallHandler(handler)
  }
}
