//
//  BifrostFlutterViewController.swift
//  bifrost
//
//  Created by Matheus Lutero on 12/03/21.
//

import UIKit

@IBDesignable open class BifrostFlutterViewController: UIViewController {
    
    @IBInspectable private var initialRoute: String = "/"
    private var arguments: Any? = nil
    
    public init(_ initialRoute: String, arguments: Any? = nil) {
        self.initialRoute = initialRoute
        self.arguments = arguments
        super.init(nibName: nil, bundle: nil)
    }
    
    required public init?(coder: NSCoder) {
        super.init(coder: coder)
    }
    
    open override func viewDidLoad() {
        super.viewDidLoad()
        let vc = BifrostBaseViewController(
            pageRoute: initialRoute,
            pageArguments: arguments,
            backgroundColor: view.backgroundColor ?? .white
        )
        vc.willMove(toParent: self)
        addChild(vc)
        view.addSubview(vc.view)
        vc.didMove(toParent: self)
        vc.view.frame = view.frame
        vc.view.autoresizingMask = [.flexibleWidth, .flexibleWidth]
    }
}
