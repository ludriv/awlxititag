# Introduction #

This page explains how to use AWLXitiTag library.


# Details #

  * Download and unzip the package into your project src directory.
  * Add the following uses-permissions to your `AndroidManifest.xml` project file :
    * `android.permission.INTERNET`
    * `android.permission.ACCESS_WIFI_STATE`
  * Use the `XitiTag.init()` method at the begining of your application life cycle. For example, in the `onCreate()` method of your first Activity
  * Use the `XitiTag.tagPage()` method to tag a page
  * Use the `XitiTag.tagAction()` method to tag an action
  * Use the `XitiTag.terminate()` method et the end of your application life cycle. For example, in the `onDestroy()` method of your first Activity.