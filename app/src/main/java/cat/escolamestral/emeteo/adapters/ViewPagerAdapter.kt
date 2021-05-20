package cat.escolamestral.emeteo.adapters

import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter

class ViewPagerAdapter(private val rootView: View, private val views: Array<Int>) : PagerAdapter() {
    override fun getCount(): Int = views.size
    override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`
    override fun instantiateItem(collection: ViewGroup, position: Int): Any =
        rootView.findViewById(views[position])
}