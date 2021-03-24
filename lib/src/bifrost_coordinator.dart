import 'package:flutter/services.dart';

import 'bifrost_manager.dart';
import 'log.dart';

const _channel = MethodChannel('bifrost/coordinator');

class BifrostCoordinator {
  final BifrostManagerState manager;

  BifrostCoordinator(this.manager) {
    _channel.setMethodCallHandler(_handler);
  }

  Future<bool> _handler(MethodCall call) async {
    log('method: ${call.method}, arguments: ${call.arguments}');

    final arguments = (call.arguments as Map).cast<String, dynamic>();
    assert(arguments != null);

    final id = arguments['id'] as int;
    assert(id != null);

    switch (call.method) {
      case 'onCreatePage':
        manager.createPageContainerIfNeed(arguments);
        return true;
      case 'onShowPage':
        manager.createPageContainerIfNeed(arguments);
        manager.showPageContainer(id);
        return true;
      case 'onDeallocPage':
        manager.deallocPageContainer(id);
        return true;
      case 'onBackPressed':
        manager.onBackPressed(id);
        return true;
      case 'canPop':
        return manager.canPop(id);
      default:
        return false;
    }
  }

  void popViewController() {
    _channel.invokeMethod('popViewController');
  }
}
