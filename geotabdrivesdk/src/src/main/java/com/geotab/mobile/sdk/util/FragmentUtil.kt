package com.geotab.mobile.sdk.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.geotab.mobile.sdk.R

/**
 * If a fragment with the given tag already exists, will replace with the given fragment.
 * Otherwise add the given fragment.
 * @param fragment: Fragment to be replaced
 * @param tag: Use this tag to search for a fragment
 * @param fragmentManager:  Used for the fragment transactions
 */

internal fun replaceFragment(fragment: Fragment, tag: String, fragmentManager: FragmentManager) {
    val browserFragment = fragmentManager.findFragmentByTag(tag)
    if (browserFragment != null) {
        fragmentManager.commit {
            remove(browserFragment)
        }
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
    fragmentManager.executePendingTransactions()
    fragmentManager.commit {
        replace(R.id.content_frame, fragment, tag)
        setReorderingAllowed(true)
        addToBackStack(null)
    }
    fragmentManager.executePendingTransactions()
}
