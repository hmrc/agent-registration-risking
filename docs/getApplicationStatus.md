Get Application Status
----
Returns the current risking status of the given application reference.

* **URL**

  `/agent-registration-risking/application-status/:applicationReference`

* **Method:**

  `GET`

    * **Success Responses:**

        * **Code:** 200 <br/>
          **Content:**
        ```json
        {   
          "status": "ReadyForSubmission"
        }
      ```

* **Error Responses:**

    * **Code:** 404 NOT_FOUND <br/>

    * **Code:** 401 UNAUTHORIZED <br/>

    * **Code:** 403 FORBIDDEN <br/>
