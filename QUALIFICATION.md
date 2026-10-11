# Qualified host build — Seeker Store + 18 utilities

Qualified code commit: `9bdf4a92851fe12f5f297cc2b51fcc8660e0002c`.

[Successful CI run](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530)

- PASS: store build, applicable JVM tests and lint.
- PASS: all 18 independently installable utility APK build/lint jobs and applicable unit tests.
- PASS: signature verification, exact package identity, min API 26 / target 34 and actual file/certificate SHA-256 metadata for all 19 APKs.
- PASS: 11 backend tests, including exact utility package binding.
- PASS: live Open-Meteo response shape and live Overpass Kochi region (89 road geometries, 74,602 bytes); these are host network checks, not phone cache tests.
- NOT_RUN: physical phone runtime, CameraX capture, SAF provider handling, Keystore runtime, sensor behavior, two-phone LAN exchange, model import/translation, airplane mode and wallet signing on device.
- PENDING: independently reviewed public devnet receiving address; stable owner-controlled release signing; hosted publisher APK metadata; existing Worker deployment access.

Each debug artifact contains an APK, observed metadata and signature verification report. GitHub login may be required to download artifacts. Per-build debug signing is not a stable release identity. No APK URL, signer pin or token return is invented. App feature limits are listed in utilities/README.md. Scanner is photo decoding; Maps is approximate cached road routing; Chat is manual trusted LAN; Vault is an unreviewed private-record utility. No feature parity with mature consumer apps is claimed.

| Artifact | Link |
|---|---|
| seeker-weather-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11718580474) |
| seeker-translation-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11718555662) |
| seeker-messaging-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11718535740) |
| seeker-device-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11718460487) |
| seeker-maps-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11718426126) |
| seeker-camera-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11718296322) |
| seeker-music-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11718261404) |
| seeker-backup-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11718161485) |
| seeker-browser-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11718056766) |
| seeker-notes-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11717996033) |
| seeker-vault-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11717991790) |
| seeker-store-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11717971907) |
| seeker-gallery-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11717921766) |
| seeker-calculator-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11717841960) |
| seeker-tasks-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11717831945) |
| seeker-scanner-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11717711364) |
| seeker-appmanager-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11717647353) |
| seeker-files-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11717492895) |
| seeker-fitness-debug | [Download](https://github.com/CodesbyFebin/SKR-Seeker-Store/actions/runs/38181310530/artifacts/11717438495) |

Later documentation and owner-helper changes do not change these qualified APK bytes.

