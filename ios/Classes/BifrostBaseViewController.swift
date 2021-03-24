//
//  BifrostBaseViewController.swift
//  bifrost
//
//  Created by Matheus Lutero on 24/02/21.
//

import Flutter
import Foundation
import UIKit

internal class BifrostBaseViewController: FlutterViewController {
    
    private let pageId: Int
    private let pageRoute: String
    private let pageArguments: Any?
    private let backgroundColor: UIColor
    
    private(set) var isShowing: Bool = false
    
    private var pageData: BifrostPageData { ["id": pageId, "route": pageRoute, "arguments": pageArguments] }
    
    init(pageRoute: String, pageArguments: Any? = nil, backgroundColor: UIColor = .white) {
        self.pageId = Bifrost.generatePageId()
        self.pageRoute = pageRoute
        self.pageArguments = pageArguments
        self.backgroundColor = backgroundColor
        
        guard let engine = Bifrost.default.engine else {
            fatalError("Bifrost Flutter engine must not be nil")
        }
        engine.viewController = nil
        super.init(engine: engine, nibName: nil, bundle: nil)
        isShowing = true
        Bifrost.onCreatePage(pageData)
    }
    
    required public init(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewWillAppear(_ animated: Bool) {
        engine?.viewController = self
        isShowing = true
        Bifrost.onShowPage(pageData)
        
        super.viewWillAppear(animated)
        view.backgroundColor = backgroundColor
        if #available(iOS 11.0, *) {
            additionalSafeAreaInsets = .zero
        }
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        isShowing = false
        Bifrost.refreshViewController()
    }
    
    deinit {
        Bifrost.onDeallocPage(pageData)
    }
}
