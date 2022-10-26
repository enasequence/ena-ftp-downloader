# Download latest version from 
http://ftp.ebi.ac.uk/pub/databases/ena/tools/ena-file-downloader.zip

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

1. Command to run jar file from Console without providing inputs `java -jar ena-file-downloader.jar`

The user will be prompted to provide inputs  (`eg : accessions, format, location, protocol, asperaLocation, email`) and
in the end will be prompted with the below options :

* `To start downloading right now, and also create a script that can be invoked directly, please enter 1`
* `To create a script that can be invoked directly (e.g. by a pipeline or a script), enter 2`

If the user selects 1, then a script file will be created with the provided arguments, that can be invoked
directly, and download will also be started. If the user selects 2, then only the script file will be created.

2. Command to run jar file from Console by providing
   inputs `java -jar ena-file-downloader.jar --accessions=SAMEA3231268,SAMEA3231287 --format=READS_FASTQ --location=C:\Users\Documents\ena --protocol=FTP --asperaLocation=null --email=email@youremail.com`
3. Command to run jar file from Console to download files from data hub
   `java -jar ena-file-downloader.jar --accessions=SAMEA3231268,SAMEA3231287 --format=READS_FASTQ --location=C:\Users\Documents\ena --protocol=FTP --asperaLocation=null --email=email@youremail.com --dataHubUsername=dcc_abc --dataHubPassword=*****`

* `--accessions` Comma separated list of accessions or file path to the accession list. If providing a list, it should
  be a plain text file in TSV (tab separated values) format. If there are more than one column, the first column must be
  the accessions. Header row is optional and will be ignored. Values can be enclosed in double quotes or not.
* `--format` The format for the download (`eg : READS_FASTQ,READS_SUBMITTED,ANALYSIS_SUBMITTED,ANALYSIS_GENERATED`)
* `--location` The location for the download
* `--protocol` The protocol to be used for download.(`eg : FTP, ASPERA`). Default is FTP.
* `--asperaLocation` The location of local Aspera Connect/CLI folder. Required if Protocol is Aspera.
* `--email` The email at which one wishes to receive the alert.(optional)
* `--dataHubUsername` Data hub username. (Required only If you want to download the data from a data hub (dcc))
* `--dataHubPassword` Data hub password. (Required only If you want to download the data from a data hub (dcc))


