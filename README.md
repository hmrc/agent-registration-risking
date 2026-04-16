# agent-registration-risking

This service handles the risking element of an agent registration made through agent-registration-frontend.

API
---

| *Task*                                                                     | *Supported Methods* | *Description*                                                                                       |
|----------------------------------------------------------------------------|---------------------|-----------------------------------------------------------------------------------------------------|
| ```/agent-registration-risking/submit-for-risking```                       | POST                | Submit a completed applicaton for risking. [More...](docs/submitForRisking.md)                      |
| ```/agent-registration-risking/application/:applicationReference```        | GET                 | Get the application for the given reference. [More...](docs/getApplication.md)                      |
| ```/agent-registration-risking/individual/:personReference```              | GET                 | Get the individual for the given reference. [More...](docs/getIndividual.md)                        |
| ```/agent-registration-risking/application-status/:applicationReference``` | GET                 | Get the application risking status for the given reference. [More...](docs/getApplicationStatus.md) |

# Running the Service

To start the service, use the following commands:

- `sbt runTestOnly` - this enables extra test endpoints
- `sbt run` to launch the service normally.

Ensure that all dependent applications, including MongoDB and other microservices, are also running.
See https://github.com/hmrc/agent-registration-frontend for that.

# Project Setup in IntelliJ

When importing a project into IntelliJ IDEA, it is recommended to configure your setup as follows to optimize the
development process:

1. **SBT Shell Integration**: Utilize the sbt shell for project reloads and builds. This integration automates project
   discovery and reduces issues when running individual tests from the IDE.

2. **Enable Debugging**: Ensure that the "Enable debugging" option is selected. This allows you to set breakpoints and
   use the debugger to troubleshoot and fine-tune your code.

3. **Library and SBT Sources**: For those working on SBT project definitions, make sure to include "library sources"
   and "sbt sources." These settings enhance code navigation and comprehension by providing access to the underlying SBT
   and library code.

Here is a visual guide to assist you in setting up:
![img.png](readme/intellij-sbt-setup.png)

## Project specific sbt commands

### Turn off strict building

In sbt command in intellij:

```
sbt> relax
```

This will turn off strict building for this sbt session.
When you restart it, or you build on jenkins, this will be turned on.

### Run with test only endpoints

```
sbt> runTestOnly
```

### Run tests before check in

```
sbt> clean test
```

### License

This code is open source software licensed under
the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

* use RiskingResultParser and parse RiskingResultRecord -> RiskingResult immediately after received from connector
* upload risking results to object store as the last step (currently named as uploadAndLogResultFile)

* remodel domain:
  * decouple IndividualsForRisking from ApplicationForRisking and store them in separate collections
  * add isSubscribed: Boolean flag to ApplicationForRisking to reflect entity was subscribed
  * introduce new entity: RiskingFile, which will aggregate all ApplicationForRisking sent for risking. Fields: riskingFileId, riskingFileName, sentToMinervaDateTime. That assumes adding optional riskingFileId to the ApplicationForRisking
  * remove ApplicationForRiskingStatus
  * use EntityRiskingOutcome, and IndividualRiskingOutcome instead of `ApplicationForRiskingStatus.CompletedApplicationRiskingOutcome`
  * change the logic for subscription to search for all applications in db which have isSubscribed=false and outcome=succeeded
  * update docs

 * out of scope for now, we'll address it later:
  * processing risking results asynchronously 
  * discarding invalid data and processing only good - needs discussions with BAs and Architect
  * lock mechanism: only one instance of processing risking results file can happen at a time
  * during subscription step, if subscription fails, just log error and carry on (don't update isSubscribed to true). Succeeded applications will be retried auomatically next time (self healing)

* make nice meaningful names for methods, and general code organisation

AvailbleFiles:
F1
F2
F3
F4

OurObjectStore:
F1
F2
F3
F4

CurrentlyProcessing:
unprocessedAvailableFiles=[]



AgentApplicatoinForRisking
- applicationRef
- riskingFileId: Option[RiskingFileId]

IndividualForRisking


RiskingFile
- riskingFileId
- timestamp
- fileName
- isSent: Boolean












- Use `RiskingResultParser` to parse `RiskingResultRecord` → `RiskingResult` immediately after receiving it from the connector.
- Remodel the domain:
    - Decouple `IndividualsForRisking` from `ApplicationForRisking` and store them in separate collections.
    - Add an `isSubscribed: Boolean` flag to `ApplicationForRisking` to indicate that the entity has been subscribed.
    - Introduce a new entity `RiskingFile` to aggregate all `ApplicationForRisking` records sent for risking. Fields: `riskingFileId`, `riskingFileName`, `sentToMinervaDateTime`. This requires adding an optional `riskingFileId` to `ApplicationForRisking`.
    - Remove `ApplicationForRiskingStatus`.
    - Use `EntityRiskingOutcome` and `IndividualRiskingOutcome` instead of `ApplicationForRiskingStatus.CompletedApplicationRiskingOutcome`.
    - Change the subscription logic to search for all applications in the database where `isSubscribed = false` and `outcome = succeeded`.
    - Update the documentation.
- **Out of scope** (to be addressed later):
    - Processing risking results asynchronously.
    - Discarding invalid data and processing only valid records (requires discussion with BAs and Architect).
    - Lock mechanism: ensure only one instance processes a risking results file at a time.
    - During the subscription step, if subscription fails, log the error and continue (do not set `isSubscribed = true`). Successfully processed applications will be automatically retried on the next run (self-healing).