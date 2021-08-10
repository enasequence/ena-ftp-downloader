/*******************************************************************************

* Copyright 2021 EMBL-EBI, Hinxton outstation
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
  ******************************************************************************/

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
   inputs `java -jar ena-file-downloader.jar --accessions=SAMEA3231268,SAMEA3231287 --format= READS_FASTQ --location=C:\Users\Documents\ena --protocol=FTP --asperaLocation=null --email=datasubs@ebi.ac.uk`

* `--accessions` Comma separated list of accessions or file path to the accession list
* `--format` The format for the download (`eg : READS_FASTQ,READS_SUBMITTED,ANALYSIS_SUBMITTED,ANALYSIS_GENERATED`)
* `--location` The location for the download
* `--protocol` The protocol to be used for download.(`eg : FTP, ASPERA`)
* `--asperaLocation` The location of local Aspera Connect/CLI folder
* `--email` The email at which one wishes to receive the alert.(`eg : datasubs@ebi.ac.uk`)

2.Command to run jar file from Console without providing inputs `java -jar ena-file-downloader.jar`

The user will be prompted to provide inputs  (`eg : accessions, format, location, protocol, asperaLocation ,email`) and
in the end will be prompted with the below options :

* `To start downloading right now, and also create a script that can be invoked directly, please enter 1`
* `To create a script that can be invoked directly (e.g. by a pipeline or a script), enter 2`

If the user selects 1, then a script file will be created inside build\libs with the inputs received that can we invoked
directly and download will also be started. If the user selects 2, then a script file will be created inside build\libs
with the inputs received

### Design document
Please refer to the below document for design considerations:

#### Project information

* [cmd-line-downloader GitHub](https://github.com/enasequence/ena-ftp-downloader/tree/cmd-line-downloader)
* [JIRA ticket DCP-3013](https://www.ebi.ac.uk/panda/jira/browse/DCP-3013)
