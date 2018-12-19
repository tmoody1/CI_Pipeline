Normally this would go in a separate repository, leaving here for simplicity
contains compose file for app
secrets are fixed by trainer-docker and can't be changed

If the app fails to connect to mysql it fails and the docker container stops.
The proper and elegant method to solve this is to tell the developers to modify their docker file in src/main/docker to check if mysql is up before connecting.
However as in this case we don't control the app I have just told docker to restart on failure until it works.

Setting a restart policy for docker is good practice as it improves durability of the app stack, if you set one make sure to use restart on failure not restart always. Otherwise there is no way of stopping the container if you need to debug something