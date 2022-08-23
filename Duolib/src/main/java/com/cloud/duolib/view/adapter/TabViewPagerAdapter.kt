package com.cloud.duolib.view.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class TabViewPagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val fmList = ArrayList<Fragment>()
    private val ftList = ArrayList<String>()

    override fun getItem(position: Int): Fragment {
        return fmList[position]
    }

    override fun getCount(): Int {
        return fmList.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return ftList[position]
    }

    fun addFragment(fragment: Fragment, title: String) {
        fmList.add(fragment)
        ftList.add(title)
    }
}