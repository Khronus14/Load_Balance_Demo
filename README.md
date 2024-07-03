# Load Balancing Logic Demo

This application demonstrates the two types of network load balancing logic: least connection and weighted round-robin.


## Compile

`javac Client_Generator_TCP.java ClientThread_TCP.java LB_Protocol.java Portal_Connection_TCP.java Portal_HealthConn_TCP.java Portal_TCP.java ServerFarm_Connection_TCP.java ServerFarm_HealthConn_TCP.java ServerFarm_TCP.java ServerThread_TCP.java`

## Running the application

Each part of the application should be run in its own window as each part of the
application has its own output and commands. First, start the server farm by running

`java TCP.ServerFarm_TCP {# of servers} {portal port number}`

Next, start the portal by running

`java TCP.Portal_TCP {# of servers} {IP address of client generator} {portal port number}`

Finally, start the client generator by running

`java TCP.Client_Generator_TCP {starting # of clients} {IP address of portal} {portal port number}`

## Commands

### Client Generator
- `add {int}` increases the maximum number of clients to keep active
- `remove {int}` decreases the maximum number of clients to keep active
- `?` prints the current number of active clients
- `!` toggles verbose mode which is off by default ('Client x counted to y out of z in n seconds')
- `exit` cleanly shuts down after all connections are closed

### Portal
- `down {int}` puts server in a failure state
- `up {int}` puts server in a standby state awaiting new portal connections
- `wrr` sets load balancing logic to weighted round-robin
- `lc` sets load balancing logic to least connection
- `?` prints the current status of the server farm
- `exit` cleanly shuts down after all connections are closed