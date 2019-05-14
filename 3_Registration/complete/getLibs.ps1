# Create tmp dir
rm -rf tmp
mkdir tmp
cd tmp

# Get WebAuthn Demo Code
git clone https://github.com/Yubico/java-webauthn-server.git

# Copy the webauthn-server-demo data package to the project
cp -r java-webauthn-server/webauthn-server-demo/src/main/java/demo/webauthn/data ../src/main/java/com/example/demo

# Copy the webauthn-server-demo yubico package to the project
cp -r java-webauthn-server/webauthn-server-demo/src/main/java/com/yubico ../src/main/java/com/

# Copy the AuthenticatedAction, Config, InMemoryRegistrationStorage, and RegistrationStorage classes to the project
cp java-webauthn-server/webauthn-server-demo/src/main/java/demo/webauthn/AuthenticatedAction.java ../src/main/java/com/example/demo
cp java-webauthn-server/webauthn-server-demo/src/main/java/demo/webauthn/Config.java ../src/main/java/com/example/demo
cp java-webauthn-server/webauthn-server-demo/src/main/java/demo/webauthn/InMemoryRegistrationStorage.java ../src/main/java/com/example/demo
cp java-webauthn-server/webauthn-server-demo/src/main/java/demo/webauthn/RegistrationStorage.java ../src/main/java/com/example/demo
cp java-webauthn-server/webauthn-server-demo/src/main/java/demo/webauthn/WebAuthnServer.java ../src/main/java/com/example/demo

# Copy the preview-metadata.json and logback.xml to the project
cp java-webauthn-server/webauthn-server-demo/src/main/resources/preview-metadata.json ../src/main/resources
cp java-webauthn-server/webauthn-server-demo/src/main/resources/logback.xml ../src/main/resources

#Copy the lib and js folders to the project
mkdir -p ../src/main/resources/static
cp -r java-webauthn-server/webauthn-server-demo/src/main/webapp/lib ../src/main/resources/static/
cp -r java-webauthn-server/webauthn-server-demo/src/main/webapp/js ../src/main/resources/static/

#Fix package names. Every file that was copied over has the incorrect package name. For each file replace 'demo.webauthn' with 'com.example.demo'. 
cd ../src

$javaFiles = Get-ChildItem  *.java -rec
foreach ($file in $javaFiles)
{
    (Get-Content $file.PSPath) |
    Foreach-Object { $_ -replace "demo.webauthn", "com.example.demo" } |
    Set-Content $file.PSPath
}

cd ..
