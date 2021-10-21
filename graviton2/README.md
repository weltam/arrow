
# read first

https://arrow.apache.org/docs/developers/contributing.html

especially this one for contributing code

https://arrow.apache.org/docs/developers/contributing.html#local-git-conventions

## add upstream

```sh
git remote add upstream https://github.com/apache/arrow
```

## start branch

```sh
$ git fetch upstream
$ git checkout master
$ git pull --ff-only upstream master
$ git checkout -b $BRANCH
```

## sync local copy daily

```sh
git pull upstream $BRANCH --rebase
```

# install prerequisite

```sh
git clone https://github.com/apache/arrow.git
cd arrow
vcpkg install \
  --x-manifest-root cpp \
  --feature-flags=versions \
  --clean-after-build
```

# create debug for development

```sh
export LC_ALL="en_US.UTF-8"
git clone https://github.com/apache/arrow.git
cd arrow
git submodule update --init --recursive
export ARROW_TEST_DATA=$PWD/testing/data
cd cpp
mkdir debug
cd debug
cmake -DCMAKE_BUILD_TYPE=Debug -DARROW_BUILD_TESTS=ON -GNinja ..
ninja unittest
```

# build java

```sh

```

```xml
            <compilerArgs>
              <arg>--add-modules=java.compiler</arg>
              <arg>-Xlint:all</arg>
              <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED</arg>
              <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED</arg>
              <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED</arg>
              <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED</arg>
              <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED</arg>
              <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED</arg>
              <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED</arg>
              <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED</arg>
              <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED</arg>
              <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED</arg>
              <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED</arg>
            </compilerArgs>
```

# TODO

- [ ] how to scan the pom dependencies to get the latest version of the libraries. i see that it's possible in gradle
- [ ] currently temporarily disable rat-check? why this is breaking? 
- [ ] 