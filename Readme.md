## Overview

A clojure(Script) full stack todo app. In this todo app you can have many users with many lists, upon supplying a deadline if it is reached, will supply an email to the specified gmail account using the smtp.gmail.com server. You can modify the contents of a list, rename it, delete it, and modify its deadline.
You can unsubscribe a user, or if you change your mind you have 2 chances of going back, also logout, and register. Note: Users and user data is persisted to file thanks to the fact that this app uses next.jdbc, specifically an sqlite database, however note that currently the alerts have to be re-triggered by modifying the deadline since this are on the backend a collection of running threads which check 2 things, this can either be if the thread is interrupted or if the deadline has been reached, they also send the mail when it is reached.



### Tests

This app has actually been tested manually by modifying the .cljs file with shadow-cljs hot reloads and reloading index.clj on the repl after adding new functionality, however some small tests could be automated. Simply run clj -T:test on the command line.


### js

You can recompile the .cljs files into .js ones simply by running npx shadow-cljs release frontend on the command line. Note that the unsubscribe.js file is written in javascript and has not been compiled into one.


### Uber


To build an uber just run clj -T:build uber, this will temporarily override the javax.net.ssl.X509TrustManager class to trust all certs, this is to avoid the infamous 'Unable to find valid certification path to the requested target', upon initiating an ssl connection with the smtp.gmail.com server on port 465, will get the session certs and persist them to a custom keystore generated at resources/public/store on this project, this file should contain 2 certificates, this are enough for connecting normally later when the app has to send a mail, else is simply a normal build.clj file.


### Run

To run this app you must have an app password enabled in your gmail account, for this you can refer to https://www.getmailbird.com/gmail-app-password/.
Set 2 environment variables on the command line before running 'java -jar TodoApp.jar':

* My_USERNAME: this one holds your email as a string.

* MY_APPPASSWORD: your app password also as a string.

This is because you are required to authenticate before sending an email through gmails servers. Once the app is running just visit localhost:8080 on your browser. When typing an email to send the reminder the email can be a different one than the one you use to authenticate. Gmail lets you send 500 mails per day without cost.



This is for demonstrative purposes.