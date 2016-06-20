# COMP90015 Distributed Systems Project I #

* Multi-server broadcast client messages.
* Broadcast server clusters manage client register & login.
* Decentralized load balancer managing the server load balancing.
* Concurrency achieved by broadcasting lock requests when a client is trying to register on one of server nodes.

## Server architecture

                 |----------------------------------|
                 |                 root             |
    client1------|                /    \            |------client4
                 |          server1    server2      |
    client2------|           / \          / \       |------client5
                 |    server3   ...    ...   ...    |
    client3------|      / \                         |
                 |   ...   ...                      |
                 |----------------------------------|

## Prerequisites

* Eclipse
* JavaSE 8+
* Licenses of dependencies:

        commons-cli-1.3.1	Apache License Version 2.0
        apache-log4j-2.5	Apache License Version 2.0
        json_simple-1.1         The JSON License
        gson-2.6.2	        Apache License Version 2.0

## Setup servers

* Run 'Server.java' with an attribute '-lp <local port number>' to start a root server. A secret will be returned can can be found in console. Same secret will be used to start other servers.
* Run 'Server.java' with attributes:

        -lp <local port>
        -rp <remote port>
        -rh <remote host>
        -lh <local host>
        -s <secret>

## Client register & login

* Start client: user can either start a client with attributes below, or type the same attributes in the window popped up.

        -u <user name>
        -rp <remote port>
        -rh <remote host>
        -s <secret>

* Register: run 'Client.java', enter username， secret, remote host，and remote port number. Click 'register' button. After that the window will close, and then user can start client again and login with the same username and secret.
* Login with username and secret: run 'Client.java', enter username， secret, remote host，and remote port number. Click 'login' button.
* Anonymous login without register: run 'Client.java', click 'Anonymous login' button.

## Broadcast activity message

* After successfully login, a new broadcast message GUI will appear.
* Enter a sentence such as 'hello', there is no need to enter a Json format string.

## File structures

    ../src/activitystreamer          ---Main entries of client and server
    ../src/activitystreamer.client   ---Client implementations
    ../src/activitystreamer.server   ---Server implementations
    ../src/activitystreamer.util     ---Helper functions
    ../src/activitystreamer.Message  ---Standardized message implementations

## Contributors

* Sangzhuoyang Yu
* Kangping Huang
* Fallie Zhang
* Boyang Xing

