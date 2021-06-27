# What does it do?
- Parse and process release files into CSV format, then store to BigQuerry for later use.
![](https://user-images.githubusercontent.com/22556115/116011036-a170ad80-a5d7-11eb-9d20-cb38a64b7756.jpg)

Modeled after ETL pipeline, this project handels everything from extraction to load

* Extraction
	* Release Parser extracts wanted data from target folder
		* <Release_Name>-FileList.csv, meta data for all target files, size, etc.
		* <Release_Name>-FeatureList.csv
		* <Release_Name>-ExeList.csv
		* <Release_Name>-ReleaseContent.csv
		* <Release_Name>-PropertiesList.csv
		* <Release_Name>-ServicesList
	* Added in function to extract content from APEX files, as well as parse symbolic links
* Transformation
	* The extracted data is written and stored as CSV files
* Load
	* Use the script inside uploadScript

# Setup
1. Clone and open the project, run uberjar
2. Edit config, point path to jar to output uberjar
3. Create a output folder
4. program args -i <target folder> -o <output folder> -a 28 -f <SubDir1, SubDir2>
	* -f allows you to specify what are the sub dir to be parsed
	* If not specified, all files and dir in the folder will be parsed 
5. Set JRE to jdk-14
6. Run and check output folder for result

# Google Cloud Build
## Build in Google Cloud Consoule
This peoject can be adapted for Google Cloud Build with the following steps:
* Clone this project in Google Cloud Shell
* Create a new JRE Docker image
	* Clone [Google Cloud Build community images](https://github.com/Alwin-Lin/cloud-builders-community) 
	* ``` gcloud builds submit --config=cloudbuild-ndk-jre11.yaml --substitutions=_ANDROID_VERSION=29``` 
	* Builds image with cloudbuild-ndk-jre11.yaml
* Pull the built image to local 
* Setup buckets
* Build ```gcloud builds submit --config=cloudbuild-ndk-jre11.yaml --substitutions=_ANDROID_VERSION=29```
	* ```./testCloudBuildLocal.sh``` does the same thing.
### Checking result(s)
Results are stored inside bucket gs://* your_bucket_ID *, this includes releaseParser.jar and index.html.
### test.sh and releaseParser.sh 
releaseParser.sh helps you run releaseParser
test.sh is an example of releaseParser.sh
## interacting with images in GCP
Here are some command lines that can be used for testing docker image
* docker image inspect <ID>
	* Inspects the image
	* Used for chekcing if the paths inside are setup correctly
* docker run -t -d --rm --name sdk <ID> bash
	* Runs <ID> docker immage
* docker exec -ti sdk sh -c "<COMMAND_PROMP>"
	* Executes <COMMAND_PROMP> on the running docker image
* docker stop <CONTAINER_ID>
	* Stops the running docker
* docker ps
	* Checks the running docker status 

# Notes and ToDo:
- The key value pair of ro.product.cpu.abilist,arm64-v8a,armeabi-v7a,armeabi inside PropertiesList can not be uploaded properly
- Finish data visualization at DataStudio
- Automate testing and uploading
	- 
