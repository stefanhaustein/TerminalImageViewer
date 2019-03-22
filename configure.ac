dnl require autoconf 2.60 (AS_ECHO/AS_ECHO_N)
AC_PREREQ([2.60])

AC_CONFIG_MACRO_DIR([m4])

AC_INIT(tiv, 1.0)
AM_INIT_AUTOMAKE([-Wall -Werror foreign])

dnl Switch to a C++ compiler, and check if it works
AC_LANG(C++)
AC_PROG_CXX([g++-8 g++])

AM_CONFIG_HEADER(config.h)

# Indicate this is a release build and that
# dependancies for changes between Makefile.am and Makefile.in
# should not be checked. This makes compiling faster. If you are working
# on the library, run: ./configure --enable-maintainer-mode to enable the dependancies
dnl AM_MAINTAINER_MODE

AC_CANONICAL_HOST

dnl Require C++17 compiler
AX_CXX_COMPILE_STDCXX([11], [noext], [mandatory], [nodefault])

TIV_CPPFLAGS=" -Wall -fpermissive -fexceptions -O2"
TIV_LDFLAGS=" -pthread"
TIV_LIBS+=" -lstdc++fs"

AC_SUBST(TIV_CPPFLAGS)
AC_SUBST(TIV_LDFLAGS)
AC_SUBST(TIV_LIBS)

dnl AC_SEARCH_LIBS([libstdc++fs],, AC_MSG_ERROR([required lib file missing]))

AC_CONFIG_FILES([
 Makefile
 src/main/cpp/Makefile
])
dnl Process Makefile.in to create Makefile
AC_OUTPUT
