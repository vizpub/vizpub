### Setting up the project
This project uses the [Gradle Automation Tool](www.gradle.org) in order
to build the project and automate task. Most modern IDE's come with
gradle support either built-in or through plug-ins. Look up the
documentation for your IDE for how to set up a gradle project.

### Installing Gephi 0.8.2 on Mac OS X 10.8 or newer
Unfortunately, installing Gephi on Mac OS X 10.8 or newer is not very
straightforward as Gephi is not compatible with JRE7:

1. Delete ``/Users/<your_username>/Library/Application Support/gephi/``
if it exists
2. Install [Apple's Java 6
Release](http://support.apple.com/kb/DL1572?viewlocale=en_US)
3. [Download Gephi](https://gephi.org/users/download/) and extract it to
the ``/Applications`` directory
4. Open
``/Applications/Gephi.app/Contents/Resources/gephi/etc/gephi.conf`` and
change the ``jdkhome`` line to
``jdkhome=/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home``
5. Launch Gephi from the Applications folder and generate a random graph
``File > Generate > Random Graph...`` to confirm its working

These steps are taken from [this
issue.](https://github.com/gephi/gephi/issues/895)

#### Caveats with Gephi
* Failing to delete the ``/Users/<your_username>/Library/Application
    Support/gephi/`` directory before installing gephi might cause it to
    work from the DMG but NOT from the Applications folder

    #### Increase memory limit in Gephi
    If Gephi reaches the maximum memory limit, it has to exit and all
    your
    work will be lost. It is recommended to increase the default memory
    size. You can increase them by editing the ``Xmx`` and ``Xms``
    parameters in the gephi.conf file. The conf file is found in the
    Application folder where you installed Gephi:

    `` <Application
    Folder>/Gephi.app/Contents/Resources/gephi/etc/gephi.conf``

    For more details see the [Gephi installation
    instructions](https://gephi.org/users/install/)
