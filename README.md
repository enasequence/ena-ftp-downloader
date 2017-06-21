# ena-ftp-downloader

JavaFX based GUI application for bulk downloading of run/analysis files from ENA FTP.


# License

Copyright 2017 EMBL - European Bioinformatics Institute Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at: http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

# Installation

Download the release .zip file and extract it to a location of your choice. The archive contains an executable jar which contains all dependencies.

# Dependencies

Requires Java 8.

# Execution
1. The jar is an executable jar. In Windows you should be able to run by just double-clicking on it.

or

2. Use the command line/shell
```
java -jar [path-to-file]/ena-ftp-downloader-[version].jar
```

# Usage

## Search Window
1. Accession search
Enter a valid accession into the Accession field and click Search. The Results window will be loaded with the available
files in Fastq, Submitted or SRA types.

2. Load a pre-generated File Report file.
Click the Load Report File button to select a previously generated report file. The file will be parsed and the available
file data will be listed in the Results window. 

## Results Window


### Requirements for the File Report
The Report file should contain at least one of the following file
URL columns.

* fastq_ftp
* submitted_ftp
* sra_ftp

Additionally, we recommend that when you generate the report file you include the support columns for whichever file
type/s you want to download.

i.e.

* fastq_bytes
* fastq_md5
* submitted_bytes
* submitted_md5
* sra_bytes
* sra_md5

The *_bytes column data allows the Downloader to track how much space is required for the download and to show the download
progress for each file.

The *_md5 column data allows the Downloader to calculate the MD5 checksum of the file after downloading it and compare it
to the original MD5 to verify that the file has been downloaded without errors.

The order of the columns in the report file is not important.

# Error Handling
1. In case of a failed/partial download, the Downloader will attempt to clean up any remnants. It is recommended that
you ensure the file download destination is cleaned of any partial files before you re-attempt downloads.
