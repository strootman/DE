FROM ubuntu:14.04

RUN useradd -m -U -s /bin/bash iplant
ADD bin/jex-events /bin/

ARG git_commit=unknown
ARG buildenv_git_commit=unknown
ARG version=unknown
LABEL org.iplantc.de.jex-events.git-ref="$git_commit" \
      org.iplantc.de.jex-events.version="$version" \
      org.iplantc.de.buildenv.git-ref="$buildenv_git_commit"

USER iplant
RUN mkdir -p /home/iplant/logs/
WORKDIR /home/iplant
EXPOSE 60000
ENTRYPOINT ["jex-events"]
CMD ["--help"]
