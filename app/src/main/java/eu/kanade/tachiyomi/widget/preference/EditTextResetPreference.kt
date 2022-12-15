package eu.kanade.tachiyomi.widget.preference

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R

class EditTextResetPreference @JvmOverloads constructor(
    activity: Activity?,
    context: Context,
    attrs: AttributeSet? = null,
) :
    MatPreference(activity, context, attrs) {
    private var defValue: String = ""

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
        defValue = defaultValue as? String ?: defValue
    }

    override var customSummaryProvider: SummaryProvider<MatPreference>? = SummaryProvider<MatPreference> {
        sharedPreferences?.getString(key, defValue)
    }

    override fun dialog(): MaterialAlertDialogBuilder {
        return super.dialog().apply {
            val attrs = intArrayOf(android.R.attr.dialogLayout)
            val a = context.obtainStyledAttributes(R.style.Preference_DialogPreference_EditTextPreference_Material, attrs)
            val resourceId = a.getResourceId(0, 0)
            val view = LayoutInflater.from(context).inflate(resourceId, null)
            val textView = view.findViewById<EditText>(android.R.id.edit)
            val message = view.findViewById<TextView>(android.R.id.message)
            message?.isVisible = false
            textView?.append(sharedPreferences?.getString(key, defValue))
            this.setView(view)
            this.setNeutralButton(R.string.reset) { _, _ ->
                if (callChangeListener(defValue)) {
                    sharedPreferences?.edit { remove(key) }
                    notifyChanged()
                }
            }
            this.setPositiveButton(android.R.string.ok) { _, _ ->
                if (callChangeListener(textView.text.toString())) {
                    sharedPreferences?.edit {
                        if (textView.text.isNullOrBlank()) {
                            remove(key)
                        } else {
                            putString(key, textView.text.toString())
                        }
                    }
                    notifyChanged()
                }
            }
            a.recycle()
        }
    }
}
