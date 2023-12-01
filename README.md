Lighty release automation

how to run:

In lighty.io
1. Checkout on the branch where the release should be preformed
2. Remove all unversioned files

In lighty-releaser
1. mvn clean compile package
2. java -jar target/lighty-releaser-1.0-SNAPSHOT-jar-with-dependencies.jar /path/to/lighty/repository <current_version> <release_version> <next_dev_phase>