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

version:unspecified

Ena File Downloader

Copyright © EMBL 2021 | EMBL-EBI is part of the European Molecular Biology Laboratory

For support/issues, please contact us at https://www.ebi.ac.uk/ena/browser/support

There are two ways to run the tool :

1. Interactive : Use this the first time you run the tool, to navigate through the available options and create a simple
   script that can be invoked directly later java -jar ena-file-downloader.jar

OR call one of the convenience scripts provided.

Linux/Unix:
./run.sh You may need to give the script permissions to be runnable as follows:
chmod + run.sh

On Windows:
run.bat

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

2. Command to run jar file from Console by providing inputs `java -jar ena-file-downloader.jar --accessions=SAMEA3231268,SAMEA3231287 --format=READS_FASTQ --location=C:\Users\Documents\ena --protocol=FTP --asperaLocation=null --email=email@youremail.com`
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

Please enclose the inputs within double quotes if it contains spaces. For eg:
`java -jar ena-file-downloader.jar --accessions=SAMEA3231268,SAMEA3231287 --format=READS_FASTQ --location="C:\Users\Documents\ena ebi" --protocol=FTP --asperaLocation=null --email=email@youremail.com`

Privacy Notice The execution of this tool may require limited processing of your personal data to function. By using
this tool you are agreeing to this as outlined in our Privacy
Notice: https://www.ebi.ac.uk/data-protection/privacy-notice/ena-presentation
and Terms of Use: https://www.ebi.ac.uk/about/terms-of-use.

This software is authored by EMBL-EBI and distributed as is. License: https://www.apache.org/licenses/LICENSE-2.0