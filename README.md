![](https://user-images.githubusercontent.com/22556115/115168581-57844680-a070-11eb-87d4-3453441d88fe.jpg)
# User manual
1. Clone the project, run uberjar
2. Edit config, point path to jar to output uberjar
3. Create a output folder
4. program args -i <target folder> -o <output folder> -a 28
5. Set JRE to jdk-14
6. Run and check output folder for result

## Additional outputs
In addition to the default output found on [release parser](https://android.googlesource.com/platform/cts/+/refs/heads/master/tools/release-parser/), the following is added
* ServicesList
* PropertiesList
* PermissionList
* FileList

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
