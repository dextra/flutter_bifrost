package br.com.dextra.bifrost

import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel

internal class BifrostCommonChannel(messenger: BinaryMessenger, handler: MethodChannel.MethodCallHandler) {

  private val channel by lazy { MethodChannel(messenger, "bifrost/common") }

  init {
    channel.setMethodCallHandler(handler)
  }
}
