Implementation of Centralized Computing System (CCS)
________________________________________
1. Overview
The Centralized Computing System (CCS) is a Java-based application that simultaneously provides three core functionalities:
1.	Service discovery (UDP)
2.	Communication with clients (TCP)
3.	Periodic statistics reporting
A single instance of CCS acts as a server that listens on a specified UDP/TCP port for discovery requests and client connections, respectively. Upon receiving valid messages, it performs arithmetic operations, returns results, and logs comprehensive statistics.
Usage
java -jar CCS.jar <port>
•	<port>: Specifies the port number (both UDP and TCP) for service discovery and client communication.
•	The application verifies that <port> is between 1024 and 65535 (system-reserved ports are excluded).
________________________________________
2. Implemented Features
2.1 Service Discovery (UDP)
•	Initialization
  o	Binds a DatagramSocket to the specified <port>.
•	Listening
  o	Continuously listens for UDP packets.
  o	Examines incoming data to see if it starts with "CCS DISCOVER".
•	Response
  o	Sends back "CCS FOUND" to the sender’s IP address and port.
  o	Returns immediately to listening for new discovery requests.
2.2 Communication with Clients (TCP)
•	Socket Binding
  o	Opens a ServerSocket on the specified <port>.
  o	Waits for incoming client connections.
•	Request Handling
  o	Spawns a new worker thread for each client, allowing multiple concurrent connections.
  o	Reads a single line from the client, expecting the format:
<OPER> <ARG1> <ARG2>
where <OPER> ∈ {ADD, SUB, MUL, DIV} and <ARG1>, <ARG2> are integers.
  o	Performs the requested operation or returns "ERROR" if the request is invalid (e.g., wrong format, division by zero).
•	Response
  o	Sends the computed result (integer) back to the client or "ERROR" if applicable.
  o	Continues listening until the client disconnects or terminates its connection.
  o	The server remains active for new or other existing clients.
2.3 Statistics Reporting
•	Global Counters
  o	Tracks the total number of connected clients, total requests received, counts of each arithmetic operation type, total sum of results, and the number of errors.
•	Periodic Printing
  o	A dedicated statistics thread runs on a 10-second interval.
  o	Prints both:
    1.	Cumulative statistics since the server started.
    2.	Recent statistics for the last 10-second window.
  o	Resets short-term counters after each report.
________________________________________
3. Unimplemented Features
IPv6 Support: the server currently handles only IPv4 addresses. The code does not explicitly support IPv6 connections or UDP-based discovery on IPv6.
________________________________________
4. Protocol Description
4.1 Communication Protocol
•	Transport Layers
  o	UDP for service discovery: fast, connectionless communication.
  o	TCP for request processing: reliable, connection-oriented communication.
•	Message Formats
  o	UDP: ASCII text starting with "CCS DISCOVER".
  o	TCP: ASCII text in the format OPER ARG1 ARG2.
4.2 Protocol Actions
UDP Service Discovery
  1.	Client broadcasts: "CCS DISCOVER ..."
  2.	Server replies: "CCS FOUND" if the message begins with "CCS DISCOVER".
TCP Client Requests
  1.	Server accepts a socket connection.
  2.	Client sends:
  <OPER> <ARG1> <ARG2>
3.	Server computes:
  o	ADD → <ARG1> + <ARG2>
  o	SUB → <ARG1> - <ARG2>
  o	MUL → <ARG1> * <ARG2>
  o	DIV → <ARG1> / <ARG2> (if <ARG2> ≠ 0, otherwise "ERROR")
  o	Invalid input results in "ERROR".
4.	Server returns the result or "ERROR".
5.	Server logs request and result.
6.	Client may disconnect at any time.
________________________________________
5. Difficulties Encountered
•	UDP Broadcasting vs. Direct Messages
  o	Ensuring correct handling of both broadcast and unicast discovery messages.
•	Concurrency
  o	Multiple clients connecting over TCP requires thread-safe access to shared data structures and counters.
•	Input Validation
  o	Parsing incorrect or malformed request lines can lead to unexpected exceptions.
________________________________________
6. Existing Errors and Limitations
Only IPv4: the server does not bind to IPv6 addresses or test for IPv6 discovery messages.
________________________________________
7. Conclusions
The CCS implementation satisfies the core requirements:
1.	Service Discovery over UDP: listens for "CCS DISCOVER" and replies with "CCS FOUND".
2.	Client Communication using TCP: accepts requests in the format <OPER> <ARG1> <ARG2>, processes arithmetic operations, and responds with results or "ERROR".
3.	Statistics Reporting: logs and prints total and recent metrics every 10 seconds.

