package br.com.dextra.bifrost_example

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class NativeFragment : Fragment() {

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_native, container, false)
  }

  companion object {
    @JvmStatic
    fun newInstance() = NativeFragment()
  }
}
