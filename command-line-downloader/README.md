# Getting Started

### Summary
User runnable tool for getting data files idempotently and resiliently from ENA

The application logs will be written to logs\app.log

### Build

Install JDK8 and use the Gradle wrapper to build the project:

    ./gradlew build

The project jar file will be available at `build/libs/`.

### Run:

There are two ways to run the tool : 
1. Command to run jar file from Console by providing
   inputs `java -jar cdp-file-downloader-0.0.1.jar --domain=VIRAL_SEQUENCES --datatype=STUDIES --format=FASTQ --location=C:\Users\Documents\ena --email=datasubs@ebi.ac.uk`

* `--domain` The domain for the download (`eg : VIRAL_SEQUENCES,HOST_SEQUENCE`)
* `--datatype` The datatype for the
  download (`eg : SEQUENCES,REFERENCE_SEQUENCES,RAW_READS,SEQUENCED_SAMPLES,STUDIES,HUMAN_READS,OTHER_SPECIES_READ`)
* `--format` The format for the download (`eg : EMBL,FASTA,XML,FASTQ,SUBMITTED`)
* `--location` The location for the download
* `--email` The email at which one wishes to receive the alert.(`eg : datasubs@ebi.ac.uk`)
* `--protocol` The protocol to be used for download.(`eg : FTP, ASPERA`)
* `--asperaLocation` The location of local Aspera Connect/CLI folder

2.Command to run jar file from Console without providing inputs `java -jar cdp-file-downloader-0.0.1.jar`

The user will be prompted to provide inputs  (`eg : domain, datatype, format, location, email`) and in the end will be
prompted with the below options :

* `To create a script that can be invoked directly (e.g. by a pipeline or a script), please enter 1`

* `To start downloading right now, please enter 2`

* `To start downloading right now, and also create a script that can be invoked directly, please enter 3`

If the user selects 1, then a script file will be created inside build\libs with the inputs received that can we invoked directly.
If the user selects 2, then download will start right away.

### Design document
Please refer to the below document for design considerations:

https://drive.google.com/file/d/1oj_wG7NAVjO24Mc_JZb3WPTVeiGPcma6/view?usp=sharing
#### Project information
* [cv19-file-downloader GitLab](https://gitlab.ebi.ac.uk/c19portal/cv19-file-downloader)
* [JIRA ticket DCP-2884](https://www.ebi.ac.uk/panda/jira/browse/DCP-2884)
