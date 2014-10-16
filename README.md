# Adding Walkntrade app to Android Studio:

*If you already have Android Studio installed: Skip to **step 2***

###1. Download and install the latest version of Android Studio and the Android SDK from [Android Developer page](developer.android.com). Follow the instructions from there

###2. Open Android Studio, exit out of any current projects

###3. **(Option 1 - Recommended)** Select Check out from Version Control --> Git, and enter SSH url of repo, and clone it into your computer
- Uncheck remember password if you do not want to create a master password
- Continue to import from Gradle

###3. **(Option 2 )** Create git repo from a directory of your choice. Clone or pull the contents using the SSH url.
- Return to Android Studio and select import project
- Go to the directory of the project, in the root folder select the 'build.gradle' file

###4. Follow messages in the terminal at the bottom. Install any missing platforms, repositories, or dependencies. And wait for any
and builds that must be performed.

###5. Go to Build -> Rebuild Project, to re-compile and check for any errors