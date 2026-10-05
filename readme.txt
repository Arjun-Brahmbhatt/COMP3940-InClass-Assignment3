COMP 3940 - In-Class Assignment 3
Arjun Brahmbhatt
A01474991

COMPILING AND RUNNING THE UPLOAD SERVER

1. Open Command Prompt.

2. Navigate to the UploadServer folder.

3. Compile the Java files:

   javac *.java

4. Run the server:

   java -classpath . UploadServer

5. Open a web browser and go to:

   http://localhost:8082/

6. The upload form can be used to enter a caption and date and select a file to upload.


COMPILING AND RUNNING THE CONSOLE APP

1. Keep the UploadServer running.

2. Open another Command Prompt.

3. Navigate to the ConsoleApp folder.

4. Compile the Java files:

   javac *.java

5. Run the ConsoleApp:

   java Activity

6. The ConsoleApp sends a multipart HTTP POST request to the UploadServer and displays the server response.