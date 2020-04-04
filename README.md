[![Build Status](https://travis-ci.org/centic9/file-type-detection.svg)](https://travis-ci.org/centic9/file-type-detection) [![Gradle Status](https://gradleupdate.appspot.com/centic9/file-type-detection/status.svg?branch=master)](https://gradleupdate.appspot.com/centic9/file-type-detection/status)

This is a small tool to use [Apache Tika](http://tika.apache.org) to detect the mime-type of files in a
directory and produce JSON output that can be used for further processing.

The JSON is printed to stdout. Summary/Error information is printed to stderr.
So a typical invocation will redirect stdout to a file via `> file-types.txt`

#### Getting started

##### Grab it

    git clone git://github.com/centic9/file-type-detection
    cd file-type-detection

##### Build it

    ./gradlew check installDist

#### Run it

    build/install/file-type-detection/bin/file-type-detection <directory> > file-types.txt

### How it works

The actual code is quite small, it uses the `DirectoryWalker` from 
[Apache Commons IO](/https://commons.apache.org/proper/commons-io/) to
search the provided directories and invokes a handler for each file that is found.

The handler uses a thread-pool to schedule a `Runnable` to an `Executor` which performs the
detection of the file-type via Apache Tika. 

The async handling allows to scan the file-system in
parallel to the file detection logic.

#### Licensing

   Copyright 2013-2020 Dominik Stadler

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
