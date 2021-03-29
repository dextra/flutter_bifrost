import 'package:flutter/services.dart';

class BifrostChannels {
  BifrostChannels._();

  static const MethodChannel common = MethodChannel('bifrost/common');

  static const MethodChannel notification = MethodChannel('bifrost/notification');
}
