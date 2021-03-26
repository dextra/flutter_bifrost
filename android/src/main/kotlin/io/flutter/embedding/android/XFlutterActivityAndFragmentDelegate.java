// Copyright 2013 The Flutter Authors. All rights reserved.
// Copyright 2021 The Bifrost Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.embedding.android;

import static android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;

import br.com.dextra.bifrost.BifrostSnapshotSplashScreen;
import io.flutter.FlutterInjector;
import io.flutter.Log;
import io.flutter.app.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.FlutterEngineCache;
import io.flutter.embedding.engine.FlutterShellArgs;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.embedding.engine.renderer.FlutterUiDisplayListener;
import io.flutter.plugin.platform.PlatformPlugin;
import java.util.Arrays;

/**
 * Delegate that implements all Flutter logic that is the same between a {@link FlutterActivity} and
 * a {@link FlutterFragment}.
 *
 * <p><strong>Why does this class exist?</strong>
 *
 * <p>One might ask why an {@code Activity} and {@code Fragment} delegate needs to exist. Given that
 * a {@code Fragment} can be placed within an {@code Activity}, it would make more sense to use a
 * {@link FlutterFragment} within a {@link FlutterActivity}.
 *
 * <p>The {@code Fragment} support library adds 100k of binary size to an app, and full-Flutter apps
 * do not otherwise require that binary hit. Therefore, it was concluded that Flutter must provide a
 * {@link FlutterActivity} based on the AOSP {@code Activity}, and an independent {@link
 * FlutterFragment} for add-to-app developers.
 *
 * <p>If a time ever comes where the inclusion of {@code Fragment}s in a full-Flutter app is no
 * longer deemed an issue, this class should be immediately decomposed between {@link
 * FlutterActivity} and {@link FlutterFragment} and then eliminated.
 *
 * <p><strong>Caution when modifying this class</strong>
 *
 * <p>Any time that a "delegate" is created with the purpose of encapsulating the internal behaviors
 * of another object, that delegate is highly susceptible to degeneration. It is easy to tack new
 * responsibilities on to the delegate which would not otherwise be added to the original object. It
 * is also easy to begin hanging listeners and callbacks on a delegate object that likewise would
 * not be added to the original object. A delegate can quickly become a complex web of dependencies
 * and optional references that are very difficult to track.
 *
 * <p>Maintainers of this class should take care to only place code in this delegate that would
 * otherwise be placed in either {@link FlutterActivity} or {@link FlutterFragment}, and in exactly
 * the same form. <strong>Do not use this class as a convenient shortcut for any other
 * behavior.</strong>
 */
/* package */ final class XFlutterActivityAndFragmentDelegate {
  private static final String TAG = "XFlutterActivityAndFragmentDelegate";
  private static final String FRAMEWORK_RESTORATION_BUNDLE_KEY = "framework";
  private static final String PLUGINS_RESTORATION_BUNDLE_KEY = "plugins";

  // bifrost implementation
  static Host currentHost;

  // The FlutterActivity or FlutterFragment that is delegating most of its calls
  // to this XFlutterActivityAndFragmentDelegate.
  @NonNull private Host host;
  @Nullable private FlutterEngine flutterEngine;
  @Nullable private FlutterSplashView flutterSplashView;
  @Nullable private FlutterView flutterView;
  @Nullable private PlatformPlugin platformPlugin;
  private boolean isFlutterEngineFromHost;
  private boolean isDetached;

  // bifrost implementation
  @Nullable private View reattachView;
  @Nullable private BifrostSnapshotSplashScreen reAttachSplashScreen;

  @NonNull
  private final FlutterUiDisplayListener flutterUiDisplayListener =
      new FlutterUiDisplayListener() {
        @Override
        public void onFlutterUiDisplayed() {
          host.onFlutterUiDisplayed();
        }

        @Override
        public void onFlutterUiNoLongerDisplayed() {
          host.onFlutterUiNoLongerDisplayed();
        }
      };

  XFlutterActivityAndFragmentDelegate(@NonNull Host host) {
    this.host = host;
  }

  /**
   * Disconnects this {@code XFlutterActivityAndFragmentDelegate} from its host {@code Activity} or
   * {@code Fragment}.
   *
   * <p>No further method invocations may occur on this {@code XFlutterActivityAndFragmentDelegate}
   * after invoking this method. If a method is invoked, an exception will occur.
   *
   * <p>This method only clears out references. It does not destroy its {@link FlutterEngine}. The
   * behavior that destroys a {@link FlutterEngine} can be found in {@link #onDetach()}.
   */
  void release() {
    this.host = null;
    this.flutterEngine = null;
    this.flutterView = null;
    this.platformPlugin = null;
  }

  // bifrost implementation
  void detach() {
    assert flutterView != null;
    assert flutterSplashView != null;
    assert flutterEngine != null;

    Log.w(TAG, "detach " + flutterView.toString());

    if (host.shouldAttachEngineToActivity()) {
      // Notify plugins that they are no longer attached to an Activity.
      Log.v(TAG, "Detaching FlutterEngine from the Activity that owns this Fragment.");
      if (requireNonNull(host.getActivity()).isChangingConfigurations()) {
        flutterEngine.getActivityControlSurface().detachFromActivityForConfigChanges();
      } else {
        flutterEngine.getActivityControlSurface().detachFromActivity();
      }
    }

    // Null out the platformPlugin to avoid a possible retain cycle between the plugin, this
    // Fragment,
    // and this Fragment's Activity.
    // if (platformPlugin != null) {
    //  platformPlugin.destroy();
    //  platformPlugin = null;
    // }
    isDetached = true;

    reAttachSplashScreen = new BifrostSnapshotSplashScreen(flutterEngine);

    flutterView.detachFromFlutterEngine();
    flutterView.removeOnFirstFrameRenderedListener(flutterUiDisplayListener);

    flutterEngine.getLifecycleChannel().appIsInactive();

    reattachView = reAttachSplashScreen.createSplashView(getAppComponent(), null);

    flutterSplashView.addView(reattachView);
    flutterSplashView.removeView(flutterView);
  }

  // bifrost implementation
  private <T> T requireNonNull(T obj) {
    if (obj == null) throw new NullPointerException();
    return obj;
  }

  // bifrost implementation
  boolean isDetached() {
    return this.isDetached;
  }

  // bifrost implementation
  void reattach() {
    assert flutterView != null;
    assert flutterSplashView != null;
    assert flutterEngine != null;

    Log.w(TAG, "reattach " + flutterView.toString());

    flutterSplashView.displayFlutterViewWithSplash(flutterView, reAttachSplashScreen);

    onAttach(host.getContext());

    flutterView.addOnFirstFrameRenderedListener(flutterUiDisplayListener);
    flutterView.attachToFlutterEngine(flutterEngine);

    flutterEngine.getLifecycleChannel().appIsResumed();

    isDetached = false;

    if (reattachView != null) {
      new Handler()
          .postDelayed(
              new Runnable() {
                @Override
                public void run() {
                  flutterSplashView.removeView(reattachView);
                }
              },
              1000);
    }
  }

  /**
   * Returns the {@link FlutterEngine} that is owned by this delegate and its host {@code Activity}
   * or {@code Fragment}.
   */
  @Nullable
  /* package */ FlutterEngine getFlutterEngine() {
    return flutterEngine;
  }

  /**
   * Returns true if the host {@code Activity}/{@code Fragment} provided a {@code FlutterEngine}, as
   * opposed to this delegate creating a new one.
   */
  /* package */ boolean isFlutterEngineFromHost() {
    return isFlutterEngineFromHost;
  }

  /**
   * Invoke this method from {@code Activity#onCreate(Bundle)} or {@code
   * Fragment#onAttach(Context)}.
   *
   * <p>This method does the following:
   *
   * <p>
   *
   * <ol>
   *   <li>Initializes the Flutter system.
   *   <li>Obtains or creates a {@link FlutterEngine}.
   *   <li>Creates and configures a {@link PlatformPlugin}.
   *   <li>Attaches the {@link FlutterEngine} to the surrounding {@code Activity}, if desired.
   *   <li>Configures the {@link FlutterEngine} via {@link
   *       Host#configureFlutterEngine(FlutterEngine)}.
   * </ol>
   */
  void onAttach(@NonNull Context context) {
    ensureAlive();

    // When "retain instance" is true, the FlutterEngine will survive configuration
    // changes. Therefore, we create a new one only if one does not already exist.
    if (flutterEngine == null) {
      setupFlutterEngine();
    }

    // Regardless of whether or not a FlutterEngine already existed, the PlatformPlugin
    // is bound to a specific Activity. Therefore, it needs to be created and configured
    // every time this Fragment attaches to a new Activity.
    // TODO(mattcarroll): the PlatformPlugin needs to be reimagined because it implicitly takes
    //                    control of the entire window. This is unacceptable for non-fullscreen
    //                    use-cases.
    platformPlugin = host.providePlatformPlugin(host.getActivity(), flutterEngine);

    if (host.shouldAttachEngineToActivity()) {
      // Notify any plugins that are currently attached to our FlutterEngine that they
      // are now attached to an Activity.
      //
      // Passing this Fragment's Lifecycle should be sufficient because as long as this Fragment
      // is attached to its Activity, the lifecycles should be in sync. Once this Fragment is
      // detached from its Activity, that Activity will be detached from the FlutterEngine, too,
      // which means there shouldn't be any possibility for the Fragment Lifecycle to get out of
      // sync with the Activity. We use the Fragment's Lifecycle because it is possible that the
      // attached Activity is not a LifecycleOwner.
      Log.v(TAG, "Attaching FlutterEngine to the Activity that owns this Fragment.");
      flutterEngine
          .getActivityControlSurface()
          .attachToActivity(host.getActivity(), host.getLifecycle());
      // bifrost implementation
      if (currentHost != null && currentHost != host) {
        currentHost.detachFromFlutterEngine();
      }
      currentHost = host;
    }

    host.configureFlutterEngine(flutterEngine);
  }

  // bifrost implementation
  Activity getAppComponent() {
    final Activity activity = host.getActivity();
    if (activity == null) {
      throw new AssertionError(
          "XFlutterActivityAndFragmentDelegate's getAppComponent should only "
              + "be queried after onAttach, when the host's activity should always be non-null");
    }
    return activity;
  }

  /**
   * Obtains a reference to a FlutterEngine to back this delegate and its {@code host}.
   *
   * <p>
   *
   * <p>First, the {@code host} is asked if it would like to use a cached {@link FlutterEngine}, and
   * if so, the cached {@link FlutterEngine} is retrieved.
   *
   * <p>Second, the {@code host} is given an opportunity to provide a {@link FlutterEngine} via
   * {@link Host#provideFlutterEngine(Context)}.
   *
   * <p>If the {@code host} does not provide a {@link FlutterEngine}, then a new {@link
   * FlutterEngine} is instantiated.
   */
  @VisibleForTesting
  /* package */ void setupFlutterEngine() {
    Log.v(TAG, "Setting up FlutterEngine.");

    // First, check if the host wants to use a cached FlutterEngine.
    String cachedEngineId = host.getCachedEngineId();
    if (cachedEngineId != null) {
      flutterEngine = FlutterEngineCache.getInstance().get(cachedEngineId);
      isFlutterEngineFromHost = true;
      if (flutterEngine == null) {
        throw new IllegalStateException(
            "The requested cached FlutterEngine did not exist in the FlutterEngineCache: '"
                + cachedEngineId
                + "'");
      }
      return;
    }

    // Second, defer to subclasses for a custom FlutterEngine.
    flutterEngine = host.provideFlutterEngine(host.getContext());
    if (flutterEngine != null) {
      isFlutterEngineFromHost = true;
      return;
    }

    // Our host did not provide a custom FlutterEngine. Create a FlutterEngine to back our
    // FlutterView.
    Log.v(
        TAG,
        "No preferred FlutterEngine was provided. Creating a new FlutterEngine for"
            + " this FlutterFragment.");
    flutterEngine =
        new FlutterEngine(
            host.getContext(),
            host.getFlutterShellArgs().toArray(),
            /*automaticallyRegisterPlugins=*/ false,
            /*willProvideRestorationData=*/ host.shouldRestoreAndSaveState());
    isFlutterEngineFromHost = false;
  }

  /**
   * Invoke this method from {@code Activity#onCreate(Bundle)} to create the content {@code View},
   * or from {@code Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}.
   *
   * <p>{@code inflater} and {@code container} may be null when invoked from an {@code Activity}.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>creates a new {@link FlutterView} in a {@code View} hierarchy
   *   <li>adds a {@link FlutterUiDisplayListener} to it
   *   <li>attaches a {@link FlutterEngine} to the new {@link FlutterView}
   *   <li>returns the new {@code View} hierarchy
   * </ol>
   */
  @NonNull
  View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    Log.v(TAG, "Creating FlutterView.");
    ensureAlive();

    if (host.getRenderMode() == RenderMode.surface) {
      FlutterSurfaceView flutterSurfaceView =
          new FlutterSurfaceView(
              host.getActivity(), host.getTransparencyMode() == TransparencyMode.transparent);

      // Allow our host to customize FlutterSurfaceView, if desired.
      host.onFlutterSurfaceViewCreated(flutterSurfaceView);

      // Create the FlutterView that owns the FlutterSurfaceView.
      flutterView = new FlutterView(host.getActivity(), flutterSurfaceView);
    } else {
      FlutterTextureView flutterTextureView = new FlutterTextureView(host.getActivity());

      // Allow our host to customize FlutterSurfaceView, if desired.
      host.onFlutterTextureViewCreated(flutterTextureView);

      // Create the FlutterView that owns the FlutterTextureView.
      flutterView = new FlutterView(host.getActivity(), flutterTextureView);
    }

    // Add listener to be notified when Flutter renders its first frame.
    flutterView.addOnFirstFrameRenderedListener(flutterUiDisplayListener);

    flutterSplashView = new FlutterSplashView(host.getContext());
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      flutterSplashView.setId(View.generateViewId());
    }
    // bifrost implementation
    // } else {
    //   // TODO(mattcarroll): Find a better solution to this ID. This is a random, static ID.
    //   // It might conflict with other Views, and it means that only a single FlutterSplashView
    //   // can exist in a View hierarchy at one time.
    //   flutterSplashView.setId(486947586);
    // }
    flutterSplashView.displayFlutterViewWithSplash(flutterView, host.provideSplashScreen());

    Log.v(TAG, "Attaching FlutterEngine to FlutterView.");
    flutterView.attachToFlutterEngine(flutterEngine);

    return flutterSplashView;
  }

  void onActivityCreated(@Nullable Bundle bundle) {
    Log.v(TAG, "onActivityCreated. Giving framework and plugins an opportunity to restore state.");
    ensureAlive();

    Bundle pluginState = null;
    byte[] frameworkState = null;
    if (bundle != null) {
      pluginState = bundle.getBundle(PLUGINS_RESTORATION_BUNDLE_KEY);
      frameworkState = bundle.getByteArray(FRAMEWORK_RESTORATION_BUNDLE_KEY);
    }

    if (host.shouldRestoreAndSaveState()) {
      flutterEngine.getRestorationChannel().setRestorationData(frameworkState);
    }

    if (host.shouldAttachEngineToActivity()) {
      flutterEngine.getActivityControlSurface().onRestoreInstanceState(pluginState);
    }
  }

  /**
   * Invoke this from {@code Activity#onStart()} or {@code Fragment#onStart()}.
   *
   * <p>This method:
   *
   * <p>
   *
   * <ol>
   *   <li>Begins executing Dart code, if it is not already executing.
   * </ol>
   */
  void onStart() {
    Log.v(TAG, "onStart()");
    ensureAlive();
    doInitialFlutterViewRun();
  }

  /**
   * Starts running Dart within the FlutterView for the first time.
   *
   * <p>Reloading/restarting Dart within a given FlutterView is not supported. If this method is
   * invoked while Dart is already executing then it does nothing.
   *
   * <p>{@code flutterEngine} must be non-null when invoking this method.
   */
  private void doInitialFlutterViewRun() {
    // Don't attempt to start a FlutterEngine if we're using a cached FlutterEngine.
    if (host.getCachedEngineId() != null) {
      return;
    }

    if (flutterEngine.getDartExecutor().isExecutingDart()) {
      // No warning is logged because this situation will happen on every config
      // change if the developer does not choose to retain the Fragment instance.
      // So this is expected behavior in many cases.
      return;
    }

    Log.v(
        TAG,
        "Executing Dart entrypoint: "
            + host.getDartEntrypointFunctionName()
            + ", and sending initial route: "
            + host.getInitialRoute());

    // The engine needs to receive the Flutter app's initial route before executing any
    // Dart code to ensure that the initial route arrives in time to be applied.
    if (host.getInitialRoute() != null) {
      flutterEngine.getNavigationChannel().setInitialRoute(host.getInitialRoute());
    }

    String appBundlePathOverride = host.getAppBundlePath();
    if (appBundlePathOverride == null || appBundlePathOverride.isEmpty()) {
      appBundlePathOverride = FlutterInjector.instance().flutterLoader().findAppBundlePath();
    }

    // Configure the Dart entrypoint and execute it.
    DartExecutor.DartEntrypoint entrypoint =
        new DartExecutor.DartEntrypoint(
            appBundlePathOverride, host.getDartEntrypointFunctionName());
    flutterEngine.getDartExecutor().executeDartEntrypoint(entrypoint);
  }

  /**
   * Invoke this from {@code Activity#onResume()} or {@code Fragment#onResume()}.
   *
   * <p>This method notifies the running Flutter app that it is "resumed" as per the Flutter app
   * lifecycle.
   */
  void onResume() {
    Log.v(TAG, "onResume()");
    ensureAlive();
    flutterEngine.getLifecycleChannel().appIsResumed();
  }

  /**
   * Invoke this from {@code Activity#onPostResume()}.
   *
   * <p>A {@code Fragment} host must have its containing {@code Activity} forward this call so that
   * the {@code Fragment} can then invoke this method.
   *
   * <p>This method informs the {@link PlatformPlugin} that {@code onPostResume()} has run, which is
   * used to update system UI overlays.
   */
  // TODO(mattcarroll): determine why this can't be in onResume(). Comment reason, or move if
  // possible.
  void onPostResume() {
    Log.v(TAG, "onPostResume()");
    ensureAlive();
    if (flutterEngine != null) {
      if (platformPlugin != null) {
        // TODO(mattcarroll): find a better way to handle the update of UI overlays than calling
        // through
        //                    to platformPlugin. We're implicitly entangling the Window, Activity,
        // Fragment,
        //                    and engine all with this one call.
        platformPlugin.updateSystemUiOverlays();
      }
    } else {
      Log.w(TAG, "onPostResume() invoked before FlutterFragment was attached to an Activity.");
    }
  }

  /**
   * Invoke this from {@code Activity#onPause()} or {@code Fragment#onPause()}.
   *
   * <p>This method notifies the running Flutter app that it is "inactive" as per the Flutter app
   * lifecycle.
   */
  void onPause() {
    Log.v(TAG, "onPause()");
    ensureAlive();
    flutterEngine.getLifecycleChannel().appIsInactive();
  }

  /**
   * Invoke this from {@code Activity#onStop()} or {@code Fragment#onStop()}.
   *
   * <p>This method:
   *
   * <p>
   *
   * <ol>
   *   <li>This method notifies the running Flutter app that it is "paused" as per the Flutter app
   *       lifecycle.
   *   <li>Detaches this delegate's {@link FlutterEngine} from this delegate's {@link FlutterView}.
   * </ol>
   */
  void onStop() {
    Log.v(TAG, "onStop()");
    ensureAlive();
    flutterEngine.getLifecycleChannel().appIsPaused();
  }

  /**
   * Invoke this from {@code Activity#onDestroy()} or {@code Fragment#onDestroyView()}.
   *
   * <p>This method removes this delegate's {@link FlutterView}'s {@link FlutterUiDisplayListener}.
   */
  void onDestroyView() {
    Log.v(TAG, "onDestroyView()");
    ensureAlive();

    flutterView.detachFromFlutterEngine();
    flutterView.removeOnFirstFrameRenderedListener(flutterUiDisplayListener);
  }

  void onSaveInstanceState(@Nullable Bundle bundle) {
    Log.v(TAG, "onSaveInstanceState. Giving framework and plugins an opportunity to save state.");
    ensureAlive();

    if (host.shouldRestoreAndSaveState()) {
      bundle.putByteArray(
          FRAMEWORK_RESTORATION_BUNDLE_KEY,
          flutterEngine.getRestorationChannel().getRestorationData());
    }

    if (host.shouldAttachEngineToActivity()) {
      final Bundle plugins = new Bundle();
      flutterEngine.getActivityControlSurface().onSaveInstanceState(plugins);
      bundle.putBundle(PLUGINS_RESTORATION_BUNDLE_KEY, plugins);
    }
  }

  /**
   * Invoke this from {@code Activity#onDestroy()} or {@code Fragment#onDetach()}.
   *
   * <p>This method:
   *
   * <p>
   *
   * <ol>
   *   <li>Detaches this delegate's {@link FlutterEngine} from its surrounding {@code Activity}, if
   *       it was previously attached.
   *   <li>Destroys this delegate's {@link PlatformPlugin}.
   *   <li>Destroys this delegate's {@link FlutterEngine} if {@link
   *       Host#shouldDestroyEngineWithHost()} ()} returns true.
   * </ol>
   */
  void onDetach() {
    Log.v(TAG, "onDetach()");
    ensureAlive();

    // Give the host an opportunity to cleanup any references that were created in
    // configureFlutterEngine().
    host.cleanUpFlutterEngine(flutterEngine);

    if (host.shouldAttachEngineToActivity()) {
      // Notify plugins that they are no longer attached to an Activity.
      Log.v(TAG, "Detaching FlutterEngine from the Activity that owns this Fragment.");
      if (host.getActivity().isChangingConfigurations()) {
        flutterEngine.getActivityControlSurface().detachFromActivityForConfigChanges();
      } else {
        flutterEngine.getActivityControlSurface().detachFromActivity();
      }
    }

    // Null out the platformPlugin to avoid a possible retain cycle between the plugin, this
    // Fragment,
    // and this Fragment's Activity.
    if (platformPlugin != null) {
      platformPlugin.destroy();
      platformPlugin = null;
    }

    flutterEngine.getLifecycleChannel().appIsDetached();

    // Destroy our FlutterEngine if we're not set to retain it.
    if (host.shouldDestroyEngineWithHost()) {
      flutterEngine.destroy();

      if (host.getCachedEngineId() != null) {
        FlutterEngineCache.getInstance().remove(host.getCachedEngineId());
      }

      flutterEngine = null;
    }
  }

  /**
   * Invoke this from {@link Activity#onBackPressed()}.
   *
   * <p>A {@code Fragment} host must have its containing {@code Activity} forward this call so that
   * the {@code Fragment} can then invoke this method.
   *
   * <p>This method instructs Flutter's navigation system to "pop route".
   */
  void onBackPressed() {
    ensureAlive();
    if (flutterEngine != null) {
      Log.v(TAG, "Forwarding onBackPressed() to FlutterEngine.");
      flutterEngine.getNavigationChannel().popRoute();
    } else {
      Log.w(TAG, "Invoked onBackPressed() before FlutterFragment was attached to an Activity.");
    }
  }

  /**
   * Invoke this from {@link Activity#onRequestPermissionsResult(int, String[], int[])} or {@code
   * Fragment#onRequestPermissionsResult(int, String[], int[])}.
   *
   * <p>This method forwards to interested Flutter plugins.
   */
  void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    ensureAlive();
    if (flutterEngine != null) {
      Log.v(
          TAG,
          "Forwarding onRequestPermissionsResult() to FlutterEngine:\n"
              + "requestCode: "
              + requestCode
              + "\n"
              + "permissions: "
              + Arrays.toString(permissions)
              + "\n"
              + "grantResults: "
              + Arrays.toString(grantResults));
      flutterEngine
          .getActivityControlSurface()
          .onRequestPermissionsResult(requestCode, permissions, grantResults);
    } else {
      Log.w(
          TAG,
          "onRequestPermissionResult() invoked before FlutterFragment was attached to an Activity.");
    }
  }

  /**
   * Invoke this from {@code Activity#onNewIntent(Intent)}.
   *
   * <p>A {@code Fragment} host must have its containing {@code Activity} forward this call so that
   * the {@code Fragment} can then invoke this method.
   *
   * <p>This method forwards to interested Flutter plugins.
   */
  void onNewIntent(@NonNull Intent intent) {
    ensureAlive();
    if (flutterEngine != null) {
      Log.v(TAG, "Forwarding onNewIntent() to FlutterEngine.");
      flutterEngine.getActivityControlSurface().onNewIntent(intent);
    } else {
      Log.w(TAG, "onNewIntent() invoked before FlutterFragment was attached to an Activity.");
    }
  }

  /**
   * Invoke this from {@code Activity#onActivityResult(int, int, Intent)} or {@code
   * Fragment#onActivityResult(int, int, Intent)}.
   *
   * <p>This method forwards to interested Flutter plugins.
   */
  void onActivityResult(int requestCode, int resultCode, Intent data) {
    ensureAlive();
    if (flutterEngine != null) {
      Log.v(
          TAG,
          "Forwarding onActivityResult() to FlutterEngine:\n"
              + "requestCode: "
              + requestCode
              + "\n"
              + "resultCode: "
              + resultCode
              + "\n"
              + "data: "
              + data);
      flutterEngine.getActivityControlSurface().onActivityResult(requestCode, resultCode, data);
    } else {
      Log.w(TAG, "onActivityResult() invoked before FlutterFragment was attached to an Activity.");
    }
  }

  /**
   * Invoke this from {@code Activity#onUserLeaveHint()}.
   *
   * <p>A {@code Fragment} host must have its containing {@code Activity} forward this call so that
   * the {@code Fragment} can then invoke this method.
   *
   * <p>This method forwards to interested Flutter plugins.
   */
  void onUserLeaveHint() {
    ensureAlive();
    if (flutterEngine != null) {
      Log.v(TAG, "Forwarding onUserLeaveHint() to FlutterEngine.");
      flutterEngine.getActivityControlSurface().onUserLeaveHint();
    } else {
      Log.w(TAG, "onUserLeaveHint() invoked before FlutterFragment was attached to an Activity.");
    }
  }

  /**
   * Invoke this from {@link Activity#onTrimMemory(int)}.
   *
   * <p>A {@code Fragment} host must have its containing {@code Activity} forward this call so that
   * the {@code Fragment} can then invoke this method.
   *
   * <p>This method sends a "memory pressure warning" message to Flutter over the "system channel".
   */
  void onTrimMemory(int level) {
    ensureAlive();
    if (flutterEngine != null) {
      // This is always an indication that the Dart VM should collect memory
      // and free any unneeded resources.
      flutterEngine.getDartExecutor().notifyLowMemoryWarning();
      // Use a trim level delivered while the application is running so the
      // framework has a chance to react to the notification.
      if (level == TRIM_MEMORY_RUNNING_LOW) {
        Log.v(TAG, "Forwarding onTrimMemory() to FlutterEngine. Level: " + level);
        flutterEngine.getSystemChannel().sendMemoryPressureWarning();
      }
    } else {
      Log.w(TAG, "onTrimMemory() invoked before FlutterFragment was attached to an Activity.");
    }
  }

  /**
   * Invoke this from {@link Activity#onLowMemory()}.
   *
   * <p>A {@code Fragment} host must have its containing {@code Activity} forward this call so that
   * the {@code Fragment} can then invoke this method.
   *
   * <p>This method sends a "memory pressure warning" message to Flutter over the "system channel".
   */
  void onLowMemory() {
    Log.v(TAG, "Forwarding onLowMemory() to FlutterEngine.");
    ensureAlive();
    flutterEngine.getDartExecutor().notifyLowMemoryWarning();
    flutterEngine.getSystemChannel().sendMemoryPressureWarning();
  }

  /**
   * Ensures that this delegate has not been {@link #release()}'ed.
   *
   * <p>An {@code IllegalStateException} is thrown if this delegate has been {@link #release()}'ed.
   */
  private void ensureAlive() {
    if (host == null) {
      throw new IllegalStateException(
          "Cannot execute method on a destroyed XFlutterActivityAndFragmentDelegate.");
    }
  }

  /**
   * The {@link FlutterActivity} or {@link FlutterFragment} that owns this {@code
   * XFlutterActivityAndFragmentDelegate}.
   */
  /* package */ interface Host
      extends SplashScreenProvider, FlutterEngineProvider, FlutterEngineConfigurator {
    /** Returns the {@link Context} that backs the host {@link Activity} or {@code Fragment}. */
    @NonNull
    Context getContext();

    /**
     * Returns the host {@link Activity} or the {@code Activity} that is currently attached to the
     * host {@code Fragment}.
     */
    @Nullable
    Activity getActivity();

    /** Returns the {@link Lifecycle} that backs the host {@link Activity} or {@code Fragment}. */
    @NonNull
    Lifecycle getLifecycle();

    /** Returns the {@link FlutterShellArgs} that should be used when initializing Flutter. */
    @NonNull
    FlutterShellArgs getFlutterShellArgs();

    /**
     * Returns the ID of a statically cached {@link FlutterEngine} to use within this delegate's
     * host, or {@code null} if this delegate's host does not want to use a cached {@link
     * FlutterEngine}.
     */
    @Nullable
    String getCachedEngineId();

    /**
     * Returns true if the {@link FlutterEngine} used in this delegate should be destroyed when the
     * host/delegate are destroyed.
     *
     * <p>The default value is {@code true} in cases where {@code FlutterFragment} created its own
     * {@link FlutterEngine}, and {@code false} in cases where a cached {@link FlutterEngine} was
     * provided.
     */
    boolean shouldDestroyEngineWithHost();

    /** Returns the Dart entrypoint that should run when a new {@link FlutterEngine} is created. */
    @NonNull
    String getDartEntrypointFunctionName();

    /** Returns the path to the app bundle where the Dart code exists. */
    @NonNull
    String getAppBundlePath();

    /** Returns the initial route that Flutter renders. */
    @Nullable
    String getInitialRoute();

    /**
     * Returns the {@link RenderMode} used by the {@link FlutterView} that displays the {@link
     * FlutterEngine}'s content.
     */
    @NonNull
    RenderMode getRenderMode();

    /**
     * Returns the {@link TransparencyMode} used by the {@link FlutterView} that displays the {@link
     * FlutterEngine}'s content.
     */
    @NonNull
    TransparencyMode getTransparencyMode();

    @Nullable
    SplashScreen provideSplashScreen();

    /**
     * Returns the {@link FlutterEngine} that should be rendered to a {@link FlutterView}.
     *
     * <p>If {@code null} is returned, a new {@link FlutterEngine} will be created automatically.
     */
    @Nullable
    FlutterEngine provideFlutterEngine(@NonNull Context context);

    /**
     * Hook for the host to create/provide a {@link PlatformPlugin} if the associated Flutter
     * experience should control system chrome.
     */
    @Nullable
    PlatformPlugin providePlatformPlugin(
        @Nullable Activity activity, @NonNull FlutterEngine flutterEngine);

    /** Hook for the host to configure the {@link FlutterEngine} as desired. */
    void configureFlutterEngine(@NonNull FlutterEngine flutterEngine);

    /**
     * Hook for the host to cleanup references that were established in {@link
     * #configureFlutterEngine(FlutterEngine)} before the host is destroyed or detached.
     */
    void cleanUpFlutterEngine(@NonNull FlutterEngine flutterEngine);

    /**
     * Returns true if the {@link FlutterEngine}'s plugin system should be connected to the host
     * {@link Activity}, allowing plugins to interact with it.
     */
    boolean shouldAttachEngineToActivity();

    /**
     * Invoked by this delegate when the {@link FlutterSurfaceView} that renders the Flutter UI is
     * initially instantiated.
     *
     * <p>This method is only invoked if the {@link
     * io.flutter.embedding.android.FlutterView.RenderMode} is set to {@link
     * io.flutter.embedding.android.FlutterView.RenderMode#surface}. Otherwise, {@link
     * #onFlutterTextureViewCreated(FlutterTextureView)} is invoked.
     *
     * <p>This method is invoked before the given {@link FlutterSurfaceView} is attached to the
     * {@code View} hierarchy. Implementers should not attempt to climb the {@code View} hierarchy
     * or make assumptions about relationships with other {@code View}s.
     */
    void onFlutterSurfaceViewCreated(@NonNull FlutterSurfaceView flutterSurfaceView);

    /**
     * Invoked by this delegate when the {@link FlutterTextureView} that renders the Flutter UI is
     * initially instantiated.
     *
     * <p>This method is only invoked if the {@link
     * io.flutter.embedding.android.FlutterView.RenderMode} is set to {@link
     * io.flutter.embedding.android.FlutterView.RenderMode#texture}. Otherwise, {@link
     * #onFlutterSurfaceViewCreated(FlutterSurfaceView)} is invoked.
     *
     * <p>This method is invoked before the given {@link FlutterTextureView} is attached to the
     * {@code View} hierarchy. Implementers should not attempt to climb the {@code View} hierarchy
     * or make assumptions about relationships with other {@code View}s.
     */
    void onFlutterTextureViewCreated(@NonNull FlutterTextureView flutterTextureView);

    /** Invoked by this delegate when its {@link FlutterView} starts painting pixels. */
    void onFlutterUiDisplayed();

    /** Invoked by this delegate when its {@link FlutterView} stops painting pixels. */
    void onFlutterUiNoLongerDisplayed();

    /**
     * Whether state restoration is enabled.
     *
     * <p>When this returns true, the instance state provided to {@code onActivityCreated(Bundle)}
     * will be forwarded to the framework via the {@code RestorationChannel} and during {@code
     * onSaveInstanceState(Bundle)} the current framework instance state obtained from {@code
     * RestorationChannel} will be stored in the provided bundle.
     *
     * <p>This defaults to true, unless a cached engine is used.
     */
    boolean shouldRestoreAndSaveState();

    // bifrost implementation
    void detachFromFlutterEngine();

    // bifrost implementation
    boolean stillAttachedForEvent(String event);
  }
}
