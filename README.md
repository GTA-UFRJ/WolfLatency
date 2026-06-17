# WolfLatency

> Android client–server toolkit for measuring end-to-end network latency under mobility, correlated with GPS location and cellular (4G/5G) signal metadata.

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84?style=flat&logo=android&logoColor=white)
![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)

WolfLatency was built at **GTA/COPPE/UFRJ** to study how **user mobility affects latency in Multi-Access Edge Computing (MEC)** scenarios over real cellular networks. It powers the data collection behind two peer-reviewed publications (see [Publications](#publications)).

## How it works

The system is a pair of Android apps that talk to each other over the **same cellular network**:

- **WolfServer** — runs on one (static) device and answers the client's HTTP GET with a small page; it also receives a follow-up POST and stores each sample as a backup.
- **WolfClient** — runs on a second (moving) device, sends **one HTTP GET per second** (via OkHttp, over **IPv6** — IPv4 device-to-device traffic is blocked inside the carrier network) and measures the **round-trip time (RTT)** of each request. For every sample it simultaneously records:
  - **Geolocation** (GPS, via FusedLocationProvider);
  - **Cellular context** (serving cell, signal strength, RSRQ/RSSNR, MCC/MNC, NR-ARFCN) and **5G NSA/SA detection** through Android's `TelephonyManager`/`TelephonyCallback`.

Each sample is appended to a CSV (via OpenCSV), producing a time series of latency vs. position vs. signal quality that can be analyzed offline. The [Haversine formula](https://en.wikipedia.org/wiki/Haversine_formula) is then used to convert GPS coordinates into client–server distance.

## Captured metrics

Each row of `clientdata.csv` / `serverdata.csv`:

| Field | Description |
|---|---|
| `Sequence` | Incremental sample id |
| `Transport` | Mode of transport during capture (on foot / car / train) |
| `Timestamp` | Capture time (`yyyy.MM.dd_HH.mm.ss`) |
| `Latency` | Round-trip latency (ms) |
| `Latitude`, `Longitude` | GPS position |
| `Signal_dbm` | Received signal power in dBm (RSSI/RSCP/RSRP by technology) |
| `Signal_level` | Signal bars, 0–4 |
| `MCC`, `MNC` | Mobile country/network code |
| `CellId`, `Tac/Lac` | Cell and tracking/location area id |
| `Mobile_Network` | Radio technology (LTE/NR, …) |
| `RSRQ`, `RSSNR` | Reference signal quality / SNR |
| `NRARFCN` | 5G NR channel number (when on NR) |

The companion `haversine*.csv` files hold the client–server distance derived from the GPS traces.

## Datasets

Real measurement campaigns collected in the city of Rio de Janeiro, organized by the publication they support:

- **`Dataset/WGRS2024/`** — short, medium and long routes (pedestrian + vehicle), mixing static-server / moving-client conditions around the UFRJ campus and Vila Isabel.
- **`Dataset/VehiClouds2024/`** — four vehicular experiments (Sept 5/9/10/12, 2024) in the University City.

Each folder contains the raw `clientdata`/`serverdata` CSVs, the computed `haversine` distances, and a `readme.txt` describing the experiment.

## Repository structure

```
WolfLatency/
├── WolfClient/   # Android app — measures latency + logs GPS & cellular metadata
├── WolfServer/   # Android app — HTTP echo endpoint
└── Dataset/      # Real-world measurement datasets (WGRS2024, VehiClouds2024)
```

## Build & run

**Requirements:** Android Studio (recent), JDK 17, two Android devices with active SIMs on the same carrier/network, location + phone-state permissions granted.

1. Open `WolfServer/` in Android Studio, build and install it on **device A**. Launch it and note the device's IP / endpoint.
2. Open `WolfClient/` in Android Studio, build and install it on **device B**.
3. In WolfClient, enter the server URL, start a measurement run, and move along the desired route.
4. Collected CSVs are written to the device storage and can be pulled for analysis.

## Publications

This toolkit produced the data for:

- L. F. C. Cristino, P. Cruz et al. — [**Vehicle Mobility Impact on Performance of Multi-Access Edge Computing**](https://ieeexplore.ieee.org/document/10815763), *VehiClouds 2024 (IEEE CloudNet)*.
- L. F. C. Cristino, P. H. C. Caminha — [**Caracterização da Latência de Borda sob Efeito de Mobilidade a Partir de Dados Reais**](https://sol.sbc.org.br/index.php/wgrs/article/view/30091), *WGRS 2024 (SBC)*.

## License

Distributed under the **GNU GPL v3**. See [`LICENSE`](LICENSE).

## Authors

Developed at the **Grupo de Teleinformática e Automação (GTA/COPPE/UFRJ)** by **Luiz Felipe Cantanhede Cristino** and **Pedro Henrique Cruz Caminha**.
