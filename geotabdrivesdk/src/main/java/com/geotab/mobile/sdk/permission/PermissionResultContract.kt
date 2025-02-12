package com.geotab.mobile.sdk.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.Keep
import com.geotab.mobile.sdk.permission.PermissionHelper.Companion.PERMISSIONS_ASKED
import com.geotab.mobile.sdk.permission.PermissionHelper.Companion.PERMISSION_GRANTED
import com.geotab.mobile.sdk.permission.PermissionHelper.Companion.PERMISSION_RESPONSE
import com.geotab.mobile.sdk.util.parcelableArrayListExtra

@Keep
data class PermissionAttribute(
    val permissions: ArrayList<Permission>,
    var result: Boolean = false,
    var wasPermissionAttributeNull: Boolean = false,
    val callback: (Boolean) -> Unit
)
class PermissionResultContract : ActivityResultContract<PermissionAttribute, PermissionAttribute>() {
    private lateinit var permissionAttribute: PermissionAttribute

    override fun createIntent(context: Context, input: PermissionAttribute): Intent {
        permissionAttribute = input
        return PermissionActivity.getIntent(context, input.permissions)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PermissionAttribute {
        if (::permissionAttribute.isInitialized) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                if (intent.getStringExtra(PERMISSION_RESPONSE) == PERMISSION_GRANTED) {
                    permissionAttribute.result = true
                }
            }
        } else {
            val permissionsBeingAsked = intent?.parcelableArrayListExtra<Permission>(PERMISSIONS_ASKED) ?: arrayListOf()
            permissionAttribute = PermissionAttribute(
                permissions = permissionsBeingAsked,
                result = false,
                wasPermissionAttributeNull = true
            ) {}
        }

        return permissionAttribute
    }
}
