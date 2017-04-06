# maven-plugins
Repository for Payara Maven plugins

## payara-micro-maven-plugin

Payara Micro Maven Plugin that incorporates payara-micro with the produced artifact

### Usage
The plugin is attached to the install phase by default. 

    <plugin>
        <groupId>fish.payara.maven.plugins</groupId>
        <artifactId>payara-micro-maven-plugin</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <executions>
            <execution>
                <goals>
                    <goal>bundle</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <startClass>my.custom.start.class.Main</startClass>
            <customJars>
                <artifactItem>
                    <groupId>org.primefaces</groupId>
                    <artifactId>primefaces</artifactId>
                    <version>6.0</version>
                </artifactItem>
            </customJars>
        </configuration>
    </plugin>

### Configuration tags

- autoDeployArtifact (optional | default: true): If the extension of the produced artifact is <b>war</b>, it will be copied automatically to ```MICRO-INF/deploy``` folder when this property is set to true.
- startClass (optional): Replaces ```Start-Class``` definition that resides in MANIFEST.MF file with the provided class.
- appendSystemProperties (optional | default: true): Appends all system properties defined into the ```payara-boot.properties``` file.
- payaraVersion (optional |  default: 4.1.1.171): By default ```bundle``` mojo fetches payara-micro with version 4.1.1.171.
- customJars (optional): Can contain a list of artifactItems, which defines the dependencies with their GAVs to be copied under ```MICRO-INF/lib``` folder.


### Goals
``
- bundle: 
Bundle mojo incorporates payara-micro with the produced artifact by following steps given as follows:
 
    - Fetch payara-micro from repository and open it to a folder. The default version is <i>4.1.1.171</i>. Specific version can be provided with ```payaraVersion``` parameter.
    - Fetch user specified jars from repository.
    - Copy any existing ```domain.xml```, ```keystore.jks```, ```login.conf``` and ```login.properties``` files from resources folder into ```/MICRO-INF/domain``` folder
    - Copy any existing ```pre-boot-commands.txt```, ```post-boot-commands.txt``` and ```post-deploy-commands.txt``` files from resources folder into ```/MICRO-INF``` folder
    - Copy produced artifact into ```/MICRO-INF/deploy``` folder if its extension is <b>war</b>.
    - Replace ```Start-Class``` entry in the manifest file with a custom bootstrap class if it's provided by user
    - Append system properties to ```MICRO-INF/payara-boot.properties```    
    - Bundle aggregated content as artifactName-microbundle.jar under target folder.