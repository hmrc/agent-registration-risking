Get Application Risking Response
----
Returns the application for the given reference.

* **URL**

  `/agent-registration-risking/application/:applicationReference`

    * **Method:**

      `GET`

        * **Success Responses:**

            * **Code:** 200 <br/>
              **Content:**
        ```json
        {
          "applicationReference": "69a80c93f33cf44004adf67f",
          "status": "ReadyForSubmission",
          "individuals": [
            {
              "personReference": "69a80c93f33cf44004adf680",
              "status": "ReadyForSubmission"
            },
            {
              "personReference": "69a80c93f33cf44004adf681",
              "status": "ReadyForSubmission"
            }
          ]
        }
         ```

* **Error Responses:**

    * **Code:** 404 NOT_FOUND <br/>

    * **Code:** 401 UNAUTHORIZED <br/>

    * **Code:** 403 FORBIDDEN <br/>
