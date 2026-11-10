package app.levitx.sync

import android.content.Context
import android.net.Uri
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.AvailabilityException
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * [DataLayer] on Google's Wearable Data Layer API.
 *
 * Follows the Wear OS guides for data items:
 * - Clients are cheap, so one is made per call instead of being kept.
 * - The API is checked for availability first, because calling it on a device
 *   without it throws.
 * - Items are marked urgent. They are small and the person is waiting for
 *   them; without it delivery can be delayed by up to 30 minutes.
 *
 * Play services only delivers items between copies of this app that share a
 * package name and signing key, so no other app can read or forge them.
 */
@Singleton
internal class PlayServicesDataLayer @Inject constructor(@param:ApplicationContext private val context: Context) :
    DataLayer {
    override suspend fun put(path: String, payload: ByteArray) {
        withClient { client ->
            client.putDataItem(PutDataRequest.create(path).setData(payload).setUrgent()).await()
        }
    }

    override suspend fun delete(path: String) {
        withClient { client ->
            // Naming this device as the host limits the delete to its own item.
            client.deleteDataItems(wearUri(path, host = localNodeId())).await()
        }
    }

    override suspend fun list(pathPrefix: String): List<DataLayerItem> = withClient { client ->
        val ownNodeId = localNodeId()
        // No host means "from any device".
        val buffer = client.getDataItems(wearUri(pathPrefix, host = null), DataClient.FILTER_PREFIX).await()
        try {
            // Copy what is needed out of the buffer: its items are invalid once it is released.
            buffer.mapNotNull { item ->
                item.uri.path?.let { path ->
                    DataLayerItem(path, item.data?.copyOf() ?: ByteArray(0), isOwn = item.uri.host == ownNodeId)
                }
            }
        } finally {
            buffer.release()
        }
    }

    private suspend fun localNodeId(): String = Wearable.getNodeClient(context).localNode.await().id

    private fun wearUri(path: String, host: String?): Uri = Uri.Builder()
        .scheme(PutDataRequest.WEAR_URI_SCHEME)
        .apply { if (host != null) authority(host) }
        .path(path)
        .build()

    /** Runs [block] with a fresh client, turning Play services failures into [DataLayerUnavailableException]. */
    private suspend fun <T> withClient(block: suspend (DataClient) -> T): T {
        val client = Wearable.getDataClient(context)
        try {
            GoogleApiAvailability.getInstance().checkApiAvailability(client).await()
            return block(client)
        } catch (error: AvailabilityException) {
            throw DataLayerUnavailableException(error)
        } catch (error: ApiException) {
            throw DataLayerUnavailableException(error)
        }
    }
}
