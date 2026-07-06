# Testing Risking Scripts

These scripts simulate risk results file processing.

All scripts talk to `localhost:22203`, which in `application.conf` is configured as the `secure-data-exchange-proxy` host — but it's actually **this service itself** running locally with test-only routes enabled.

> **Note:** The service must be started with test-only routes enabled:
> ```
> sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
> ```

---

## The Flow

### Prep
1. Create an application in mongo
2. Capture the Application and Individual References
3. Update the risking results file with the relevant Application and Individual References plus the desired risk results
4. Rename the risking file if already used (previously used files will be in the processed-results-files folder and won't be processed again)

### Step 1 — Setup Object Store (`20-setup-object-store.sh`)

```bash
./20-setup-object-store.sh
```

Registers an auth token (`1234`) with the locally running **object-store stub** (port 8464). This matches `internal-auth.token = "1234"` in `application.conf`. Must be run first before uploading any files.

---

### Step 2 — Upload a Results File (`15-upload-risking-results-file.sh`)

```bash
./15-upload-risking-results-file.sh risking-results-1.json
```

POSTs the JSON file body to the test-only endpoint, which stores the file in object-store under the service's namespace — simulating what the real SDES would do when delivering a file.

The **results files** (`risking-results-1.json`, `risking-results-2.json`, etc.) contain an array of `Entity` and `Individual` records with risking failure codes, for example:

```json
[
  {
    "recordType": "Entity",
    "applicationReference": "XKXP9HEZB",
    "failures": [
      { "reasonCode": "7", "reasonDescription": "Insolvent", ... }
    ]
  },
  {
    "recordType": "Individual",
    "personReference": "JJFCXYTM4",
    "failures": [
      { "reasonCode": "9", "reasonDescription": "Relevant criminal convictions", ... }
    ]
  }
]
```

---

### Step 3 — Send the "File Ready" Notification (`50-send-file-ready-notification.sh`)

```bash
./50-send-file-ready-notification.sh
```

POSTs `notification-file-ready.json` to the **real production endpoint** `/receive-sdes-notifications`. This tells the service a results file is ready, but **does not trigger processing** — the service only logs the notification. Processing now happens via an hourly scheduler (see Step 5).

The notification payload looks like:

```json
{
  "notification": "FileReady",
  "filename": "file-name-whatever.json",
  "checksumAlgorithm": "md5-whatever",
  "checksum": "123456-whatever",
  "correlationID": "correlationId-whatever",
  "availableUntil": "2027-01-01T00:00:00.000Z-whatever",
  "dateTime": "2027-01-01T00:00:00.000Z-whatever"
}
```

---

### Step 4 — Verify the File is Available (`51-process-results-files.sh`)

```bash
./51-process-results-files.sh
```

Calls the SDES stub's `files-available/list` endpoint to confirm the uploaded results file is visible and ready to be picked up. Use this as a sanity check before triggering processing.

---

### Step 5 — Trigger Risking (`60-run-risking.sh`)

```bash
./60-run-risking.sh
```

Calls the **test-only endpoint** `/agent-registration-risking/test-only/run-risking`, which immediately triggers the same logic the hourly scheduler runs. Use this to process the staged results files without waiting up to an hour for the scheduler to fire.

> **Note:** This requires the service to be running with test-only routes enabled (see above).

---

## Key Design Point: How Files Are Processed

The service does **not** process files when it receives a notification. The notification only tells the service **a file is ready** (with a filename and checksum). Processing happens separately, on an **hourly schedule** (or immediately via `60-run-risking.sh` when testing). When processing runs, the service:

1. **Fetches the list of available files from object-store** (port 8464) using the internal auth token
2. Parses and processes the unprocessed risking results

The test-only upload script (step 2) pre-loads the file into object-store so it's there when the service goes to fetch it.

---

## Supporting / Housekeeping Scripts

| Script | Purpose |
|---|---|
| `10-list-available-files.sh` | Lists files currently staged in the SDES stub, filtered by information type (see `secure-data-exchange-proxy.inbound.information-type` in `application.conf`) |
| `21-list-object-store-files.sh` | Directly lists files on disk at `/tmp/object-store/agent-registration-risking/` — the local object-store stub stores files here |
| `11-delete-all-risking-results-files.sh` | Clears all staged results files via a test-only endpoint |
| `12-delete-risking-results-file.sh` | Deletes a specific results file by name |

Note: None of the above scripts clear the list of processed results files so you will need to give the results file a unique name each time you want to test a new file. The service will ignore any files that have already been processed.