//
//  BifrostCoordinatorChannel.swift
//  bifrost
//
//  Created by Matheus Lutero on 23/02/21.
//

import Flutter
import Foundation

internal typealias BifrostPageData = Dictionary<String, Any?>

internal class BifrostCoordinatorChannel {
    
    private var channel: FlutterMethodChannel!
    
    init(_ messenger: FlutterBinaryMessenger) {
        channel = FlutterMethodChannel(name: "bifrost/coordinator", binaryMessenger: messenger)
        channel.resizeBuffer(2)
        channel.setMethodCallHandler(handle(_:result:))
    }
    
    func onCreatePage(_ data: BifrostPageData) {
        channel.invokeMethod("onCreatePage", arguments: data)
    }

    func onShowPage(_ data: BifrostPageData) {
        channel.invokeMethod("onShowPage", arguments: data)
    }

    func onDeallocPage(_ data: BifrostPageData) {
        channel.invokeMethod("onDeallocPage", arguments: data)
    }
    
    private func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if call.method == "popViewController" {
            Bifrost.popViewController()
        }
    }
}
