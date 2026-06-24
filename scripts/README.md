# Testing Risking Scripts

These scripts simulate risk results file processing.

By default all scripts talk to `localhost:22203`. To run against a remote environment set `BASE_URL`:

```bash
export BASE_URL=https://<qa-or-staging-host>
```

> **Note:** The service must be started with test-only routes enabled:
> ```
> sbt run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
> ```
> For QA/Staging this must be set in the service-manager profile:
> ```
> -Dapplication.router=testOnlyDoNotUseInAppConf.Routes
> ```

---

## Two Flows

There are two ways to trigger risking depending on the environment.

### Local Flow (truer end-to-end test — includes object-store upload)

Uses the production notification endpoint. The service goes through the full SDES-proxy loop: list files → download → process → upload to object-store.

**Steps:** `20` → `15` → `50`

### QA/Staging Flow (test-only — bypasses SDES proxy and HTTP download)

Reads uploaded files directly from MongoDB. Avoids the SDES proxy and HTTP download which are not reliably accessible in higher environments.

**Steps:** `15` → `55`

---

## Step by Step

### Step 1 — Setup Object Store — **Local only** (`20-setup-object-store.sh`)

```bash
./20-setup-object-store.sh
```

Registers the auth token with the locally running **object-store stub** (port 8470). **Not needed in QA/Staging.**

---

### Step 2 — Upload a Results File (`15-upload-risking-results-file.sh`)

```bash
./15-upload-risking-results-file.sh risking-results-1.json
```

POSTs the JSON file from your local machine to the test-only endpoint, storing it in MongoDB. Works in all environments.

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

### Step 3a — Trigger via SDES notification — **Local only** (`50-send-file-ready-notification.sh`)

```bash
./50-send-file-ready-notification.sh
```

POSTs a "FileReady" notification to the production `/receive-sdes-notifications` endpoint. The service then fetches the file list from itself (via the SDES proxy config loop), downloads and processes the files, and uploads them to object-store as a backup.

You must use a **unique filename** each time — files already uploaded to object-store won't be processed again.

---

### Step 3b — Trigger directly from MongoDB — **QA/Staging** (`55-trigger-risking-from-mongo.sh`)

```bash
BASE_URL=https://<staging-host> ./55-trigger-risking-from-mongo.sh
```

Calls the test-only `/test-only/trigger-risking` endpoint. The service reads files directly from MongoDB, processes the risking results, and **deletes the file from MongoDB** once processed. No HTTP download, no proxy, no object-store upload.

Because files are deleted after processing, you **can reuse the same filename** across test runs.

---

## Supporting / Housekeeping Scripts

| Script | Purpose | Env |
|---|---|---|
| `10-list-available-files.sh` | Lists files currently staged (in MongoDB) | Any (respects `BASE_URL`) |
| `11-delete-all-risking-results-files.sh` | Clears all staged results files | Any (respects `BASE_URL`) |
| `12-delete-risking-results-file.sh` | Deletes a specific results file by name | Any (respects `BASE_URL`) |
| `20-setup-object-store.sh` | Registers auth token with the local object-store stub | Local only |
| `21-list-object-store-files.sh` | Lists files at `/tmp/object-store/agent-registration-risking/` | Local only |
