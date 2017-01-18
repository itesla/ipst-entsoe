[![Build Status](https://travis-ci.org/itesla/ipst-entsoe.svg?branch=master)](https://travis-ci.org/itesla/ipst-entsoe)
[![Coverage Status](https://coveralls.io/repos/github/itesla/ipst-entsoe/badge.svg?branch=master)](https://coveralls.io/github/itesla/ipst-entsoe?branch=master)
[![Dependency Status](https://www.versioneye.com/user/projects/587f65d5452b83003d3c8fa4/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/587f65d5452b83003d3c8fa4)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)

# iTESLA
http://www.itesla-project.eu/

http://www.itesla-pst.org

## Environment requirements
In order to build iPST you need:
  * JDK *(1.8 or greater)*
  * Maven 
  * [ipst-core](https://github.com/itesla/ipst-core)

## Install
To easily compile iPST, you can use the toolchain:
```
$> git clone https://github.com/itesla/ipst-entsoe.git
$> ./install.sh
```
By default, the toolchain will:
  * compile Java modules
  * install iPST

### Targets

| Target | Description |
| ------ | ----------- |
| clean | Clean iPST modules |
| compile | Compile iPST modules |
| package | Compile iPST modules and create a distributable package |
| __install__ | __Compile iPST modules and install it__ |
| docs | Generate the documentation (Javadoc) |
| help | Display this help |

### Options

The toolchain options are saved in the *install.cfg* configuration file. This configuration file is loaded and updated
each time you use the toolchain.

#### iPST

| Option | Description | Default value |
| ------ | ----------- | ------------- |
| --help | Display this help | |
| --prefix | Set the installation directory | $HOME/itesla |
| --package-type | Set the package format. The supported formats are zip, tar, tar.gz and tar.bz2 | zip |

### Default configuration file
```
ipst_prefix=$HOME/itesla
ipst_package_type=zip
```

## License
https://www.mozilla.org/en-US/MPL/2.0/

