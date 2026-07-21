#![no_main]
//! Fuzz the anti-zip-bomb boundary (mesh-build-plan.md §2.1 / M1 gate).
//!
//! Guarantees checked on EVERY input:
//!   * `decode` never panics / hangs / OOMs (libfuzzer catches these).
//!   * `decode` performs ZERO heap allocations — enforced by a counting global allocator,
//!     so a malformed frame can never trigger an unbounded alloc (the zip-bomb class).
//!   * Any frame that decodes `Ok` round-trips: `decode(encode(f)) == f`, and encodes to 194 B.

use libfuzzer_sys::fuzz_target;
use mesh_core::codec::{FRAME_LEN, decode, encode};
use std::alloc::{GlobalAlloc, Layout, System};
use std::sync::atomic::{AtomicUsize, Ordering};

static ALLOCS: AtomicUsize = AtomicUsize::new(0);

/// System allocator that counts alloc/realloc calls so the target can assert alloc-free decode.
struct Counting;

unsafe impl GlobalAlloc for Counting {
    unsafe fn alloc(&self, l: Layout) -> *mut u8 {
        ALLOCS.fetch_add(1, Ordering::Relaxed);
        unsafe { System.alloc(l) }
    }
    unsafe fn dealloc(&self, p: *mut u8, l: Layout) {
        unsafe { System.dealloc(p, l) }
    }
    unsafe fn realloc(&self, p: *mut u8, l: Layout, n: usize) -> *mut u8 {
        ALLOCS.fetch_add(1, Ordering::Relaxed);
        unsafe { System.realloc(p, l, n) }
    }
}

#[global_allocator]
static ALLOC: Counting = Counting;

fuzz_target!(|data: &[u8]| {
    let before = ALLOCS.load(Ordering::Relaxed);
    let res = decode(data);
    let after = ALLOCS.load(Ordering::Relaxed);
    assert_eq!(before, after, "decode allocated on input of len {}", data.len());

    if let Ok(frame) = res {
        let buf = encode(&frame);
        assert_eq!(buf.len(), FRAME_LEN);
        let re = decode(&buf).expect("re-decode of a just-encoded valid frame must succeed");
        assert_eq!(re, frame, "encode/decode round-trip diverged");
    }
});
