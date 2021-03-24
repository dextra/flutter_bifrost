import UIKit
import bifrost

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {
    
    var window: UIWindow?
    
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
 
        // start bifrost flutter engine
        // you can also call Bifrost.startFlutterEngine() without common handler
        Bifrost.startFlutterEngine(commonHandler: handle)
        
        return true
    }

    // common channel handle
    private func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if (call.method == "getAppVersion") {
            let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"]
            result(version)
        }
    }
}
