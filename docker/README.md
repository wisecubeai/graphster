# Graphster Quickstart Docker 

This Docker image is a single-node deployment of Graphster's 100% open-source distribution including Apache Spark, and Apache Zeppelin. They are ideal environments for learning about Graphster, trying out new ideas, testing and demoing your application.

This docker image provides a local *spark* installation of the *graphster* library and example notebooks.
It is uploaded in [dockerhub](https://hub.docker.com/r/wisecubeai/graphster/) in a public repository.


## Introduction to Docker
Docker is different from other platforms you may have worked with, because it uses Linux containers. Most "virtual machines" work by isolating or simulating access to the host's hardware so an entire guest operating system can run on it. Linux containers, however, work by partitioning the resources of the host operating system: they have their own view of the filesystem and other resources, but they are running on the same kernel. This is similar to BSD jails or Solaris zones. Docker provides tooling, a packaging format, and infrastructure around Linux containers and related technologies.

Docker is well supported in several recent Linux distributions. For instance, on Ubuntu 14.04, it can be installed as follows:
```
  sudo apt-get install docker.io
```

## Importing the Graphster QuickStart Image
You can import the Graphster QuickStart image from Docker Hub:
```
  docker pull wisecubeai/graphster:latest
```

## Build the container image
```
  docker build -t wisecubeai/graphster .
```

## Components
- Spark version="3.1.2"
- Zeppelin version="0.10.0"
- Hadoop version="3.2"
 
## Running a Graphster QuickStart Container
To run a container using the image, you must know the name or hash of the image. If you followed the Importing instructions above, the name could be wisecubeai/graphster:latest (or something else if you have multiple versions downloaded). The hash is also printed in the terminal when you import, or you can look up the hashes of all imported images with:

```
  docker run -p 8080:8080 --rm --name graphster wisecubeai/graphster:latest
```

To persist logs and notebook directories, use the volume option for docker container.
```
docker run -u $(id -u) -p 8080:8080 --rm -v $PWD/logs:/logs -v $PWD/notebook:/notebook \
           -e ZEPPELIN_LOG_DIR='/logs' -e ZEPPELIN_NOTEBOOK_DIR='/notebook' \
           --name graphster wisecubeai/graphster
```
-u $(id -u) is to make sure you have the permission to write logs and notebooks.


## Open Graphster

In your local browser 
- Graphster: http://localhost:8088/#/

you might have to wait around 10 second for the graphster daemon to start, right after running the container.


## Run Sample Graphster Notebooks

Navigate to 'Graphster Tutorials' to launch and run example graphster notebooks
  


## Build your own Changes in dockerfile
 
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
Connecting To The Shell
If you do not pass the -d flag to docker run, your terminal will automatically attach to that of the container.

A container will die when you exit the shell, but you can disconnect and leave the container running by hitting Ctrl+P -> Ctrl+Q.

If you have disconnected from the shell or did pass the -d flag, you can connect to the shell later with:
```
docker start CONTAINER_ID
docker attach CONTAINER_ID
```

### run as background session
add `- d`
