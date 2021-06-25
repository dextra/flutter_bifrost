import 'package:flutter/widgets.dart';

import 'bifrost_observer.dart';

class BifrostSettings {
  final int id;
  final String route;
  final dynamic arguments;
  final BifrostObserver observer;

  BifrostSettings({
    required this.id,
    required this.route,
    this.arguments,
    List<NavigatorObserver>? observers,
  }) : observer = BifrostObserver(observers);

  factory BifrostSettings.from({
    required Navigator navigator,
    Map<String, dynamic> arguments = const {},
  }) =>
      BifrostSettings(
        id: arguments['id'] ?? 0,
        route: arguments['route'] ?? navigator.initialRoute,
        arguments: arguments['arguments'],
        observers: navigator.observers,
      );
}
