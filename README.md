# Messenger

Welcome to Messenger. An Android messaging app built using:
- Firebase Cloud Messaging
- Firebase Phone Auth
- Firebase Firestore
- Firebase Storage
- MVVM architecture
- StateFlow
- Dagger Hilt
- Retrofit 2
- Coroutines
- Room Persistence Library
- ViewBinding
- Kotlin

Note: This project is still in alpha.

### Contribute
We use similar contributing conventions as [DuckDuckGo](https://github.com/duckduckgo/Android).
See [CONTRIBUTING.md](CONTRIBUTING.md).

## Installation
1. Create a Firebase Project from your Firebase Console.
1. Follow the setup, but do not add any dependencies. Only add the `google-services.json` to `./app/<paste google-services.json>`.
1. Go to the Authentication -> Sign-in method -> Phone. Make sure to follow the setup documentation but do not add any dependencies to your project.
1. Make sure to enable Android Device Verification in your Google Cloud Console, as listed in the documentation. PS: Do not forget to add your SHA-256 and SHA-1 to your project in the Project Settings section.
1. You can find the Phone Auth Documentation [here](https://firebase.google.com/docs/auth/android/phone-auth#enable-app-verification).
1. You can find how to get your debug SHA using this [link](https://developers.google.com/android/guides/client-auth#using_keytool).
1. Once you retrieve your SHA fingerprints, you can add them in: Project Settings, and scroll to the bottom of the page, under `Package name` you can find `SHA certificate fingerprints` section where you can add your SHA fingerprints.
1. After you have completed the setup, head to Project Settings -> Cloud Messaging and copy your Server Key.
1. Now go to your project directory, inside the `./app` folder, create a new file and name it `app.properties`.
1. Inside `./app/app.properties` add the following ```SERVER_KEY=<paste your server key>```
1. Now go to `Build` menu and click on `Rebuild Project`. This will generate the necessary environment variables and will remove any errors that refers to `BuildConfig.SERVER_KEY`
