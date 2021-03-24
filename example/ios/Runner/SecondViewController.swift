//
//  SecondViewController.swift
//  Runner
//
//  Created by Matheus Lutero on 12/03/21.
//

import UIKit
import bifrost

class SecondViewController: BifrostFlutterViewController {
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // register notification
        Bifrost.registerNotification("openGreetingsPage", handler: openGreetingsPage)
    }
    
    // notification handler
    private func openGreetingsPage(arguments: Any?) {
        let vc = BifrostFlutterViewController.init("/greetings", arguments: arguments)
        vc.hidesBottomBarWhenPushed = true
        self.navigationController?.pushViewController(vc, animated: true)
    }
    
}
