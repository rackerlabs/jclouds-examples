# blobstore-clojure

Basic blobstore usage example including creating a container and then listing all containers in your account.

## Build

Ensure you have [Leiningen](http://github.com/technomancy/leiningen) installed, then execute 'lein deps' to grab the jclouds dependencies.

## Run

    $ lein repl
    user=> (use 'create-list.containers)
    user=> (create-and-list "cloudfiles-us" "RACKSPACE_USERNAME" "RACKSPACE_API_KEY" "mycontainer")
    user=> (create-and-list "aws-s3" "AWS_ACCESS_KEY" "AWS_SECRET_ACCESS_KEY" "mybucket")

## License

Copyright (C) 2009-2014 The Apache Software Foundation

Licensed under the Apache License, Version 2.0

