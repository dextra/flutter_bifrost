// Copyright 2013 The Flutter Authors. All rights reserved.
// Copyright 2021 The Bifrost Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.embedding.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import io.flutter.Log;
import io.flutter.embedding.android.FlutterActivityLaunchConfigs.BackgroundMode;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.FlutterShellArgs;
import io.flutter.embedding.engine.loader.FlutterLoader;
import io.flutter.embedding.engine.plugins.activity.ActivityControlSurface;
import io.flutter.embedding.engine.plugins.util.GeneratedPluginRegister;
import io.flutter.plugin.platform.PlatformPlugin;

import static io.flutter.embedding.android.FlutterActivityLaunchConfigs.DART_ENTRYPOINT_META_DATA_KEY;
import static io.flutter.embedding.android.FlutterActivityLaunchConfigs.DEFAULT_BACKGROUND_MODE;
import static io.flutter.embedding.android.FlutterActivityLaunchConfigs.DEFAULT_DART_ENTRYPOINT;
import static io.flutter.embedding.android.FlutterActivityLaunchConfigs.DEFAULT_INITIAL_ROUTE;
import static io.flutter.embedding.android.FlutterActivityLaunchConfigs.EXTRA_BACKGROUND_MODE;
import static io.flutter.embedding.android.FlutterActivityLaunchConfigs.EXTRA_CACHED_ENGINE_ID;
import static io.flutter.embedding.android.FlutterActivityLaunchConfigs.EXTRA_DESTROY_ENGINE_WITH_ACTIVITY;
import static io.flutter.embedding.android.FlutterActivityLaunchConfigs.EXTRA_ENABLE_STATE_RESTORATION;
import static io.flutter.embedding.android.FlutterActivityLaunchConfigs.EXTRA_INITIAL_ROUTE;
import static io.flutter.embedding.android.FlutterActivityLaunchConfigs.INITIAL_ROUTE_META_DATA_KEY;
import static io.flutter.embedding.android.FlutterActivityLaunchConfigs.NORMAL_THEME_META_DATA_KEY;
import static io.flutter.embedding.android.FlutterActivityLaunchConfigs.SPLASH_SCREEN_META_DATA_KEY;

/**
 * {@code Activity} which displays a fullscreen Flutter UI.
 *
 * <p>{@code XFlutterActivity} is the simplest and most direct way to integrate Flutter within an
 * Android app.
 *
 * <p><strong>XFlutterActivity responsibilities</strong>
 *
 * <p>{@code XFlutterActivity} maintains the following responsibilities:
 *
 * <ul>
 *   <li>Displays an Android launch screen.
 *   <li>Displays a Flutter splash screen.
 *   <li>Configures the status bar appearance.
 *   <li>Chooses the Dart execution app bundle path and entrypoint.
 *   <li>Chooses Flutter's initial route.
 *   <li>Renders {@code Activity} transparently, if desired.
 *   <li>Offers hooks for subclasses to provide and configure a {@link FlutterEngine}.
 *   <li>Save and restore instance state, see {@code #shouldRestoreAndSaveState()};
 * </ul>
 *
 * <p><strong>Dart entrypoint, initial route, and app bundle path</strong>
 *
 * <p>The Dart entrypoint executed within this {@code Activity} is "main()" by default. To change
 * the entrypoint that a {@code XFlutterActivity} executes, subclass {@code XFlutterActivity} and
 * override {@link #getDartEntrypointFunctionName()}. For non-main Dart entrypoints to not be
 * tree-shaken away, you need to annotate those functions with {@code @pragma('vm:entry-point')} in
 * Dart.
 *
 * <p>The Flutter route that is initially loaded within this {@code Activity} is "/". The initial
 * route may be specified explicitly by passing the name of the route as a {@code String} in {@link
 * FlutterActivityLaunchConfigs#EXTRA_INITIAL_ROUTE}, e.g., "my/deep/link".
 *
 * <p>The initial route can each be controlled using a {@link NewEngineIntentBuilder} via {@link
 * NewEngineIntentBuilder#initialRoute}.
 *
 * <p>The app bundle path, Dart entrypoint, and initial route can also be controlled in a subclass
 * of {@code XFlutterActivity} by overriding their respective methods:
 *
 * <ul>
 *   <li>{@link #getAppBundlePath()}
 *   <li>{@link #getDartEntrypointFunctionName()}
 *   <li>{@link #getInitialRoute()}
 * </ul>
 *
 * <p>The Dart entrypoint and app bundle path are not supported as {@code Intent} parameters since
 * your Dart library entrypoints are your private APIs and Intents are invocable by other processes.
 *
 * <p><strong>Using a cached FlutterEngine</strong>
 *
 * <p>{@code XFlutterActivity} can be used with a cached {@link FlutterEngine} instead of creating a
 * new one. Use {@link #withCachedEngine(String)} to build a {@code XFlutterActivity} {@code Intent}
 * that is configured to use an existing, cached {@link FlutterEngine}. {@link
 * io.flutter.embedding.engine.FlutterEngineCache} is the cache that is used to obtain a given
 * cached {@link FlutterEngine}. You must create and put a {@link FlutterEngine} into the {@link
 * io.flutter.embedding.engine.FlutterEngineCache} yourself before using the {@link
 * #withCachedEngine(String)} builder. An {@code IllegalStateException} will be thrown if a cached
 * engine is requested but does not exist in the cache.
 *
 * <p>When using a cached {@link FlutterEngine}, that {@link FlutterEngine} should already be
 * executing Dart code, which means that the Dart entrypoint and initial route have already been
 * defined. Therefore, {@link CachedEngineIntentBuilder} does not offer configuration of these
 * properties.
 *
 * <p>It is generally recommended to use a cached {@link FlutterEngine} to avoid a momentary delay
 * when initializing a new {@link FlutterEngine}. The two exceptions to using a cached {@link
 * FlutterEngine} are:
 *
 * <p>
 *
 * <ul>
 *   <li>When {@code XFlutterActivity} is the first {@code Activity} displayed by the app, because
 *       pre-warming a {@link FlutterEngine} would have no impact in this situation.
 *   <li>When you are unsure when/if you will need to display a Flutter experience.
 * </ul>
 *
 * <p>See https://flutter.dev/docs/development/add-to-app/performance for additional performance
 * explorations on engine loading.
 *
 * <p>The following illustrates how to pre-warm and cache a {@link FlutterEngine}:
 *
 * <pre>{@code
 * // Create and pre-warm a FlutterEngine.
 * FlutterEngine flutterEngine = new FlutterEngine(context);
 * flutterEngine.getDartExecutor().executeDartEntrypoint(DartEntrypoint.createDefault());
 *
 * // Cache the pre-warmed FlutterEngine in the FlutterEngineCache.
 * FlutterEngineCache.getInstance().put("my_engine", flutterEngine);
 * }</pre>
 *
 * <p><strong>Alternatives to XFlutterActivity</strong>
 *
 * <p>If Flutter is needed in a location that cannot use an {@code Activity}, consider using a
 * {@link FlutterFragment}. Using a {@link FlutterFragment} requires forwarding some calls from an
 * {@code Activity} to the {@link FlutterFragment}.
 *
 * <p>If Flutter is needed in a location that can only use a {@code View}, consider using a {@link
 * FlutterView}. Using a {@link FlutterView} requires forwarding some calls from an {@code
 * Activity}, as well as forwarding lifecycle calls from an {@code Activity} or a {@code Fragment}.
 *
 * <p><strong>Launch Screen and Splash Screen</strong>
 *
 * <p>{@code XFlutterActivity} supports the display of an Android "launch screen" as well as a
 * Flutter-specific "splash screen". The launch screen is displayed while the Android application
 * loads. It is only applicable if {@code XFlutterActivity} is the first {@code Activity} displayed
 * upon loading the app. After the launch screen passes, a splash screen is optionally displayed.
 * The splash screen is displayed for as long as it takes Flutter to initialize and render its first
 * frame.
 *
 * <p>Use Android themes to display a launch screen. Create two themes: a launch theme and a normal
 * theme. In the launch theme, set {@code windowBackground} to the desired {@code Drawable} for the
 * launch screen. In the normal theme, set {@code windowBackground} to any desired background color
 * that should normally appear behind your Flutter content. In most cases this background color will
 * never be seen, but for possible transition edge cases it is a good idea to explicitly replace the
 * launch screen window background with a neutral color.
 *
 * <p>Do not change aspects of system chrome between a launch theme and normal theme. Either define
 * both themes to be fullscreen or not, and define both themes to display the same status bar and
 * navigation bar settings. To adjust system chrome once the Flutter app renders, use platform
 * channels to instruct Android to do so at the appropriate time. This will avoid any jarring visual
 * changes during app startup.
 *
 * <p>In the AndroidManifest.xml, set the theme of {@code XFlutterActivity} to the defined launch
 * theme. In the metadata section for {@code XFlutterActivity}, defined the following reference to
 * your normal theme:
 *
 * <p>{@code <meta-data android:name="io.flutter.embedding.android.NormalTheme"
 * android:resource="@style/YourNormalTheme" /> }
 *
 * <p>With themes defined, and AndroidManifest.xml updated, Flutter displays the specified launch
 * screen until the Android application is initialized.
 *
 * <p>Flutter also requires initialization time. To specify a splash screen for Flutter
 * initialization, subclass {@code XFlutterActivity} and override {@link #provideSplashScreen()}.
 * See {@link SplashScreen} for details on implementing a splash screen.
 *
 * <p>Flutter ships with a splash screen that automatically displays the exact same {@code
 * windowBackground} as the launch theme discussed previously. To use that splash screen, include
 * the following metadata in AndroidManifest.xml for this {@code XFlutterActivity}:
 *
 * <p>{@code <meta-data android:name="io.flutter.app.android.SplashScreenUntilFirstFrame"
 * android:value="true" /> }
 *
 * <p><strong>Alternative Activity</strong> {@link FlutterFragmentActivity} is also available, which
 * is similar to {@code XFlutterActivity} but it extends {@code FragmentActivity}. You should use
 * {@code XFlutterActivity}, if possible, but if you need a {@code FragmentActivity} then you should
 * use {@link FlutterFragmentActivity}.
 */
// A number of methods in this class have the same implementation as FlutterFragmentActivity. These
// methods are duplicated for readability purposes. Be sure to replicate any change in this class in
// FlutterFragmentActivity, too.
public class XFlutterActivity extends Activity
    implements XFlutterActivityAndFragmentDelegate.Host, LifecycleOwner {
  private static final String TAG = "XFlutterActivity";

  /**
   * Creates an {@link Intent} that launches a {@code XFlutterActivity}, which creates a {@link
   * FlutterEngine} that executes a {@code main()} Dart entrypoint, and displays the "/" route as
   * Flutter's initial route.
   *
   * <p>Consider using the {@link #withCachedEngine(String)} {@link Intent} builder to control when
   * the {@link FlutterEngine} should be created in your application.
   */
  @NonNull
  public static Intent createDefaultIntent(@NonNull Context launchContext) {
    return withNewEngine().build(launchContext);
  }

  /**
   * Creates an {@link NewEngineIntentBuilder}, which can be used to configure an {@link Intent} to
   * launch a {@code XFlutterActivity} that internally creates a new {@link FlutterEngine} using the
   * desired Dart entrypoint, initial route, etc.
   */
  @NonNull
  public static NewEngineIntentBuilder withNewEngine() {
    return new NewEngineIntentBuilder(XFlutterActivity.class);
  }

  /**
   * Builder to create an {@code Intent} that launches a {@code XFlutterActivity} with a new {@link
   * FlutterEngine} and the desired configuration.
   */
  public static class NewEngineIntentBuilder {
    private final Class<? extends XFlutterActivity> activityClass;
    private String initialRoute = DEFAULT_INITIAL_ROUTE;
    private String backgroundMode = DEFAULT_BACKGROUND_MODE;

    /**
     * Constructor that allows this {@code NewEngineIntentBuilder} to be used by subclasses of
     * {@code XFlutterActivity}.
     *
     * <p>Subclasses of {@code XFlutterActivity} should provide their own static version of {@link
     * #withNewEngine()}, which returns an instance of {@code NewEngineIntentBuilder} constructed
     * with a {@code Class} reference to the {@code XFlutterActivity} subclass, e.g.:
     *
     * <p>{@code return new NewEngineIntentBuilder(MyFlutterActivity.class); }
     */
    public NewEngineIntentBuilder(@NonNull Class<? extends XFlutterActivity> activityClass) {
      this.activityClass = activityClass;
    }

    /**
     * The initial route that a Flutter app will render in this {@link FlutterFragment}, defaults to
     * "/".
     */
    @NonNull
    public NewEngineIntentBuilder initialRoute(@NonNull String initialRoute) {
      this.initialRoute = initialRoute;
      return this;
    }

    /**
     * The mode of {@code XFlutterActivity}'s background, either {@link BackgroundMode#opaque} or
     * {@link BackgroundMode#transparent}.
     *
     * <p>The default background mode is {@link BackgroundMode#opaque}.
     *
     * <p>Choosing a background mode of {@link BackgroundMode#transparent} will configure the inner
     * {@link FlutterView} of this {@code XFlutterActivity} to be configured with a {@link
     * FlutterTextureView} to support transparency. This choice has a non-trivial performance
     * impact. A transparent background should only be used if it is necessary for the app design
     * being implemented.
     *
     * <p>A {@code XFlutterActivity} that is configured with a background mode of {@link
     * BackgroundMode#transparent} must have a theme applied to it that includes the following
     * property: {@code <item name="android:windowIsTranslucent">true</item>}.
     */
    @NonNull
    public NewEngineIntentBuilder backgroundMode(@NonNull BackgroundMode backgroundMode) {
      this.backgroundMode = backgroundMode.name();
      return this;
    }

    /**
     * Creates and returns an {@link Intent} that will launch a {@code XFlutterActivity} with the
     * desired configuration.
     */
    @NonNull
    public Intent build(@NonNull Context context) {
      return new Intent(context, activityClass)
          .putExtra(EXTRA_INITIAL_ROUTE, initialRoute)
          .putExtra(EXTRA_BACKGROUND_MODE, backgroundMode)
          .putExtra(EXTRA_DESTROY_ENGINE_WITH_ACTIVITY, true);
    }
  }

  /**
   * Creates a {@link CachedEngineIntentBuilder}, which can be used to configure an {@link Intent}
   * to launch a {@code XFlutterActivity} that internally uses an existing {@link FlutterEngine}
   * that is cached in {@link io.flutter.embedding.engine.FlutterEngineCache}.
   */
  public static CachedEngineIntentBuilder withCachedEngine(@NonNull String cachedEngineId) {
    return new CachedEngineIntentBuilder(XFlutterActivity.class, cachedEngineId);
  }

  /**
   * Builder to create an {@code Intent} that launches a {@code XFlutterActivity} with an existing
   * {@link FlutterEngine} that is cached in {@link io.flutter.embedding.engine.FlutterEngineCache}.
   */
  public static class CachedEngineIntentBuilder {
    private final Class<? extends XFlutterActivity> activityClass;
    private final String cachedEngineId;
    private boolean destroyEngineWithActivity = false;
    private String backgroundMode = DEFAULT_BACKGROUND_MODE;

    // changed
    /// **
    // * Constructor that allows this {@code CachedEngineIntentBuilder} to be used by subclasses of
    // * {@code XFlutterActivity}.
    // *
    // * <p>Subclasses of {@code XFlutterActivity} should provide their own static version of {@link
    // * #withCachedEngine()}, which returns an instance of {@code CachedEngineIntentBuilder}
    // * constructed with a {@code Class} reference to the {@code XFlutterActivity} subclass, e.g.:
    // *
    // * <p>{@code return new CachedEngineIntentBuilder(MyFlutterActivity.class, engineId); }
    // */

    /**
     * Constructor that allows this {@code CachedEngineIntentBuilder} to be used by subclasses of
     * {@code XFlutterActivity}.
     *
     * <p>Subclasses of {@code XFlutterActivity} should provide their own static version of
     * withCachedEngine(), which returns an instance of {@code CachedEngineIntentBuilder}
     * constructed with a {@code Class} reference to the {@code XFlutterActivity} subclass, e.g.:
     *
     * <p>{@code return new CachedEngineIntentBuilder(MyFlutterActivity.class, engineId); }
     */
    public CachedEngineIntentBuilder(
        @NonNull Class<? extends XFlutterActivity> activityClass, @NonNull String engineId) {
      this.activityClass = activityClass;
      this.cachedEngineId = engineId;
    }

    /**
     * Returns true if the cached {@link FlutterEngine} should be destroyed and removed from the
     * cache when this {@code XFlutterActivity} is destroyed.
     *
     * <p>The default value is {@code false}.
     */
    public CachedEngineIntentBuilder destroyEngineWithActivity(boolean destroyEngineWithActivity) {
      this.destroyEngineWithActivity = destroyEngineWithActivity;
      return this;
    }

    /**
     * The mode of {@code XFlutterActivity}'s background, either {@link BackgroundMode#opaque} or
     * {@link BackgroundMode#transparent}.
     *
     * <p>The default background mode is {@link BackgroundMode#opaque}.
     *
     * <p>Choosing a background mode of {@link BackgroundMode#transparent} will configure the inner
     * {@link FlutterView} of this {@code XFlutterActivity} to be configured with a {@link
     * FlutterTextureView} to support transparency. This choice has a non-trivial performance
     * impact. A transparent background should only be used if it is necessary for the app design
     * being implemented.
     *
     * <p>A {@code XFlutterActivity} that is configured with a background mode of {@link
     * BackgroundMode#transparent} must have a theme applied to it that includes the following
     * property: {@code <item name="android:windowIsTranslucent">true</item>}.
     */
    @NonNull
    public CachedEngineIntentBuilder backgroundMode(@NonNull BackgroundMode backgroundMode) {
      this.backgroundMode = backgroundMode.name();
      return this;
    }

    /**
     * Creates and returns an {@link Intent} that will launch a {@code XFlutterActivity} with the
     * desired configuration.
     */
    @NonNull
    public Intent build(@NonNull Context context) {
      return new Intent(context, activityClass)
          .putExtra(EXTRA_CACHED_ENGINE_ID, cachedEngineId)
          .putExtra(EXTRA_DESTROY_ENGINE_WITH_ACTIVITY, destroyEngineWithActivity)
          .putExtra(EXTRA_BACKGROUND_MODE, backgroundMode);
    }
  }

  // Delegate that runs all lifecycle and OS hook logic that is common between
  // XFlutterActivity and FlutterFragment. See the XFlutterActivityAndFragmentDelegate
  // implementation for details about why it exists.
  @VisibleForTesting protected XFlutterActivityAndFragmentDelegate delegate;

  @NonNull private LifecycleRegistry lifecycle;

  public XFlutterActivity() {
    lifecycle = new LifecycleRegistry(this);
  }

  /**
   * This method exists so that JVM tests can ensure that a delegate exists without putting this
   * Activity through any lifecycle events, because JVM tests cannot handle executing any lifecycle
   * methods, at the time of writing this.
   *
   * <p>The testing infrastructure should be upgraded to make XFlutterActivity tests easy to write
   * while exercising real lifecycle methods. At such a time, this method should be removed.
   */
  // TODO(mattcarroll): remove this when tests allow for it
  // (https://github.com/flutter/flutter/issues/43798)
  @VisibleForTesting
  /* package */ void setDelegate(@NonNull XFlutterActivityAndFragmentDelegate delegate) {
    this.delegate = delegate;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    switchLaunchThemeForNormalTheme();

    super.onCreate(savedInstanceState);

    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);

    delegate = new XFlutterActivityAndFragmentDelegate(this);
    delegate.onAttach(this);
    delegate.onActivityCreated(savedInstanceState);

    configureWindowForTransparency();
    setContentView(createFlutterView());
    configureStatusBarForFullscreenFlutterExperience();
  }

  /**
   * Switches themes for this {@code Activity} from the theme used to launch this {@code Activity}
   * to a "normal theme" that is intended for regular {@code Activity} operation.
   *
   * <p>This behavior is offered so that a "launch screen" can be displayed while the application
   * initially loads. To utilize this behavior in an app, do the following:
   *
   * <ol>
   *   <li>Create 2 different themes in style.xml: one theme for the launch screen and one theme for
   *       normal display.
   *   <li>In the launch screen theme, set the "windowBackground" property to a {@code Drawable} of
   *       your choice.
   *   <li>In the normal theme, customize however you'd like.
   *   <li>In the AndroidManifest.xml, set the theme of your {@code XFlutterActivity} to your launch
   *       theme.
   *   <li>Add a {@code <meta-data>} property to your {@code XFlutterActivity} with a name of
   *       "io.flutter.embedding.android.NormalTheme" and set the resource to your normal theme,
   *       e.g., {@code android:resource="@style/MyNormalTheme}.
   * </ol>
   *
   * <p>With the above settings, your launch theme will be used when loading the app, and then the
   * theme will be switched to your normal theme once the app has initialized.
   *
   * <p>Do not change aspects of system chrome between a launch theme and normal theme. Either
   * define both themes to be fullscreen or not, and define both themes to display the same status
   * bar and navigation bar settings. If you wish to adjust system chrome once your Flutter app
   * renders, use platform channels to instruct Android to do so at the appropriate time. This will
   * avoid any jarring visual changes during app startup.
   */
  private void switchLaunchThemeForNormalTheme() {
    try {
      ActivityInfo activityInfo =
          getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
      if (activityInfo.metaData != null) {
        int normalThemeRID = activityInfo.metaData.getInt(NORMAL_THEME_META_DATA_KEY, -1);
        if (normalThemeRID != -1) {
          setTheme(normalThemeRID);
        }
      } else {
        Log.v(TAG, "Using the launch theme as normal theme.");
      }
    } catch (PackageManager.NameNotFoundException exception) {
      Log.e(
          TAG,
          "Could not read meta-data for XFlutterActivity. Using the launch theme as normal theme.");
    }
  }

  @Nullable
  @Override
  public SplashScreen provideSplashScreen() {
    Drawable manifestSplashDrawable = getSplashScreenFromManifest();
    if (manifestSplashDrawable != null) {
      return new DrawableSplashScreen(manifestSplashDrawable);
    } else {
      return null;
    }
  }

  /**
   * Returns a {@link Drawable} to be used as a splash screen as requested by meta-data in the
   * {@code AndroidManifest.xml} file, or null if no such splash screen is requested.
   *
   * <p>See {@link FlutterActivityLaunchConfigs#SPLASH_SCREEN_META_DATA_KEY} for the meta-data key
   * to be used in a manifest file.
   */
  @Nullable
  @SuppressWarnings("deprecation")
  private Drawable getSplashScreenFromManifest() {
    try {
      ActivityInfo activityInfo =
          getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
      Bundle metadata = activityInfo.metaData;
      int splashScreenId = metadata != null ? metadata.getInt(SPLASH_SCREEN_META_DATA_KEY) : 0;
      return splashScreenId != 0
          ? Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP
              ? getResources().getDrawable(splashScreenId, getTheme())
              : getResources().getDrawable(splashScreenId)
          : null;
    } catch (PackageManager.NameNotFoundException e) {
      // This is never expected to happen.
      return null;
    }
  }

  /**
   * Sets this {@code Activity}'s {@code Window} background to be transparent, and hides the status
   * bar, if this {@code Activity}'s desired {@link BackgroundMode} is {@link
   * BackgroundMode#transparent}.
   *
   * <p>For {@code Activity} transparency to work as expected, the theme applied to this {@code
   * Activity} must include {@code <item name="android:windowIsTranslucent">true</item>}.
   */
  private void configureWindowForTransparency() {
    BackgroundMode backgroundMode = getBackgroundMode();
    if (backgroundMode == BackgroundMode.transparent) {
      getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
  }

  @NonNull
  private View createFlutterView() {
    return delegate.onCreateView(
        null /* inflater */, null /* container */, null /* savedInstanceState */);
  }

  private void configureStatusBarForFullscreenFlutterExperience() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      Window window = getWindow();
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      window.setStatusBarColor(0x40000000);
      window.getDecorView().setSystemUiVisibility(PlatformPlugin.DEFAULT_SYSTEM_UI);
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
    delegate.onStart();
    // bifrost implementation
    if (delegate.isDetached()) {
      delegate.reattach();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    delegate.onResume();
  }

  @Override
  public void onPostResume() {
    super.onPostResume();
    delegate.onPostResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    delegate.onPause();
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
  }

  @Override
  protected void onStop() {
    super.onStop();
    // bifrost implementation
    if (stillAttachedForEvent("onStop")) {
      delegate.onStop();
    }
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    // bifrost implementation
    if (stillAttachedForEvent("onSaveInstanceState")) {
      delegate.onSaveInstanceState(outState);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    // bifrost implementation
    if (stillAttachedForEvent("onDestroy")) {
      delegate.onDestroyView();
      delegate.onDetach();
      delegate.release();
      delegate = null;
    }
    lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // bifrost implementation
    if (stillAttachedForEvent("onActivityResult")) {
      delegate.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  protected void onNewIntent(@NonNull Intent intent) {
    // TODO(mattcarroll): change G3 lint rule that forces us to call super
    super.onNewIntent(intent);
    // bifrost implementation
    if (stillAttachedForEvent("onNewIntent")) {
      delegate.onNewIntent(intent);
    }
  }

  @Override
  public void onBackPressed() {
    // bifrost implementation
    if (stillAttachedForEvent("onBackPressed")) {
      delegate.onBackPressed();
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    // bifrost implementation
    if (stillAttachedForEvent("onRequestPermissionsResult")) {
      delegate.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
  }

  @Override
  public void onUserLeaveHint() {
    delegate.onUserLeaveHint();
  }

  @Override
  public void onTrimMemory(int level) {
    super.onTrimMemory(level);
    // bifrost implementation
    if (stillAttachedForEvent("onTrimMemory")) {
      delegate.onTrimMemory(level);
    }
  }

  /**
   * {@link XFlutterActivityAndFragmentDelegate.Host} method that is used by {@link
   * XFlutterActivityAndFragmentDelegate} to obtain a {@code Context} reference as needed.
   */
  @Override
  @NonNull
  public Context getContext() {
    return this;
  }

  /**
   * {@link XFlutterActivityAndFragmentDelegate.Host} method that is used by {@link
   * XFlutterActivityAndFragmentDelegate} to obtain an {@code Activity} reference as needed. This
   * reference is used by the delegate to instantiate a {@link FlutterView}, a {@link
   * PlatformPlugin}, and to determine if the {@code Activity} is changing configurations.
   */
  @Override
  @NonNull
  public Activity getActivity() {
    return this;
  }

  /**
   * {@link XFlutterActivityAndFragmentDelegate.Host} method that is used by {@link
   * XFlutterActivityAndFragmentDelegate} to obtain a {@code Lifecycle} reference as needed. This
   * reference is used by the delegate to provide Flutter plugins with access to lifecycle events.
   */
  @Override
  @NonNull
  public Lifecycle getLifecycle() {
    return lifecycle;
  }

  /**
   * {@link XFlutterActivityAndFragmentDelegate.Host} method that is used by {@link
   * XFlutterActivityAndFragmentDelegate} to obtain Flutter shell arguments when initializing
   * Flutter.
   */
  @NonNull
  @Override
  public FlutterShellArgs getFlutterShellArgs() {
    return FlutterShellArgs.fromIntent(getIntent());
  }

  /**
   * Returns the ID of a statically cached {@link FlutterEngine} to use within this {@code
   * XFlutterActivity}, or {@code null} if this {@code XFlutterActivity} does not want to use a
   * cached {@link FlutterEngine}.
   */
  @Override
  @Nullable
  public String getCachedEngineId() {
    return getIntent().getStringExtra(EXTRA_CACHED_ENGINE_ID);
  }

  /**
   * Returns false if the {@link FlutterEngine} backing this {@code XFlutterActivity} should outlive
   * this {@code XFlutterActivity}, or true to be destroyed when the {@code XFlutterActivity} is
   * destroyed.
   *
   * <p>The default value is {@code true} in cases where {@code XFlutterActivity} created its own
   * {@link FlutterEngine}, and {@code false} in cases where a cached {@link FlutterEngine} was
   * provided.
   */
  @Override
  public boolean shouldDestroyEngineWithHost() {
    boolean explicitDestructionRequested =
        getIntent().getBooleanExtra(EXTRA_DESTROY_ENGINE_WITH_ACTIVITY, false);
    if (getCachedEngineId() != null || delegate.isFlutterEngineFromHost()) {
      // Only destroy a cached engine if explicitly requested by app developer.
      return explicitDestructionRequested;
    } else {
      // If this Activity created the FlutterEngine, destroy it by default unless
      // explicitly requested not to.
      return getIntent().getBooleanExtra(EXTRA_DESTROY_ENGINE_WITH_ACTIVITY, true);
    }
  }

  /**
   * The Dart entrypoint that will be executed as soon as the Dart snapshot is loaded.
   *
   * <p>This preference can be controlled by setting a {@code <meta-data>} called {@link
   * FlutterActivityLaunchConfigs#DART_ENTRYPOINT_META_DATA_KEY} within the Android manifest
   * definition for this {@code XFlutterActivity}.
   *
   * <p>Subclasses may override this method to directly control the Dart entrypoint.
   */
  @NonNull
  public String getDartEntrypointFunctionName() {
    try {
      ActivityInfo activityInfo =
          getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
      Bundle metadata = activityInfo.metaData;
      String desiredDartEntrypoint =
          metadata != null ? metadata.getString(DART_ENTRYPOINT_META_DATA_KEY) : null;
      return desiredDartEntrypoint != null ? desiredDartEntrypoint : DEFAULT_DART_ENTRYPOINT;
    } catch (PackageManager.NameNotFoundException e) {
      return DEFAULT_DART_ENTRYPOINT;
    }
  }

  /**
   * The initial route that a Flutter app will render upon loading and executing its Dart code.
   *
   * <p>This preference can be controlled with 2 methods:
   *
   * <ol>
   *   <li>Pass a boolean as {@link FlutterActivityLaunchConfigs#EXTRA_INITIAL_ROUTE} with the
   *       launching {@code Intent}, or
   *   <li>Set a {@code <meta-data>} called {@link
   *       FlutterActivityLaunchConfigs#INITIAL_ROUTE_META_DATA_KEY} for this {@code Activity} in
   *       the Android manifest.
   * </ol>
   *
   * <p>If both preferences are set, the {@code Intent} preference takes priority.
   *
   * <p>The reason that a {@code <meta-data>} preference is supported is because this {@code
   * Activity} might be the very first {@code Activity} launched, which means the developer won't
   * have control over the incoming {@code Intent}.
   *
   * <p>Subclasses may override this method to directly control the initial route.
   */
  @NonNull
  public String getInitialRoute() {
    if (getIntent().hasExtra(EXTRA_INITIAL_ROUTE)) {
      return getIntent().getStringExtra(EXTRA_INITIAL_ROUTE);
    }

    try {
      ActivityInfo activityInfo =
          getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
      Bundle metadata = activityInfo.metaData;
      String desiredInitialRoute =
          metadata != null ? metadata.getString(INITIAL_ROUTE_META_DATA_KEY) : null;
      return desiredInitialRoute != null ? desiredInitialRoute : DEFAULT_INITIAL_ROUTE;
    } catch (PackageManager.NameNotFoundException e) {
      return DEFAULT_INITIAL_ROUTE;
    }
  }

  /**
   * A custom path to the bundle that contains this Flutter app's resources, e.g., Dart code
   * snapshots.
   *
   * <p>When this {@code XFlutterActivity} is run by Flutter tooling and a data String is included
   * in the launching {@code Intent}, that data String is interpreted as an app bundle path.
   *
   * <p>When otherwise unspecified, the value is null, which defaults to the app bundle path defined
   * in {@link FlutterLoader#findAppBundlePath()}.
   *
   * <p>Subclasses may override this method to return a custom app bundle path.
   */
  @NonNull
  public String getAppBundlePath() {
    // If this Activity was launched from tooling, and the incoming Intent contains
    // a custom app bundle path, return that path.
    // TODO(mattcarroll): determine if we should have an explicit FlutterTestActivity instead of
    // conflating.
    if (isDebuggable() && Intent.ACTION_RUN.equals(getIntent().getAction())) {
      String appBundlePath = getIntent().getDataString();
      if (appBundlePath != null) {
        return appBundlePath;
      }
    }

    return null;
  }

  /**
   * Returns true if Flutter is running in "debug mode", and false otherwise.
   *
   * <p>Debug mode allows Flutter to operate with hot reload and hot restart. Release mode does not.
   */
  private boolean isDebuggable() {
    return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
  }

  /**
   * {@link XFlutterActivityAndFragmentDelegate.Host} method that is used by {@link
   * XFlutterActivityAndFragmentDelegate} to obtain the desired {@link RenderMode} that should be
   * used when instantiating a {@link FlutterView}.
   */
  @NonNull
  @Override
  public RenderMode getRenderMode() {
    return getBackgroundMode() == BackgroundMode.opaque ? RenderMode.surface : RenderMode.texture;
  }

  /**
   * {@link XFlutterActivityAndFragmentDelegate.Host} method that is used by {@link
   * XFlutterActivityAndFragmentDelegate} to obtain the desired {@link TransparencyMode} that should
   * be used when instantiating a {@link FlutterView}.
   */
  @NonNull
  @Override
  public TransparencyMode getTransparencyMode() {
    return getBackgroundMode() == BackgroundMode.opaque
        ? TransparencyMode.opaque
        : TransparencyMode.transparent;
  }

  /**
   * The desired window background mode of this {@code Activity}, which defaults to {@link
   * BackgroundMode#opaque}.
   */
  @NonNull
  protected BackgroundMode getBackgroundMode() {
    if (getIntent().hasExtra(EXTRA_BACKGROUND_MODE)) {
      return BackgroundMode.valueOf(getIntent().getStringExtra(EXTRA_BACKGROUND_MODE));
    } else {
      return BackgroundMode.opaque;
    }
  }

  /**
   * Hook for subclasses to easily provide a custom {@link FlutterEngine}.
   *
   * <p>This hook is where a cached {@link FlutterEngine} should be provided, if a cached {@link
   * FlutterEngine} is desired.
   */
  @Nullable
  @Override
  public FlutterEngine provideFlutterEngine(@NonNull Context context) {
    // No-op. Hook for subclasses.
    return null;
  }

  /**
   * Hook for subclasses to obtain a reference to the {@link FlutterEngine} that is owned by this
   * {@code XFlutterActivity}.
   */
  @Nullable
  protected FlutterEngine getFlutterEngine() {
    return delegate.getFlutterEngine();
  }

  @Nullable
  @Override
  public PlatformPlugin providePlatformPlugin(
      @Nullable Activity activity, @NonNull FlutterEngine flutterEngine) {
    if (activity != null) {
      return new PlatformPlugin(getActivity(), flutterEngine.getPlatformChannel());
    } else {
      return null;
    }
  }

  /**
   * Hook for subclasses to easily configure a {@code FlutterEngine}.
   *
   * <p>This method is called after {@link #provideFlutterEngine(Context)}.
   *
   * <p>All plugins listed in the app's pubspec are registered in the base implementation of this
   * method. To avoid automatic plugin registration, override this method without invoking super().
   * To keep automatic plugin registration and further configure the flutterEngine, override this
   * method, invoke super(), and then configure the flutterEngine as desired.
   */
  @Override
  public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
    GeneratedPluginRegister.registerGeneratedPlugins(flutterEngine);
  }

  /**
   * Hook for the host to cleanup references that were established in {@link
   * #configureFlutterEngine(FlutterEngine)} before the host is destroyed or detached.
   *
   * <p>This method is called in {@link #onDestroy()}.
   */
  @Override
  public void cleanUpFlutterEngine(@NonNull FlutterEngine flutterEngine) {
    // No-op. Hook for subclasses.
  }

  /**
   * Hook for subclasses to control whether or not the {@link FlutterFragment} within this {@code
   * Activity} automatically attaches its {@link FlutterEngine} to this {@code Activity}.
   *
   * <p>This property is controlled with a protected method instead of an {@code Intent} argument
   * because the only situation where changing this value would help, is a situation in which {@code
   * XFlutterActivity} is being subclassed to utilize a custom and/or cached {@link FlutterEngine}.
   *
   * <p>Defaults to {@code true}.
   *
   * <p>Control surfaces are used to provide Android resources and lifecycle events to plugins that
   * are attached to the {@link FlutterEngine}. If {@code shouldAttachEngineToActivity} is true then
   * this {@code XFlutterActivity} will connect its {@link FlutterEngine} to itself, along with any
   * plugins that are registered with that {@link FlutterEngine}. This allows plugins to access the
   * {@code Activity}, as well as receive {@code Activity}-specific calls, e.g., {@link
   * Activity#onNewIntent(Intent)}. If {@code shouldAttachEngineToActivity} is false, then this
   * {@code XFlutterActivity} will not automatically manage the connection between its {@link
   * FlutterEngine} and itself. In this case, plugins will not be offered a reference to an {@code
   * Activity} or its OS hooks.
   *
   * <p>Returning false from this method does not preclude a {@link FlutterEngine} from being
   * attaching to a {@code XFlutterActivity} - it just prevents the attachment from happening
   * automatically. A developer can choose to subclass {@code XFlutterActivity} and then invoke
   * {@link ActivityControlSurface#attachToActivity(Activity, Lifecycle)} and {@link
   * ActivityControlSurface#detachFromActivity()} at the desired times.
   *
   * <p>One reason that a developer might choose to manually manage the relationship between the
   * {@code Activity} and {@link FlutterEngine} is if the developer wants to move the {@link
   * FlutterEngine} somewhere else. For example, a developer might want the {@link FlutterEngine} to
   * outlive this {@code XFlutterActivity} so that it can be used later in a different {@code
   * Activity}. To accomplish this, the {@link FlutterEngine} may need to be disconnected from this
   * {@code FluttterActivity} at an unusual time, preventing this {@code XFlutterActivity} from
   * correctly managing the relationship between the {@link FlutterEngine} and itself.
   */
  @Override
  public boolean shouldAttachEngineToActivity() {
    return true;
  }

  @Override
  public void onFlutterSurfaceViewCreated(@NonNull FlutterSurfaceView flutterSurfaceView) {
    // Hook for subclasses.
  }

  @Override
  public void onFlutterTextureViewCreated(@NonNull FlutterTextureView flutterTextureView) {
    // Hook for subclasses.
  }

  @Override
  public void onFlutterUiDisplayed() {
    // Notifies Android that we're fully drawn so that performance metrics can be collected by
    // Flutter performance tests.
    // This was supported in KitKat (API 19), but has a bug around requiring
    // permissions. See https://github.com/flutter/flutter/issues/46172
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      reportFullyDrawn();
    }
  }

  @Override
  public void onFlutterUiNoLongerDisplayed() {
    // no-op
  }

  @Override
  public boolean shouldRestoreAndSaveState() {
    if (getIntent().hasExtra(EXTRA_ENABLE_STATE_RESTORATION)) {
      return getIntent().getBooleanExtra(EXTRA_ENABLE_STATE_RESTORATION, false);
    }
    // Prevent overwriting the existing state in a cached engine with restoration state.
    return getCachedEngineId() == null;
  }

  // bifrost implementation
  @Override
  public void detachFromFlutterEngine() {
    if (delegate != null) {
      Log.v(
          TAG,
          "XFlutterActivity "
              + this
              + " connection to the engine "
              + getFlutterEngine()
              + " evicted by another attaching activity");
      delegate.detach();
    } else {
      Log.w(TAG, "delegate has been released !!");
    }
  }

  // bifrost implementation
  @Override
  public boolean stillAttachedForEvent(String event) {
    if (delegate.isDetached()) {
      Log.v(TAG, "XFlutterActivity " + hashCode() + " " + event + " called after release.");
      return false;
    }
    return true;
  }
}
