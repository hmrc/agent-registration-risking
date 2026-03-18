Get Individual Risking Response
----
Returns the individual for the given reference.

* **URL**

  `/agent-registration-risking/individual/:personReference`

    * **Method:**

      `GET`

        * **Success Responses:**

            * **Code:** 200 <br/>
              **Content:**
        ```json
        {
          "personReference": "69a80c93f33cf44004adf67f",
          "status": "ReadyForSubmission"
        }
         ```
      
        * **Code:** 201 <br/>
          Returned when no matching individual found

* **Error Responses:**

    * **Code:** 401 UNAUTHORIZED <br/>

    * **Code:** 403 FORBIDDEN <br/>
