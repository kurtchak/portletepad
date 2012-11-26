Welcome to PortletEpad
======================

This document describes what is PortletEpad and how you can configure and deploy it to a GateIn Portal Portlet Container.

What is PortletEpad?
====================

PortletEpad is a portlet application providing tool for collaborative editing of text documents.
Basically, it is a simple text editor that allows two or more remote users edit same documents simultaneously.

Prerequisities
==============

  - GateIn Portal 3.5.0.Beta02 running on JBoss AS 7.1.1

Configuration
=============

1.1 Portlet container
---------------------
    
PortletEpad uses the portlet services provided by the GateIn Portal and JMS provided by the downlying JBoss AS.
Brief configuration must be thus provided:

JMS

- JMS Topic Configuration

In order to use JMS, the messaging subsystem must be declared as used within the JBoss AS configuration file standalone.xml or standalone-full.xml with all modules turned on.
You can use the exact parts from standalone-full.xml with small modification in a security-settings part:

  1.  Security settings

      <subsystem xmlns="urn:jboss:domain:messaging:1.1">
          <security-settings>
              <security-setting match="#">
                  <permission type="send" roles="PortletepadUser"/>
                  <permission type="consume" roles="PortletepadUser"/>
                  <permission type="createNonDurableQueue" roles="PortletepadUser"/>
                  <permission type="deleteNonDurableQueue" roles="PortletepadUser"/>
              </security-setting>
          </security-settings>

  2. The JMS destinations must be configured with the exact names. Copy this code to other JMS destinations:

          <jms-destinations>
              ...
              <jms-topic name="padTopic">
                  <entry name="jms/topic/padTopic"/>
                  <entry name="java:jboss/exported/jms/topic/padTopic"/>
              </jms-topic>
              <jms-topic name="push">
                  <entry name="topic/push"/>
                  <entry name="java:jboss/exported/jms/topic/push"/>
              </jms-topic>
          </jms-destinations>

  Other Setting concerning the JMS configuration let set as in the referencial standalon-full.xml.

- JMS User
  
In order to leverage the functionality of JMS topics, the user with appropriate role must created within the JBoss AS.

    $ cd $JBOSS_HOME
    $ ./bin/add-user.sh
    
    1. Create Application User with ApplicationRealm and following credentials:
    
        name = kurtcha
        pass = portletepad
  
    2. Assign role PortletEpad for this user in the next step.

Build and deploy
================

    1. Build and install application using maven command:

        $ cd path/to/portletepad
        $ mvn install

    2. Once the war is created you can deploy it to GateIn Portal Portlet Container by copying it to underlying JBoss AS deploy folder:
        
        $ cp target/portletepad.war path/to/GateInPortal/standalone/deployments/

Using application
=================

  As the application is pretty simple and intuitive, so once you are logged in, you can create, delete and edit pads. Enjoy.
