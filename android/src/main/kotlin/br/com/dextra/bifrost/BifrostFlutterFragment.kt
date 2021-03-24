package br.com.dextra.bifrost

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import io.flutter.embedding.android.RenderMode
import io.flutter.embedding.android.SplashScreen
import io.flutter.embedding.android.TransparencyMode
import io.flutter.embedding.android.XFlutterFragment
import io.flutter.embedding.engine.FlutterEngine
import java.io.*

open class BifrostFlutterFragment : XFlutterFragment() {

  companion object {
    @JvmStatic
    @JvmOverloads
    fun <T : BifrostFlutterFragment> newInstance(
        route: String,
        arguments: Any? = null,
        backgroundColor: Int? = null,
        fragmentClass: Class<out BifrostFlutterFragment> = BifrostFlutterFragment::class.java
    ) = SingleEngineFragmentBuilder(route, arguments, backgroundColor, fragmentClass)
        .build<T>()

    // only available for kotlin
    inline fun <reified T : BifrostFlutterFragment> newInstanceByCast(
        route: String,
        arguments: Any? = null,
        backgroundColor: Int? = null
    ) = SingleEngineFragmentBuilder(route, arguments, backgroundColor, T::class.java)
        .build<T>()
  }

  data class SingleEngineFragmentBuilder(val route: String,
                                         val arguments: Any?,
                                         val backgroundColor: Int?,
                                         val fragmentClass: Class<out BifrostFlutterFragment>) {

    fun <T : BifrostFlutterFragment> build(): T {
      return try {
        @Suppress("UNCHECKED_CAST")
        val frag = fragmentClass.getDeclaredConstructor().newInstance() as? T
            ?: throw RuntimeException(
                "The BifrostFlutterFragment subclass sent in the constructor ("
                    + fragmentClass.canonicalName
                    + ") does not match the expected return type.")

        val id = Bifrost.generatePageId()
        val data = hashMapOf("id" to id, "route" to route, "arguments" to arguments)
        BifrostCoordinatorChannel.onCreatePage(data)

        val args = Bundle().apply {
          putInt(BifrostConstants.ID, id)
          putString(BifrostConstants.ROUTE, route)
          putSerializable(BifrostConstants.ARGUMENTS, serializeObject(arguments))
          putInt(BifrostConstants.BACKGROUND_COLOR, backgroundColor ?: Color.WHITE)
        }
        frag.arguments = args
        frag
      } catch (e: Exception) {
        throw RuntimeException(
            "Could not instantiate BifrostFlutterFragment subclass (" +
                fragmentClass.name + ")", e)
      }
    }
  }

  private val pageId: Int
    get() = arguments?.getInt(BifrostConstants.ID) ?: 0

  private val pageRoute: String
    get() = arguments?.getString(BifrostConstants.ROUTE) ?: ""

  private val pageArguments: Serializable?
    get() = arguments?.getString(BifrostConstants.ARGUMENTS)

  private val pageData: HashMap<String, Any?>
    get() = hashMapOf("id" to pageId, "route" to pageRoute, "arguments" to pageArguments)

  private val backgroundColor: Int
    get() = arguments?.getInt(BifrostConstants.BACKGROUND_COLOR) ?: Color.WHITE

  override fun getRenderMode(): RenderMode {
    return RenderMode.texture
  }

  override fun getTransparencyMode(): TransparencyMode {
    return TransparencyMode.transparent
  }

  override fun provideFlutterEngine(context: Context): FlutterEngine {
    return Bifrost.engine
  }

  override fun provideSplashScreen(): SplashScreen {
    val splashScreen = super.provideSplashScreen()
    if (splashScreen != null) return splashScreen
    return BifrostSplashScreen(backgroundColor)
  }

  override fun onStart() {
    super.onStart()
    if (!isHidden) {
      BifrostCoordinatorChannel.onShowPage(pageData)
    }
  }

  override fun onHiddenChanged(hidden: Boolean) {
    super.onHiddenChanged(hidden)
    if (!isHidden) {
      BifrostCoordinatorChannel.onShowPage(pageData)
    }
  }

  override fun onDetach() {
    super.onDetach()
    BifrostCoordinatorChannel.onDeallocPage(pageData)
  }

  override fun shouldAttachEngineToActivity(): Boolean {
    return true
  }

  override fun onBackPressed() {
    if (stillAttachedForEvent("onBackPressed")) {
      BifrostCoordinatorChannel.canPop(pageData) { canPop ->
        if (canPop) {
          BifrostCoordinatorChannel.onBackPressed(pageData)
        } else {
          activity?.onBackPressedDispatcher?.onBackPressed()
        }
      }
    }
  }
}
