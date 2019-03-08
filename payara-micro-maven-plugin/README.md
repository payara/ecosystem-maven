# Payara Micro Maven Plugin

## Summary
Payara Micro Maven Plugin that incorporates payara-micro with the produced artifact. It requires JDK 1.8+.
 
### Latest version available: 1.0.4-SNAPSHOT

## bundle
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

## Configuration tags

- __autoDeployArtifact__ (optional | default: true): If the extension of the produced artifact is <b>war</b>, it will be copied automatically to ```MICRO-INF/deploy``` folder when this property is set to true.
- __startClass__ (optional): Replaces ```Start-Class``` definition that resides in MANIFEST.MF file with the provided class.
- __appendSystemProperties__ (optional | default: true): Appends all system properties defined into the ```payara-boot.properties``` file.
- __payaraVersion__ (optional |  default: 5.191): By default ```bundle``` mojo fetches payara-micro with version 5.191.
- __deployArtifacts__ (optional): Can contain a list of artifactItems, which defines the dependencies with their GAVs to be copied under ```MICRO-INF/deploy``` folder.
- __customJars__ (optional): Can contain a list of artifactItems, which defines the dependencies with their GAVs to be copied under ```MICRO-INF/lib``` folder.

## start
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
            <javaPath>/path/to/Java/Executable</javaPath>
            <payaraMicroAbsolutePath>/path/to/payara-micro.jar</payaraMicroAbsolutePath>
            <payaraVersion>5.191</payaraVersion>
            <artifactItem>
                <groupId>fish.payara.extras</groupId>
                <artifactId>payara-micro</artifactId>
                <version>5.191</version>
            </artifactItem>
            <deployWar>true</deployWar>
            <classpathArtifactItems>
                <artifactItem>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-api</artifactId>
                    <version>1.7.25</version>
                </artifactItem>
            </classpathArtifactItems>
            <javaCommandLineOptions>
                <option>
                    <value>-Xdebug</value>
                </option>
                <option>
                    <key>-Xrunjdwp:transport</key>
                    <value>dt_socket,server=y,suspend=y,address=5005</value>
                 </option>
            </javaCommandLineOptions>            
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
    
If you want to execute the payara-micro plugin along with ```maven-toolchains-plugin``` coooperatively, you need to execute the plugin as: ```mvn toolchains:toolchain payara-micro:start```.  

## Configuration tags

- __useUberJar__ (optional | default: false): Use created uber-jar that resides in ```target``` folder. The name of the jar artifact will be resolved automatically by evaluating its final name, artifact id and version. This configuration has the higher precedence (in given order) compared to ```payaraMicroAbsolutePath```, ```payaraVersion``` and ```artifactItem```.   
- __daemon__ (optional | default: false): Starts payara-micro in separate JVM process and continues with the maven build.
- __immediateExit__ (optional | default: false): If payara-micro is executed in ```daemon``` mode, the executor thread will wait for the ready message before shutting down its process. By setting ```immediateExit``` to ```true``` you can skip this and instantly interrupt the executor thread. 
- __javaPath__ (optional): Absolute path to the ```java``` executable. This has higher priority to the java executable identified via Maven toolchain.
- __payaraMicroAbsolutePath__ (optional): Absolute path to payara-micro executable.
- __payaraVersion__ (optional | default: "5.191"): The payara-micro version that will be used with ```start``` mojo.
- __artifactItem__ (optional): Defines payara-micro artifact with its coordinates. Specified artifact should be available in local maven repository.
- __deployWar__ (optional | default: false): If the attached project is of type WAR, it will automatically be deployed to payara-micro if ```deployWar``` is set to ```true```. 
- __copySystemProperties__ (deprecated): System properties propagate to the payara-micro execution by default so we deprecated and are ignoring this property from now on.
- __classpathArtifactItems__ (optional): Defines a list of artifact items with their GAV coordinates that will be passed to ```java``` executable as classpath parameter. If this tag contains at least one artifact item then execution of Payara Micro will be done with ```-cp``` instead of ```-jar```. The absolute file paths will be calculated and concatenated with OS specific path parameter. 
- __javaCommandLineOptions__ (optional): Defines a list of command line options that will be passed to ```java``` executable. Command line options can either be defined as key-value pairs or just as list of values. key-value pairs will be formatted as ``key=value``.
- __commandLineOptions__ (optional): Defines a list of command line options that will be passed onto payara-micro. Command line options can either be defined as key,value pairs or just as list of keys or values separately.


## stop
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
                <version>5.191</version>
            </artifactItem>
        </configuration>        
    </plugin>
    
If you want to execute the payara-micro plugin along with ```maven-toolchains-plugin``` coooperatively, you need to execute the plugin as: ```mvn toolchains:toolchain payara-micro:stop```.  

## Configuration tags

- __processId__ (optional): Process id of the running payara-micro.
- __artifactItem__ (optional): Defines payara-micro artifact with its coordinates. This information is used to identify the process id of the running payara-micro.
