package app.levitx.data

/**
 * The person's data can't be read on this device right now, for example
 * because the key that unlocks the database is missing or doesn't match.
 *
 * The app fails closed: it shows an error and never falls back to an
 * unencrypted or empty database, which could hide or overwrite real data.
 * The message never contains data or key material.
 */
class DataUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)
