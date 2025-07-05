# Google Docs to Markdown CLI

A command-line tool that converts Google Docs documents to valid Markdown.

## Background

This tool was created to simplify the process of converting Google Docs documents to Markdown format for usage in Markdown-based static site generators (e.g. Docusaurus).

Google Docs natively supports exporting to Markdown; however, the output includes invalid header links and heavily
compressed images. The following CLI tool addresses these issues by:

- Converting invalid header links to valid Markdown anchor links
- Replacing compressed, embedded base64 images with the original images, either using Google's content URIs or downloading them to a local directory

## Prerequisites

- **JDK 21 or higher** - Required to build and run the application
- **Google Cloud Project** with the following APIs enabled:
  - [Google Docs API](https://console.cloud.google.com/apis/api/docs.googleapis.com/)
  - [Google Drive API](https://console.cloud.google.com/apis/api/drive.googleapis.com/)

### Authentication Setup

You have two options for authentication:

#### Option 1: Application Default Credentials (Recommended for local development)

1. Install and initialize the [Google Cloud CLI](https://cloud.google.com/sdk/docs/install)
2. Run: `gcloud auth application-default login --scopes=https://www.googleapis.com/auth/drive,https://www.googleapis.com/auth/drive,https://www.googleapis.com/auth/cloud-platform`

#### Option 2: OAuth 2.0 Client ID

1. Follow the [Google Workspace API Quickstart](https://developers.google.com/workspace/drive/api/quickstart/java#set-up-environment) to create OAuth credentials
2. Save the credentials file as `~/.gdocs-md-cli/credentials.json`.
3. On the first run, the CLI will prompt you to authorize the application.

## Getting Started

### Option 1: Using Pre-built Binaries (Recommended)

You can download the latest pre-built binary from [GitHub Releases](https://github.com/benjaminromano/gdocs-md-cli/releases).

```bash
curl -L https://github.com/benjaminRomano/gdocs-md-cli/releases/download/1.0/gdocs-md.jar -o gdocs-md.jar
```

### Option 2: Building from Source

```bash
# Clone the repo
git clone https://github.com/benjaminromano/gdocs-md-cli.git
cd gdocs-md-cli

# Build CLI
./gradlew build

java -jar build/libs/gdocs-md.jar --help
```

## Usage

```bash
$ java -jar gdocs-md.jar --help
Usage: gdocs-md [<options>]

Options:
  -f, --file-id=<text>     Google Docs file ID or URL (e.g. 'https://docs.google.com/document/d/...')
  -o, --output=<path>      Output file path
  -a, --auth=(adc|oauth)   Authentication method
  -d, --download-images    Download images to a local directory
  -i, --images-dir=<path>  Directory to save downloaded images (default: 'images' next to output file)
  -h, --help               Show this message and exit
```

### Convert a Google Doc to Markdown

Convert a Google Doc to Markdown

```bash
# Basic conversion (images will be referenced using Google's content URIs)
$ java -jar build/libs/gdocs-md.jar --file-id=1234567890 --output=output.md

# Convert using a document URL
$ java -jar build/libs/gdocs-md.jar --file-id=https://docs.google.com/document/d/1234567890 --output=output.md

# Convert using OAuth authentication
$ java -jar build/libs/gdocs-md.jar --file-id=1234567890 --output=output.md --auth=oauth

# Download images to an images directory next to the output file
$ java -jar build/libs/gdocs-md.jar --file-id=1234567890 --output=output.md --download-images

# Specify a custom directory for downloaded images
$ java -jar build/libs/gdocs-md.jar --file-id=1234567890 --output=output.md --download-images --images-dir=assets/images
```

### Image Handling

By default, the tool will use Google's image URLs in the generated Markdown. However, you can choose to download images locally:

- `--download-images`: When specified, images will be downloaded to a local directory
- `--images-dir`: (Optional) Specify a custom directory for downloaded images (default: `images` next to the output file)

When using `--download-images`, the tool will:

1. Create the specified images directory (or `images` if not specified)
2. Download all images from the document
3. Update the Markdown to reference the local image files
4. Preserve the original image filenames and extensions

Example output structure when using `--download-images`:

```
output.md
images/
  image1.png
  image2.jpg
```

**Tip**: It's recommended to use an LLM to fix-up the images by using descriptive names and generating alt-text based on the context around the image in the outputted Markdown file.

## Development

### Building the Project

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Running ktlint

This project uses ktlint for code style enforcement. To check the code style:

```bash
./gradlew ktlintCheck
```

To automatically fix style issues:

```bash
./gradlew ktlintFormat
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
