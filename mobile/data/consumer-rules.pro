# R8 rules for apps that use :data.

# SQLCipher's native code finds its Java classes by name over JNI, so R8 must
# not rename or remove them. sqlcipher-android doesn't ship consumer rules
# (sqlcipher/sqlcipher-android issue #72), so we add them here. These are the
# rules Zetetic gives on its forum for the net.zetetic package.
-keep class net.zetetic.database.** { *; }
