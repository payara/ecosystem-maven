# maven-plugins
Repository for Payara Maven plugins

## payara-micro-maven-plugin

Payara Micro Maven Plugin that incorporates payara-micro with the produced artifact. It requires JDK 1.7+.
 
### Latest version available: 1.0.1-SNAPSHOT

### bundle
This goal bundles the attached project's artifact into uber jar with specified configurations. ```bundle``` is attached to the ```install``` phase by default. A sample usage would as follows:

    <plugin>
        <groupId>fish.payara.maven.plugins</groupId>
        <artifactId>payara-micro-maven-plugin</artifactId>
        <version>${payaramicro.maven.plugin.version}</version>
        <executions>
            <execution>
                <goals>
                    <goal>bundle</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <startClass>my.custom.start.class.Main</startClass>
            <deployArtifacts>
                <artifactItem>
                    <groupId>org.mycompany</groupId>
                    <artifactId>my-project</artifactId>
                    <version>1.0</version>
                    <type>ear</type>
                </artifactItem>
            </deployArtifacts>            
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

- __autoDeployArtifact__ (optional | default: true): If the extension of the produced artifact is <b>war</b>, it will be copied automatically to ```MICRO-INF/deploy``` folder when this property is set to true.
- __startClass__ (optional): Replaces ```Start-Class``` definition that resides in MANIFEST.MF file with the provided class.
- __appendSystemProperties__ (optional | default: true): Appends all system properties defined into the ```payara-boot.properties``` file.
- __payaraVersion__ (optional |  default: 4.1.2.174): By default ```bundle``` mojo fetches payara-micro with version 4.1.2.174.
- __deployArtifacts__ (optional): Can contain a list of artifactItems, which defines the dependencies with their GAVs to be copied under ```MICRO-INF/deploy``` folder.
- __customJars__ (optional): Can contain a list of artifactItems, which defines the dependencies with their GAVs to be copied under ```MICRO-INF/lib``` folder.

### start
This goal start payara-micro with specified configurations. ```start``` is attached to the ```payara-micro``` phase. It can be executed as ```mvn payara-micro:start```. A sample usage would as follows:

    <plugin>
        <groupId>fish.payara.maven.plugins</groupId>
        <artifactId>payara-micro-maven-plugin</artifactId>
        <version>${payaramicro.maven.plugin.version}</version>
        <executions>
            <execution>
                <goals>
                    <goal>start</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <useUberJar>true</useUberJar>
            <daemon>true</daemon>
            <immediateExit>false</immediateExit>
            <javaPath>/path/to/Java/Home</javaPath>
            <payaraMicroAbsolutePath>/path/to/payara-micro.jar</payaraMicroAbsolutePath>
            <payaraVersion>4.1.2.174</payaraVersion>
            <artifactItem>
                <groupId>fish.payara.extras</groupId>
                <artifactId>payara-micro</artifactId>
                <version>4.1.2.174</version>
            </artifactItem>
            <deployWar>true</deployWar>
            <copySystemProperties>true</copySystemProperties>
            <commandLineOptions>
                <option>
                    <key>--domainconfig</key>
                    <value>/path/to/domain.xml</value>
                </option>
                <option>
                    <key>--autoBindHttp</key>
                    <value>true</value>
                </option>
            </commandLineOptions>
        </configuration>
    </plugin>

### Configuration tags

- __useUberJar__ (optional | default: false): Use created uber-jar that resides in ```target``` folder. The name of the jar artifact will be resolved automatically by evaluating its final name, artifact id and version. This configuration has the higher precedence (in given order) compared to ```payaraMicroAbsolutePath```, ```payaraVersion``` and ```artifactItem```.   
- __daemon__ (optional | default: false): Starts payara-micro in separate JVM process and continues with the maven build.
- __immediateExit__ (optional | default: false): If payara-micro is executed in ```daemon``` mode, the executor thread will wait for the ready message before shutting down its process. By setting ```immediateExit``` to ```true``` you can skip this and instantly interrupt the executor thread. 
- __javaPath__ (optional | default: "java"): Absolute path to the ```java``` executable.
- __payaraMicroAbsolutePath__ (optional): Absolute path to payara-micro executable.
- __payaraVersion__ (optional): default: 4.1.2.174): The payara-micro version that will be used with ```start``` mojo.
- __artifactItem__ (optional): Defines payara-micro artifact with its coordinates. Specified artifact should be available in local maven repository.
- __deployWar__ (optional | default: false): If the attached project is of type WAR, it will automatically be deployed to payara-micro if ```deployWar``` is set to ```true```. 
- __copySystemProperties__ (optional | default: false): Allows passing all system properties available within the maven build to the payara-micro execution.
- __commandLineOptions__ (optional): Defines a lists of command line options that will be passed onto payara-micro. Command line options can either be defined as key,value pairs or just as list of keys or values separately.


### stop
This goal stops payara-micro with specified configurations. By default this goal tries to find out currently executing payara-micro by checking the running uberjar. 
If an ```artifactItem``` is defined, it will take precedence for identifying currently running payara-micro. If ```processId``` is defined, this takes the highest precedence and goal immediately kills the executing payara-micro process. 
```stop``` is attached to the ```payara-micro``` phase. It can be executed as ```mvn payara-micro:stop```. A sample usage would as follows:

    <plugin>
        <groupId>fish.payara.maven.plugins</groupId>
        <artifactId>payara-micro-maven-plugin</artifactId>
        <version>${payaramicro.maven.plugin.version}</version>
        <executions>
            <execution>
                <goals>
                    <goal>stop</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <processId>32333</processId>
            <artifactItem>
                <groupId>fish.payara.extras</groupId>
                <artifactId>payara-micro</artifactId>
                <version>4.1.2.174</version>
            </artifactItem>
        </configuration>        
    </plugin>

### Configuration tags

- __processId__ (optional |): Process id of the running payara-micro.
- __artifactItem__ (optional): Defines payara-micro artifact with its coordinates. This information is used to identify the process id of the running payara-micro.
