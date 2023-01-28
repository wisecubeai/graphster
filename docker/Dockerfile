
# Licensed to Wisecube AI under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# Wisecube licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM ubuntu:23.04


ENV Z_VERSION="0.10.0"
ARG SPARK_VERSION="3.1.2"
ARG HADOOP_VERSION="3.2"

LABEL maintainer="Wisecube AI  <insights@wisecube.ai>"
LABEL zeppelin.version=${Z_VERSION}
LABEL spark.version=${SPARK_VERSION}
LABEL hadoop.version=${HADOOP_VERSION}


ENV LOG_TAG="[ZEPPELIN_${Z_VERSION}]:" \
    ZEPPELIN_HOME="/opt/zeppelin" \
    HOME="/opt/zeppelin" \
    LANG=en_US.UTF-8 \
    LC_ALL=en_US.UTF-8 \
    JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 \
    ZEPPELIN_ADDR="0.0.0.0"

RUN echo "$LOG_TAG install basic packages" && \
    apt-get -y update && \
    # Switch back to install JRE instead of JDK when moving to JDK9 or later.
    DEBIAN_FRONTEND=noninteractive apt-get install -y locales language-pack-en tini openjdk-8-jdk-headless wget curl less vim unzip && \
    # Cleanup
    rm -rf /var/lib/apt/lists/* && \
    apt-get autoclean && \
    apt-get clean

# Install conda to manage python and R packages
ARG miniconda_version="py37_4.9.2"
# Hashes via https://docs.conda.io/en/latest/miniconda_hashes.html
ARG miniconda_sha256="79510c6e7bd9e012856e25dcb21b3e093aa4ac8113d9aa7e82a86987eabe1c31"
# Install python and R packages via conda
COPY env_python_3_with_R.yml /env_python_3_with_R.yml



RUN set -ex && \
    wget -nv https://repo.anaconda.com/miniconda/Miniconda3-${miniconda_version}-Linux-x86_64.sh -O miniconda.sh && \
    echo "${miniconda_sha256} miniconda.sh" > anaconda.sha256 && \
    sha256sum --strict -c anaconda.sha256 && \
    bash miniconda.sh -b -p /opt/conda && \
    export PATH=/opt/conda/bin:$PATH && \
    conda config --set always_yes yes --set changeps1 no && \
    conda info -a && \
    conda install mamba -c conda-forge && \
    mamba env update -f /env_python_3_with_R.yml --prune && \
    # Cleanup
    rm -v miniconda.sh anaconda.sha256  && \
    # Cleanup based on https://github.com/ContinuumIO/docker-images/commit/cac3352bf21a26fa0b97925b578fb24a0fe8c383
    find /opt/conda/ -follow -type f -name '*.a' -delete && \
    find /opt/conda/ -follow -type f -name '*.js.map' -delete && \
    mamba clean -ay
    # Allow to modify conda packages. This allows malicious code to be injected into other interpreter sessions, therefore it is disabled by default
    # chmod -R ug+rwX /opt/conda
ENV PATH /opt/conda/envs/python_3_with_R/bin:/opt/conda/bin:$PATH

RUN echo "$LOG_TAG Download Zeppelin binary" && \
    mkdir -p ${ZEPPELIN_HOME} && \
    wget -nv -O /tmp/zeppelin-${Z_VERSION}-bin-all.tgz https://archive.apache.org/dist/zeppelin/zeppelin-${Z_VERSION}/zeppelin-${Z_VERSION}-bin-all.tgz && \
    tar --strip-components=1 -zxvf  /tmp/zeppelin-${Z_VERSION}-bin-all.tgz -C ${ZEPPELIN_HOME} && \
    rm -f /tmp/zeppelin-${Z_VERSION}-bin-all.tgz && \
    chown -R root:root ${ZEPPELIN_HOME} && \
    mkdir -p ${ZEPPELIN_HOME}/logs ${ZEPPELIN_HOME}/run ${ZEPPELIN_HOME}/webapps && \
    # Allow process to edit /etc/passwd, to create a user entry for zeppelin
    chgrp root /etc/passwd && chmod ug+rw /etc/passwd && \
    # Give access to some specific folders
    chmod -R 775 "${ZEPPELIN_HOME}/logs" "${ZEPPELIN_HOME}/run" "${ZEPPELIN_HOME}/notebook" "${ZEPPELIN_HOME}/conf" && \
    # Allow process to create new folders (e.g. webapps)
    chmod -R 777 ${ZEPPELIN_HOME} && \
    chmod -R 775 /opt/conda

COPY log4j.properties ${ZEPPELIN_HOME}/conf/
COPY log4j_docker.properties ${ZEPPELIN_HOME}/conf/
COPY log4j2.properties ${ZEPPELIN_HOME}/conf/
COPY log4j2_docker.properties ${ZEPPELIN_HOME}/conf/



##########################################
# SPARK
##########################################
ARG SPARK_ARCHIVE=https://archive.apache.org/dist/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz
RUN mkdir /usr/local/spark &&\
    mkdir /tmp/spark-events  &&\
    chmod -R 777 /tmp/spark-events   # log-events for spark history server
ENV SPARK_HOME /usr/local/spark

ENV PATH $PATH:${SPARK_HOME}/bin
RUN curl -s ${SPARK_ARCHIVE} | tar -xz -C  /usr/local/spark --strip-components=1
COPY spark-defaults.conf ${SPARK_HOME}/conf/



USER 1000

EXPOSE 8080

ENTRYPOINT [ "/usr/bin/tini", "--" ]
WORKDIR ${ZEPPELIN_HOME}
CMD ["bin/zeppelin.sh"]