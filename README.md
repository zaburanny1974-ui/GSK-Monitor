# GSK Monitor

First-stage Android BLE diagnostic foundation for a GSIKE inverter.

## Current scope
- Android BLE permission handling
- 10-second BLE scan
- GATT connection/discovery foundation
- Service/characteristic UUID and property display
- GitHub Actions debug APK build

## Important
The GSIKE proprietary BLE protocol is intentionally **not invented** here. The next stage requires the real service/characteristic UUIDs and packet format (from the inverter/app/protocol evidence) before implementing live inverter values and commands.
