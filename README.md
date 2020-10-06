# Swapping Docker Base Images
This is a work-in-progress project prompted by humoring a question from [this StackOverflow question](https://stackoverflow.com/q/64123071/14352161):

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
