# TLDR
Instructions to view the result of Module 4

1. Go to the complete folder
```
cd java-webauthn-passwordless-workshop/4_Authentication/complete
```
2. Build and run the web app
   * Java and Maven
   ```
   mvn clean package spring-boot:run
   ```

   or

   * Docker
   ```
   docker run -p 8443:8443 example/demo:module4
   docker build -t example/demo:module4 .
   ```

3. Open `https://localhost:8443` and sign in with `user` and `password`
4. Register a security key
5. Sign out
6. Click `Passworldless sign in` to sign in without a username or password
