package com.lagradost.cloudstream3.ui.settings.extensions

import android.content.res.Resources
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.lagradost.cloudstream3.PROVIDER_STATUS_DOWN
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.utils.AppUtils.html
import com.lagradost.cloudstream3.utils.GlideApp
import com.lagradost.cloudstream3.utils.SubtitleHelper.getFlagFromIso
import com.lagradost.cloudstream3.utils.UIHelper.setImage
import com.lagradost.cloudstream3.utils.UIHelper.toPx
import kotlinx.android.synthetic.main.repository_item.view.*
import org.junit.Assert
import org.junit.Test


data class PluginViewData(
    val plugin: Plugin,
    val isDownloaded: Boolean,
)

class PluginAdapter(
    val iconClickCallback: (Plugin) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val plugins: MutableList<PluginViewData> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return PluginViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.repository_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PluginViewHolder -> {
                holder.bind(plugins[position])
            }
        }
    }

    override fun getItemCount(): Int {
        return plugins.size
    }

    fun updateList(newList: List<PluginViewData>) {
        val diffResult = DiffUtil.calculateDiff(
            PluginDiffCallback(this.plugins, newList)
        )

        plugins.clear()
        plugins.addAll(newList)

        diffResult.dispatchUpdatesTo(this)
    }

    /*
    private var storedPlugins: Array<PluginData> = reloadStoredPlugins()

    private fun reloadStoredPlugins(): Array<PluginData> {
        return PluginManager.getPluginsOnline().also { storedPlugins = it }
    }*/

    // Clear glide image because setImageResource doesn't override
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        holder.itemView.entry_icon?.let { pluginIcon ->
            GlideApp.with(pluginIcon).clear(pluginIcon)
        }
        super.onViewRecycled(holder)
    }

    companion object {
        private tailrec fun findClosestBase2(target: Int, current: Int = 16, max: Int = 512): Int {
            if (current >= max) return max
            if (current >= target) return current
            return findClosestBase2(target, current * 2, max)
        }

        @Test
        fun testFindClosestBase2() {
            Assert.assertEquals(16, findClosestBase2(0))
            Assert.assertEquals(256, findClosestBase2(170))
            Assert.assertEquals(256, findClosestBase2(256))
            Assert.assertEquals(512, findClosestBase2(257))
            Assert.assertEquals(512, findClosestBase2(700))
        }

        val iconSize by lazy {
            findClosestBase2(24.toPx, 16, 512)
        }
    }

    inner class PluginViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        fun bind(
            data: PluginViewData,
        ) {
            val metadata = data.plugin.second
            val alpha = if (metadata.status == PROVIDER_STATUS_DOWN) 0.6f else 1f
            itemView.main_text?.alpha = alpha
            itemView.sub_text?.alpha = alpha

            val drawableInt = if (data.isDownloaded)
                R.drawable.ic_baseline_delete_outline_24
            else R.drawable.netflix_download

            itemView.nsfw_marker?.isVisible = metadata.tvTypes?.contains("NSFW") ?: false
            itemView.action_button?.setImageResource(drawableInt)

            itemView.action_button?.setOnClickListener {
                iconClickCallback.invoke(data.plugin)
            }
            testFindClosestBase2()
            //if (itemView.context?.isTrueTvSettings() == false) {
            //    val siteUrl = metadata.repositoryUrl
            //    if (siteUrl != null && siteUrl.isNotBlank() && siteUrl != "NONE") {
            //        itemView.setOnClickListener {
            //            openBrowser(siteUrl)
            //        }
            //    }
            //}

            if (data.isDownloaded) {
                val plugin = PluginManager.urlPlugins[metadata.url]
                if (plugin?.openSettings != null) {
                    itemView.action_settings?.isVisible = true
                    itemView.action_settings.setOnClickListener {
                        try {
                            plugin.openSettings!!.invoke(itemView.context)
                        } catch (e: Throwable) {
                            Log.e(
                                "PluginAdapter",
                                "Failed to open ${metadata.name} settings: ${
                                    Log.getStackTraceString(e)
                                }"
                            )
                        }

                    }
                } else {
                    itemView.action_settings?.isVisible = false
                }
            } else {
                itemView.action_settings?.isVisible = false
            }

            if (itemView.entry_icon?.setImage(//itemView.entry_icon?.height ?:
                    metadata.iconUrl?.replace(
                        "&sz=24",
                        "&sz=$iconSize"
                    ), // lazy fix for better resolution
                    null,
                    errorImageDrawable = R.drawable.ic_baseline_extension_24
                ) != true
            ) {
                itemView.entry_icon?.setImageResource(R.drawable.ic_baseline_extension_24)
            }

            itemView.ext_version?.isVisible = true
            itemView.ext_version?.text = "v${metadata.version}"

            if (metadata.language != null) {
                itemView.lang_icon?.isVisible = true
                itemView.lang_icon.text = getFlagFromIso(metadata.language)
            } else {
                itemView.lang_icon?.isVisible = false
            }

            itemView.main_text?.text = metadata.name
            itemView.sub_text?.isGone = metadata.description.isNullOrBlank()
            itemView.sub_text?.text = metadata.description.html()
        }
    }
}

class PluginDiffCallback(
    private val oldList: List<PluginViewData>,
    private val newList: List<PluginViewData>
) :
    DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition].plugin.second.internalName == newList[newItemPosition].plugin.second.internalName && oldList[oldItemPosition].plugin.first == newList[newItemPosition].plugin.first

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition] == newList[newItemPosition]
}