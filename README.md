# ena-ftp-downloader

Graphical user interface (GUI) for bulk downloading of run/analysis files from ENA via FTP.


# License

Copyright 2017 EMBL - European Bioinformatics Institute Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
You may obtain a copy of the License at: http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

# Installation

Download the release .zip file and extract it to a location of your choice. The archive contains an executable jar which contains all dependencies.

# Dependencies

Requires [Java 8 runtime environment](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html).

# Execution

The jar is an executable file. In Windows you should be able to run it via a double-click. Alternatively, on all platforms you can use the command line:
```
java -jar [path-to-file]/ena-ftp-downloader-[version].jar
```

# Usage

## Accession search

Enter a valid study, project, run, experiment, analysis, submission or sample accession into the Accession field and click Search. The Results window will be loaded with the available files in Fastq, Submitted or SRA types (one tab for each type).

## Load pre-generated File report

Click the Load Report File button to select a previously generated report file. The file will be parsed and the available
file data will be listed in the Results window.

### Requirements

1. The report file is a plain text file with the contents in Tab Separated fomat

2. The report file must contain the column headers

3. The report file should contain at least one of the following file URL columns.
- fastq_ftp
- submitted_ftp
- sra_ftp

4. Additionally, we recommend that when you generate the report file you include the support columns for whichever file
type/s you want to download. Note that if these columns are not available in the report, the FTP Downloader will not be able to display progress bars and do file integrity checks.
- [format]_bytes : The length of the file in bytes. Allows the Downloader to track how much space is required for the download and to show the download progress for each file.
- [format]_md5 : The MD5 checksum of the file. Allows the Downloader to calculate the MD5 checksum of the file after downloading it and compare it to the original MD5 to verify that the file has been downloaded without errors.

e.g.
- fastq_bytes
- fastq_md5
- submitted_bytes
- submitted_md5
- sra_bytes
- sra_md5

Note: The order of the columns in the report file is insignificant.

## Fetching results and downloading files

### Local Download Directory

In order to download your files you must select a location on your file system which you have write access to. You can either use the "Browse" button to select your download directory or manually enter a path into the text field. If the provided directory does not exist, the Downloader will attempt to create it for you. The total size of the files selected for downloading will be calculated and there will be a local filesystem check to ensure there is enough free space.  If there isn't sufficient space, the download will not begin and an error message will be displayed.

Note: remember that if you are using the report file option and have failed to include they file size columns, this pre-download capacity check will not be performed.

### Remote Files

The available files are separated into three tabs, based on the file type: FASTQ, submitted, SRA. If there are no files available of a given type, the corresponding tab will be disabled.

To download the required files, indidually select the files of interest, or use the "Select All" button. The files will start downloading as soon as you click on "Start Download".  You can stop the download at any time, and if you choose to restart it, it will continue from the point it was paused.

Note: Download only acts on the files selected in the current tab. To download files in a different tab, please wait for the current downloads to complete.

If a file you've selected for downloading already exists in the selected download directory, and if the MD5 information for the file is available the Downloader will compute the MD5 checksum of the existing file and compare it against the target MD5. If the MD5 is verified, the Downloader will mark the file as successfully downloaded and move on, if not it will re-download the file.

# Error Handling

In case of a failed/partial download, the Downloader will attempt to clean up any remnants. It is recommended that
you ensure the file download destination is cleaned of any partial files before you re-attempt downloads.

Should you have any problems please contact us at datasubs@ebi.ac.uk with "FTP Downloader problem" in the subject line.


