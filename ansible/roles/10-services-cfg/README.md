10-services-cfg
===============

Role which generates and places DE service configuration files, logging 
configuration, and systemd files.

This role does not start nor stop any services.

Refer to the `meta/main.yaml` for the list of services.


Requirements
------------
Requires sudo.


Role Variables
--------------

N/A

Dependencies
------------

cfg-logging
cfg-service
cfg-systemd-unit

Example Playbook
----------------

    - hosts: services
      sudo: yes
      roles:
         - role: 10-services-cfg

License
-------

BSD

Author Information
------------------

Jonathan Strootman - jstroot@iplantcollaborative.org
