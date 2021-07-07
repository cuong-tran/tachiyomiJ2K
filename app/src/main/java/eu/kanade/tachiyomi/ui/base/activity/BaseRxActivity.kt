package eu.kanade.tachiyomi.ui.base.activity

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.LocaleHelper
import nucleus.view.NucleusAppCompatActivity

abstract class BaseRxActivity<P : BasePresenter<*>> : NucleusAppCompatActivity<P>() {

    val scope = lifecycleScope

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.createLocaleWrapper(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecureActivityDelegate.setSecure(this)
    }

    override fun onResume() {
        super.onResume()
        SecureActivityDelegate.promptLockIfNeeded(this)
    }
}
