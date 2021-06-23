package eu.kanade.tachiyomi.ui.extension

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import coil.clear
import coil.load
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.data.image.coil.CoverViewTarget
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.ExtensionCardItemBinding
import eu.kanade.tachiyomi.util.view.resetStrokeColor
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

class ExtensionHolder(view: View, val adapter: ExtensionAdapter) :
    BaseFlexibleViewHolder(view, adapter) {

    private val binding = ExtensionCardItemBinding.bind(view)
    init {
        binding.extButton.setOnClickListener {
            adapter.buttonClickListener.onButtonClick(flexibleAdapterPosition)
        }
        binding.cancelButton.setOnClickListener {
            adapter.buttonClickListener.onCancelClick(flexibleAdapterPosition)
        }
    }

    private val shouldLabelNsfw by lazy {
        Injekt.get<PreferencesHelper>().labelNsfwExtension()
    }

    fun bind(item: ExtensionItem) {
        val extension = item.extension

        // Set source name
        binding.extTitle.text = extension.name
        binding.version.text = extension.versionName
        binding.lang.text = LocaleHelper.getDisplayName(extension.lang)
        binding.warning.text = when {
            extension is Extension.Untrusted -> itemView.context.getString(R.string.untrusted)
            extension is Extension.Installed && extension.isObsolete -> itemView.context.getString(R.string.obsolete)
            extension is Extension.Installed && extension.isUnofficial -> itemView.context.getString(R.string.unofficial)
            extension.isNsfw && shouldLabelNsfw -> itemView.context.getString(R.string.nsfw_short)
            else -> ""
        }.uppercase(Locale.ROOT)
        binding.installProgress.progress = item.sessionProgress ?: 0
        binding.installProgress.isVisible = item.sessionProgress != null
        binding.cancelButton.isVisible = item.sessionProgress != null

        binding.sourceImage.clear()

        if (extension is Extension.Available) {
            binding.sourceImage.load(extension.iconUrl) {
                target(CoverViewTarget(binding.sourceImage))
            }
        } else {
            extension.getApplicationIcon(itemView.context)?.let { binding.sourceImage.setImageDrawable(it) }
        }
        bindButton(item)
    }

    @Suppress("ResourceType")
    fun bindButton(item: ExtensionItem) = with(binding.extButton) {
        isEnabled = true
        isClickable = true
        isActivated = false

        binding.installProgress.progress = item.sessionProgress ?: 0
        binding.cancelButton.isVisible = item.sessionProgress != null
        binding.installProgress.isVisible = item.sessionProgress != null
        val extension = item.extension
        val installStep = item.installStep
        strokeColor = ColorStateList.valueOf(Color.TRANSPARENT)
        if (installStep != null) {
            setText(
                when (installStep) {
                    InstallStep.Pending -> R.string.pending
                    InstallStep.Downloading -> R.string.downloading
                    InstallStep.Loading -> R.string.loading
                    InstallStep.Installing -> R.string.installing
                    InstallStep.Installed -> R.string.installed
                    InstallStep.Error -> R.string.retry
                }
            )
            if (installStep != InstallStep.Error) {
                isEnabled = false
                isClickable = false
            }
        } else if (extension is Extension.Installed) {
            when {
                extension.hasUpdate -> {
                    isActivated = true
                    setText(R.string.update)
                }
                else -> {
                    setText(R.string.settings)
                }
            }
        } else if (extension is Extension.Untrusted) {
            resetStrokeColor()
            setText(R.string.trust)
        } else {
            resetStrokeColor()
            setText(R.string.install)
        }
    }
}
