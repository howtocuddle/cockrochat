# bileichat-laptop

BLE mesh chat node for Linux laptops — interoperates with the Android bileichat app.

**Build:** `cd laptop && cargo build --release`

**Run:** `sudo ./target/release/bileichat-laptop [--epoch-ms 10000] [--rssi-floor -80] [--text "hello"]`

**Output lines:**
- `[HH:MM:SS.mmm] rssi=X dBm mark=AABBCCDD epoch=N text="..."` — received peer frame (deduplicated)
- `[epoch N] ended — K distinct neighbours | KMV: v0 v1 … v15` — paste the 16 space-separated u64 values into the Android app's Compare box to compute Jaccard co-presence similarity
- `[adv] epoch=N text="..." registered OK` — own advertisement live; type a new line on stdin to change the outgoing message (max 63 bytes)

**Requirements:** BlueZ ≥ 5.65, `systemctl start bluetooth`, adapter supporting BT 5 extended advertising (AUX_ADV_IND), root or `CAP_NET_ADMIN`+`CAP_NET_RAW`.
