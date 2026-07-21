//! UniFFI binding generator (library mode). Build the crate, then:
//!   cargo run --bin uniffi-bindgen -- generate --library <libmesh_core.so> \
//!       --language kotlin --out-dir bindings/kotlin
//! (and again with --language swift). See mesh-build-plan.md M0 task 6.
fn main() {
    uniffi::uniffi_bindgen_main()
}
