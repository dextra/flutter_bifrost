//
//  Bifrost.swift
//  bifrost
//
//  Created by Matheus Lutero on 23/02/21.
//

import Flutter
import Foundation

public class Bifrost {
    
    public static let `default` = Bifrost()
    
    public private(set) var engine: FlutterEngine!
    
    private var notificationChannel: BifrostNotificationChannel!
    private var coordinatorChannel: BifrostCoordinatorChannel!
    private var commonChannel: BifrostCommonChannel!
    private var nextPageId: Int = 0
    
    private static var currentViewController: BifrostBaseViewController? {
        Bifrost.default.engine.viewController as? BifrostBaseViewController
    }
    
    /// start flutter engine
    ///
    /// - Parameters:
    ///   - commonHandler: Common flutter method call handler
    ///
    public static func startFlutterEngine(commonHandler: FlutterMethodCallHandler? = nil) {
        let bifrost = Bifrost.default
        if bifrost.engine != nil {
            debugPrint("bifrost flutter engine is already started")
            return
        }
        
        let engine = FlutterEngine(name: "io.flutter.bifrost", project: nil, allowHeadlessExecution: true)
        guard engine.run(withEntrypoint: "main") else {
            fatalError("run FlutterEngine failed")
        }
        bifrost.engine = engine
        bifrost.notificationChannel = BifrostNotificationChannel(engine.binaryMessenger)
        bifrost.coordinatorChannel = BifrostCoordinatorChannel(engine.binaryMessenger)
        bifrost.commonChannel = BifrostCommonChannel(engine.binaryMessenger)
        
        if let handler = commonHandler {
            bifrost.commonChannel.setMethodCallHandler(handler)
        }
        
        guard let clazz: AnyObject = NSClassFromString("GeneratedPluginRegistrant") else {
            fatalError("missing GeneratedPluginRegistrant")
        }
        
        let registerSelector: Selector = Selector(("registerWithRegistry:"))
        let _ = clazz.perform(registerSelector, with: engine)
    }
    
    /// refresh view controller
    ///
    internal static func refreshViewController() {
        guard let viewController = currentViewController, viewController.isShowing else { return }
        viewController.engine?.viewController = nil
        viewController.viewWillAppear(false)
        viewController.viewDidLayoutSubviews()
        viewController.viewDidAppear(false)
    }
    
    /// pop current view controller
    ///
    internal static func popViewController() {
        guard let viewController = currentViewController else { return }
        guard let navigationController = viewController.navigationController else {
            viewController.dismiss(animated: true, completion: nil); return
        }
        navigationController.popViewController(animated: true)
    }
    
    /// generate page id
    ///
    internal static func generatePageId() -> Int {
        Bifrost.default.nextPageId += 1
        return Bifrost.default.nextPageId
    }
}

// MARK: Notification

extension Bifrost {
    
    /// receive notification from flutter
    ///
    public static func registerNotification(_ key: String, handler: @escaping BifrostNotificationHandler) {
        Bifrost.default.notificationChannel.register(key, handler)
    }
    
    /// unregister notification from flutter
    ///
    public static func unregisterNotification(_ key: String) {
        Bifrost.default.notificationChannel.unregister(key)
    }
}


// MARK: Coordinator

extension Bifrost {
    
    /// create page with initial route
    ///
    internal static func onCreatePage(_ data: BifrostPageData) {
        Bifrost.default.coordinatorChannel.onCreatePage(data)
    }
    
    /// show page by initial route
    ///
    internal static func onShowPage(_ data: BifrostPageData) {
        Bifrost.default.coordinatorChannel.onShowPage(data)
    }
    
    /// remove page container by initial route
    ///
    internal static func onDeallocPage(_ data: BifrostPageData) {
        Bifrost.default.coordinatorChannel.onDeallocPage(data)
    }
}
