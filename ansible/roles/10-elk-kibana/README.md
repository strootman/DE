10-elk-kibana
=============

For deploying and configuring the Kibana instance for the DE ELK stack.

Requirements
------------

Requires sudo.

Role Variables
--------------

TBD

Dependencies
------------

cfg-systemd-unit

Example Playbook
----------------

    - hosts: elk
      roles:
         - role: 10-elk-kibana

License
-------

BSD

Author Information
------------------

Jonathan Strootman - jstroot@iplantcollaborative.org
