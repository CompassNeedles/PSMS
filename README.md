Author:
Sami Mourad

PSMS:

- Java implementation of a Publish/Subscribe Messaging Service

Client-side:

- MultiCli.java
- PSDaemon.java
- PSListen.java
- PSWrite.java

- MultiCliTester.java

Server-side:

- MultiSer.java
- PSMultiServe.java
- PSProtocol.java
- PSBroadcast.java

Shared classes:

- Pair.java


To-do:
Testing for (multiple) Server concurrency.


Description:

- Uses socket communication for servers and clients to send requests
and interchange data in a Pub/Sub service.

- The server class MultiSer is programmed with entry-point main.
It is runnable from the command line, and runs in an endless loop,
spawning threads to service incoming requests concurrently.

Messages are written as files into a new output directory.

Program memory stores a list of users with metadata (including java
universally unique identifiers) and timestamps for synchronization
with the database. History of publications to a topic is also stored
(by topic) using messages' metadata.

All lists are implemented using ConcurrentHashMaps for efficient lookup
(whether by user or topic) and thread-safety.

After a PUBLISH request is serviced, the server issues a broadcast,
which spawns another conccurent thread to iterate over all subscribers
to the topic in question and update them with new message content.

- The class MultiCli implements an API to use in programs on the
client side.

Messages are written as files into a new output directory, just like
the server.

The constructor for MultiCli initializes a Daemon to listen to server
broadcasts but also hold the data structure to iterate over and store
metadata relating to a user's subscriptions. Any modifications in
MultiCli are therefore reflected in the Daemon and the Daemon 
concurrently updates its own data structure (and the filesystem) with
new broadcast content, made directly accessible to MultiCli.

- See MultiCliTester.java for a demo


Final Notes:

All of this is to allow for concurrency. ConcurrentHashMap is thread-
safe but not locking and therefore does not entirely eliminate
data races.

PSMS can support an unlimited number of users and servers*. It can
hold as many messages as the hosting disks allow and so long as
user and topic message metadata can be stored in program memory.
PSMS does not support unsubscriptions or deletion of messages/topics.

Also, once a user subscribes to a particular topic, the server
issues a universally unique identifier and sends it back to the user.
The user must present this token everytime in all subsequent requests.

The user must also provide a username, and the current requirement is
that this be presented as the hostname (name of the computer that is
running the client) so that it can be used by the server for Socket
communication in broadcasts.


Sources/ideas for code:

https://docs.oracle.com/javase/tutorial/networking/sockets/index.html
https://docs.oracle.com/javafx/2/api/javafx/util/Pair.html
