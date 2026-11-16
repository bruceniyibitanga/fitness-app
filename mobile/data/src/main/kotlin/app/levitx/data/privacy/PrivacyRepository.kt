package app.levitx.data.privacy

import java.io.OutputStream

/** The person's own copy of their data, and deleting it all. */
interface PrivacyRepository {
    /**
     * Writes everything Levitx has stored about the person to [out] as a zip:
     * `levitx.json` with all of it, and `workouts.csv` and `weigh-ins.csv` for
     * a spreadsheet. The file is not encrypted; the person chose where it goes.
     * Doesn't close [out].
     */
    suspend fun export(out: OutputStream)

    /**
     * Deletes every row in the encrypted database: the plan, workouts,
     * weigh-ins, kit and the workout in progress. It can't be undone.
     */
    suspend fun deleteEverything()
}
