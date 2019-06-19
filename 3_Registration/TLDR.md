# TLDR
Instructions to view the result of Module 3

1. Go to the complete folder
```
cd java-webauthn-passwordless-workshop/3_Registration/complete
```
2. Build and run the web app
   * Java and Maven
   ```
   mvn clean package spring-boot:run
   ```

   or

   * Docker
   ```
   docker build -t example/demo:module3 .
   docker run -p 8443:8443 example/demo:module3
   ```

3. Open `https://localhost:8443` and sign in with `user` and `password`
4. Register a security key
