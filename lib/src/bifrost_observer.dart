import 'package:flutter/widgets.dart';

class BifrostObserver extends NavigatorObserver {
  int _pagesIndStack = 0;

  bool get onlyPage => _pagesIndStack == 0;

  BifrostObserver(List<NavigatorObserver> observers) {
    if (observers != null) {
      for (final NavigatorObserver observer in observers) {
        addProxyObserver(observer);
      }
    }
  }

  static final Set<NavigatorObserver> _proxyObservers = <NavigatorObserver>{};

  void addProxyObserver(NavigatorObserver observer) {
    _proxyObservers.add(observer);
  }

  @override
  void didPush(Route<dynamic> route, Route<dynamic> previousRoute) {
    _pagesIndStack++;
    for (final NavigatorObserver observer in _proxyObservers) {
      observer.didPush(route, previousRoute);
    }
  }

  @override
  void didPop(Route<dynamic> route, Route<dynamic> previousRoute) {
    _pagesIndStack--;
    for (final NavigatorObserver observer in _proxyObservers) {
      observer.didPop(route, previousRoute);
    }
  }

  @override
  void didRemove(Route<dynamic> route, Route<dynamic> previousRoute) {
    _pagesIndStack--;
    for (final NavigatorObserver observer in _proxyObservers) {
      observer.didRemove(route, previousRoute);
    }
  }

  @override
  void didReplace({Route<dynamic> newRoute, Route<dynamic> oldRoute}) {
    for (final NavigatorObserver observer in _proxyObservers) {
      observer.didReplace(newRoute: newRoute, oldRoute: oldRoute);
    }
  }
}
