# Andhra Pradesh locations

Updated 12 October 2026. These are fictional presentation households using TFI actor names. Addresses, ward numbers and approximate city reference points do not identify the actors' homes or represent verified municipal boundaries.

| City / neighborhood | Households |
| --- | --- |
| Vijayawada / Benz Circle | Mahesh Babu, Ram Charan, Venkatesh, Rana Daggubati, Rashmika Mandanna, Akhil Akkineni |
| Rajahmundry / Danavaipeta | Prabhas, Pawan Kalyan, Nani, Samantha Ruth Prabhu, Keerthy Suresh, Sharwanand |
| Guntur / Brodipet | Allu Arjun, Chiranjeevi, Ravi Teja, Anushka Shetty, Ram Pothineni, Adivi Sesh |
| Anantapur / Ram Nagar | Jr NTR, Nagarjuna, Vijay Deverakonda, Sai Pallavi, Naga Chaitanya, Sreeleela |

`AndhraPradeshLocations` supplies the shared locations for household addresses, ward labels and new fixture report coordinates. The driver's eight stops stay within Vijayawada; this is not an intercity collection route. Crew and fine fixtures span all four wards. Fleet labels use AP registration prefixes and the built-in recovery facilities use Andhra Pradesh city names. Citizen headers follow the selected household; the driver header follows its Vijayawada route; the broader staff views show Andhra Pradesh.

## Existing installations

Room stays at version 4: this is a transactional content update, not a schema change. The `tfi-andhra-pradesh-v1` seed marker applies it once before missing data is added.

- A household moves only when its QR identity, built-in name, old address and old ward match. Custom names, addresses and wards are preserved.
- Existing QR IDs and PNGs remain valid, including the historical `BIN-JH` prefix. IDs, fees, payments, scores, streaks and preferences are unchanged.
- Collection snapshots matching a moved household's old address receive its new address; other collection locations are preserved.
- Recognizable generated reports move only when ownership, title, category, description, old address, old ward and generated coordinates match. Status, inspection proof, resolution and timestamps are preserved. Independently entered reports retain their recorded locations.
- Built-in crew assignments, vehicle/facility labels and fine locations are updated through matching source values. Attendance, PPE, weights, amounts and payment states remain intact.
- Later edits are not overwritten on relaunch. The printable QR sheet is regenerated with the new ward labels.

## Suggested next improvements

1. A city selector for Collector, Supervisor and Admin, keeping queues, crew and metrics in the same selected city. This is the most useful next step for the expanded geography.
2. A compact pickup timeline on Home with scheduled, collected and exception states, including a clear next action when collection is missed.
3. A report-detail comparison showing the original photo and resolution evidence together, so the outcome is immediately understandable.
4. A shareable receipt document and collection summary, using the stored ledger and history.

These are recommendations, not features added by this update.
