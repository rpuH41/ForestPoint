package com.liulkovich.florapoint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.liulkovich.florapoint.shared.ui.ForestPointSharedScreen

class SharedPreviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ForestPointSharedScreen()
        }
    }
}
