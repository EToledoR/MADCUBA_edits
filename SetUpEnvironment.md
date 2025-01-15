## Setting up the development environment for MADCUBA
This document aims to cover how to setup a mainstream environment to contribute to the MADCUBA project. The original potential public for this is the internal developers at Centro de Astrobiologia (CAB) in Madrid. 
Eventually, this document will cover how possible contributors can make contrbutions to the project and it will include style guides and instructions. 

MADCUBA is a legacy project and because of that it has very specific dependencies on old versions of IDEs, JDKs and Libraries that we will detail in this document. 

### Prerequisites
In order to run MADCUBA and the adequate development environment, we need to install the jdk 1.8
You can install multiple versions of java (in a Linux-based environment) and switch between them according to your needs, so you can just got installed this old version of the jdk when some coding in MADCUBA is needed.

To install and switch between multiple versions of java: 

- Simply install as many versions of java as you need with sudo apt install.
- Then, with ```update-java-alternatives --list``` you can get a list of all the installed versions.
- And with ```sudo update-java-alternatives --set /path/to/java/version``` where the path to the specific version has been detailed in the previous list.
- Remember that at any given moment you can check which java version are you using by typing in the terminal ```java -version```

### IDE
The recommended development environment to collaborate with MADCUBA is Eclipse, a particular release: 2019-09 R (4.13.0) build id: 20190917-2100. 
This environment is only recomended and the recommendation on the particular version is mostly a suggestion. Any setup that allows you to run a middle-size Java project shoud work as long as it is using the jre 1.8. 

MADCUBA repository is hosted inside the CAB private network in a Subversion instance. So, in order to be able to contribute and retrieve the changes in the code, we need a plugin for subversion installed in Eclipse. 

We recommend the installation of **Subversive SVN Team Provider 4.8**. To install it:

- In Eclipse go to Help > Eclipse Market Place and search for SVN , as per today (Jan 2025) the only available one is the one we suggested.
- Alternatively you can go to Help > Install new software

Asides from the SVN plugin you would need svn connectors, those connectors again are sort of legacy code and they are rehosted over time. Currently (Jan 2025), they are hosted here:


https://osspit.org/eclipse/subversive-connectors/

Again, to install them go to Help > Instal new Software and type this url as source. 

### Getting the code
Once you got the svn plugin and the connector you can get the latest version of the code from the repository from the url:

https://projects2.cab.inta-csic.es/svn/lam/trunk/MADCUBA_IJ

The correct access and user and password need to be provided by IT Services at CAB. 

MADCUBA codebase is held in three separate java projects:

- External Libraries
- MADCORE
- MADCUBA PLUGINS

You need to go in Eclipse to File > New > Project  and then in the wizard choose from svn and retrieve one by one the above projects in that specific order, as each one is dependant on the previous one. 

When the project is found and you start to get the code, there would be a wizard to ask you how to configurate the project and there you need to choose as a Java application. 

### Setting up the projects
In the External Libraries one there is nothing else to do. In this case the repository just hold the specific version of the libraries that we need so MADCUBA can run properly. 

In MADCORE you need to add the libraries from 'External Libraries' but you need to do it in a specific order:

1. ij.jar (versión 1.51w)
2. commons-compress-1.9.jar
3. swingx-all-1.6.4.jar
4. stil.jar
5. need_topcat.jar
6. log4j-1.2.15.jar
7. jsr-275.jar
8. hsqldb.jar
9. numeric.jar
10. hcss.jar
11. hifi.jar
12. nom_tam_fits_v1_11_1.jar
13. skyview_v2_2_nuestra.jar
14. hcssExtLibs.jar
15. Image_5D.jar
16. imgenes.jar

