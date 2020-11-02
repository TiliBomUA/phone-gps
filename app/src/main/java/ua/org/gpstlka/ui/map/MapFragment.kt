package ua.org.gpstlka.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_map.view.*
import ua.org.gpstlka.R

class MapFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_map, container, false)
        val adapter = ViewPagerAdapter(childFragmentManager)
        adapter.addFragment(MapLocalDataFragment(), getString(R.string.text_local))
        adapter.addFragment(MapServerDataFragment(), getString(R.string.text_server))

        val viewPager = root.viewpager
        viewPager.adapter = adapter
        val tabs = root.tabLayout2
        tabs.setupWithViewPager(viewPager)

        return root
    }
}