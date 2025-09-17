# WatchGuide

*WatchGuide* is an Android app for anime fans to search, filter, and manage their anime library. Users can follow friends, track activities, and customize themes. The app integrates Firebase for authentication and Firestore for real-time data storage, while Retrofit is used to fetch anime data from the [Jikan API](https://jikan.moe/).

---

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Key Classes](#key-classes)
- [Dependencies](#dependencies)
- [Contributing](#contributing)
- [License](#license)

---

## Features

- **Anime Search & Filter**: Search by title or filter by genre.
- **User Library**: Save favorite anime, mark as watched, and rate series.
- **Activity Feed**: See your own activity and that of followed users.
- **Friends System**: Search for users, follow/unfollow friends.
- **Profile Management**: Update username and profile picture.
- **Custom Themes**: Multiple anime-themed visual styles (Naruto, One Piece, Bleach, etc.).
- **Interactive Tutorial**: Guided walkthrough for first-time users using TapTargetView.
- **Firebase Integration**: Authentication, Firestore, and AppCheck security.
- **Retrofit API Calls**: Fetch anime data using Jikan API.

---

## Installation

1. Clone the repository:  
```bash
git clone https://github.com/yourusername/watchguide.git
```
2. Open in Android Studio.

3. Add Firebase configuration:

- Place google-services.json in app/.

- Enable Firebase Authentication (Google Sign-In).

- Enable Firestore Database.

4. Ensure Gradle dependencies include:
```bash
implementation 'com.google.firebase:firebase-auth'
implementation 'com.google.firebase:firebase-firestore'
implementation 'com.google.firebase:firebase-appcheck-playintegrity'
implementation 'com.squareup.retrofit2:retrofit'
implementation 'com.squareup.retrofit2:converter-gson'
implementation 'com.github.bumptech.glide:glide'
implementation 'com.google.android.material:material'
implementation 'androidx.recyclerview:recyclerview'
implementation 'androidx.constraintlayout:constraintlayout'
implementation 'androidx.appcompat:appcompat'
implementation 'com.getkeepsafe.taptargetview:taptargetview:1.13.0'
implementation 'androidx.activity:activity-ktx:1.7.2'

```
5. Run on an Android device or emulator (min SDK 21).

## Usage
- Sign in with Google.

- Search for anime or filter by genre.

- Add anime to your library, mark as watched, or favorite them.

- Navigate between Library, Feed, and Friends using the bottom navigation bar.

- Access your profile or change themes via the side navigation drawer.

- First-time users will see an interactive tutorial guiding them through key features.

## Key Classes

| Class | Description |
|-------|-------------|
| MainActivity | Central activity managing search, filters, feed, friends, and themes. |
| AnimeAdapter | Displays anime search results in RecyclerView. |
| FeedAdapter | Displays activity feed of user and followed friends. |
| UserAdapter | Handles user list in Friends dialog. |
| FirestoreUserLibrary | Manages Firestore library operations (add/remove anime, mark watched/favorite). |
| FirestoreFollowManager | Handles following system and listens to friends’ activities. |
| AnimeApi | Retrofit interface for interacting with Jikan API. |
| ProfileActivity | Displays user profile and library history. |
| TapTargetView | Guides first-time users with an interactive tutorial. |


## Dependencies
- Firebase Authentication & Firestore

- Retrofit 2 + Gson converter

- Glide (image loading)

- Material Components for Android

- TapTargetView (interactive tutorial)

## Contributing
- Fork the repository.

- Create a branch: git checkout -b feature/YourFeature.

- Commit your changes: git commit -m "Add new feature".

- Push to the branch: git push origin feature/YourFeature.

- Open a pull request describing your changes.

## License
