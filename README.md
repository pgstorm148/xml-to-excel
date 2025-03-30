# xml-to-excel
Java plugin for extracting XML data from Actimize RCM and ActOne alerts to Excel format. This plugin will take an alert ID as input and generate a downloadable Excel file from the XML content.

How to Use This Plugin

Setup: Install the plugin in your Actimize environment by adding the Java files to your project and configuring them in your Spring Boot application.
Dependencies: Make sure you have the following dependencies:

Spring Boot
Apache POI (for Excel operations)
DOM XML parser
Actimize RCM and ActOne API clients (these are referenced in the code and would need to be implemented or provided by your Actimize environment)


Usage Flow:

User enters an alert ID in the web interface
The backend fetches the XML data for that alert from either RCM or ActOne
The XML is parsed and converted to Excel format with multiple sheets:

Alert Details: Contains general alert information
Transactions: Lists all transactions related to the alert
Entities: Lists all entities (customers, accounts, etc.) related to the alert


The Excel file is generated and made available for download


Configuration: You might need to adjust the API paths and XML parsing logic based on your specific Actimize implementation and XML structure.

This implementation supports both RCM and ActOne alert formats, and automatically adapts to differences in XML structure by searching for common element names.

The plugin consists of three main files:

1. XmlToExcelExtractorPlugin.java
This is the main plugin class containing the extraction logic
Contains the API endpoint for extracting XML and converting to Excel
Has methods for parsing different sections of the XML data


2. FileDownloadController.java
Handles the file download functionality
Contains the endpoint that allows users to download the generated Excel file


3. aml-xml-extractor.html
Contains the frontend user interface
Simple form for entering alert IDs
JavaScript for handling the API calls



When setting up the plugin in your Actimize environment, you'll need to place these files in the appropriate directories according to your project structure, ensuring they follow the package path com.actimize.plugins.xml as specified in the Java files.
