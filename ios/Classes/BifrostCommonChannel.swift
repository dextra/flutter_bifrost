//
//  BifrostCommonChannel.swift
//  bifrost
//
//  Created by Matheus Lutero on 23/02/21.
//

import Flutter
import Foundation

internal class BifrostCommonChannel {
    
    private var channel: FlutterMethodChannel!
    
    init(_ messenger: FlutterBinaryMessenger) {
        channel = FlutterMethodChannel(name: "bifrost/common", binaryMessenger: messenger)
    }
    
    func setMethodCallHandler(_ handler: @escaping FlutterMethodCallHandler) {
        channel.setMethodCallHandler(handler)
    }
}
