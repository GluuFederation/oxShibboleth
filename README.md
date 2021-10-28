# oxShibboleth

# About The Project 

  This project contains modification and installation files for Gluu's Shibboleth IDP 
implementation. It works out of the box by delegating authentication to oxAuth
, working in tandem with the latter to provide outbound SAML Single Sign-On (SSO). 
This also comes with the advantage of sharing  useful information between the 
SAML IDP and oxAuth e.g. the entitID of the Relying Party. 

# Project Structure 

The project is organized into a a series of subproject/modules which each have different 
uses. 

- `idp-conf`. Since IDP 4.1.x  , the Shibboleth IDP project moved almost all system 
   configuration xml files into jars, and made them classpath loadable. 
   Given how somewhat limiting this can be as it is sometimes required to modify core system 
   configuration files for some specific functionalities, this module was created to hold 
   such modifications and act as an in-replacement jar for Shibboleth IDP's core configuration jar. 
   It is to note however that the aim would be to retire and completely remove this module as 
   better ways are found to add functionality without modifying core system files in Shibboleth IDP.
- `keygenerator`. This module is a tiny application which was previously used to generate the 
   cryptographic material for Shibboleth IDP's component called the "DataSealer". 
   Given this functionality exists in Shibboleth IDP itself , the setup tool now relies on that
   fact to generate the required cryptographic material.
- `oxLogoServlet`. This module is a simple servlet used to serve the logo used in various pages 
   displayed by Shibboleth IDP during authentication.
- `oxShibbolethWebApp`. This module unpacks the Shibboleth IDP war and repacks it with all the custom 
   modifications (mostly in the form of jars) required to make Shibboleth IDP work in tandem with oxAuth.
- `shib-oxauth-authn`. This module contains the plugin which delegates authentication from Shibboleth IDP
   to oxAuth. 
- `static`. This module contains Shibboleth IDP configuration file changes. The changes are mostly in the 
   form of patch files. More on that later. 

# Prerequisites 

The current mininum supported Shibboleth IDP version is 4.1.4 

#  Building The Project

The project is a java based project, and uses the maven tool for builds. To build the project, open a terminal 
,set it's current directory to the project's directory, and run `mvn package`


# Additional Notes

This section will contains additional information about performing specific tasks when working on this project. 

## Creating a new configuration patch file 

  As mentionned above , the `static` module contains Shibboleth IDP configuration file changes. Those changes are stored 
as patch files. Patch files names are usually of the format `xxx.<patch-content-description>.patch` where `xxx` is a number
between 000 and 999 and `patch-content-description` is a short description of what changes were made to the configuration file.
Make sure to choose a unique number, which can be verified by checking the existing patches in `oxShibboleth/static/src/patches`.
Here are the steps to make configuration changes and create patch files. 

1. Clone the project. It will be assumed it will be in a directory named `oxShibboleth`
1. Build the project in it's current state. This will apply the existing patches. 
1. Make a copy of `oxShibboleth/static/target/classes/shibboleth-idp`. This will contain the Shibboleth IDP 
   configuration file with the patches applied. Rename it to `shibboleth-idp.orig` 
1. Apply your changes to the `shibboleth-idp` directory. 
1. Open a terminal whose current directory points to the directory where both `shibboleth-idp.orig` and `shibboleth-idp` reside 
   then run `diff -aurN shibboleth-idp.orig shibboleth-idp > xxx.<patch-content-description>.patch`
1. Move the generated patch file to `oxShibboleth/static/src/patches`