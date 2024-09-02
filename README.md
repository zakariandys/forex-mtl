# Forex Rate Service

This project provides a service for fetching and caching forex rates using a scheduled task. It supports fetching rates from an external API, processing them, and storing them in a local cache for fast retrieval.

## Assumptions and Limitations

- There are only 9 supported currencies: AUD, CAD, CHF, EUR, GBP, NZD, JPY, SGD, and USD.
- The OneFrame service supports a maximum of 1.000 requests per day.
- The OneFrame service supports making requests for one or more currency pairs in a single API call. And for 72 pairs of currency is still on acceptable timecost (less than 1 second)
- The age of rates should be less than 5 minutes. If a rate is older than 5 minutes, or no update to a currency pair within 5 minutes the service will treat it as outdated and return an error.
- The proxy service is designed to handle at least 10.000 successful requests per day using a single API token.

## Simplifications and Choices

 `RateServiceInterpreter` class is responsible for retrieving exchange rates from a local cache and ensuring that the data is current and valid.

`LiveRateServiceInterpreter` class handles the live fetching of currency rates from the OneFrame API and stores the fetched rates in a local cache.

![image](https://github.com/user-attachments/assets/bfda6271-69f2-4c1d-96df-2fdac4ed70c6)

- **Cache Implementation**: The cache is implemented in-memory cache library. The initial implementation uses a simple local memory cache without persistence, which can be easily swapped out for a distributed cache if needed in the future.
- **Error Handling**: Error handling is managed by the proxy service, which handles all possible error codes returned by the OneFrame API. For logging, the current implementation uses basic println statements. It’s recommended to replace this with a proper logging for better metrics measurement
- **Scheduling**: The forex service is configured to fetch rates every 2 minutes, ensuring that the cached data is no more than 5 minutes old. Here’s how the scheduling affects the number of API calls:
  - **Total Currency Pairs**: 72 unique pairs (from 9 currencies).
  - **Fetch Interval**: Every 2 minutes.
  - **API Calls per Fetch**: The service requests all 72 currency pairs in a single API call.
  - **Total API Calls per Day**: Since each fetch makes 1 API call, the service makes 720 API calls per day.

## How to Run the Code

### Prerequisites

- Scala (version 2.13.x recommended)
- SBT (Scala Build Tool)
- Java (version 8 or above)
- Docker

### Running One-Frame Service
1. **Pull the One-Frame API:**
   ```bash
   docker pull paidyinc/one-frame:latest
   ```

2. **Run the One-Frame Service:**
   ```bash
   docker run -p 8080:8080 paidyinc/one-frame
   ```

### Running Proxy Service

1. Clone the repository:
    ```bash
    git clone git@github.com:zakariandys/forex-mtl.git
    cd forex-mtl
    ```

2. Compile and run the application using SBT:
    ```bash
    sbt run
    ```

3. The application will start and begin fetching forex rates at the configured interval.

4. Invoke the forex service
    ```bash
    curl 'http://127.0.0.1:8081/rates?from=USD&to=EUR'
    ```

### Running Tests

1. To run the test, use the following command:
    ```bash
    sbt test
    ```

2. The tests cover various scenarios, including positive cases (e.g., successful rate retrieval and caching) and negative cases (e.g., handling errors and invalid data).

## Next Phase of Development / Improvement

- Implement parameter validation to prevent unexpected currency parameters before invoking the OneFrame API, which can reduce the token usage.
- Implement a logging framework to enhance metrics tracking, such as the most requested currency pairs, time cost for each scheduling task, the most common errors from the OneFrame API, etc. This will help inform future improvements.
- Implement a retry mechanism with exponential backoff to handle failures when the OneFrame API is unavailable.
- Improve the scheduling task starts immediately upon application launch.
- Add support for a distributed cache as the primary caching solution, with in-memory caching as a fallback.
- Dockerize the application to improve scalability and deployment.
