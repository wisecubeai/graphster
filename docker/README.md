# Purpose 

This docker image provides a local *spark* installation with *zeppelin* bundled with the *graphster* library and example notebooks.
It is uploaded in [dockerhub](https://hub.docker.com/r/wisecubeai/graphster/) in a public repository.

You can use it to evaluate graphster's functionality in a local environment quickly.

## Components
- Spark version="3.1.2"
- Zeppelin version="0.10.0"
- Hadoop version="3.2"


## Build the container image
```
  docker build -t wisecubeai/graphster .
```
 
## Start the container
```
  docker run -p 8080:8080 --rm --name graphster wisecubeai/graphster
```

To persist logs and notebook directories, use the volume option for docker container.
```
docker run -u $(id -u) -p 8080:8080 --rm -v $PWD/logs:/logs -v $PWD/notebook:/notebook \
           -e ZEPPELIN_LOG_DIR='/logs' -e ZEPPELIN_NOTEBOOK_DIR='/notebook' \
           --name graphster wisecubeai/graphster
```
-u $(id -u) is to make sure you have the permission to write logs and notebooks.


## Open Zeppelin

In your local browser 
- Zeppelin: http://localhost:8088/#/

you might have to wait around 10 second for the zeppelin daemon to start, right after running the container.



## Run Sample Graphster Notebooks

TBD
  

## Changes in dockerfile
 
After changes in `Dockerfile` goto project home dir and run
```
docker build  -t wisecubeai/graphster-docker .
```

This repo is connected to an automated build in docker hub, so the following *no push* to docker hub is not required.
```
docker push  wisecubeai/graphster-docker
```


## Misc

### if container session is lost
```
docker start CONTAINER_ID
docker attach CONTAINER_ID
```

### run as background session
add `- d`
