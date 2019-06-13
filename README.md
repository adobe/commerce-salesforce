Adobe AEM Commerce - Salesforce Commerce integration
===========================================

This project has a number features to integrate Adobe AEM with Salesforce Commerce platform.

This module is provided as two seperate packages - *cq-commerce-demandware-content* containing the core integration and
*cq-commerce-demandware-sample-content* providing the sample content based on the Salesforce Commerce SiteGenesis demo site.


### Features
* Create, maintain and publish AEM pages as content assets on the Salesforce Commerce instance
  * Body content is created out of AEM page content
  * Control meta attributes of the content asset directly from AEM page properties
  * Supports MSM to manage multi-site / multi-region / multi-language setup's
* Create, maintain and publish Salesforce Commerce content slot configuration's from within AEM
* Publish AEM pages as Salesforce Commerce rendering templates (using Velocity markup)
* Publish AEM assets to Salesforce Commerce
* Live preview of AEM page content with dynamic catalog and product information
* Connects to Salesforce Commerce via OCAPI and WebDav

### Installation

This project supports AEM 6.4 and later versions and requires access to an Salesforce Commerce instance (developer sandbox will work).

* Start AEM 6.4 author instance
* Install the main connector content package `cq-commerce-demandware-content`
* Optionally install the sample content package `cq-commerce-demandware-sample-content`
* Configure the connector to connect to your Saleforce Commerce instance (see below)

### Build

The project has the following requirements:

* Java SE Development Kit 8
* Apache Maven 3.3.1 or newer

For ease of build and installation the following profiles are provided:
* autoInstallPackage - installs the package and embedded bundles to an existing AEM author instance
