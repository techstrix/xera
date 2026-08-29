package com.phlox.tvwebbrowser.activity.main.dialogs

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.singleton.shortcuts.Shortcut
import com.phlox.tvwebbrowser.singleton.shortcuts.ShortcutMgr
import com.phlox.tvwebbrowser.ui.dialogs.ShortcutDialogCompose
import com.phlox.tvwebbrowser.ui.theme.XeraTheme
import com.phlox.tvwebbrowser.utils.NavigationReservedShortcutKeyCodes

class ShortcutDialog(context: Context, private val shortcut: Shortcut) : Dialog(context) {
    private var keyListenMode by mutableStateOf(false)
    private var currentKeyText by mutableStateOf(
        if (shortcut.keyCode == 0) context.getString(R.string.not_set) else Shortcut.shortcutKeysToString(shortcut, context)
    )

    init {
        setCancelable(true)
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(context as? LifecycleOwner)
            setViewTreeViewModelStoreOwner(context as? ViewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(context as? SavedStateRegistryOwner)
            setContent {
                XeraTheme {
                    ShortcutDialogCompose(
                        actionTitle = context.getString(shortcut.titleResId),
                        currentKey = currentKeyText,
                        onSetKey = { toggleKeyListenState() },
                        onClearKey = { clearKey() },
                        onDismiss = { dismiss() }
                    )
                }
            }
        }
        setContentView(composeView)
    }

    private fun clearKey() {
        if (keyListenMode) toggleKeyListenState()
        shortcut.keyCode = 0
        shortcut.modifiers = 0
        shortcut.longPressFlag = false
        ShortcutMgr.getInstance().save(shortcut)
        updateShortcutNameDisplay()
    }

    private fun updateShortcutNameDisplay() {
        currentKeyText = if (shortcut.keyCode == 0) context.getString(R.string.not_set) else Shortcut.shortcutKeysToString(shortcut, context)
    }

    private fun toggleKeyListenState() {
        keyListenMode = !keyListenMode
    }

    private fun resolveKeyCode(keyCode: Int, event: KeyEvent): Int =
        if (keyCode != 0) keyCode else event.scanCode

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!keyListenMode) return super.onKeyDown(keyCode, event)
        Log.d(TAG, "onKeyDown: keyCode = $keyCode, event = $event")
        event.startTracking()
        shortcut.longPressFlag = false
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (!keyListenMode) return super.onKeyUp(keyCode, event)
        Log.d(TAG, "onKeyUp: keyCode = $keyCode, event = $event")
        val resolved = resolveKeyCode(keyCode, event)
        if (resolved in NavigationReservedShortcutKeyCodes.reservedForUserShortcuts) {
            Toast.makeText(context, R.string.shortcut_key_reserved_for_navigation, Toast.LENGTH_SHORT).show()
            toggleKeyListenState()
            return true
        }
        shortcut.keyCode = resolved
        shortcut.modifiers = event.modifiers
        ShortcutMgr.getInstance().save(shortcut)
        toggleKeyListenState()
        updateShortcutNameDisplay()
        return true
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        if (!keyListenMode) return super.onKeyLongPress(keyCode, event)
        Log.d(TAG, "onKeyLongPress: keyCode = $keyCode, event = $event")
        val resolved = resolveKeyCode(keyCode, event)
        if (resolved in NavigationReservedShortcutKeyCodes.reservedForUserShortcuts) {
            Toast.makeText(context, R.string.shortcut_key_reserved_for_navigation, Toast.LENGTH_SHORT).show()
            toggleKeyListenState()
            return true
        }
        shortcut.keyCode = resolved
        shortcut.modifiers = event.modifiers
        shortcut.longPressFlag = true
        ShortcutMgr.getInstance().save(shortcut)
        toggleKeyListenState()
        updateShortcutNameDisplay()
        return true
    }

    companion object {
        val TAG: String = ShortcutDialog::class.java.simpleName
    }
}
