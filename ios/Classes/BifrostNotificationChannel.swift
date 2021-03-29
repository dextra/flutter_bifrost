//
//  BifrostNotificationChannel.swift
//  bifrost
//
//  Created by Matheus Lutero on 23/02/21.
//

import Flutter
import Foundation

public typealias BifrostNotificationHandler = (_ arguments: Any?) -> Void

internal class BifrostNotificationChannel {
    
    private var notifications: Dictionary<String, BifrostNotificationHandler> = [:]
    private var channel: FlutterMethodChannel!
    
    init(_ messenger: FlutterBinaryMessenger) {
        channel = FlutterMethodChannel(name: "bifrost/notification", binaryMessenger: messenger)
        channel.setMethodCallHandler(handle(_:result:))
    }
    
    func register(_ key: String, _ handler: @escaping BifrostNotificationHandler) {
        notifications[key] = handler
    }
    
    func unregister(_ key: String) {
        notifications.removeValue(forKey: key)
    }
    
    private func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        let key = call.method
        let args = call.arguments
        
        if let notification = notifications[key] {
            notification(args); result(true)
        } else {
            result(false)
        }
    }
}
