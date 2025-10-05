# GCP Release Parser

This project provides an ETL (Extract, Transform, Load) pipeline to parse Android release artifacts, process them into a structured CSV format, and prepare them for upload to Google BigQuery for analysis.

![](https://user-images.githubusercontent.com/22556115/116011036-a170ad80-a5d7-11eb-9d20-cb38a64b7756.jpg)

## Features

* **Extraction**: Parses various Android file formats to extract data from a target release folder.
  * Handles APEX files and symbolic links.
  * Extracts metadata, features, executables, properties, services, and more.
* **Transformation**: Transforms and stores the extracted data in multiple CSV files.
* **Load**: The generated CSV files can be loaded into data analysis platforms like Google BigQuery.

## Output Files

The parser generates the following CSV files:

* `<Release_Name>-FileList.csv`: Metadata for all target files (size, path, etc.).
* `<Release_Name>-FeatureList.csv`: List of declared system features.
* `<Release_Name>-ExeList.csv`: List of executable files.
* `<Release_Name>-ReleaseContent.csv`: Comprehensive content summary.
* `<Release_Name>-PropertiesList.csv`: System properties from `build.prop` files.
* `<Release_Name>-ServicesList.csv`: List of system services.

## Local Development

Follow these steps to run the parser on your local machine.

### Prerequisites

* Java Development Kit (JDK) 14

### Setup and Execution

1. **Clone the repository:**

    ```bash
    git clone <repository-url>
    cd gcpReleaseParser
    ```

2. **Build the executable JAR:**
    Navigate to the `releaseParserProj` directory and run the Gradle wrapper to build the `uber.jar`.

    ```bash
    cd releaseParserProj
    ./gradlew uberJar
    ```

    The output JAR will be located at `releaseParserProj/build/libs/releaseParser.jar`.

3. **Create an output folder:**

    ```bash
    mkdir output
    ```

4. **Run the parser:**
    Execute the JAR with the appropriate arguments.

    ```bash
    java -jar releaseParserProj/build/libs/releaseParser.jar -i <target-folder> -o <output-folder> -a 28 -f <SubDir1,SubDir2>
    ```

    **Program Arguments:**
    * `-i <target-folder>`: (Required) The path to the directory containing the release files to parse.
    * `-o <output-folder>`: (Required) The path to the folder where CSV output will be saved.
    * `-a <api-level>`: (Required) The Android API level.
    * `-f <subdirectories>`: (Optional) A comma-separated list of specific subdirectories to parse. If not specified, all files and directories in the target folder will be parsed.

## Google Cloud Build

This project is configured for use with Google Cloud Build.

### Build Process

1. **Clone the project** in your Google Cloud Shell environment.
2. **Build a custom JRE Docker image** if needed. This project uses a pre-existing image, but for reference, you can build one using the configurations in the [Google Cloud Build community images](https://github.com/GoogleCloudPlatform/cloud-builders-community) repository.
3. **Submit the build job:**

    ```bash
    gcloud builds submit --config=cloudbuild.yaml
    ```

    Alternatively, you can run a local simulation of the cloud build using the provided script:

    ```bash
    ./testCloudBuildLocal.sh
    ```

4. **Check Results**: The build artifacts, including `releaseParser.jar` and test reports (`index.html`), will be stored in the Google Cloud Storage bucket specified in `cloudbuild.yaml`.

### Helper Scripts

* `releaseParser.sh`: A wrapper script to run the `releaseParser.jar`.
* `test.sh`: An example script demonstrating how to use `releaseParser.sh`.

## Developer Notes

Here are some useful Docker commands for inspecting and interacting with container images in the Google Cloud environment.

* **Inspect an image:**

    ```bash
    docker image inspect <IMAGE_ID>
    ```

* **Run a container in the background:**

    ```bash
    docker run -t -d --rm --name sdk <IMAGE_ID> bash
    ```

* **Execute a command in a running container:**

    ```bash
    docker exec -ti sdk sh -c "<COMMAND>"
    ```

* **Stop a running container:**

    ```bash
    docker stop <CONTAINER_ID>
    ```

* **List running containers:**

    ```bash
    docker ps
    ```

## To-Do / Known Issues

* The key-value pair for `ro.product.cpu.abilist` in `PropertiesList.csv` is not parsed correctly.
* Complete data visualization setup in Google DataStudio.
* Automate testing and uploading processes.
