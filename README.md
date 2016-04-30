# COMP90015 Distributed Systems Project I #

Multi-server broadcast client messages.

## Server architecture

                 |----------------------------------|
                 |                 root             |
    client1------|                /    \            |------client4
                 |          server1    server2      |
    client2------|           / \          / \       |------client5
                 |    server3   ...    ...   ...    |
    client3------|      / \                         |------client6
                 |   ...   ...                      |
                 |----------------------------------|

## Prerequisites of running the project

* Eclipse
* JavaSE 8+
* Dependencies: Gson 2.6.2, commons-cli-1.3.1, log4j-2.5, json_simple-1.1

## Setup servers

* Run 'Server.java' with an attribute '-lp <local port number>' to start a root server.
a secret will be returned can can be found in console. Same secret will be used to start other servers.
* Run 'Server.java' with attributes:

    -lp <local port>
    -rp <remote port>
    -rh <remote host>
    -lh <local host>
    -s <secret>

## Client register && login

* Run Client.java with attributes:

    - <remote port>
    -rh <remote host>
    -lh <local host>
    -s <secret>

* Register: enter username， secret, remote host，and remote port number. Click 'register' button.
* Login with username and secret: enter username， secret, remote host，and remote port number. Click 'login' button.
* Anonymous login without register: Click 'Anonymous login' button.

## File structures

    ../src/activitystreamer          ---Main entries of client and server
    ../src/activitystreamer.client   ---Client implementations
    ../src/activitystreamer.server   ---Server implementations
    ../src/activitystreamer.util     ---Helper functions
    ../src/activitystreamer.message  ---Standardized message implementations

## Contributers

* Sangzhuoyang Yu
* Kangping Huang
* Fallie Zhang
* Boyang Xing