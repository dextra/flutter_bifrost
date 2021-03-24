package br.com.dextra.bifrost

import android.content.Context
import android.content.Intent
import android.graphics.Color
import io.flutter.embedding.android.XFlutterActivity
import io.flutter.embedding.android.SplashScreen
import io.flutter.embedding.engine.FlutterEngine
import java.io.Serializable

open class BifrostFlutterActivity : XFlutterActivity() {

  companion object {
    @JvmStatic
    @JvmOverloads
    fun createIntent(
        context: Context,
        route: String,
        arguments: Any? = null,
        backgroundColor: Int? = null,
        activityClass: Class<out BifrostFlutterActivity> = BifrostFlutterActivity::class.java
    ) = SingleEngineIntentBuilder(route, arguments, backgroundColor, activityClass)
        .build(context)

    // only available for kotlin
    inline fun <reified T : BifrostFlutterActivity> createIntentByCast(
        context: Context,
        route: String,
        arguments: Any? = null,
        backgroundColor: Int? = null
    ) = SingleEngineIntentBuilder(route, arguments, backgroundColor, T::class.java)
        .build(context)
  }

  data class SingleEngineIntentBuilder(val route: String,
                                       val arguments: Any?,
                                       val backgroundColor: Int?,
                                       val activityClass: Class<out BifrostFlutterActivity>) {

    fun build(context: Context): Intent {
      val id = Bifrost.generatePageId()
      val data = hashMapOf("id" to id, "route" to route, "arguments" to arguments)
      BifrostCoordinatorChannel.onCreatePage(data)

      return Intent(context, activityClass).apply {
        putExtra(BifrostConstants.ID, id)
        putExtra(BifrostConstants.ROUTE, route)
        putExtra(BifrostConstants.ARGUMENTS, serializeObject(arguments))
        putExtra(BifrostConstants.BACKGROUND_COLOR, backgroundColor)
      }
    }
  }

  private val pageId: Int
    get() = intent.getIntExtra(BifrostConstants.ID, 0)

  private val pageRoute: String
    get() = intent.getStringExtra(BifrostConstants.ROUTE) ?: ""

  private val pageArguments: Serializable?
    get() = intent.getSerializableExtra(BifrostConstants.ARGUMENTS)

  private val pageData: HashMap<String, Any?>
    get() = hashMapOf("id" to pageId, "route" to pageRoute, "arguments" to pageArguments)

  private val backgroundColor: Int
    get() = intent.getIntExtra(BifrostConstants.BACKGROUND_COLOR, Color.WHITE)

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    BifrostCoordinatorChannel.onShowPage(pageData)
  }

  override fun provideFlutterEngine(context: Context): FlutterEngine? {
    return Bifrost.engine
  }

  override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
  }

  override fun shouldDestroyEngineWithHost(): Boolean {
    return false
  }

  override fun onResume() {
    super.onResume()
    BifrostCoordinatorChannel.onShowPage(pageData)
  }

  override fun onDestroy() {
    BifrostCoordinatorChannel.onDeallocPage(pageData)
    super.onDestroy()
  }

  override fun onBackPressed() {
    if (stillAttachedForEvent("onBackPressed")) {
      BifrostCoordinatorChannel.canPop(pageData) { canPop ->
        if (canPop) {
          BifrostCoordinatorChannel.onBackPressed(pageData)
        } else {
          finish()
        }
      }
    }
  }

  override fun provideSplashScreen(): SplashScreen? {
    val splashScreen = super.provideSplashScreen()
    if (splashScreen != null) return splashScreen
    return BifrostSplashScreen(backgroundColor)
  }
}
