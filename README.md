# COMP90015 Distributed Systems Project II #

* Multi-server broadcaster with an improved architecture and security protocals.
* Centralized load balancer managing the server load balancing, client register, login and redirection.
* Load balancer can optionally have mirrored backups for better stability.
* Support SSL protocal (compatible with plain connections for outgoing connection, but not incomming connection).

## Server architecture

    (registering    |--------|      |----------------------------------|
     / logging in)  |        |      |                  root            |    (logged in)
       client1------|        |      |                  /  \            |------client4
                    |  load  |      |       broadcaster1  broadcaster2 |
       client2------|balancer|------|           /  \          /  \     |------client5
                    |        |      | broadcaster3  ...    ...    ...  |
       client3------|        |      |     /  \                         |
                    |        |      |  ...    ...                      |
                    |--------|      |----------------------------------|

## Prerequisites

* Eclipse
* JavaSE 8+
* Dependencies:

        commons-cli-1.3.1
        apache-log4j-2.6
        gson-2.6.2

## Setup servers

* Run 'LoadBalancer.java' with an attribute '-lp <local port number>' to start a load balancer server.
* Run 'Server.java' with following attributes '-lp <local port number>' to start a root server. A secret will be returned can can be found in console. Same secret will be used to start other servers.

        -lp <local port number>
        -lh <local host name> (optional, 'localhost' by default)
        -lbp <load balancer port number>
        -lbh <load balancer host name>
        -s <secret>

* Run 'Server.java' with following attributes to start child broadcasters (note: each broadcast server can have 5 children, and 50 client connections at most):

        -lp <local port>
        -rp <remote port>
        -rh <remote host>
        -lh <local host name> (optional, 'localhost' by default)
        -lbp <load balancer port number>
        -lbh <load balancer host name>
        -s <secret>

## Client register & login

* Start client: user can either start a client with attributes below, or type the same attributes in the window popped up.

        -u <user name>
        -rp <load balancer port number>
        -rh <load balancer host name>
        -s <client secret>

* Register: run 'Client.java', enter username, secret, remote host, and remote port number. Click 'register' button.
* Login with username and secret: enter username, secret, remote host, and remote port number. Click 'login' button.
* Anonymous login without register: run 'Client.java', click 'Anonymous login' button.

## Broadcast activity message

* After successful login, a new GUI for broadcasting message will appear.
* Enter a sentence such as 'hello', there is no need to enter a Json format string.

## File structures

    ../src/activitystreamer                           ---Main entries of client and server
    ../src/activitystreamer.client                    ---Client implementations
    ../src/activitystreamer.server.Broadcaster        ---Broadcast server implementations
    ../src/activitystreamer.server.LoadBalancer       ---Load balancer server implementations
    ../src/activitystreamer.util                      ---Helper functions and util classes
    ../src/activitystreamer.message                   ---Standardized message implementations
    ../Certificate                                    ---Certificate file for authentication

## Contributors

* Sangzhuoyang Yu
* Kangping Huang
* Fallie Zhang
* Boyang Xing
