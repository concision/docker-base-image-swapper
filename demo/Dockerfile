FROM openjdk:8-slim

# use several non-layer commands
LABEL maintainer="Author <email@example.com>"
EXPOSE 80/tcp 43/UDP
VOLUME /test/
CMD /scripts/test.sh
WORKDIR /scripts/
# intentionally mangling the $PATH variable that holds JDK8 path values
ENV PATH="/scripts/:${PATH}"

# add a file
RUN mkdir -p /scripts && \
    echo "#!/bin/bash" > /scripts/test.sh && \
    echo "echo Path: \${PATH}" >> /scripts/test.sh && \
    echo "echo Java Home: \${JAVA_HOME}" >> /scripts/test.sh && \
    echo "echo Java Version:" >> /scripts/test.sh && \
    echo "java -version" >> /scripts/test.sh && \
    chmod +x /scripts/test.sh
