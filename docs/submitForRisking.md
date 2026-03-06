Submit for Risking
----
Submit a completed application for risking.

* **URL**

  `/agent-registration-risking/submit-for-risking`

* **Method:**

  `POST`

* **Body**
  > Uses shared [SubmitForRiskingRequest](../app/uk/gov/hmrc/agentregistration/shared/risking/SubmitForRiskingRequest.scala) model

```json
{
  "agentApplication": {
    "_id": "APP111113",
    "internalUserId": "USR9999",
    "linkId": "LINK001",
    "groupId": "GRP001",
    "createdAt": "2025-01-15T10:30:00Z",
    "applicationState": "GrsDataReceived",
    "userRole": "Owner",
    "businessDetails": {
      "safeId": "123",
      "saUtr": "1234567890",
      "companyProfile": {
        "companyNumber": "OC123456",
        "companyName": "Test LLP Partnership"
      }
    },
    "applicantContactDetails": {
      "applicantName": "Test guy",
      "telephoneNumber": "01234567890",
      "applicantEmailAddress": {
        "emailAddress": "test@example.com",
        "isVerified": true
      }
    },
    "amlsDetails": {
      "supervisoryBody": "HMRC",
      "amlsRegistrationNumber": "AML123456",
      "amlsExpiryDate": "2030-01-01",
      "amlsEvidence": {
        "uploadId": "123",
        "fileName": "this.txt",
        "objectStoreLocation": {
          "directory": {
            "value": ""
          },
          "fileName": "store"
        }
      }
    },
    "agentDetails": {
      "businessName": {
        "agentBusinessName": "test bus",
        "otherAgentBusinessName": "test bus2"
      },
      "telephoneNumber": {
        "agentTelephoneNumber": "01234123123"
      },
      "agentEmailAddress": {
        "emailAddress": {
          "agentEmailAddress": "test@test.com"
        },
        "isVerified": true
      },
      "agentCorrespondenceAddress": {
        "addressLine1": "21 Test Street",
        "countryCode": "GB"
      }
    },
    "vrns": [
      "X1234",
      "X2234"
    ],
    "payeRefs": [
      "Y1234",
      "Y2234"
    ],
    "refusalToDealWithCheckResult": "Pass",
    "companyStatusCheckResult": "Pass",
    "hmrcStandardForAgentsAgreed": "Agreed",
    "type": "AgentApplicationLlp"
  },
  "individuals": [
    {
      "_id": "test-individual-id-456",
      "individualName": "John Smith",
      "isPersonOfControl": true,
      "internalUserId": "test-user-id-456",
      "createdAt": "2025-01-15T10:30:00Z",
      "providedDetailsState": "Finished",
      "agentApplicationId": "app-id-789",
      "individualDateOfBirth": {
        "type": "Provided",
        "dateOfBirth": "1980-05-15"
      },
      "telephoneNumber": "01234567890",
      "emailAddress": {
        "emailAddress": "john.smith@example.com",
        "isVerified": true
      },
      "individualNino": {
        "type": "Provided",
        "nino": "AA123456A"
      },
      "individualSaUtr": {
        "type": "Provided",
        "saUtr": "1234567890"
      },
      "hmrcStandardForAgentsAgreed": "Agreed",
      "hasApprovedApplication": true,
      "vrns": [
        "X1234",
        "X2234"
      ],
      "payeRefs": [
        "Y1234",
        "Y2234"
      ]
    },
    {
      "_id": "test-individual-id-789",
      "individualName": "John Smith",
      "isPersonOfControl": true,
      "internalUserId": "test-user-id-456",
      "createdAt": "2025-01-15T10:30:00Z",
      "providedDetailsState": "Finished",
      "agentApplicationId": "app-id-789",
      "individualDateOfBirth": {
        "type": "Provided",
        "dateOfBirth": "1980-05-15"
      },
      "telephoneNumber": "01234567890",
      "emailAddress": {
        "emailAddress": "john.smith@example.com",
        "isVerified": true
      },
      "individualNino": {
        "type": "Provided",
        "nino": "AA123456A"
      },
      "individualSaUtr": {
        "type": "Provided",
        "saUtr": "1234567890"
      },
      "hmrcStandardForAgentsAgreed": "Agreed",
      "hasApprovedApplication": true,
      "vrns": [
        "X1234",
        "X2234"
      ],
      "payeRefs": [
        "Y1234",
        "Y2234"
      ]
    }
  ]
}
```

* **Success Responses:**

    * **Code:** 201

* **Error Responses:**

    * **Code:** 401 UNAUTHORIZED <br/>
      **Content:** `{"code":"UNAUTHORIZED","message":"Bearer token is missing or not authorized for access"}`

    * **Code:** 403 FORBIDDEN <br/>
      **Content:** `{"code":"FORBIDDEN","message":Authenticated user is not authorised for this resource"}`
