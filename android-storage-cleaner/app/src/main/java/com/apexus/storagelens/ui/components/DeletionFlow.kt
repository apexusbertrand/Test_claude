package com.apexus.storagelens.ui.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.apexus.storagelens.data.delete.DeletionManager
import com.apexus.storagelens.data.delete.DeletionPlan
import com.apexus.storagelens.data.delete.DeletionTarget
import com.apexus.storagelens.data.delete.MediaConfirmationRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Demande de confirmation système en attente d'affichage. */
data class PendingSystemConfirmation(val request: MediaConfirmationRequest, val shown: Boolean = false)

/**
 * Enchaîne une suppression confirmée dans l'application :
 * préparation → fenêtres de confirmation d'Android pour les photos/vidéos → exécution.
 */
class DeletionFlow(
    private val scope: CoroutineScope,
    private val manager: DeletionManager,
) {
    private val _pending = MutableStateFlow<PendingSystemConfirmation?>(null)
    val pending: StateFlow<PendingSystemConfirmation?> = _pending.asStateFlow()

    private var plan: DeletionPlan? = null
    private val approvals = ArrayList<Boolean>()
    private var onDone: () -> Unit = {}

    fun start(targets: List<DeletionTarget>, onDone: () -> Unit = {}) {
        if (plan != null) return
        this.onDone = onDone
        scope.launch {
            plan = manager.plan(targets)
            approvals.clear()
            next()
        }
    }

    /** L'interface a lancé la fenêtre système : on ne la relance pas (rotation, recomposition). */
    fun markShown() {
        _pending.value = _pending.value?.copy(shown = true)
    }

    fun onSystemResult(approved: Boolean) {
        if (_pending.value == null) return
        approvals += approved
        _pending.value = null
        next()
    }

    private fun next() {
        val current = plan ?: return
        val request = current.mediaRequests.getOrNull(approvals.size)
        if (request != null) {
            _pending.value = PendingSystemConfirmation(request)
            return
        }
        val decisions = approvals.toList()
        plan = null
        scope.launch {
            manager.execute(current, decisions)
            onDone()
        }
    }
}

/** Affiche la fenêtre de confirmation d'Android (« Supprimer ces photos ? ») quand elle est demandée. */
@Composable
fun SystemMediaConfirmation(
    pending: PendingSystemConfirmation?,
    onShown: () -> Unit,
    onResult: (approved: Boolean) -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        onResult(result.resultCode == Activity.RESULT_OK)
    }
    LaunchedEffect(pending) {
        if (pending != null && !pending.shown) {
            onShown()
            launcher.launch(IntentSenderRequest.Builder(pending.request.intentSender).build())
        }
    }
}
