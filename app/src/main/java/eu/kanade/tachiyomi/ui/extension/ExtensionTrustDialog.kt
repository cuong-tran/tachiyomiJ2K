package eu.kanade.tachiyomi.ui.extension

import android.app.Dialog
import android.os.Bundle
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.materialAlertDialog

class ExtensionTrustDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : ExtensionTrustDialog.Listener {

    lateinit var listener: Listener
    constructor(target: T, signatureHash: String, pkgName: String) : this(
        Bundle().apply {
            putString(SIGNATURE_KEY, signatureHash)
            putString(PKGNAME_KEY, pkgName)
        },
    ) {
        listener = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return activity!!.materialAlertDialog()
            .setTitle(R.string.untrusted_extension)
            .setMessage(R.string.untrusted_extension_message)
            .setPositiveButton(R.string.trust) { _, _ ->
                listener.trustSignature(args.getString(SIGNATURE_KEY)!!)
            }
            .setNegativeButton(R.string.uninstall) { _, _ ->
                listener.uninstallExtension(args.getString(PKGNAME_KEY)!!)
            }.create()
    }

    private companion object {
        const val SIGNATURE_KEY = "signature_key"
        const val PKGNAME_KEY = "pkgname_key"
    }

    interface Listener {
        fun trustSignature(signatureHash: String)
        fun uninstallExtension(pkgName: String)
    }
}
