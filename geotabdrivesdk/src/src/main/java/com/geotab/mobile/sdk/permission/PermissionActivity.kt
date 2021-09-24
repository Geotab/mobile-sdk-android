package com.geotab.mobile.sdk.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.geotab.mobile.sdk.permission.PermissionHelper.Companion.PERMISSION_DENIED
import com.geotab.mobile.sdk.permission.PermissionHelper.Companion.PERMISSION_GRANTED
import com.geotab.mobile.sdk.permission.PermissionHelper.Companion.PERMISSION_RESPONSE

class PermissionActivity : AppCompatActivity() {
    private var permissions: ArrayList<Permission> = arrayListOf()
    companion object {
        const val PERMISSION_EXTRA = "PERMISSION"
        fun getIntent(context: Context, permissions: ArrayList<Permission>): Intent =
            Intent(context, PermissionActivity::class.java).apply {
                putExtra(PERMISSION_EXTRA, permissions)
            }
    }

    private val askPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
        val resIntent = Intent()
        if (!map.containsValue(false)) {
            resIntent.putExtra(PERMISSION_RESPONSE, PERMISSION_GRANTED)
            setResult(Activity.RESULT_OK, resIntent)
        } else {
            resIntent.putExtra(PERMISSION_RESPONSE, PERMISSION_DENIED)
            setResult(Activity.RESULT_OK, resIntent)
        }
        this.finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionList = (intent.getSerializableExtra(PERMISSION_EXTRA) as? ArrayList<*>)?.filterIsInstance<Permission>()
        permissionList?.let {
            this.permissions = ArrayList(permissionList)
        }
        askPermissions.launch(permissions.map { it.request }.toTypedArray())
    }
}
