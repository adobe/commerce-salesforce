# AEM - Salesforce Commerce Cloud Integration

This wiki provides instructions to help you setup the integration between AEM and Salesforce Commerce Cloud. Please read the [README](https://github.com/adobe/commerce-salesforce/blob/master/README.md) for more information regarding this project.

We assume, that you do have some in-depth knowledge about AEM and at least basic Salesforce know-how already as this document can only describe the integration part between the two solutions.

Whenever we saw it necessary, we try to briefly describe features in both solutions to illustrate how they relate together. But that does not suffice to understand the solution as a whole. 

## SFCC Connector - Open Source - Past and Future 

The SFCC Connector started as a closed source program with Adobe. We soon found out, that the requirements from various customers varied widely - and one single solution would not fit to all these requirements.

Thus we decided to make the connector open source. Feel free to fork your own version and you are welcome to contribute your changes back to the open source version.

Contributions to the open source version shoud - of course - be backwards compatible. Ideally, any user of the connector should be able to just install an updated version of the connector without having to worry about migrating his installation or adapting the configurations. Services should provide sufficient default configurations to make the service self-sufficient. Code and configuration must be divided into different packages. Code must always be deployable without without overwriting configuration that already is customized.

## From Version 1.x to Version 2.0

The first major contribution to the Open Source version however had to break with the rules above. The version 2.0 is *not 100 % compatible* with the 1.x version for various reasons:

- The 1.0 version was delivered by Adobe as part of AEM. Thus, some of the code was deployed inot "/libs". Being an Open Source project now where everyone can contribute, we decided to move all content to "/apps". 

- In 1.0 code and configuration where closely coupled. That made it difficult to update the code when fixes where provided. We moved the configuration into separate packages as explained in [Modules Overview](5.2-Modules-Overview)

- A major driver for the 2.0 version was to introduce the capability to attach more than one SFCC instance to a single AEM system. This required some re-arranging of the OSGi configurations. [Multi Instance Support](8.3-Multiple-Instance) provides more details.

Thus said, please take some time to understand the new layout and reserve enough time when you plan to migrate from 1.x to 2.x.

> **Note**: This Wiki will focus on the 2.0 version of the conector. If you need access to an older revision of this documentation you can clone the Wiki an checkout the according version locally.

# Overview

This documentation is organized as follows.

## Introduction

The [Introduction](1.-Introduction) gives a brief  overview over the architecture of the integration so you can more easily follow along the following chapters.

## Replication Details

[AEM to SFCC Replication](2.-AEM-to-SFCC-Replication) goes deeper into the mechanics of the replication

## Configuration
The Configuration chapter helps you configure your connector setup. There are several sub-chapters dealing with

- [Service Configuration](4.1-Configuration)
- [Basic Setup Steps](4.1-Basic-Setup-and-Configuration)
- [Prepare the content tree](4.2-Content-Mapping)
- [Set up users and ACLs](4.3-Users-and-Permissions)


## Codebase

You probably have requirements you need to implement yourself. We try to give a quick overview over the Codebase:

- [Demandware ready components](5.1-SFCC-ready-Components) work a bit differently from traditional AEM components. Find out how to implement your own components.

- [Modules, Bundles and Build](5.2-Modules-Overview): Learn how to build the connector and what modules are in the project


## Preview Content

[Previewing Content](6.-SFCC-Content-Preview) in AEM in the context of an SFCC page can be a bit tricky. Learn how it is supposed to work.

## PIM

The connector also has an import facility to import product related data from SFCC into AEM. Most likely you want to use the images from your SFCC installation in AEM components. Visit the [PIM](7.-PIM) section to learn more.

## Advanced Setups

When you understood the basics, we will sketch some examples or  more complex setups. As examples you will find ideas how to

- set up AEM to synchronize to [Multiple Libraries for DAM](8.1-Multi-Library-Support-for-Assets)

- set up a structure to use [Multiple Regions and Languages]( 8.2-Multiple-Regions-and-Languages) in AEM with the Multi Site Manager and how that maps to Salesforce Commerce Cloud

- Attach [Multiple Saleforce Instances](8.3-Multiple-Instance) to a single AEM instance. This might come in handy, when you are operating a global business with SFCC servers distributed all over the world.

## Known Issues

We compiled a [Trouble shooting guide](9.1-Troubleshooting) to help you finding issues in your setup.

And there are a few [known issues and limitations](9.2-Known-Issues) you should know about, too. 