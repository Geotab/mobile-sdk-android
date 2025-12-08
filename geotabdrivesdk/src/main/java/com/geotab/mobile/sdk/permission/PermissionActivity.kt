package com.geotab.mobile.sdk.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.geotab.mobile.sdk.permission.PermissionHelper.Companion.PERMISSIONS_ASKED
import com.geotab.mobile.sdk.permission.PermissionHelper.Companion.PERMISSION_DENIED
import com.geotab.mobile.sdk.permission.PermissionHelper.Companion.PERMISSION_GRANTED
import com.geotab.mobile.sdk.permission.PermissionHelper.Companion.PERMISSION_RESPONSE
import com.geotab.mobile.sdk.util.parcelableArrayListExtra

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
        } else {
            resIntent.putExtra(PERMISSION_RESPONSE, PERMISSION_DENIED)
        }

        resIntent.putParcelableArrayListExtra(PERMISSIONS_ASKED, permissions)

        setResult(Activity.RESULT_OK, resIntent)
        this.finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val permissionList = intent.parcelableArrayListExtra<Permission>(PERMISSION_EXTRA) ?: emptyList()

        if (permissionList.isEmpty()) {
            finish()
            return
        }

        this.permissions = ArrayList(permissionList)
        askPermissions.launch(permissions.map { it.request }.toTypedArray())
    }
}
