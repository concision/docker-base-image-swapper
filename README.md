# Swapping Docker Base Images

This is a work-in-progress proof-of-concept project that enables swapping the base image of an already-built application Docker image. While normally Docker images _should_ be rebuilt from sources using a different base image, there are some scenarios where this is not possible. This CLI tool directly manipulates Docker image archives to change the base image (bottom layers) of an existing image, without building the image from the original application sources.

The bottom layers in the application are swapped out, and all configuration that is inherited from the old base image is reconciled with the new base image. The container configuration will be updated to inherit the following command configurations from the new base image: `CMD`, `ENTRYPOINT`, `ENV`, `EXPOSE`, `LABEL`, `USER`, `VOLUME`, `WORKDIR`.

> This process is error-prone and may have various bugs; furthermore, the tool is not written terribly well. **Use at your own risk.**

## Prompt
This project was prompted by humoring a question from [this StackOverflow question](https://stackoverflow.com/q/64123071/14352161):

> I am wondering if it is possible to swap out a layer of a container for another.  Here is my scenario:
> 
> I have a docker file that does the following:
> 
> 1.  It pulls the .net core 3.1 debian slim (buster) image
> 2.  Builds the source code of my application and adds it as a layer on top of the .net core image
> 
> When a new version of the .net core 3.1 runtime image comes out, I would like to make a new image that has that new version, but has the same application layer on top of it.
> 
> I do NOT want to have to find the exact version of the code I used to build the application and re-build.
> 
> The idea is to replicate upgrading the machine and runtime, but not have any alteration to the application (less to test with the upgrade).
> 
> **Is there a docker command that I can use to swap out a layer of an image?**

## Demonstration

A demonstration of this can be project can be run in Bash with the following command (in the project's root directory):
```
./demo/demo.sh
```
> Note: The only requirements are Bash and Docker

A single image is built using `openjdk:8-slim`, and the tool upgrades the base to `openjdk:11-slim`. The `Dockerfile` prints `$PATH`, `$JAVA_HOME`, and the Java version. This generates the following output when running the image (once on the original built image, and another on the base-swapped image):
```
[TEST] Output from running original image:
Path: /scripts/:/usr/local/openjdk-8/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
Java Home: /usr/local/openjdk-8
Java Version:
openjdk version "1.8.0_265"
OpenJDK Runtime Environment (build 1.8.0_265-b01)
OpenJDK 64-Bit Server VM (build 25.265-b01, mixed mode)
[TEST] Output from running swapped-base image:
Path: /scripts/:/usr/local/openjdk-11/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
Java Home: /usr/local/openjdk-11
Java Version:
openjdk version "11.0.8" 2020-07-14
OpenJDK Runtime Environment 18.9 (build 11.0.8+10)
OpenJDK 64-Bit Server VM 18.9 (build 11.0.8+10, mixed mode)
```
