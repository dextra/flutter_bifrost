package br.com.dextra.bifrost

import android.content.Context
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

object Bifrost {

  private val nextCode = AtomicInteger(1)

  @JvmStatic
  lateinit var engine: FlutterEngine
    private set

  internal var pluginRef: WeakReference<BifrostPlugin>? = null

  /**
   *  start flutter engine
   *
   *  @param context Application Context
   *  @param commonHandler Common method call handler
   *
   *  @return true if plugins registered otherwise return false.
   */
  @JvmStatic
  fun startFlutterEngine(context: Context,
                         commonHandler: MethodCallHandler? = null): Boolean {
    engine = FlutterEngine(context, null, true)
    FlutterEngineCache.getInstance().put("io.flutter.bifrost", engine)
    engine.dartExecutor.executeDartEntrypoint(DartExecutor.DartEntrypoint.createDefault())

    if (commonHandler != null) {
      BifrostCommonChannel(engine.dartExecutor, commonHandler)
    }

    return pluginRef != null
  }

  internal fun generatePageId(): Int {
    return nextCode.getAndIncrement()
  }

  /**
   * receive notification from flutter
   */
  @JvmStatic
  fun registerNotification(key: String, callback: (arguments: Any?) -> Unit) {
    BifrostNotificationChannel.register(key, callback)
  }

  /**
   * receive notification from flutter
   */
  @JvmStatic
  fun registerNotification(key: String, callback: BifrostNotificationCallback) {
    BifrostNotificationChannel.register(key, callback)
  }

  /**
   * unregister notification from flutter
   */
  @JvmStatic
  fun unregisterNotification(key: String) {
    BifrostNotificationChannel.unregister(key)
  }
}
