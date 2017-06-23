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

1. Local Download Directory

Select a location on your file system which you have write access to. You can edit this field and manually enter a path as well.
If the entered path does not exist, the Downloader will attempt to create the directory for you.
The Downloader will calculate the total size of the files you select for downloading
and check if the selected file system location has enough free space.

2. Remote Files

The available files are displayed separately by type.
* FASTQ
* Submitted
* SRA

If there are no files available of a given type, the corresponding tab will be disabled.

Use the checkboxes in the Download column to selectively pick the files you want or click Select All.

Click the Start Download button to download the selected files in the currently active tab sequentially.

Note: Start Download only acts on the files selected in the current tab. To download files in a different tab, please
wait for the current downloads to complete.

If a file you've selected for downloading already exists in the selected
download directory, and if the MD5 information for the file is available (either from an accession search or
included in the loaded Report file) the Downloader will compute the MD5 checksum of the existing file
 and compare it against the target MD5. If the MD5 is verified, the Downloader will 
  mark the file as successfully downloaded and move on.

### Requirements for the File Report

1. The file is a plain text file with the contents in Tab Separated values fomat

2. The file must contain the column headers

3. The Report file should contain at least one of the following file
URL columns.

* fastq_ftp
* submitted_ftp
* sra_ftp

4. Additionally, we recommend that when you generate the report file you include the support columns for whichever file
type/s you want to download.

* *_bytes : The length of the file in bytes. Allows the Downloader to track how much space is required for the download and to show the download
progress for each file.

* *_md5 : The MD5 checksum of the file. Allows the Downloader to calculate the MD5 checksum of the file after downloading it and compare it to the original MD5 to verify that the file has been downloaded without errors.

e.g.

* fastq_bytes
* fastq_md5
* submitted_bytes
* submitted_md5
* sra_bytes
* sra_md5

5. The order of the columns in the report file is insignificant.

# Error Handling
1. In case of a failed/partial download, the Downloader will attempt to clean up any remnants. It is recommended that
you ensure the file download destination is cleaned of any partial files before you re-attempt downloads.


