package br.com.dextra.bifrost

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

internal object BifrostNotificationChannel : MethodChannel.MethodCallHandler {

  private val notifications = hashMapOf<String, BifrostNotificationCallback>()
  private val channel = MethodChannel(Bifrost.engine.dartExecutor, "bifrost/notification")

  init {
    channel.setMethodCallHandler(this)
  }

  fun register(key: String, callback: (arguments: Any?) -> Unit) {
    notifications[key] = object : BifrostNotificationCallback {
      override fun onReceiveNotification(arguments: Any?) {
        callback.invoke(arguments)
      }
    }
  }

  fun register(key: String, callback: BifrostNotificationCallback) {
    notifications[key] = callback
  }

  fun unregister(key: String) {
    notifications.remove(key)
  }

  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    val key = call.method
    val args = call.arguments
    notifications[key]?.onReceiveNotification(args)

    result.success(notifications.containsKey(key))
  }
}

interface BifrostNotificationCallback {

  fun onReceiveNotification(arguments: Any?)
}
