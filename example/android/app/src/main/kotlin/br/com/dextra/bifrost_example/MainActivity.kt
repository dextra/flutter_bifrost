package br.com.dextra.bifrost_example

import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import br.com.dextra.bifrost.Bifrost
import br.com.dextra.bifrost.BifrostFlutterActivity
import br.com.dextra.bifrost.BifrostFlutterFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : FragmentActivity() {

  private lateinit var firstFragment: BifrostFlutterFragment
  private lateinit var secondFragment: BifrostFlutterFragment
  private lateinit var nativeFragment: NativeFragment
  private lateinit var selectedFragment: Fragment

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setupBottomNavigation()
    setupNotifications()
    createFragments()
  }

  private fun setupBottomNavigation() {
    bottomNavigationView.setOnNavigationItemSelectedListener {
      when (it.itemId) {
        R.id.firstFragment -> selectFragment(firstFragment)
        R.id.secondFragment -> selectFragment(secondFragment)
        R.id.nativeFragment -> selectFragment(nativeFragment)
        else -> false
      }
    }
  }

  private fun setupNotifications() {
    Bifrost.registerNotification("openGreetingsPage") { openGreetingsPage(it) }
  }

  private fun openGreetingsPage(arguments: Any?) {
    val intent = BifrostFlutterActivity.createIntent(this, "/greetings", arguments)
    startActivity(intent.setFlags(FLAG_ACTIVITY_NEW_TASK))
  }

  private fun createFragments() {
    firstFragment = BifrostFlutterFragment.newInstance("/first")
    secondFragment = BifrostFlutterFragment.newInstance("/second")
    nativeFragment = NativeFragment.newInstance()
    selectedFragment = firstFragment

    supportFragmentManager.beginTransaction()
        .add(R.id.container, firstFragment, "first")
        .commit()

    supportFragmentManager.beginTransaction()
        .add(R.id.container, secondFragment, "second")
        .hide(secondFragment)
        .commit()

    supportFragmentManager.beginTransaction()
        .add(R.id.container, nativeFragment, "native")
        .hide(nativeFragment)
        .commit()
  }

  private fun selectFragment(fragment: Fragment): Boolean {
    if (selectedFragment == fragment)
      return false

    supportFragmentManager.beginTransaction()
        .show(fragment)
        .hide(selectedFragment)
        .commitAllowingStateLoss()

    selectedFragment = fragment

    return true
  }
}
