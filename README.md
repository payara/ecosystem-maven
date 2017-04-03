# maven-plugins
Repository for Payara Maven plugins

## payara-micro-maven-plugin

Payara Micro Maven Plugin that incorporates payara-micro with the produced artefact

### Goals

- bundle: 
Bundle mojo incorporates payara-micro with the produced artefact by following steps given as follows:
 
    - Fetch payara-micro from repository and open it to a folder. The default version is <i>4.1.1.171</i>. Specific version can be provided with ```payaraVersion``` parameter.
    - Fetch user specified jars from repository.
    - Copy produced artefact into /MICRO-INF/deploy folder if its extension is <b>war</b>.
    - Copy any existing ```domain.xml```, ```keystore.jks```, ```login.conf``` and ```login.properties``` file from resources folder into /MICRO-INF/domain folder
    - Bundle aggregated content as artefactName-microbundle.jar under target folder.