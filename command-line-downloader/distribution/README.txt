/*******************************************************************************
 * Copyright 2021 EMBL-EBI, Hinxton outstation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

version:1.0.0
Ena File Downloader
Copyright © EMBL 2021 | EMBL-EBI is part of the European Molecular Biology Laboratory

For support/issues, please contact us at https://www.ebi.ac.uk/ena/browser/support

There are two ways to run the tool :

1. Interactive : Use this the first time you run the tool, to navigate through the available options and create a simple
script that can be invoked directly later
java -jar ena-file-downloader.jar

OR call one of the convenience scripts provided.

Linux/Unix:
./run.sh
You may need to give the script permissions to be runnable as follows:
chmod + run.sh

On Windows:
run.bat

The user will be prompted to select options  (`eg : accessions, format, location, protocol, aspera connect location,email`) and in the end will be
prompted with the below options :

* `To start downloading right now, and also create a script that can be invoked directly, please enter 1`

*  `To create a script that can be invoked directly (e.g. by a pipeline or a script), enter 2`

If the user selects 1, then download will start right away in the current console, but also create a script.
If the user selects 2, then a script file will be created inside the current folder with the inputs received that can be invoked directly.

2. Non-interactive: Provide parameters to the tool directly. It is recommended you use Interactive mode to create a
script and modify it if required.

java -jar ena-file-downloader.jar --accessions=SAMEA3231268,SAMEA3231287 --format= READS_FASTQ --location=C:\location --protocol=FTP --asperaLocation=null --email=youremail@email.com

* `--accessions` comma separated list of accessions or the file path with accessions that one wishes to download.
* `--format` The format for the download (`eg : READS_FASTQ,READS_SUBMITTED,ANALYSIS_SUBMITTED,ANALYSIS_GENERATED`)
* `--location` The location for the download
* `--protocol` The protocol to be used for download.(`eg : FTP, ASPERA`)
* `--asperaLocation` The location of local Aspera Connect/CLI folder. Required if protocol is Aspera.
* `--email` (Optional) The email at which one wishes to receive an alert on completion.

Privacy Notice
The execution of this tool may require limited processing of your personal data to function. By using this tool you are agreeing to this as outlined in our
Privacy Notice: https://www.ebi.ac.uk/data-protection/privacy-notice/ena-presentation
and
Terms of Use: https://www.ebi.ac.uk/about/terms-of-use.

This software is authored by EMBL-EBI and distributed as is.
License: https://www.apache.org/licenses/LICENSE-2.0
