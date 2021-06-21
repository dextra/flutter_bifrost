import 'package:flutter/widgets.dart';

import 'bifrost_container.dart';
import 'bifrost_coordinator.dart';
import 'bifrost_settings.dart';

class BifrostManager extends StatefulWidget {
  final Navigator initialNavigator;

  const BifrostManager(this.initialNavigator);

  static BifrostManagerState? of(BuildContext context) {
    if (context is StatefulElement && context.state is BifrostManagerState) {
      return context.state as BifrostManagerState;
    }
    return context.findAncestorStateOfType<BifrostManagerState>();
  }

  @override
  BifrostManagerState createState() => BifrostManagerState();
}

class BifrostManagerState extends State<BifrostManager> {
  final List<BifrostContainer> _containers = <BifrostContainer>[];

  late BifrostCoordinator _coordinator;

  int? _index;

  Navigator get initialNavigator => widget.initialNavigator;

  @override
  void initState() {
    _coordinator = BifrostCoordinator(this);
    _createDefaultPageContainer();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return IndexedStack(
      children: _containers,
      index: _index ?? 0,
    );
  }

  /// create page container if need
  void createPageContainerIfNeed(Map<String, dynamic> arguments) {
    assert(arguments['id'] != null);
    assert(arguments['route'] != null);
    final container = BifrostSettings.from(
      navigator: initialNavigator,
      arguments: arguments,
    );
    final id = container.id;
    if (_containers.lastIndexWhere((e) => e.settings.id == id) == -1) {
      _createPageContainer(container);
    }
  }

  /// create container based on initial navigator, this is necessary to run
  /// integration tests.
  void _createDefaultPageContainer() {
    final bifrostSettings = BifrostSettings.from(navigator: initialNavigator);
    final routeSettings = RouteSettings(name: bifrostSettings.route);
    if (initialNavigator.onGenerateRoute?.call(routeSettings) != null) {
      _createPageContainer(bifrostSettings);
    }
  }

  /// create page container
  void _createPageContainer(BifrostSettings settings) {
    final container = BifrostContainer.obtain(initialNavigator, settings);
    _containers.add(container);
  }

  /// show page container by id
  void showPageContainer(int id) {
    final index = _containers.lastIndexWhere((e) => e.settings.id == id);
    if (index > -1 && index != _index) {
      setState(() => _index = index);
    }
  }

  /// remove page container by id
  void deallocPageContainer(int id) {
    _containers.removeWhere((e) => e.settings.id == id);
  }

  /// on back button pressed
  void onBackPressed(int id) {
    final container = _containers.firstWhere((e) => e.settings.id == id);
    BifrostContainer.stateOf(container)?.performBackPressed();
  }

  /// check if page container can pop
  bool canPop(int id) {
    final container = _containers.firstWhere((e) => e.settings.id == id);
    return BifrostContainer.stateOf(container)?.canPop() ?? false;
  }

  /// pop ios view controller
  void popViewController() {
    _coordinator.popViewController();
  }
}
