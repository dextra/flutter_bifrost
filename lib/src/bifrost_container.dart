import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'bifrost_manager.dart';
import 'bifrost_settings.dart';
import 'log.dart';

class BifrostContainer extends Navigator {
  final BifrostSettings settings;

  const BifrostContainer({
    GlobalKey<BifrostContainerState>? key,
    String? initialRoute,
    RouteFactory? onGenerateRoute,
    RouteFactory? onUnknownRoute,
    List<NavigatorObserver>? observers,
    required this.settings,
  }) : super(
          key: key,
          initialRoute: initialRoute,
          onGenerateRoute: onGenerateRoute,
          onUnknownRoute: onUnknownRoute,
          observers: observers ?? const [],
        );

  factory BifrostContainer.obtain(
    Navigator initialNavigator,
    BifrostSettings bifrostSettings,
  ) =>
      BifrostContainer(
        key: GlobalKey<BifrostContainerState>(),
        initialRoute: bifrostSettings.route,
        settings: bifrostSettings,
        onGenerateRoute: (RouteSettings routeSettings) {
          // this is necessary so that the initial navigator does not replace
          // your custom initial route
          if (bifrostSettings.observer.onlyPage &&
              routeSettings.name != bifrostSettings.route) {
            return null;
          }
          // if this route is the first in the stack then it takes bifrost
          // route and arguments
          if (bifrostSettings.observer.onlyPage) {
            routeSettings = RouteSettings(
              name: bifrostSettings.route,
              arguments: bifrostSettings.arguments,
            );
          }
          return initialNavigator.onGenerateRoute?.call(routeSettings);
        },
        onUnknownRoute: initialNavigator.onUnknownRoute,
        observers: <NavigatorObserver>[
          bifrostSettings.observer,
          HeroController(),
        ],
      );

  static BifrostContainerState? stateOf(BifrostContainer container) {
    if (container.key is GlobalKey<BifrostContainerState>) {
      final GlobalKey<BifrostContainerState> globalKey =
          container.key as GlobalKey<BifrostContainerState>;
      return globalKey.currentState;
    }
    log('key of BifrostContainer must be GlobalKey<BifrostContainerState>',
        level: Level.WARNING);
    return null;
  }

  @override
  BifrostContainerState createState() => BifrostContainerState();
}

class BifrostContainerState extends NavigatorState {
  VoidCallback? _backPressedHandler;

  @override
  BifrostContainer get widget => super.widget as BifrostContainer;

  BifrostSettings get settings => widget.settings;

  @override
  void initState() {
    super.initState();
    _backPressedHandler = () => maybePop();
  }

  void performBackPressed() => _backPressedHandler?.call();

  @override
  void pop<T extends Object?>([T? result]) {
    if (canPop()) {
      super.pop<T>(result);
    } else {
      if (Platform.isIOS) {
        BifrostManager.of(context)?.popViewController();
      } else {
        SystemNavigator.pop();
      }
    }
  }

  @override
  Future<bool> maybePop<T extends Object?>([T? result]) async {
    final maybe = await super.maybePop(result);
    if (!maybe) {
      pop(result);
    }
    return maybe;
  }
}
