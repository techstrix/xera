package com.phlox.tvwebbrowser.activity.main.dialogs.favorites

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import com.phlox.tvwebbrowser.model.FavoriteItem

/**
 * Created by PDT on 13.09.2016.
 * Migrated to RecyclerView + MaterialCardView (M3)
 */
class FavoritesListAdapter(private val favorites: List<FavoriteItem>, private val itemsListener: FavoriteItemView.Listener) : RecyclerView.Adapter<FavoritesListAdapter.ViewHolder>() {
    var isEditMode = false
        set(editMode) {
            field = editMode
            notifyDataSetChanged()
        }
    var onItemClick: ((FavoriteItem) -> Unit)? = null

    class ViewHolder(val view: FavoriteItemView) : RecyclerView.ViewHolder(view)

    override fun getItemCount(): Int = favorites.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = FavoriteItemView(parent.context)
        view.listener = itemsListener
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = favorites[position]
        holder.view.bind(item, isEditMode)
        holder.view.setOnClickListener {
            if (!isEditMode && !item.isFolder) {
                onItemClick?.invoke(item)
            }
        }
    }
}
