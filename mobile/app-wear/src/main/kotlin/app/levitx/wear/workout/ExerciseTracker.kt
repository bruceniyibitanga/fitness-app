package app.levitx.wear.workout

import android.content.Context
import android.util.Log
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.clearUpdateCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseTrackedStatus
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.endExercise
import androidx.health.services.client.getCapabilities
import androidx.health.services.client.getCurrentExerciseInfo
import androidx.health.services.client.startExercise
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "LevitxWorkout"

/**
 * The exercise types to record a session as, best match first. Not every
 * watch supports every type, so the first one this watch offers is used.
 */
private val PREFERRED_EXERCISE_TYPES =
    listOf(ExerciseType.CALISTHENICS, ExerciseType.STRENGTH_TRAINING, ExerciseType.WORKOUT)

/**
 * Runs a Health Services exercise for the length of a session and reports
 * heart rate from it.
 *
 * Health Services is the Wear OS way to read sensors during a workout: it
 * batches readings to save battery and lets the processor sleep between them.
 * This is the only class that talks to it.
 *
 * Heart rate is held in memory for the screen and nothing else. It is not
 * written to the database and never leaves the watch.
 *
 * Tracking is a nice-to-have. If anything here fails, the session carries on
 * without a heart rate, so failures are logged (by type, never with data) and
 * reported as "not tracking" instead of thrown.
 */
@Singleton
class ExerciseTracker @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val client: ExerciseClient by lazy { HealthServices.getClient(context).exerciseClient }
    private val mutableHeartRate = MutableStateFlow<Int?>(null)

    /** Latest heart rate in beats per minute, or null when there is no reading. */
    val heartRate: StateFlow<Int?> = mutableHeartRate.asStateFlow()

    private val callback = object : ExerciseUpdateCallback {
        override fun onRegistered() = Unit

        override fun onRegistrationFailed(throwable: Throwable) {
            Log.w(TAG, "Health Services callback failed: ${throwable.javaClass.simpleName}")
            mutableHeartRate.value = null
        }

        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            if (update.exerciseStateInfo.state.isEnded) {
                // Ended by us, by another workout app taking over, or by a permission being taken away.
                mutableHeartRate.value = null
                return
            }
            val latest = update.latestMetrics.getData(DataType.HEART_RATE_BPM).lastOrNull()
            if (latest != null) mutableHeartRate.value = latest.value.roundToInt()
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit

        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {
            // A reading taken off-wrist or mid-acquisition isn't worth showing.
            if (dataType == DataType.HEART_RATE_BPM && availability != DataTypeAvailability.AVAILABLE) {
                mutableHeartRate.value = null
            }
        }
    }

    /**
     * Starts recording, or picks up this app's exercise if one is already
     * running (after the app was restarted mid-session).
     *
     * Health Services runs one exercise at a time across every app. If
     * another app is recording a workout, it is left alone: taking it over
     * would end that workout without the person asking.
     *
     * @return True when heart rate is now being tracked.
     */
    suspend fun start(): Boolean = guarded("start") {
        val capabilities = client.getCapabilities()
        val type = PREFERRED_EXERCISE_TYPES.firstOrNull { it in capabilities.supportedExerciseTypes }
        val status = client.getCurrentExerciseInfo().exerciseTrackedStatus
        if (type == null || status == ExerciseTrackedStatus.OTHER_APP_IN_PROGRESS) {
            false
        } else {
            // The callback is set before the exercise starts, as the guide says, so no update is missed.
            client.setUpdateCallback(callback)
            if (status == ExerciseTrackedStatus.NO_EXERCISE_IN_PROGRESS) {
                // Only ask for what this watch can give: an unsupported data type fails the whole request.
                val supported = capabilities.getExerciseTypeCapabilities(type).supportedDataTypes
                client.startExercise(
                    ExerciseConfig(
                        exerciseType = type,
                        dataTypes = setOf<DataType<*, *>>(DataType.HEART_RATE_BPM).intersect(supported),
                        isAutoPauseAndResumeEnabled = false,
                        isGpsEnabled = false,
                    ),
                )
            }
            true
        }
    }

    /** Stops recording and turns the sensor off. Only ever ends this app's own exercise. */
    suspend fun stop() {
        guarded("stop") {
            if (client.getCurrentExerciseInfo().exerciseTrackedStatus ==
                ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS
            ) {
                client.endExercise()
            }
            client.clearUpdateCallback(callback)
            true
        }
        mutableHeartRate.value = null
    }

    /**
     * Runs [call] and returns false if Health Services fails.
     *
     * Health Services reports failures with several unrelated exception
     * types (remote, security, illegal state), and none of them should take
     * the session down, so everything but cancellation is caught.
     */
    private suspend fun guarded(what: String, call: suspend () -> Boolean): Boolean = try {
        call()
    } catch (error: CancellationException) {
        throw error
    } catch (
        @Suppress("TooGenericExceptionCaught") error: Exception,
    ) {
        Log.w(TAG, "Health Services $what failed: ${error.javaClass.simpleName}")
        false
    }
}
