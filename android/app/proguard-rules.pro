# ---------------------------------------------------------------------------
# UniFFI + JNA
#
# The mesh_core bindings are JNA-backed. Two things are load-bearing and both are
# invisible to the compiler:
#
#   * com.sun.jna.Library method names map DIRECTLY to exported symbols in
#     libmesh_core.so. Renaming one produces an UnsatisfiedLinkError.
#   * Structure subclasses are marshalled by field NAME and declaration ORDER via
#     @Structure.FieldOrder (see RustBuffer / ForeignBytes / UniffiRustCallStatus in
#     uniffi/mesh_core/mesh_core.kt). Stripping, reordering or renaming a field
#     silently corrupts every FFI call.
#
# Both failures happen at RUNTIME, on first FFI use. A green assembleRelease proves
# nothing — the release APK must be installed and exercised (private send is the
# FFI-heaviest path).
# ---------------------------------------------------------------------------
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

-keep class uniffi.** { *; }
-keepclassmembers class uniffi.** { *; }

-keep class com.sun.jna.** { *; }
-keep interface com.sun.jna.** { *; }
-keepclassmembers class com.sun.jna.** { *; }

# Anything JNA marshals: the field layout IS the ABI.
-keep class * extends com.sun.jna.Structure { *; }
-keepclassmembers class * extends com.sun.jna.Structure { public *; }
-keep class * implements com.sun.jna.Library { *; }
-keep class * implements com.sun.jna.Callback { *; }

-dontwarn java.awt.**
-dontwarn com.sun.jna.**

# ---------------------------------------------------------------------------
# androidx.security-crypto -> Tink -> protobuf-lite
#
# EncryptedSharedPreferences holds the long-term X25519 secret and every pair chain
# key. Tink resolves key managers and protobuf messages reflectively; under R8 full
# mode that surfaces as a KeyStoreException / GeneralSecurityException at first
# prefs access, which PairStore.prefs() catches and degrades to memory-only storage
# (D4). The failure mode is therefore "pairings silently die on process death",
# not a crash — worth keeping loudly in mind.
# ---------------------------------------------------------------------------
-keep class com.google.crypto.tink.** { *; }
-keepclassmembers class * extends com.google.protobuf.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.api.**
-dontwarn javax.annotation.**
-dontwarn org.joda.time.**

# ---------------------------------------------------------------------------
# ZXing (on-device QR encode/decode for pairing)
# ---------------------------------------------------------------------------
-dontwarn com.google.zxing.**

# ---------------------------------------------------------------------------
# Keep our own enum names: SendTier / SendState / WitVerdict values are persisted
# and logged by name.
# ---------------------------------------------------------------------------
-keepclassmembers enum org.bileichat.mesh.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
